/**
 * ═══════════════════════════════════════════════════════════════════════════════
 *  SplashActivity.kt — Animated Startup Screen (2-second "Vision AI" reveal)
 *
 *  Similar to YouTube's startup animation: logo fades/scales in with a glow
 *  effect, then transitions to the main dashboard.
 * ═══════════════════════════════════════════════════════════════════════════════
 */

package com.yoloseg.edgeai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yoloseg.edgeai.ui.theme.VisionColors
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.yoloseg.edgeai.ui.theme.VisionThemeManager.init(applicationContext)
        com.yoloseg.edgeai.utils.UiUtils.setImmersiveMode(this)

        setContent {
            SplashScreen(
                onFinished = {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    @Suppress("DEPRECATION")
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            )
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    // Animation states
    var startAnim by remember { mutableStateOf(false) }

    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "logoAlpha"
    )
    val logoScale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.6f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "logoScale"
    )
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(600, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "subAlpha"
    )

    // Glow ring animation
    val infiniteTransition = rememberInfiniteTransition(label = "splashGlow")
    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 100f, targetValue = 300f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowR"
    )
    val glowPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "glowPhase"
    )

    LaunchedEffect(Unit) {
        startAnim = true
        delay(2200)
        onFinished()
    }

    val glowColor1 = VisionColors.PrimaryCyan
    val glowColor2 = VisionColors.SecondaryMagenta
    val t = (kotlin.math.sin(Math.toRadians(glowPhase.toDouble())).toFloat() + 1f) / 2f
    val currentGlow = Color(
        red = glowColor1.red + (glowColor2.red - glowColor1.red) * t,
        green = glowColor1.green + (glowColor2.green - glowColor1.green) * t,
        blue = glowColor1.blue + (glowColor2.blue - glowColor1.blue) * t
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VisionColors.Background),
        contentAlignment = Alignment.Center
    ) {
        // Animated glow behind logo
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2; val cy = size.height / 2
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(currentGlow.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = Offset(cx, cy)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(logoScale)
                .alpha(logoAlpha)
        ) {
            // Animated helmet icon
            com.yoloseg.edgeai.ui.theme.AnimatedHelmetIcon(
                modifier = Modifier.size(64.dp),
                tintColor = VisionColors.PrimaryCyan
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Vision AI",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(VisionColors.PrimaryCyan, VisionColors.SecondaryPurple, VisionColors.TertiaryPink)
                    ),
                    shadow = Shadow(
                        color = currentGlow.copy(alpha = 0.5f),
                        blurRadius = 20f
                    )
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Industrial Safety Intelligence",
                color = VisionColors.OnSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.alpha(subtitleAlpha),
                textAlign = TextAlign.Center
            )
        }
    }
}
