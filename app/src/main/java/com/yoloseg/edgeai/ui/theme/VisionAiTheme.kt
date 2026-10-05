/**
 * ═══════════════════════════════════════════════════════════════════════════════
 *  VisionAiTheme.kt — Animated Dark/Daylight Glassmorphic Design System
 * ═══════════════════════════════════════════════════════════════════════════════
 */

package com.yoloseg.edgeai.ui.theme

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// ═══════════════════════════════════════════════════════════════════════════════
//  THEME STATE & MANAGER
// ═══════════════════════════════════════════════════════════════════════════════

object VisionThemeManager {
    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("vision_ai_prefs", Context.MODE_PRIVATE)
        isDarkTheme = prefs?.getBoolean("isDarkTheme", true) ?: true
        isHighSensitivity = prefs?.getBoolean("isHighSensitivity", true) ?: true
        notificationsEnabled = prefs?.getBoolean("notificationsEnabled", true) ?: true
        telemetryEnabled = prefs?.getBoolean("telemetryEnabled", false) ?: false
        soundAlertsEnabled = prefs?.getBoolean("soundAlertsEnabled", true) ?: true
        useModelV2 = prefs?.getBoolean("useModelV2", false) ?: false
        modelVersion = prefs?.getInt("modelVersion", if (useModelV2) 1 else 0) ?: 0
        isFieldMode = prefs?.getBoolean("isFieldMode", false) ?: false
    }

    private fun save(key: String, value: Boolean) {
        prefs?.edit()?.putBoolean(key, value)?.apply()
    }
    private fun saveInt(key: String, value: Int) {
        prefs?.edit()?.putInt(key, value)?.apply()
    }

    var isDarkTheme by mutableStateOf(true)
        internal set
    fun setDarkTheme(value: Boolean) { isDarkTheme = value; save("isDarkTheme", value) }

    var isHighSensitivity by mutableStateOf(true)
        internal set
    fun setHighSensitivity(value: Boolean) { isHighSensitivity = value; save("isHighSensitivity", value) }

    val detectionThreshold: Float get() = if (isHighSensitivity) 0.3f else 0.5f

    var notificationsEnabled by mutableStateOf(true)
        internal set
    fun setNotificationsEnabled(value: Boolean) { notificationsEnabled = value; save("notificationsEnabled", value) }

    var telemetryEnabled by mutableStateOf(false)
        internal set
    fun setTelemetryEnabled(value: Boolean) { telemetryEnabled = value; save("telemetryEnabled", value) }

    var soundAlertsEnabled by mutableStateOf(true)
        internal set
    fun setSoundAlertsEnabled(value: Boolean) { soundAlertsEnabled = value; save("soundAlertsEnabled", value) }

    // ── Interface Mode: Classic (false) vs Field (true) ──
    var isFieldMode by mutableStateOf(false)
        internal set
    fun setFieldMode(value: Boolean) { isFieldMode = value; save("isFieldMode", value) }

    // Legacy — kept for migration
    var useModelV2 by mutableStateOf(false)
        internal set
    fun setUseModelV2(value: Boolean) { useModelV2 = value; save("useModelV2", value) }

    // ── New multi-model selector: 0 = v1, 1 = v2, 2 = v3 ──
    var modelVersion by mutableIntStateOf(0)
        internal set
    fun setModelVersion(v: Int) { modelVersion = v; saveInt("modelVersion", v) }

    // ── Model-dependent helpers ──
    val modelPath: String get() = when (modelVersion) {
        2 -> "best_int8_v3.tflite"
        1 -> "best_int8_v2.tflite"
        else -> "yolov8_det_int8.tflite"
    }
    val modelDisplayName: String get() = when (modelVersion) {
        2 -> "YOLOv8 INT8 v3 — Field (3-class)"
        1 -> "YOLOv8 INT8 v2 (3-class)"
        else -> "YOLOv8 INT8 v1 (4-class)"
    }
    val numClasses: Int get() = when (modelVersion) {
        2 -> 3
        1 -> 3
        else -> 4
    }
    val classNames: Array<String> get() = when (modelVersion) {
        2 -> arrayOf("With Helmet", "Without Helmet", "Ladder")
        1 -> arrayOf("With Helmet", "Without Helmet", "Ladder")
        else -> arrayOf("Helmet", "No Helmet", "Ladder", "Worker")
    }
    val classesDisplay: String get() = classNames.joinToString(", ")
    val modelSize: String get() = when (modelVersion) {
        2 -> "3.3 MB"
        1 -> "3.3 MB"
        else -> "3.3 MB"
    }
}

