/**
 * ═══════════════════════════════════════════════════════════════════════════════
 *  LiveCameraActivity.kt
 *  CameraX Real-Time Inference Pipeline with 3-FPS Throttle
 *  Vision AI Glassmorphic HUD — No vignette, animated detection sheet
 * ═══════════════════════════════════════════════════════════════════════════════
 */

package com.yoloseg.edgeai.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.yoloseg.edgeai.ui.OverlayView
import com.yoloseg.edgeai.ui.theme.*
import kotlinx.coroutines.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.cos
import kotlin.math.sin

class LiveCameraActivity : ComponentActivity() {

    companion object {
        private const val TAG = "LiveCameraActivity"
        private const val INFERENCE_INTERVAL_MS = 333L
        private const val VERIFICATION_DURATION_MS = 2000L
        private const val VERIFICATION_MAX_GAP_MS = 500L
        private const val MIN_CONSECUTIVE_FRAMES = 6
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA

        private val NOTIFY_CLASSES get() = com.yoloseg.edgeai.ui.theme.VisionThemeManager.classNames.filter { it != "No Helmet" && it != "Without Helmet" }.toSet()
    }

    private lateinit var detector: com.yoloseg.edgeai.ml.YoloObjectDetector
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null

    private var lastInferenceTimestamp = 0L
    private var frameCount = 0
    private var fpsTimestamp = 0L

    private val detectionTrackers = mutableMapOf<String, DetectionTracker>()
    private val verifiedClasses = mutableSetOf<String>()

    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var toneGenerator: ToneGenerator? = null

    // Compose States
    private var lensFacingState = mutableIntStateOf(CameraSelector.LENS_FACING_BACK)
    private var fpsTextState = mutableStateOf("FPS: 0")
    private var detectionCountState = mutableStateOf("Detections: 0")
    private var isVerificationBannerVisible = mutableStateOf(false)
    private var lastVerifiedClassName = mutableStateOf("")
    private var lastVerifiedConfidence = mutableStateOf(0f)
    private var activeDetectionsState = mutableStateOf<List<com.yoloseg.edgeai.ml.DetectionResult>>(emptyList())
    private var classCountsState = mutableStateOf<Map<String, Int>>(emptyMap())

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView

    // Gallery picker launcher (Field Mode)
    private val galleryPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val intent = Intent(this, com.yoloseg.edgeai.analysis.StaticAnalysisActivity::class.java)
            intent.putParcelableArrayListExtra("imageUris", ArrayList(uris))
            startActivity(intent)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else { Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show(); finish() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.yoloseg.edgeai.utils.UiUtils.setImmersiveMode(this)

        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ToneGenerator", e)
        }

        previewView = PreviewView(this)
        overlayView = OverlayView(this)

