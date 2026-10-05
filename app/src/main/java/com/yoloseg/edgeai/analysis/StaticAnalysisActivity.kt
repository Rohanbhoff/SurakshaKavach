/**
 * ═══════════════════════════════════════════════════════════════════════════════
 *  StaticAnalysisActivity.kt
 *  Gallery Image Picker with Detection Analysis — Vision AI Animated Theme
 * ═══════════════════════════════════════════════════════════════════════════════
 */

package com.yoloseg.edgeai.analysis

import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yoloseg.edgeai.ui.theme.*
import kotlinx.coroutines.*

class StaticAnalysisActivity : ComponentActivity() {

    companion object {
        private const val TAG = "StaticAnalysis"
        private val CLASS_COLORS = mapOf(
            0 to Color.parseColor("#4CAF50"), 1 to Color.parseColor("#F44336"),
            2 to Color.parseColor("#00BCD4"), 3 to Color.parseColor("#FF9800")
        )
        private const val DEFAULT_COLOR = 0xFF9E9E9EL.toInt()
    }

    private lateinit var detector: com.yoloseg.edgeai.ml.YoloObjectDetector
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    data class AnalyzedImage(
        val originalUri: Uri, val originalBitmap: Bitmap, val resultBitmap: Bitmap,
        val detections: List<com.yoloseg.edgeai.ml.DetectionResult>, val inferenceMs: Long
    )