object VisionColors {
    val DarkBg = Color(0xFF0A0A0C)
    val DarkSurface = Color(0xFF1F1F25)
    val DarkText = Color(0xFFE5E1E4)
    val DarkTextMuted = Color(0xFFB9CACB)
    val DarkGlassBg = Color(0x661F1F25)
    val DarkGlassBorder = Color(0x1AFFFFFF)

    val LightBg = Color(0xFFF3F4F6)
    val LightSurface = Color(0xFFFFFFFF)
    val LightText = Color(0xFF111827)
    val LightTextMuted = Color(0xFF4B5563)
    val LightGlassBg = Color(0xB3FFFFFF) // Frosted glass light
    val LightGlassBorder = Color(0x1A000000)

    val Background @Composable get() = if (VisionThemeManager.isDarkTheme) DarkBg else LightBg
    val Surface @Composable get() = if (VisionThemeManager.isDarkTheme) DarkSurface else LightSurface
    val OnSurface @Composable get() = if (VisionThemeManager.isDarkTheme) DarkText else LightText
    val OnSurfaceVariant @Composable get() = if (VisionThemeManager.isDarkTheme) DarkTextMuted else LightTextMuted
    val GlassBg @Composable get() = if (VisionThemeManager.isDarkTheme) DarkGlassBg else LightGlassBg
    val GlassBorder @Composable get() = if (VisionThemeManager.isDarkTheme) DarkGlassBorder else LightGlassBorder

    val PrimaryCyan = Color(0xFF00DBE9)
    val PrimaryContainer = Color(0xFF00F0FF)
    val PrimaryFixed = Color(0xFF7DF4FF)
    val SecondaryMagenta = Color(0xFFBD00FF)
    val SecondaryContainer = Color(0xFFFE00FE)
    val SecondaryPurple = Color(0xFFECB2FF)
    val TertiaryPink = Color(0xFFEC4899)

    val ClassHelmet = Color(0xFF4CAF50)
    val ClassNoHelmet = Color(0xFFF44336)
    val ClassLadder = Color(0xFFF59E0B)
    val ClassWorker = Color(0xFFFF9800)

    val Outline = Color(0xFF3B494B)
    val Error = Color(0xFFFFB4AB)
    val WhiteDivider = Color(0x1AFFFFFF)
}

// ═══════════════════════════════════════════════════════════════════════════════
//  ANIMATED GRADIENTS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun animatedBrandBrush(): Brush {
    val infiniteTransition = rememberInfiniteTransition(label = "brandColor")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "colorPhase"
    )
    val colors = listOf(
        VisionColors.PrimaryCyan,
        VisionColors.SecondaryPurple,
        VisionColors.TertiaryPink,
        VisionColors.PrimaryFixed,
        VisionColors.PrimaryCyan
    )
    return Brush.horizontalGradient(
        colors = colors,
        startX = -500f + phase * 1500f,
        endX = 500f + phase * 1500f
    )
}

@Composable
fun AnimatedMeshBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = VisionThemeManager.isDarkTheme
    val bgColor = if (isDark) VisionColors.DarkBg else VisionColors.LightBg

    val infiniteTransition = rememberInfiniteTransition(label = "meshBg")

    // Blob movement animations
    val blob1X by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse), label = "b1x")
    val blob1Y by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse), label = "b1y")
    val blob2X by infiniteTransition.animateFloat(1f, 0f, infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse), label = "b2x")
    val blob2Y by infiniteTransition.animateFloat(0.3f, 0.8f, infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse), label = "b2y")
    val blob3X by infiniteTransition.animateFloat(0.5f, 0.2f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "b3x")
    val blob3Y by infiniteTransition.animateFloat(0.8f, 0.1f, infiniteRepeatable(tween(11000, easing = LinearEasing), RepeatMode.Reverse), label = "b3y")

    // Color cycling phase
    val colorPhase by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart), label = "colorPhase")

    // Dynamic colors for the blobs
    val blob1Color = lerpHue(VisionColors.SecondaryMagenta, VisionColors.PrimaryCyan, colorPhase, 0f)
    val blob2Color = lerpHue(VisionColors.PrimaryCyan, VisionColors.TertiaryPink, colorPhase, 120f)
    val blob3Color = lerpHue(VisionColors.TertiaryPink, VisionColors.SecondaryMagenta, colorPhase, 240f)

    // Opacity based on Dark vs Light theme (softer in daylight theme for high contrast text)
    val alphaFactor = if (isDark) 1.0f else 0.45f

    Box(modifier = modifier.fillMaxSize().background(bgColor)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height

            // Blob 1
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blob1Color.copy(alpha = 0.35f * alphaFactor), Color.Transparent),
                    center = Offset(blob1X * w, blob1Y * h),
                    radius = w * 0.5f
                ),
                radius = w * 0.5f,
                center = Offset(blob1X * w, blob1Y * h)
            )
            // Blob 2
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blob2Color.copy(alpha = 0.25f * alphaFactor), Color.Transparent),
                    center = Offset(blob2X * w, blob2Y * h),
                    radius = w * 0.45f
                ),
                radius = w * 0.45f,
                center = Offset(blob2X * w, blob2Y * h)
            )
            // Blob 3
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blob3Color.copy(alpha = 0.18f * alphaFactor), Color.Transparent),
                    center = Offset(blob3X * w, blob3Y * h),
                    radius = w * 0.4f
                ),
                radius = w * 0.4f,
                center = Offset(blob3X * w, blob3Y * h)
            )
        }
        content()
    }
}