        detector = com.yoloseg.edgeai.ml.YoloObjectDetector(
            context = this,
            modelPath = com.yoloseg.edgeai.ui.theme.VisionThemeManager.modelPath,
            useGpu = false, useNnapi = false,
            numClasses = com.yoloseg.edgeai.ui.theme.VisionThemeManager.numClasses,
            classNames = com.yoloseg.edgeai.ui.theme.VisionThemeManager.classNames
        )
        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            MaterialTheme {
                LiveCameraScreen(
                    fpsText = fpsTextState.value,
                    detectionText = detectionCountState.value,
                    isBannerVisible = isVerificationBannerVisible.value,
                    verifiedClassName = lastVerifiedClassName.value,
                    verifiedConfidence = lastVerifiedConfidence.value,
                    activeDetections = activeDetectionsState.value,
                    classCounts = classCountsState.value,
                    isFieldMode = VisionThemeManager.isFieldMode,
                    onSwitchCamera = {
                        lensFacingState.intValue = if (lensFacingState.intValue == CameraSelector.LENS_FACING_BACK)
                            CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        startCamera()
                    },
                    onBack = { finish() },
                    onCloseBanner = {
                        isVerificationBannerVisible.value = false
                        finish()
                    },
                    onContinueBanner = {
                        isVerificationBannerVisible.value = false
                        detectionTrackers.clear()
                        verifiedClasses.clear()
                    },
                    onSettings = {
                        startActivity(Intent(this, com.yoloseg.edgeai.settings.SettingsActivity::class.java))
                    },
                    onOpenGallery = {
                        startActivity(Intent(this, com.yoloseg.edgeai.analysis.StaticAnalysisActivity::class.java))
                    },
                    previewView = previewView,
                    overlayView = overlayView
                )
            }
        }

        if (ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            permissionLauncher.launch(CAMERA_PERMISSION)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
        cameraExecutor.shutdown()
        detector.close()
        toneGenerator?.release()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val lensFacing = lensFacingState.intValue
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider
            val preview = Preview.Builder()
                .setTargetResolution(android.util.Size(1280, 720))
                .build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(640, 640))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build().also { it.setAnalyzer(cameraExecutor, ThrottledFrameAnalyzer()) }
            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private inner class ThrottledFrameAnalyzer : ImageAnalysis.Analyzer {
        @androidx.annotation.OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastInferenceTimestamp < INFERENCE_INTERVAL_MS) { imageProxy.close(); return }
            lastInferenceTimestamp = currentTime
            try {
                val bitmap = imageProxyToBitmap(imageProxy) ?: run { imageProxy.close(); return }
                val rotation = imageProxy.imageInfo.rotationDegrees
                val rotatedBitmap = if (rotation != 0) {
                    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also { bitmap.recycle() }
                } else bitmap
                val results = detector.detect(rotatedBitmap)
                frameCount++
                if (currentTime - fpsTimestamp >= 1000L) {
                    val fps = frameCount; frameCount = 0; fpsTimestamp = currentTime
                    runOnUiThread { fpsTextState.value = "FPS: $fps | Max Conf: ${String.format("%.2f", detector.lastMaxScore)}" }
                }
                runOnUiThread {
                    overlayView.setResults(results, rotatedBitmap.width, rotatedBitmap.height)
                    detectionCountState.value = "Detections: ${results.size}"
                    // Update per-class counts for Field Mode HUD
                    val counts = mutableMapOf<String, Int>()
                    for (r in results) {
                        counts[r.className] = (counts[r.className] ?: 0) + 1
                    }
                    classCountsState.value = counts
                    updateVerificationEngine(results, currentTime)
                }
                rotatedBitmap.recycle()
            } catch (e: Exception) { Log.e(TAG, "Inference error", e) }
            finally { imageProxy.close() }
        }
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val plane = imageProxy.planes[0]; val buffer = plane.buffer
            val pixelStride = plane.pixelStride; val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * imageProxy.width
            val bitmap = Bitmap.createBitmap(
                imageProxy.width + rowPadding / pixelStride, imageProxy.height, Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            if (rowPadding > 0) Bitmap.createBitmap(bitmap, 0, 0, imageProxy.width, imageProxy.height) else bitmap
        } catch (e: Exception) { null }
    }

    private data class DetectionTracker(
        var firstSeenTimestamp: Long = 0L, var lastSeenTimestamp: Long = 0L,
        var consecutiveFrameCount: Int = 0, var isVerified: Boolean = false
    )

    private fun updateVerificationEngine(results: List<com.yoloseg.edgeai.ml.DetectionResult>, currentTime: Long) {
        val validResults = results.filter { it.confidence >= com.yoloseg.edgeai.ui.theme.VisionThemeManager.detectionThreshold }
        activeDetectionsState.value = validResults

        val detectedClasses = validResults
            .map { com.yoloseg.edgeai.ui.theme.VisionThemeManager.classNames.getOrElse(it.classId) { "Unknown" } }
            .toSet()

        for (className in detectedClasses) {
            val tracker = detectionTrackers.getOrPut(className) { DetectionTracker(firstSeenTimestamp = currentTime) }
            if (tracker.lastSeenTimestamp > 0 && currentTime - tracker.lastSeenTimestamp > VERIFICATION_MAX_GAP_MS) {
                tracker.firstSeenTimestamp = currentTime; tracker.consecutiveFrameCount = 0; tracker.isVerified = false
            }
            tracker.lastSeenTimestamp = currentTime; tracker.consecutiveFrameCount++
            val duration = tracker.lastSeenTimestamp - tracker.firstSeenTimestamp
            if (!tracker.isVerified && duration >= VERIFICATION_DURATION_MS && tracker.consecutiveFrameCount >= MIN_CONSECUTIVE_FRAMES) {
                tracker.isVerified = true; verifiedClasses.add(className); onClassVerified(className)
            }
        }
        for ((className, tracker) in detectionTrackers) {
            if (className !in detectedClasses && currentTime - tracker.lastSeenTimestamp > VERIFICATION_MAX_GAP_MS) {
                tracker.consecutiveFrameCount = 0; tracker.isVerified = false; verifiedClasses.remove(className)
            }
        }
        updateVerificationBanner()

        if (validResults.isNotEmpty()) {
            val best = validResults.maxByOrNull { it.confidence }
            if (best != null) {
                lastVerifiedClassName.value = best.className.uppercase()
                lastVerifiedConfidence.value = best.confidence
            }
        }
    }

    private fun onClassVerified(className: String) {
        Log.i(TAG, "✓ TARGET VERIFIED: $className")
        if (com.yoloseg.edgeai.ui.theme.VisionThemeManager.soundAlertsEnabled) {
            try { toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2) } catch (_: Exception) {}
        }
        if (com.yoloseg.edgeai.ui.theme.VisionThemeManager.notificationsEnabled && className in NOTIFY_CLASSES) {
            val currentList = activeDetectionsState.value
            val total = currentList.size
            val classNamesStr = currentList.map { it.className }.distinct().joinToString(", ")
            val alertMsg = if (com.yoloseg.edgeai.ui.theme.VisionThemeManager.isFieldMode) {
                // Field: no confidence values
                if (classNamesStr.isNotEmpty()) {
                    "⚠ ALERT ($total Detection${if (total != 1) "s" else ""}): $classNamesStr"
                } else {
                    "⚠ ALERT: $className Verified"
                }
            } else {
                // Classic: include confidence
                val maxConf = currentList.maxOfOrNull { it.confidence } ?: lastVerifiedConfidence.value
                if (classNamesStr.isNotEmpty()) {
                    "⚠ ALERT ($total Detection${if (total != 1) "s" else ""}): $classNamesStr | Conf: ${String.format("%.0f%%", maxConf * 100)}"
                } else {
                    "⚠ ALERT: $className Verified | Conf: ${String.format("%.0f%%", lastVerifiedConfidence.value * 100)}"
                }
            }
            runOnUiThread {
                Toast.makeText(this, alertMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateVerificationBanner() {
        val notifiableVerified = verifiedClasses.filter { it in NOTIFY_CLASSES }
        if (notifiableVerified.isNotEmpty()) {
            lastVerifiedClassName.value = notifiableVerified.last().uppercase()
            isVerificationBannerVisible.value = true
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  COMPOSABLE: Camera HUD Screen
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun LiveCameraScreen(
    fpsText: String,
    detectionText: String,
    isBannerVisible: Boolean,
    verifiedClassName: String,
    verifiedConfidence: Float,
    activeDetections: List<com.yoloseg.edgeai.ml.DetectionResult> = emptyList(),
    classCounts: Map<String, Int> = emptyMap(),
    isFieldMode: Boolean = false,
    onSwitchCamera: () -> Unit,
    onBack: () -> Unit,
    onCloseBanner: () -> Unit,
    onContinueBanner: () -> Unit,
    onSettings: () -> Unit,
    onOpenGallery: () -> Unit = {},
    previewView: PreviewView,
    overlayView: OverlayView
) {
    val classNames = VisionThemeManager.classNames
    val classColors = listOf(VisionColors.ClassHelmet, Color(0xFFEF4444), VisionColors.PrimaryCyan)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        AndroidView(factory = { overlayView }, modifier = Modifier.fillMaxSize())

        // Top bar
        VisionTopBar(
            showBack = true,
            onBack = onBack,
            onSettings = onSettings,
            showThemeToggle = false
        )

        // ── HUD: Classic vs Field ──
        if (isFieldMode) {
            // Field Mode: Class-Count Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 85.dp, start = 12.dp, end = 12.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                classNames.forEachIndexed { index, name ->
                    val count = classCounts[name] ?: 0
                    val color = classColors.getOrElse(index) { VisionColors.PrimaryCyan }
                    FieldClassCountCard(
                        className = name,
                        count = count,
                        accentColor = color,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            // Classic Mode: FPS + Detection count badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 85.dp, start = 20.dp, end = 20.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassInfoBadge(text = fpsText, textColor = VisionColors.PrimaryCyan)
                GlassInfoBadge(text = detectionText, textColor = VisionColors.ClassHelmet)
            }

            // Detection Label Pill (Classic only)
            if (verifiedClassName.isNotEmpty() && verifiedConfidence > 0.3f) {
                Column(
                    modifier = Modifier
                        .padding(top = 135.dp, start = 20.dp)
                        .align(Alignment.TopStart)
                ) {
                    Column(
                        modifier = Modifier
                            .background(VisionColors.Background.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
                            .border(1.dp, VisionColors.PrimaryCyan.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).background(VisionColors.PrimaryCyan, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                verifiedClassName, color = VisionColors.OnSurface, fontSize = 13.sp,
                                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp
                            )
                        }
                        Row(modifier = Modifier.padding(start = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).background(VisionColors.ClassHelmet, CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${String.format("%.0f", verifiedConfidence * 100)}% Confidence",
                                color = VisionColors.ClassHelmet, fontSize = 12.sp, fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // ── Bottom: Animated Verification Detection Sheet (compact, positioned safely above bottom FABs) ──
        AnimatedVisibility(
            visible = isBannerVisible,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 108.dp, start = 16.dp, end = 16.dp)
        ) {
            val glowTransition = rememberInfiniteTransition(label = "sheetGlow")
            val sheetGlow by glowTransition.animateFloat(
                0.15f, 0.35f,
                infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "sheetAlpha"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(VisionColors.Surface.copy(alpha = 0.94f))
                    .border(1.dp, VisionColors.PrimaryCyan.copy(alpha = sheetGlow), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val pulseTransition = rememberInfiniteTransition(label = "alertPulse")
                    val alertScale by pulseTransition.animateFloat(
                        0.9f, 1.1f,
                        infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                        label = "alertScale"
                    )
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(VisionColors.ClassHelmet.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓",
                            fontSize = (20 * alertScale).sp,
                            color = VisionColors.ClassHelmet
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (activeDetections.isNotEmpty()) "${activeDetections.size} TARGET(S) VERIFIED" else "$verifiedClassName DETECTED",
                            color = VisionColors.OnSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isFieldMode) {
                            val maxConf = activeDetections.maxOfOrNull { it.confidence } ?: verifiedConfidence
                            Text(
                                text = "Confidence: ${String.format("%.0f%%", maxConf * 100)}",
                                color = VisionColors.ClassHelmet,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            Text(
                                text = "Verified Successfully",
                                color = VisionColors.ClassHelmet,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Detected targets list (compact row spacing)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "DETECTED TARGETS:",
                        color = VisionColors.PrimaryCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))

                    if (isFieldMode) {
                        val countMap = mutableMapOf<String, Int>()
                        activeDetections.forEach { countMap[it.className] = (countMap[it.className] ?: 0) + 1 }
                        if (countMap.isEmpty() && verifiedClassName.isNotEmpty()) {
                            countMap[verifiedClassName] = 1
                        }
                        countMap.forEach { (name, count) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                val idx = classNames.indexOfFirst { it.equals(name, ignoreCase = true) }.coerceAtLeast(0)
                                Box(Modifier.size(6.dp).background(classColors.getOrElse(idx) { VisionColors.ClassHelmet }, CircleShape))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "$name × $count",
                                    color = VisionColors.OnSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    } else {
                        val uniqueClasses = activeDetections.map { it.className }.distinct().ifEmpty { listOf(verifiedClassName) }
                        uniqueClasses.forEach { name ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Box(Modifier.size(6.dp).background(VisionColors.ClassHelmet, CircleShape))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = name,
                                    color = VisionColors.OnSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, VisionColors.Outline, RoundedCornerShape(10.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onCloseBanner() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Close", color = VisionColors.OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(VisionColors.PrimaryCyan.copy(alpha = 0.8f), VisionColors.SecondaryPurple.copy(alpha = 0.6f))
                                )
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onContinueBanner() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Continue →", color = VisionColors.OnSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // ── Controls Layer (Gallery & Switch Camera FABs) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isFieldMode) {
                OrbitingGalleryButton(onClick = onOpenGallery)
            } else {
                Spacer(Modifier.size(56.dp))
            }

            // Switch Camera FAB
            OrbitingCameraButton(onClick = onSwitchCamera)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Field Mode: Class Count Card
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun FieldClassCountCard(className: String, count: Int, accentColor: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "countGlow_$className")
    val borderAlpha by infiniteTransition.animateFloat(
        0.2f, 0.6f,
        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "countBorder_$className"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(VisionColors.Background.copy(alpha = 0.8f))
            .border(1.dp, accentColor.copy(alpha = borderAlpha), RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(accentColor, CircleShape)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = count.toString(),
            color = accentColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = className,
            color = VisionColors.OnSurface.copy(alpha = 0.85f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 11.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Orbiting Gallery Button — unique animated icon
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun OrbitingGalleryButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbitGallery")

    // Main icon rotation
    val iconRotation by infiniteTransition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "iconSpin"
    )

    // Orbiting particles angle
    val orbitAngle by infiniteTransition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "orbitAngle"
    )

    // Breathing scale
    val breathScale by infiniteTransition.animateFloat(
        0.92f, 1.08f,
        infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathScale"
    )

    // Glow alpha
    val glowAlpha by infiniteTransition.animateFloat(
        0.15f, 0.45f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "galleryGlow"
    )

    Box(
        modifier = Modifier.size(72.dp),
        contentAlignment = Alignment.Center
    ) {
        // Orbiting particles (3 small dots)
        val orbitRadius = 30f
        for (i in 0 until 3) {
            val angle = orbitAngle + (i * 120f)
            val radians = Math.toRadians(angle.toDouble())
            val particleColor = when (i) {
                0 -> VisionColors.PrimaryCyan
                1 -> VisionColors.SecondaryPurple
                else -> VisionColors.TertiaryPink
            }
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer {
                        translationX = (cos(radians) * orbitRadius).toFloat()
                        translationY = (sin(radians) * orbitRadius).toFloat()
                        alpha = 0.7f
                    }
                    .background(particleColor, CircleShape)
            )
        }

        // Main button
        Box(
            modifier = Modifier
                .size(56.dp)
                .scale(breathScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            VisionColors.SecondaryPurple.copy(alpha = 0.3f),
                            VisionColors.Surface.copy(alpha = 0.7f)
                        )
                    )
                )
                .border(
                    1.5.dp,
                    Brush.sweepGradient(
                        listOf(
                            VisionColors.PrimaryCyan.copy(alpha = glowAlpha),
                            VisionColors.SecondaryPurple.copy(alpha = glowAlpha),
                            VisionColors.TertiaryPink.copy(alpha = glowAlpha),
                            VisionColors.PrimaryCyan.copy(alpha = glowAlpha)
                        )
                    ),
                    CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            // Modern vector gallery stack icon (drawn with Compose Canvas)
            androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
                val cyan = VisionColors.PrimaryCyan
                val purple = VisionColors.SecondaryPurple

                // Back card (tilted)
                drawRoundRect(
                    color = purple.copy(alpha = 0.6f),
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.1f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.7f, size.height * 0.65f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )

                // Front main card
                drawRoundRect(
                    color = cyan,
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.22f, size.height * 0.25f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.7f, size.height * 0.65f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                )

                // Mountain / Picture peak
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.3f, size.height * 0.75f)
                    lineTo(size.width * 0.5f, size.height * 0.45f)
                    lineTo(size.width * 0.65f, size.height * 0.65f)
                    lineTo(size.width * 0.75f, size.height * 0.55f)
                    lineTo(size.width * 0.88f, size.height * 0.75f)
                    close()
                }
                drawPath(path = path, color = cyan.copy(alpha = 0.85f))

                // Sun dot in image
                drawCircle(
                    color = Color(0xFFFBBF24),
                    radius = 4f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.42f, size.height * 0.4f)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Orbiting Camera Switch Button — matching gallery icon color theme & effects
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun OrbitingCameraButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbitCamera")

    val orbitAngle by infiniteTransition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "orbitAngleCam"
    )

    val breathScale by infiniteTransition.animateFloat(
        0.92f, 1.08f,
        infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathScaleCam"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        0.15f, 0.45f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cameraGlow"
    )

    Box(
        modifier = Modifier.size(72.dp),
        contentAlignment = Alignment.Center
    ) {
        // Orbiting particles matching gallery theme
        val orbitRadius = 30f
        for (i in 0 until 3) {
            val angle = orbitAngle + (i * 120f)
            val radians = Math.toRadians(angle.toDouble())
            val particleColor = when (i) {
                0 -> VisionColors.PrimaryCyan
                1 -> VisionColors.SecondaryPurple
                else -> VisionColors.TertiaryPink
            }
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer {
                        translationX = (cos(radians) * orbitRadius).toFloat()
                        translationY = (sin(radians) * orbitRadius).toFloat()
                        alpha = 0.7f
                    }
                    .background(particleColor, CircleShape)
            )
        }

        // Main button matching gallery gradient & border theme
        Box(
            modifier = Modifier
                .size(56.dp)
                .scale(breathScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            VisionColors.SecondaryPurple.copy(alpha = 0.3f),
                            VisionColors.Surface.copy(alpha = 0.7f)
                        )
                    )
                )
                .border(
                    1.5.dp,
                    Brush.sweepGradient(
                        listOf(
                            VisionColors.PrimaryCyan.copy(alpha = glowAlpha),
                            VisionColors.SecondaryPurple.copy(alpha = glowAlpha),
                            VisionColors.TertiaryPink.copy(alpha = glowAlpha),
                            VisionColors.PrimaryCyan.copy(alpha = glowAlpha)
                        )
                    ),
                    CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            AnimatedSwitchCameraIcon(tintColor = VisionColors.PrimaryCyan)
        }
    }
}