    private var analysisResultsState = mutableStateListOf<AnalyzedImage>()
    private var isProcessingState = mutableStateOf(false)
    private var isFullScreenModalVisible = mutableStateOf(false)
    private var analysisStatusState = mutableStateOf("Select images to begin analysis")
    private var detectionSummaryState = mutableStateOf("")
    private var selectedResultState = mutableStateOf<AnalyzedImage?>(null)

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) processSelectedImages(uris)
        else analysisStatusState.value = "No images selected"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.yoloseg.edgeai.utils.UiUtils.setImmersiveMode(this)

        detector = com.yoloseg.edgeai.ml.YoloObjectDetector(
            context = this,
            modelPath = com.yoloseg.edgeai.ui.theme.VisionThemeManager.modelPath,
            useGpu = false, useNnapi = false,
            numClasses = com.yoloseg.edgeai.ui.theme.VisionThemeManager.numClasses,
            classNames = com.yoloseg.edgeai.ui.theme.VisionThemeManager.classNames
        )

        setContent {
            MaterialTheme {
                StaticAnalysisScreen(
                    analysisResults = analysisResultsState,
                    isProcessing = isProcessingState.value,
                    analysisStatus = analysisStatusState.value,
                    detectionSummary = detectionSummaryState.value,
                    selectedResult = selectedResultState.value,
                    isFullScreen = isFullScreenModalVisible.value,
                    onOpenFullScreen = { isFullScreenModalVisible.value = true },
                    onCloseFullScreen = { isFullScreenModalVisible.value = false },
                    onSelectImages = { imagePickerLauncher.launch("image/*") },
                    onThumbnailSelected = { img ->
                        selectedResultState.value = img
                        detectionSummaryState.value = buildSingleImageSummary(img)
                    },
                    onBack = { finish() },
                    onSettings = {
                        startActivity(Intent(this, com.yoloseg.edgeai.settings.SettingsActivity::class.java))
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel(); detector.close()
        for (r in analysisResultsState) { r.originalBitmap.recycle(); r.resultBitmap.recycle() }
    }

    private fun processSelectedImages(uris: List<Uri>) {
        isProcessingState.value = true
        analysisStatusState.value = "Analyzing ${uris.size} image(s)..."

        activityScope.launch {
            var totalDetections = 0; var totalInferenceMs = 0L
            val newResults = mutableListOf<AnalyzedImage>()
            for ((index, uri) in uris.withIndex()) {
                analysisStatusState.value = "Processing image ${index + 1}/${uris.size}..."
                try {
                    val originalBitmap = withContext(Dispatchers.IO) { loadBitmapFromUri(uri) } ?: continue
                    val (detections, inferenceMs) = withContext(Dispatchers.Default) {
                        val start = System.currentTimeMillis()
                        val results = detector.detect(originalBitmap)
                        Pair(results, System.currentTimeMillis() - start)
                    }
                    val resultBitmap = withContext(Dispatchers.Default) { drawOverlaysOnBitmap(originalBitmap, detections) }
                    val analyzed = AnalyzedImage(uri, originalBitmap, resultBitmap, detections, inferenceMs)
                    newResults.add(analyzed)
                    totalDetections += detections.size; totalInferenceMs += inferenceMs
                } catch (e: Exception) { Log.e(TAG, "Failed: $uri", e) }
            }
            analysisResultsState.addAll(newResults)
            if (selectedResultState.value == null && newResults.isNotEmpty()) {
                selectedResultState.value = newResults.first()
            }
            isProcessingState.value = false
            val allCount = analysisResultsState.sumOf { it.detections.size }
            val avgMs = if (analysisResultsState.isNotEmpty()) analysisResultsState.sumOf { it.inferenceMs } / analysisResultsState.size else 0
            val allDets = analysisResultsState.flatMap { it.detections }
            val confSummary = if (allDets.isNotEmpty()) {
                allDets.joinToString(" · ") { "${it.className} (${String.format("%.0f%%", it.confidence * 100)})" }
            } else "None"
            analysisStatusState.value = "Analysis Complete — $allCount Target(s) Verified\n$confSummary"
            detectionSummaryState.value = buildDetectionSummary(allCount, avgMs)
        }
    }

    private fun drawOverlaysOnBitmap(source: Bitmap, detections: List<com.yoloseg.edgeai.ml.DetectionResult>): Bitmap {
        // Return clean bitmap with no bounding box drawings as requested
        return source.copy(Bitmap.Config.ARGB_8888, true)
    }

    @Suppress("DEPRECATION")
    private fun loadBitmapFromUri(uri: Uri): Bitmap? = try { MediaStore.Images.Media.getBitmap(contentResolver, uri) } catch (_: Exception) { null }

    private fun buildDetectionSummary(total: Int, avgMs: Long): String {
        val names = com.yoloseg.edgeai.ui.theme.VisionThemeManager.classNames
        val isField = com.yoloseg.edgeai.ui.theme.VisionThemeManager.isFieldMode
        val sb = StringBuilder("Total: $total detection(s)")
        for ((idx, name) in names.withIndex()) {
            val count = analysisResultsState.sumOf { r -> r.detections.count { it.classId == idx } }
            sb.append("\n$name: $count")
        }
        if (!isField) {
            sb.append("\nAvg Inference: ${avgMs}ms/image")
        }
        return sb.toString()
    }

    private fun buildSingleImageSummary(a: AnalyzedImage): String {
        val names = com.yoloseg.edgeai.ui.theme.VisionThemeManager.classNames
        val isField = com.yoloseg.edgeai.ui.theme.VisionThemeManager.isFieldMode
        val sb = StringBuilder("Image Total: ${a.detections.size} detection(s)")
        for ((idx, name) in names.withIndex()) {
            val count = a.detections.count { it.classId == idx }
            sb.append("\n$name: $count")
        }
        if (!isField) {
            if (a.detections.isNotEmpty()) {
                val confList = a.detections.joinToString(", ") { "${it.className}: ${String.format("%.0f%%", it.confidence * 100)}" }
                sb.append("\nDetections: $confList")
            }
            sb.append("\nInference: ${a.inferenceMs}ms")
        }
        return sb.toString()
    }
}

@Composable
fun StaticAnalysisScreen(
    analysisResults: List<StaticAnalysisActivity.AnalyzedImage>,
    isProcessing: Boolean,
    isFullScreen: Boolean,
    onOpenFullScreen: () -> Unit,
    onCloseFullScreen: () -> Unit,
    analysisStatus: String,
    detectionSummary: String,
    selectedResult: StaticAnalysisActivity.AnalyzedImage?,
    onSelectImages: () -> Unit,
    onThumbnailSelected: (StaticAnalysisActivity.AnalyzedImage) -> Unit,
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    AnimatedMeshBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            VisionTopBar(showBack = true, onBack = onBack, onSettings = onSettings, showThemeToggle = false)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(10.dp))

                val brandBrush = animatedBrandBrush()
                Text(
                    text = "Gallery Analysis",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(brush = brandBrush),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Upload high-resolution imagery for deep neural network processing. The system automatically detects anomalies and classifies structures.",
                    color = VisionColors.OnSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))

                if (analysisResults.isEmpty() && !isProcessing) {
                    val uploadGlow = rememberInfiniteTransition(label = "uploadGlow")
                    val uploadBorderAlpha by uploadGlow.animateFloat(0.15f, 0.5f,
                        infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "uBA")
                    val uploadScale by uploadGlow.animateFloat(1f, 1.01f,
                        infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "uS")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { scaleX = uploadScale; scaleY = uploadScale }
                            .clip(RoundedCornerShape(20.dp))
                            .background(VisionColors.GlassBg)
                            .border(1.dp, VisionColors.PrimaryCyan.copy(alpha = uploadBorderAlpha), RoundedCornerShape(20.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelectImages() }
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedUploadIcon(tintColor = VisionColors.PrimaryCyan)
                        Spacer(Modifier.height(20.dp))
                        Text("Select Image for\nAnalysis", color = VisionColors.OnSurface, fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 24.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Tap to browse your device gallery.\nAll common image formats supported.",
                            color = VisionColors.OnSurfaceVariant, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
                        Spacer(Modifier.height(16.dp))

                        // Grid layout to prevent HEIC wrap misalignment
                        FormatChipsGrid()
                    }
                } else {
                    // Pager State for smooth swiping
                    val pagerState = rememberPagerState(pageCount = { analysisResults.size })

                    // Automatically update selectedResult and single summary when user swipes
                    LaunchedEffect(pagerState.currentPage, isProcessing) {
                        if (analysisResults.isNotEmpty() && !isProcessing) {
                            val page = pagerState.currentPage.coerceIn(0, analysisResults.size - 1)
                            val img = analysisResults[page]
                            onThumbnailSelected(img)
                        }
                    }

                    // Animate pager when user taps on a thumbnail
                    LaunchedEffect(selectedResult) {
                        val index = analysisResults.indexOf(selectedResult)
                        if (index >= 0 && index != pagerState.currentPage) {
                            pagerState.animateScrollToPage(index)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(VisionColors.Surface)
                            .border(1.dp, VisionColors.GlassBorder, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val item = analysisResults.getOrNull(page)
                            if (item != null) {
                                Image(
                                    bitmap = item.originalBitmap.asImageBitmap(),
                                    contentDescription = "Result Image $page",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { onOpenFullScreen() }
                                )
                            }
                        }

                        if (isProcessing) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = VisionColors.PrimaryCyan, modifier = Modifier.size(40.dp), strokeWidth = 3.dp)
                                Spacer(Modifier.height(12.dp))
                                Text(analysisStatus, color = VisionColors.OnSurfaceVariant, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (analysisResults.size > 1) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(12.dp))
                                .background(VisionColors.GlassBg).border(1.dp, VisionColors.GlassBorder, RoundedCornerShape(12.dp)).padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(analysisResults) { r ->
                                Image(bitmap = r.resultBitmap.asImageBitmap(), contentDescription = "Thumb",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, if (r == selectedResult) VisionColors.PrimaryCyan else VisionColors.GlassBorder, RoundedCornerShape(8.dp))
                                        .clickable { onThumbnailSelected(r) })
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    if (detectionSummary.isNotEmpty()) {
                        AnimatedDetectionSummary(status = analysisStatus, summary = detectionSummary)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Select More Images Button with animated hue gradient border (clean solid glass background)
                    val btnHueAnim = rememberInfiniteTransition(label = "btnHue")
                    val btnGlowAlpha by btnHueAnim.animateFloat(0.3f, 0.8f, infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bHA")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(VisionColors.GlassBg)
                            .border(
                                1.5.dp,
                                Brush.sweepGradient(
                                    listOf(
                                        VisionColors.PrimaryCyan.copy(alpha = btnGlowAlpha),
                                        VisionColors.SecondaryPurple.copy(alpha = btnGlowAlpha),
                                        VisionColors.TertiaryPink.copy(alpha = btnGlowAlpha),
                                        VisionColors.PrimaryCyan.copy(alpha = btnGlowAlpha)
                                    )
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null, enabled = !isProcessing
                            ) { onSelectImages() }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Select More Images", color = VisionColors.PrimaryCyan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }

        // Full Screen Pinch-to-Zoom Modal Dialog
        if (isFullScreen && selectedResult != null) {
            FullScreenImageModal(
                bitmap = selectedResult.originalBitmap,
                onDismiss = onCloseFullScreen
            )
        }
    }
}

@Composable
fun FormatChipsGrid() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("PNG", "JPG", "WEBP").forEach { FormatChip(it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("BMP", "GIF", "HEIC").forEach { FormatChip(it) }
        }
    }
}

@Composable
fun FormatChip(format: String) {
    Text(
        text = format, color = VisionColors.OnSurface, fontSize = 10.sp,
        fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .background(VisionColors.Surface, RoundedCornerShape(50))
            .border(1.dp, VisionColors.Outline, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
fun AnimatedDetectionSummary(status: String, summary: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "summaryGlow")
    val borderAlpha by infiniteTransition.animateFloat(
        0.15f, 0.45f, infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sBA"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(VisionColors.GlassBg)
            .border(1.dp, VisionColors.PrimaryCyan.copy(alpha = borderAlpha), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Text(
            "DETECTION SUMMARY", color = VisionColors.PrimaryCyan, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        // Class Counts List with continuous animations per box
        val lines = summary.split("\n")
        val classColors = listOf(
            VisionColors.PrimaryCyan,
            VisionColors.ClassHelmet,
            androidx.compose.ui.graphics.Color(0xFFEF4444),
            VisionColors.ClassLadder, // Amber/Gold (0xFFF59E0B)
            VisionColors.ClassWorker
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            lines.forEachIndexed { index, line ->
                val parts = line.split(":")
                val title = parts.getOrNull(0)?.trim() ?: line
                val value = parts.getOrNull(1)?.trim() ?: ""
                val dotColor = classColors.getOrElse(index) { VisionColors.PrimaryCyan }

                // Per-item continuous animations
                val itemTransition = rememberInfiniteTransition(label = "itemAnim_$index")
                val itemGlow by itemTransition.animateFloat(
                    0.2f, 0.6f,
                    infiniteRepeatable(tween(1800 + index * 200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "itemGlow_$index"
                )
                val dotScale by itemTransition.animateFloat(
                    0.85f, 1.25f,
                    infiniteRepeatable(tween(1200 + index * 150, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "dotScale_$index"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(VisionColors.Surface.copy(alpha = 0.5f))
                        .border(1.dp, dotColor.copy(alpha = itemGlow), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pulsating dot icon
                        Box(
                            Modifier
                                .size(9.dp)
                                .scale(dotScale)
                                .background(dotColor, CircleShape)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = title,
                            color = VisionColors.OnSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (value.isNotEmpty()) {
                        Text(
                            text = value,
                            color = dotColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  FULL SCREEN PINCH & DOUBLE-TAP ZOOM MODAL
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun FullScreenImageModal(
    bitmap: Bitmap,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        // 2.5x sensitivity boost for rapid, effortless panning when zoomed in
        val sensitiveOffset = offsetChange * 2.5f
        offset = if (scale > 1f) offset + sensitiveOffset else androidx.compose.ui.geometry.Offset.Zero
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Full Screen Zoomable Image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(state = transformState)
            )

            // Top Bar Controls (Close button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(VisionColors.Surface.copy(alpha = 0.8f))
                        .border(1.dp, VisionColors.GlassBorder, CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = VisionColors.OnSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Bottom Hint
            Text(
                text = "Pinch to Zoom · Drag to Pan",
                color = VisionColors.OnSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp)
            )
        }
    }
}