private fun lerpHue(c1: Color, c2: Color, phase: Float, offset: Float): Color {
    val t = ((sin(Math.toRadians((phase + offset).toDouble())).toFloat() + 1f) / 2f)
    return Color(
        red = c1.red + (c2.red - c1.red) * t,
        green = c1.green + (c2.green - c1.green) * t,
        blue = c1.blue + (c2.blue - c1.blue) * t,
        alpha = 1f
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
//  GLASS PANELS & CARDS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(VisionColors.GlassBg)
            .border(1.dp, VisionColors.GlassBorder, RoundedCornerShape(cornerRadius))
            .padding(24.dp),
        content = content
    )
}

@Composable
fun AnimatedGlowCard(
    modifier: Modifier = Modifier,
    glowColor: Color = VisionColors.PrimaryContainer,
    cornerRadius: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glowCard")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowPulse"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .drawBehind {
                drawRoundRect(
                    color = glowColor.copy(alpha = glowAlpha),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, 3.dp.toPx()),
                    cornerRadius = CornerRadius(cornerRadius.toPx())
                )
            }
            .background(VisionColors.GlassBg)
            .border(1.dp, VisionColors.GlassBorder, RoundedCornerShape(cornerRadius))
            .padding(24.dp),
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
//  UNIVERSAL TOP BAR (fixed center layout, only toggle in settings)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun VisionTopBar(
    modifier: Modifier = Modifier,
    showBack: Boolean = false,
    onBack: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
    showThemeToggle: Boolean = false
) {
    val brandBrush = animatedBrandBrush()
    val subColor = VisionColors.OnSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 28.dp, start = 20.dp, end = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (showBack && onBack != null) {
            AestheticBackButton(onClick = onBack)
        } else {
            Spacer(modifier = Modifier.width(40.dp))
        }

        Text(
            text = "Vision AI",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            style = androidx.compose.ui.text.TextStyle(brush = brandBrush),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showThemeToggle) {
                ThemeToggleIcon()
            } else if (onSettings != null) {
                val settingsAnim = rememberInfiniteTransition(label = "settingsAnim")
                val sRot by settingsAnim.animateFloat(0f, 360f, infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "sRot")
                val sPulse by settingsAnim.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sPulse")
                val sGlow by settingsAnim.animateFloat(0.2f, 0.6f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sGlow")
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .scale(sPulse)
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
                                    VisionColors.PrimaryCyan.copy(alpha = sGlow),
                                    VisionColors.SecondaryPurple.copy(alpha = sGlow),
                                    VisionColors.TertiaryPink.copy(alpha = sGlow),
                                    VisionColors.PrimaryCyan.copy(alpha = sGlow)
                                )
                            ),
                            CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚙", color = VisionColors.PrimaryCyan, fontSize = 16.sp, modifier = Modifier.rotate(sRot))
                }
            } else {
                Spacer(modifier = Modifier.width(40.dp))
            }
        }
    }
}

