/**
 * ═══════════════════════════════════════════════════════════════════════════════
 *  OverlayView.kt
 *  Custom Canvas Overlay for Bounding Boxes and Segmentation Masks
 *
 *  Renders detection results (boxes, labels, translucent masks) on top of
 *  the camera preview or static image analysis view.
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 *  Place at: app/src/main/java/com/yoloseg/edgeai/ui/OverlayView.kt
 */

package com.yoloseg.edgeai.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View


/**
 * Custom View that draws segmentation results (bounding boxes, masks, labels)
 * over the camera preview or image analysis output.
 *
 * Usage:
 *   overlayView.setResults(detections, sourceWidth, sourceHeight)
 *   overlayView.clear()
 */
class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Class-specific Colors ──
    companion object {
        private val COLOR_HELMET = Color.parseColor("#4CAF50")      // Green
        private val COLOR_NO_HELMET = Color.parseColor("#F44336")   // Red
        private val COLOR_LADDER = Color.parseColor("#00BCD4")      // Cyan
        private val COLOR_WORKER = Color.parseColor("#FF9800")      // Orange
        private val COLOR_DEFAULT = Color.parseColor("#9E9E9E")     // Grey

        private const val BOX_STROKE_WIDTH = 4f
        private const val LABEL_TEXT_SIZE = 36f
        private const val LABEL_PADDING = 8f
        private const val CORNER_RADIUS = 6f
    }

    // ── State ──
    private var results: List<com.yoloseg.edgeai.ml.DetectionResult> = emptyList()
    private var sourceWidth: Int = 1
    private var sourceHeight: Int = 1

    // ── Paint Objects (reused) ──
    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = BOX_STROKE_WIDTH
        isAntiAlias = true
    }

    private val labelTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = LABEL_TEXT_SIZE
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val labelBgPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun setResults(detections: List<com.yoloseg.edgeai.ml.DetectionResult>, srcWidth: Int, srcHeight: Int) {
        results = detections
        sourceWidth = srcWidth
        sourceHeight = srcHeight
        invalidate()
    }

    fun clear() {
        results = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Bounding box overlay drawing completely disabled as requested
    }

    private fun drawLabel(canvas: Canvas, label: String, left: Float, top: Float, color: Int) {
        val textWidth = labelTextPaint.measureText(label)
        val textHeight = labelTextPaint.textSize

        val bgLeft = left
        val bgTop = top - textHeight - LABEL_PADDING * 2
        val bgRight = left + textWidth + LABEL_PADDING * 2
        val bgBottom = top

        labelBgPaint.color = color
        canvas.drawRoundRect(
            bgLeft, bgTop, bgRight, bgBottom,
            CORNER_RADIUS, CORNER_RADIUS, labelBgPaint
        )

        canvas.drawText(label, left + LABEL_PADDING, top - LABEL_PADDING, labelTextPaint)
    }

    private fun getClassColor(classId: Int): Int = when (classId) {
        0 -> COLOR_HELMET
        1 -> COLOR_NO_HELMET
        2 -> COLOR_LADDER
        3 -> COLOR_WORKER
        else -> COLOR_DEFAULT
    }
}
