package com.yoloseg.edgeai.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.DataType
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * A single detection result from the YOLOv8 model.
 */
data class DetectionResult(
    val classId: Int,
    val className: String,
    val confidence: Float,
    val boundingBox: RectF
)

class YoloObjectDetector(
    context: Context,
    modelPath: String = "yolov8_det_int8.tflite",
    useGpu: Boolean = false,
    useNnapi: Boolean = false,
    private val numClasses: Int = 4,
    private val classNames: Array<String> = arrayOf("Helmet", "No Helmet", "Ladder", "Worker")
) : AutoCloseable {

    companion object {
        const val INPUT_SIZE = 640
        const val NUM_ANCHORS = 8400

        const val CONFIDENCE_THRESHOLD = 0.5f
        const val IOU_THRESHOLD = 0.45f

        // Keep a default for backward compatibility
        val CLASS_NAMES = arrayOf("Helmet", "No Helmet", "Ladder", "Worker")
    }

    private val valuesPerAnchor = 4 + numClasses

    private val interpreter: Interpreter
    private val gpuDelegate: GpuDelegate?
    private val nnapiDelegate: NnApiDelegate?
    private val lock = Any()
    
    var lastMaxScore: Float = 0f

    private val inputBuffer: ByteBuffer

    private var inputScale: Float = 1f
    private var inputZeroPoint: Int = 0
    private var output0Scale: Float = 1f
    private var output0ZeroPoint: Int = 0

    private var isInputQuantized: Boolean = false
    private var isOutput0Quantized: Boolean = false
    private var isInputNCHW: Boolean = false

    private var inputDataType: DataType = DataType.FLOAT32
    private var output0DataType: DataType = DataType.FLOAT32

    init {
        val modelBuffer = loadModelFile(context, modelPath)
        val probeInterpreter = Interpreter(modelBuffer, Interpreter.Options())
        
        val inputDetail = probeInterpreter.getInputTensor(0)
        inputDataType = inputDetail.dataType()
        isInputQuantized = (inputDataType == DataType.INT8 || inputDataType == DataType.UINT8)
        if (isInputQuantized) {
            val quant = inputDetail.quantizationParams()
            inputScale = quant.scale
            inputZeroPoint = quant.zeroPoint.toInt()
        }

        // Detect NCHW vs NHWC from input tensor shape
        // NCHW: [1, 3, 640, 640]  —  channels-first (planar)
        // NHWC: [1, 640, 640, 3]  —  channels-last (interleaved)
        val inputShape = inputDetail.shape()
        isInputNCHW = inputShape.size == 4 && inputShape[1] == 3 && inputShape[2] == INPUT_SIZE

        val out0Tensor = probeInterpreter.getOutputTensor(0)
        output0DataType = out0Tensor.dataType()
        isOutput0Quantized = (output0DataType == DataType.INT8 || output0DataType == DataType.UINT8)
        if (isOutput0Quantized) {
            val quant = out0Tensor.quantizationParams()
            output0Scale = quant.scale
            output0ZeroPoint = quant.zeroPoint.toInt()
        }
        
        probeInterpreter.close()

        val effectiveUseGpu = useGpu && !isInputQuantized

        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }

        var gpuDel: GpuDelegate? = null
        var nnapiDel: NnApiDelegate? = null

        if (effectiveUseGpu) {
            try {
                gpuDel = GpuDelegate()
                options.addDelegate(gpuDel)
            } catch (e: Exception) {
                gpuDel = null
            }
        }

        if (useNnapi && gpuDel == null) {
            try {
                nnapiDel = NnApiDelegate()
                options.addDelegate(nnapiDel)
            } catch (e: Exception) {
                nnapiDel = null
            }
        }

        gpuDelegate = gpuDel
        nnapiDelegate = nnapiDel
        interpreter = Interpreter(modelBuffer, options)

        val inputTensor = interpreter.getInputTensor(0)
        val inputBytes = inputTensor.numBytes()
        inputBuffer = ByteBuffer.allocateDirect(inputBytes).apply {
            order(ByteOrder.nativeOrder())
        }
    }

    val modelDetails: String
        get() {
            val inputShape = interpreter.getInputTensor(0).shape().contentToString()
            val out0Shape = interpreter.getOutputTensor(0).shape().contentToString()
            return "Input: $inputDataType, shape=$inputShape (scale=$inputScale, zp=$inputZeroPoint)\n" +
                   "Out0: $output0DataType, shape=$out0Shape (scale=$output0Scale, zp=$output0ZeroPoint)"
        }

    fun detect(bitmap: Bitmap): List<DetectionResult> = synchronized(lock) {
        val resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        fillInputBuffer(resized)
        if (resized != bitmap) resized.recycle()

        val output0Tensor = interpreter.getOutputTensor(0)
        val output0Shape = output0Tensor.shape()

        val isTransposed = output0Shape[1] == valuesPerAnchor && output0Shape[2] == NUM_ANCHORS

        val output0: Any

        if (isOutput0Quantized) {
            output0 = Array(output0Shape[0]) { Array(output0Shape[1]) { ByteArray(output0Shape[2]) } }
        } else {
            output0 = Array(output0Shape[0]) { Array(output0Shape[1]) { FloatArray(output0Shape[2]) } }
        }

        val outputMap = mapOf(0 to output0)

        interpreter.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputMap)

        val detections = parseDetections(output0, isTransposed)
        val nmsResults = applyNms(detections)

        return@synchronized nmsResults.map { det ->
            DetectionResult(
                classId = det.classId,
                className = classNames.getOrElse(det.classId) { "Unknown" },
                confidence = det.confidence,
                boundingBox = det.box
            )
        }.sortedByDescending { it.confidence }
    }

    private fun fillInputBuffer(bitmap: Bitmap) {
        inputBuffer.rewind()
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        if (isInputNCHW) {
            // ── NCHW (planar): write ALL red, then ALL green, then ALL blue ──
            if (isInputQuantized) {
                val minQ = if (inputDataType == DataType.UINT8) 0 else -128
                val maxQ = if (inputDataType == DataType.UINT8) 255 else 127
                for (pixel in pixels) inputBuffer.put((((pixel shr 16 and 0xFF) / 255.0f / inputScale) + inputZeroPoint).toInt().coerceIn(minQ, maxQ).toByte())
                for (pixel in pixels) inputBuffer.put((((pixel shr  8 and 0xFF) / 255.0f / inputScale) + inputZeroPoint).toInt().coerceIn(minQ, maxQ).toByte())
                for (pixel in pixels) inputBuffer.put((((pixel         and 0xFF) / 255.0f / inputScale) + inputZeroPoint).toInt().coerceIn(minQ, maxQ).toByte())
            } else {
                for (pixel in pixels) inputBuffer.putFloat((pixel shr 16 and 0xFF) / 255.0f)
                for (pixel in pixels) inputBuffer.putFloat((pixel shr  8 and 0xFF) / 255.0f)
                for (pixel in pixels) inputBuffer.putFloat((pixel         and 0xFF) / 255.0f)
            }
        } else {
            // ── NHWC (interleaved): write R,G,B per pixel ──
            for (pixel in pixels) {
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                if (isInputQuantized) {
                    val minQ = if (inputDataType == DataType.UINT8) 0 else -128
                    val maxQ = if (inputDataType == DataType.UINT8) 255 else 127
                    inputBuffer.put(((r / 255.0f / inputScale) + inputZeroPoint).toInt().coerceIn(minQ, maxQ).toByte())
                    inputBuffer.put(((g / 255.0f / inputScale) + inputZeroPoint).toInt().coerceIn(minQ, maxQ).toByte())
                    inputBuffer.put(((b / 255.0f / inputScale) + inputZeroPoint).toInt().coerceIn(minQ, maxQ).toByte())
                } else {
                    inputBuffer.putFloat(r / 255.0f)
                    inputBuffer.putFloat(g / 255.0f)
                    inputBuffer.putFloat(b / 255.0f)
                }
            }
        }
    }

    private data class RawDetection(
        val classId: Int,
        val confidence: Float,
        val box: RectF
    )

    private fun parseDetections(output0: Any, isTransposed: Boolean): List<RawDetection> {
        val detections = mutableListOf<RawDetection>()
        var maxRawScore = -9999f
        var maxSigScore = -9999f
        var maxScoreClass = 0

        for (a in 0 until NUM_ANCHORS) {
            val values = FloatArray(valuesPerAnchor)

            for (v in 0 until valuesPerAnchor) {
                val rawVal: Float = if (isOutput0Quantized) {
                    @Suppress("UNCHECKED_CAST")
                    val arr = output0 as Array<Array<ByteArray>>
                    val byteVal = if (isTransposed) arr[0][v][a] else arr[0][a][v]
                    val intVal = if (output0DataType == DataType.UINT8) {
                        byteVal.toInt() and 0xFF
                    } else {
                        byteVal.toInt()
                    }
                    (intVal - output0ZeroPoint) * output0Scale
                } else {
                    @Suppress("UNCHECKED_CAST")
                    val arr = output0 as Array<Array<FloatArray>>
                    if (isTransposed) arr[0][v][a] else arr[0][a][v]
                }
                values[v] = rawVal
            }

            val cx = values[0]
            val cy = values[1]
            val w = values[2]
            val h = values[3]

            var bestClassId = 0
            var bestRawScore = values[4]
            for (c in 1 until numClasses) {
                if (values[4 + c] > bestRawScore) {
                    bestRawScore = values[4 + c]
                    bestClassId = c
                }
            }

            // Apply sigmoid activation to convert raw logit → probability [0, 1] if not already baked in
            // For YOLOv8 INT8 converted models from PyTorch, we sometimes need it depending on the export tool
            // But we will leave it as bestRawScore for now since it usually is baked in.
            // If predictions are zero, we may need to add sigmoid back.
            val bestScore = bestRawScore

            if (bestRawScore > maxRawScore) {
                maxRawScore = bestRawScore
                maxSigScore = bestScore
                maxScoreClass = bestClassId
            }

            if (bestScore < com.yoloseg.edgeai.ui.theme.VisionThemeManager.detectionThreshold) continue

            val x1 = (cx - w / 2f) / INPUT_SIZE
            val y1 = (cy - h / 2f) / INPUT_SIZE
            val x2 = (cx + w / 2f) / INPUT_SIZE
            val y2 = (cy + h / 2f) / INPUT_SIZE

            val box = RectF(
                max(0f, x1), max(0f, y1),
                min(1f, x2), min(1f, y2)
            )

            detections.add(RawDetection(bestClassId, bestScore, box))
        }

        lastMaxScore = maxSigScore
        return detections
    }

    private fun applyNms(detections: List<RawDetection>): List<RawDetection> {
        if (detections.isEmpty()) return emptyList()

        val sorted = detections.sortedByDescending { it.confidence }
        val selected = mutableListOf<RawDetection>()
        val suppressed = BooleanArray(sorted.size)

        for (i in sorted.indices) {
            if (suppressed[i]) continue
            selected.add(sorted[i])

            for (j in i + 1 until sorted.size) {
                if (suppressed[j]) continue
                if (sorted[i].classId != sorted[j].classId) continue

                val iou = computeIou(sorted[i].box, sorted[j].box)
                if (iou > IOU_THRESHOLD) {
                    suppressed[j] = true
                }
            }
        }

        return selected
    }

    private fun computeIou(a: RectF, b: RectF): Float {
        val interLeft = max(a.left, b.left)
        val interTop = max(a.top, b.top)
        val interRight = min(a.right, b.right)
        val interBottom = min(a.bottom, b.bottom)

        val interArea = max(0f, interRight - interLeft) * max(0f, interBottom - interTop)
        val aArea = (a.right - a.left) * (a.bottom - a.top)
        val bArea = (b.right - b.left) * (b.bottom - b.top)
        val unionArea = aArea + bArea - interArea

        return if (unionArea > 0f) interArea / unionArea else 0f
    }

    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    override fun close() {
        synchronized(lock) {
            interpreter.close()
            gpuDelegate?.close()
            nnapiDelegate?.close()
        }
    }
}