@Composable
fun ThemeToggleIcon() {
    val isDark = VisionThemeManager.isDarkTheme
    val themeAnim = rememberInfiniteTransition(label = "themeAnim")
    val tPulse by themeAnim.animateFloat(0.95f, 1.05f,
        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "tP")
    val tGlow by themeAnim.animateFloat(0.2f, 0.6f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "tG")
    val accentColor = if (isDark) Color(0xFFFFA726) else Color(0xFF7C4DFF)
    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(tPulse)
            .clip(CircleShape)
            .background(VisionColors.GlassBg)
            .border(1.dp, accentColor.copy(alpha = tGlow), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                VisionThemeManager.setDarkTheme(!VisionThemeManager.isDarkTheme)
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isDark,
            transitionSpec = {
                (scaleIn() + fadeIn() + slideInVertically { it }).togetherWith(
                    scaleOut() + fadeOut() + slideOutVertically { -it })
            },
            label = "themeIcon"
        ) { dark ->
            Text(text = if (dark) "🌙" else "☀️", fontSize = 16.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Aesthetic Aligned Back Button
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun AestheticBackButton(onClick: () -> Unit) {
    val backAnim = rememberInfiniteTransition(label = "backAnim")
    val bPulse by backAnim.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bPulse")
    val bShift by backAnim.animateFloat(-2f, 2f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bShift")
    val bGlow by backAnim.animateFloat(0.2f, 0.6f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bGlow")

    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(bPulse)
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
                        VisionColors.PrimaryCyan.copy(alpha = bGlow),
                        VisionColors.SecondaryPurple.copy(alpha = bGlow),
                        VisionColors.TertiaryPink.copy(alpha = bGlow),
                        VisionColors.PrimaryCyan.copy(alpha = bGlow)
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
        Canvas(modifier = Modifier.size(16.dp).graphicsLayer { translationX = bShift }) {
            val w = size.width; val h = size.height
            val path = Path().apply {
                moveTo(w * 0.7f, h * 0.15f)
                lineTo(w * 0.3f, h * 0.5f)
                lineTo(w * 0.7f, h * 0.85f)
            }
            drawPath(
                path = path,
                color = if (VisionThemeManager.isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f),
                style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  3D ROTATING COMBINED LOGO
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun Animated3dLadderLogo(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo3d")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Canvas(modifier = modifier.size(120.dp)) {
        val cx = size.width / 2; val cy = size.height / 2
        val radius = size.width * 0.45f * pulse
        val scaleX = cos(Math.toRadians(rotation.toDouble())).toFloat()

        this.scale(scaleX = scaleX, scaleY = 1f, pivot = Offset(cx, cy)) {
            val leftRailGrad = Brush.verticalGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)))
            drawLine(leftRailGrad, Offset(cx - radius * 0.32f, cy - radius * 0.5f), Offset(cx - radius * 0.32f, cy + radius * 0.6f), strokeWidth = 10f, cap = StrokeCap.Round)

            val rightRailGrad = Brush.verticalGradient(listOf(Color(0xFFA855F7), Color(0xFFEC4899)))
            drawLine(rightRailGrad, Offset(cx + radius * 0.32f, cy - radius * 0.5f), Offset(cx + radius * 0.32f, cy + radius * 0.6f), strokeWidth = 10f, cap = StrokeCap.Round)

            for (i in 0..4) {
                val fraction = i / 4f
                val y = (cy - radius * 0.35f) + (radius * 0.85f) * fraction
                drawLine(Color.White.copy(alpha = 0.85f), Offset(cx - radius * 0.32f, y), Offset(cx + radius * 0.32f, y), strokeWidth = 5f, cap = StrokeCap.Round)
            }

            val helmetColor = Color(0xFF00DBE9)
            val hy = cy - radius * 0.55f

            val domePath = Path().apply {
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(cx - radius * 0.4f, hy - radius * 0.4f, cx + radius * 0.4f, hy + radius * 0.1f),
                    startAngleDegrees = 180f, sweepAngleDegrees = 180f, forceMoveTo = true
                )
                close()
            }
            drawPath(domePath, helmetColor)

            val ridgeColor = Color.Black.copy(alpha = 0.3f)
            drawLine(ridgeColor, Offset(cx - radius * 0.1f, hy - radius * 0.4f), Offset(cx - radius * 0.05f, hy - radius * 0.02f), strokeWidth = 3f)
            drawLine(ridgeColor, Offset(cx + radius * 0.1f, hy - radius * 0.4f), Offset(cx + radius * 0.05f, hy - radius * 0.02f), strokeWidth = 3f)

            drawLine(helmetColor, Offset(cx - radius * 0.48f, hy - radius * 0.02f), Offset(cx + radius * 0.48f, hy - radius * 0.02f), strokeWidth = 6f, cap = StrokeCap.Round)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  SLEEK GLASSMORPHISM MODULE ICONS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun SleekLiveVideoIcon(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "videoIcon")
    val pulse by infiniteTransition.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val blink by infiniteTransition.animateFloat(0.2f, 1.0f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "blink")

    Canvas(modifier = modifier.size(56.dp)) {
        val w = size.width; val h = size.height; val cx = w / 2; val cy = h / 2
        val s = w * 0.35f * pulse

        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Color(0xFF00DBE9), Color(0xFF3B82F6))),
            topLeft = Offset(cx - s, cy - s * 0.6f),
            size = Size(s * 1.4f, s * 1.2f),
            cornerRadius = CornerRadius(12f),
            style = Stroke(3f)
        )
        drawRoundRect(
            color = Color(0xFF00DBE9).copy(alpha = 0.15f),
            topLeft = Offset(cx - s, cy - s * 0.6f),
            size = Size(s * 1.4f, s * 1.2f),
            cornerRadius = CornerRadius(12f)
        )
        drawCircle(
            color = Color.White,
            radius = s * 0.25f,
            center = Offset(cx - s * 0.3f, cy),
            style = Stroke(2f)
        )
        drawCircle(
            color = Color.Red.copy(alpha = blink),
            radius = 6f,
            center = Offset(cx + s * 0.1f, cy - s * 0.3f)
        )
    }
}

@Composable
fun SleekGalleryPhotoIcon(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "photoIcon")
    val rot by infiniteTransition.animateFloat(-5f, 5f, infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "rot")

    Canvas(modifier = modifier.size(56.dp)) {
        val w = size.width; val h = size.height; val cx = w / 2; val cy = h / 2
        val s = w * 0.35f

        this.rotate(rot, pivot = Offset(cx, cy)) {
            drawRoundRect(
                color = Color(0xFFEC4899).copy(alpha = 0.2f),
                topLeft = Offset(cx - s * 0.8f, cy - s * 0.8f),
                size = Size(s * 1.3f, s * 1.3f),
                cornerRadius = CornerRadius(12f)
            )
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(Color(0xFFBD00FF), Color(0xFFEC4899))),
                topLeft = Offset(cx - s * 0.6f, cy - s * 0.6f),
                size = Size(s * 1.3f, s * 1.3f),
                cornerRadius = CornerRadius(12f),
                style = Stroke(3f)
            )
            drawCircle(
                color = Color(0xFFFE00FE),
                radius = s * 0.15f,
                center = Offset(cx + s * 0.2f, cy - s * 0.2f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  SUPPORT SVG BUTTON ICONS (EXACT REPLICA DESIGN DRAWN ON CANVAS)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun WhatsAppSvgIcon(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        painter = painterResource(id = com.yoloseg.edgeai.R.drawable.ic_whatsapp),
        contentDescription = "WhatsApp",
        modifier = modifier.size(24.dp)
    )
}

@Composable
fun GitHubSvgIcon(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        painter = painterResource(id = com.yoloseg.edgeai.R.drawable.ic_github),
        contentDescription = "GitHub",
        modifier = modifier.size(24.dp)
    )
}

@Composable
fun RedCrossIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width; val h = size.height
        drawLine(Color(0xFFEF4444), Offset(w * 0.2f, h * 0.2f), Offset(w * 0.8f, h * 0.8f), strokeWidth = 3.5f, cap = StrokeCap.Round)
        drawLine(Color(0xFFEF4444), Offset(w * 0.8f, h * 0.2f), Offset(w * 0.2f, h * 0.8f), strokeWidth = 3.5f, cap = StrokeCap.Round)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  REST OF THE DESIGN SYSTEM
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun StatusChip(text: String, chipColor: Color = VisionColors.PrimaryCyan, modifier: Modifier = Modifier) {
    val anim = rememberInfiniteTransition(label = "chipAnim_$text")
    val chipGlow by anim.animateFloat(0.15f, 0.4f,
        infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "cG")
    val chipPulse by anim.animateFloat(1f, 1.06f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "cP")
    Text(
        text = text, color = chipColor, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace, letterSpacing = 1.sp,
        modifier = modifier
            .graphicsLayer { scaleX = chipPulse; scaleY = chipPulse }
            .background(chipColor.copy(alpha = chipGlow), RoundedCornerShape(50))
            .border(1.dp, chipColor.copy(alpha = chipGlow + 0.2f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}

@Composable
fun GlassInfoBadge(text: String, textColor: Color = VisionColors.PrimaryCyan, modifier: Modifier = Modifier) {
    Text(
        text = text, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = modifier
            .background(VisionColors.Background.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .border(1.dp, VisionColors.GlassBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
fun AnimatedHelmetIcon(modifier: Modifier = Modifier, tintColor: Color = VisionColors.PrimaryCyan) {
    val infiniteTransition = rememberInfiniteTransition(label = "helmetAnim")
    val pulse by infiniteTransition.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val glow by infiniteTransition.animateFloat(0.4f, 0.8f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "glow")

    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height; val cx = w / 2; val cy = h / 2
        // Max radius = 0.3 * 1.1 * 1.5 = 0.495w, perfectly fits inside w/2 bounds
        val s = w * 0.3f * pulse
        val sw1 = maxOf(w * 0.05f, 2f)
        val sw2 = maxOf(w * 0.03f, 1f)
        
        drawCircle(tintColor.copy(alpha = glow * 0.2f), radius = s * 1.5f, center = Offset(cx, cy))
        val domePath = Path().apply {
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(cx - s, cy - s * 0.8f, cx + s, cy + s * 0.4f),
                startAngleDegrees = 180f, sweepAngleDegrees = 180f, forceMoveTo = true
            )
            close()
        }
        drawPath(domePath, tintColor, style = Stroke(width = sw1, cap = StrokeCap.Round))
        drawLine(tintColor, Offset(cx - s * 1.15f, cy + s * 0.1f), Offset(cx + s * 1.15f, cy + s * 0.1f), strokeWidth = sw1, cap = StrokeCap.Round)
        drawLine(tintColor.copy(alpha = 0.5f), Offset(cx - s * 0.7f, cy + s * 0.35f), Offset(cx + s * 0.7f, cy + s * 0.35f), strokeWidth = sw2, cap = StrokeCap.Round)
    }
}

fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (_: Exception) {}
}

@Composable
fun AnimatedUploadIcon(modifier: Modifier = Modifier, tintColor: Color = VisionColors.PrimaryCyan) {
    val infiniteTransition = rememberInfiniteTransition(label = "uploadAnim")
    val float by infiniteTransition.animateFloat(-3f, 3f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")

    Canvas(modifier = modifier.size(48.dp)) {
        val w = size.width; val h = size.height; val cx = w / 2; val cy = h / 2
        val cloudY = cy - 4 + float
        drawCircle(tintColor, radius = w * 0.15f, center = Offset(cx, cloudY - w * 0.05f), style = Stroke(2.5f))
        drawCircle(tintColor, radius = w * 0.11f, center = Offset(cx - w * 0.13f, cloudY + w * 0.02f), style = Stroke(2f))
        drawCircle(tintColor, radius = w * 0.11f, center = Offset(cx + w * 0.13f, cloudY + w * 0.02f), style = Stroke(2f))
        val arrowTop = cloudY + w * 0.18f
        drawLine(tintColor, Offset(cx, arrowTop), Offset(cx, arrowTop + w * 0.2f), strokeWidth = 2.5f, cap = StrokeCap.Round)
        drawLine(tintColor, Offset(cx - w * 0.08f, arrowTop + w * 0.08f), Offset(cx, arrowTop), strokeWidth = 2.5f, cap = StrokeCap.Round)
        drawLine(tintColor, Offset(cx + w * 0.08f, arrowTop + w * 0.08f), Offset(cx, arrowTop), strokeWidth = 2.5f, cap = StrokeCap.Round)
    }
}

@Composable
fun AnimatedSwitchCameraIcon(modifier: Modifier = Modifier, tintColor: Color = Color.White) {
    val infiniteTransition = rememberInfiniteTransition(label = "switchCam")
    val rot by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart), label = "rot")

    Canvas(modifier = modifier.size(28.dp)) {
        val cx = size.width / 2; val cy = size.height / 2; val r = size.width * 0.35f
        drawArc(tintColor, startAngle = rot, sweepAngle = 270f, useCenter = false,
            topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2),
            style = Stroke(2.5f, cap = StrokeCap.Round))
        val angle = Math.toRadians((rot + 270.0))
        val ax = cx + cos(angle).toFloat() * r
        val ay = cy + sin(angle).toFloat() * r
        drawCircle(tintColor, radius = 3f, center = Offset(ax, ay))
    }
}
