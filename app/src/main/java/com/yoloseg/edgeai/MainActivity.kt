package com.yoloseg.edgeai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yoloseg.edgeai.analysis.StaticAnalysisActivity
import com.yoloseg.edgeai.camera.LiveCameraActivity
import com.yoloseg.edgeai.settings.SettingsActivity
import com.yoloseg.edgeai.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.yoloseg.edgeai.utils.UiUtils.setImmersiveMode(this)

        setContent {
            MaterialTheme {
                MainScreen(
                    onLiveCameraClick = {
                        startActivity(Intent(this, LiveCameraActivity::class.java))
                    },
                    onStaticAnalysisClick = {
                        startActivity(Intent(this, StaticAnalysisActivity::class.java))
                    },
                    onSettingsClick = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    onLiveCameraClick: () -> Unit,
    onStaticAnalysisClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    AnimatedMeshBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Theme toggle hidden on home dashboard to center title
            VisionTopBar(
                showBack = false,
                onBack = null,
                onSettings = onSettingsClick,
                showThemeToggle = false
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // App icon style 3D rotating ladder
                Animated3dLadderLogo(modifier = Modifier.size(120.dp))

                Spacer(modifier = Modifier.height(16.dp))

                val brandBrush = animatedBrandBrush()
                Text(
                    text = "Workspace",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(brush = brandBrush),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select an operational module to initiate object detection.",
                    color = VisionColors.OnSurfaceVariant,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Card 1: Live Real-Time Detection
                AnimatedGlowCard(
                    glowColor = VisionColors.PrimaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onLiveCameraClick() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Sleek glassmorphic camera module icon
                        SleekLiveVideoIcon()
                        StatusChip(text = "LIVE", chipColor = VisionColors.PrimaryCyan)
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(
                        text = "Real-Time Detection",
                        color = VisionColors.OnSurface,
                        fontSize = 22.sp,
                        style = androidx.compose.ui.text.TextStyle(brush = brandBrush),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Run continuous high-performance safety analysis on live camera feeds.",
                        color = VisionColors.OnSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Card 2: Gallery Analysis
                AnimatedGlowCard(
                    glowColor = VisionColors.SecondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onStaticAnalysisClick() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Sleek glassmorphic gallery photo icon
                        SleekGalleryPhotoIcon()
                        StatusChip(text = "BATCH", chipColor = VisionColors.SecondaryPurple)
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(
                        text = "Gallery Analysis",
                        color = VisionColors.OnSurface,
                        fontSize = 22.sp,
                        style = androidx.compose.ui.text.TextStyle(brush = brandBrush),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Select multi-resolution images from gallery for rapid classification and analysis.",
                        color = VisionColors.OnSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}