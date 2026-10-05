/**
 * ═══════════════════════════════════════════════════════════════════════════════
 *  SettingsActivity.kt — System Configuration & Support
 *
 *  Glass-panel settings with toggles, support links, and session controls.
 *  Matches the "Settings & Support" reference design.
 * ═══════════════════════════════════════════════════════════════════════════════
 */

package com.yoloseg.edgeai.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yoloseg.edgeai.ui.theme.*

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.yoloseg.edgeai.utils.UiUtils.setImmersiveMode(this)

        setContent {
            MaterialTheme {
                SettingsScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    AnimatedMeshBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            VisionTopBar(
                showBack = true,
                onBack = onBack,
                onSettings = null,
                showThemeToggle = true
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                val brandBrush = animatedBrandBrush()
                Text(
                    text = "System Configuration",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(brush = brandBrush),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Manage your AI operational parameters and support channels.",
                    color = VisionColors.OnSurfaceVariant,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ── Interface Mode ──
                GlassPanel(cornerRadius = 20.dp) {
                    Text(
                        text = "INTERFACE MODE",
                        color = VisionColors.SecondaryPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Choose how detection data is displayed during live and gallery analysis.",
                        color = VisionColors.OnSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    InterfaceModeSelectorRow(
                        isFieldMode = VisionThemeManager.isFieldMode,
                        onSelect = { VisionThemeManager.setFieldMode(it) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (VisionThemeManager.isFieldMode) "🏗️ Field: Practical class counts, no technical metrics."
                               else "🔬 Classic: Full FPS, confidence & detection stats.",
                        color = VisionColors.OnSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Model Selection ──
                GlassPanel(cornerRadius = 20.dp) {
                    Text(
                        text = "MODEL SELECTION",
                        color = VisionColors.TertiaryPink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Switch between detection models to compare performance.",
                        color = VisionColors.OnSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ModelSelectorRow(
                        selectedVersion = VisionThemeManager.modelVersion,
                        onSelect = { VisionThemeManager.setModelVersion(it) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ Switching models requires restarting detection screens.",
                        color = VisionColors.OnSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Core Preferences ──
                GlassPanel(cornerRadius = 20.dp) {
                    Text(
                        text = "CORE PREFERENCES",
                        color = VisionColors.PrimaryCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    AnimatedSettingsToggleRow(
                        icon = "🔔",
                        title = "Push Notifications",
                        subtitle = "Alerts for critical safety events",
                        isChecked = VisionThemeManager.notificationsEnabled,
                        accentColor = VisionColors.PrimaryCyan,
                        onCheckedChange = { VisionThemeManager.setNotificationsEnabled(it) }
                    )

                    SettingsDivider()

                    AnimatedSettingsToggleRow(
                        icon = "📊",
                        title = "Data Telemetry",
                        subtitle = "Share anonymized usage metrics",
                        isChecked = VisionThemeManager.telemetryEnabled,
                        accentColor = VisionColors.TertiaryPink,
                        onCheckedChange = { VisionThemeManager.setTelemetryEnabled(it) }
                    )

                    SettingsDivider()

                    AnimatedSettingsToggleRow(
                        icon = "🎯",
                        title = "High Sensitivity",
                        subtitle = "Lower confidence threshold for detections",
                        isChecked = VisionThemeManager.isHighSensitivity,
                        accentColor = VisionColors.ClassHelmet,
                        onCheckedChange = { VisionThemeManager.setHighSensitivity(it) }
                    )

                    SettingsDivider()

                    AnimatedSettingsToggleRow(
                        icon = "🔊",
                        title = "Sound Alerts",
                        subtitle = "Audio feedback on detection",
                        isChecked = VisionThemeManager.soundAlertsEnabled,
                        accentColor = VisionColors.ClassWorker,
                        onCheckedChange = { VisionThemeManager.setSoundAlertsEnabled(it) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Model Information (Dynamic) ──
                GlassPanel(cornerRadius = 20.dp) {
                    Text(
                        text = "MODEL INFORMATION",
                        color = VisionColors.SecondaryPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsInfoRow("Model", VisionThemeManager.modelDisplayName)
                    SettingsInfoRow("Input", "640×640 NCHW Float32")
                    SettingsInfoRow("Classes", VisionThemeManager.classesDisplay)
                    SettingsInfoRow("Sensitivity Threshold", "${String.format("%.1f", VisionThemeManager.detectionThreshold * 100)}%")
                    SettingsInfoRow("Size", VisionThemeManager.modelSize)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Support ──
                AnimatedGlowCard(glowColor = VisionColors.PrimaryCyan, cornerRadius = 20.dp) {
                    // Removed the 🛟 icon above heading as requested
                    Text(
                        text = "Need Assistance?",
                        color = VisionColors.OnSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Our team is ready to help you optimize your deployment.",
                        color = VisionColors.OnSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // GitHub button with SVG icon
                    AnimatedSupportButton(
                        iconContent = { GitHubSvgIcon() },
                        label = "GitHub Repository",
                        borderColor = VisionColors.PrimaryCyan.copy(alpha = 0.4f),
                        onClick = {
                            openUrl(context, "https://github.com/Rohanbhoff")
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // WhatsApp Community button with SVG icon
                    AnimatedSupportButton(
                        iconContent = { WhatsAppSvgIcon() },
                        label = "WhatsApp Community",
                        borderColor = VisionColors.ClassHelmet.copy(alpha = 0.4f),
                        onClick = {
                            openUrl(context, "https://chat.whatsapp.com/IFWw4LpiE28AhMocREDqyJ")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── Terminate Session (Animated Red Cross Icon) ──
                AnimatedTerminateButton {
                    (context as ComponentActivity).finishAffinity()
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  MODEL SELECTOR ROW
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun ModelSelectorRow(selectedVersion: Int, onSelect: (Int) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "modelGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        0.3f, 0.8f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "mGA"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VisionColors.GlassBg)
            .border(1.dp, VisionColors.GlassBorder, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // V1 Button
        ModelOptionChip(
            label = "v1 · 4-class",
            subtitle = "Original",
            isSelected = selectedVersion == 0,
            color = VisionColors.PrimaryCyan,
            glowAlpha = glowAlpha,
            onClick = { onSelect(0) }
        )

        Spacer(Modifier.width(4.dp))

        // V2 Button
        ModelOptionChip(
            label = "v2 · 3-class",
            subtitle = "Retrained",
            isSelected = selectedVersion == 1,
            color = VisionColors.TertiaryPink,
            glowAlpha = glowAlpha,
            onClick = { onSelect(1) }
        )

        Spacer(Modifier.width(4.dp))

        // V3 Button
        ModelOptionChip(
            label = "v3 · 3-class",
            subtitle = "Field+Lab",
            isSelected = selectedVersion == 2,
            color = Color(0xFF10B981),
            glowAlpha = glowAlpha,
            onClick = { onSelect(2) }
        )
    }
}

@Composable
fun RowScope.ModelOptionChip(
    label: String, subtitle: String, isSelected: Boolean,
    color: Color, glowAlpha: Float, onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        if (isSelected) 1f else 0.95f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "chipScale"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(color.copy(alpha = glowAlpha * 0.3f), color.copy(alpha = glowAlpha * 0.1f))
                ) else androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Transparent)
                )
            )
            .border(
                if (isSelected) 1.5.dp else 1.dp,
                if (isSelected) color.copy(alpha = glowAlpha) else VisionColors.GlassBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Selected indicator dot
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.height(6.dp))
        }
        Text(
            label, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            color = if (isSelected) color else VisionColors.OnSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(2.dp))
        Text(
            subtitle, fontSize = 9.sp,
            color = if (isSelected) color.copy(alpha = 0.8f) else VisionColors.OnSurfaceVariant.copy(alpha = 0.6f),
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  INTERFACE MODE SELECTOR (Classic / Field)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun InterfaceModeSelectorRow(isFieldMode: Boolean, onSelect: (Boolean) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "modeGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        0.3f, 0.8f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "modeGA"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VisionColors.GlassBg)
            .border(1.dp, VisionColors.GlassBorder, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Classic chip
        InterfaceModeChip(
            icon = "🔬",
            label = "Classic",
            subtitle = "FPS · Conf · Stats",
            isSelected = !isFieldMode,
            color = VisionColors.PrimaryCyan,
            glowAlpha = glowAlpha,
            onClick = { onSelect(false) }
        )

        Spacer(Modifier.width(4.dp))

        // Field chip
        InterfaceModeChip(
            icon = "🏗️",
            label = "Field",
            subtitle = "Counts · Practical",
            isSelected = isFieldMode,
            color = Color(0xFF10B981),
            glowAlpha = glowAlpha,
            onClick = { onSelect(true) }
        )
    }
}

@Composable
fun RowScope.InterfaceModeChip(
    icon: String, label: String, subtitle: String, isSelected: Boolean,
    color: Color, glowAlpha: Float, onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        if (isSelected) 1f else 0.95f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "modeChipScale"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(color.copy(alpha = glowAlpha * 0.3f), color.copy(alpha = glowAlpha * 0.1f))
                ) else androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Transparent)
                )
            )
            .border(
                if (isSelected) 1.5.dp else 1.dp,
                if (isSelected) color.copy(alpha = glowAlpha) else VisionColors.GlassBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.height(6.dp))
        }
        Text(
            icon, fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            color = if (isSelected) color else VisionColors.OnSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(2.dp))
        Text(
            subtitle, fontSize = 9.sp,
            color = if (isSelected) color.copy(alpha = 0.8f) else VisionColors.OnSurfaceVariant.copy(alpha = 0.6f),
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  ANIMATED TOGGLE ROW (with breathing icon & glowing switch)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun AnimatedSettingsToggleRow(
    icon: String,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "toggleAnim_$title")
    val iconPulse by infiniteTransition.animateFloat(
        0.95f, 1.08f,
        infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "iconPulse"
    )
    val iconRotation by infiniteTransition.animateFloat(
        -3f, 3f,
        infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "iconRot"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        0.1f, 0.3f,
        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "iconGlow"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .scale(iconPulse)
                .rotate(iconRotation)
                .background(accentColor.copy(alpha = glowAlpha), CircleShape)
                .border(1.dp, accentColor.copy(alpha = glowAlpha * 1.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = VisionColors.OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = VisionColors.OnSurfaceVariant, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = VisionColors.Outline,
                uncheckedTrackColor = VisionColors.Outline.copy(alpha = 0.5f)
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  ANIMATED SUPPORT BUTTON (with shimmer border)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun AnimatedSupportButton(
    iconContent: @Composable () -> Unit,
    label: String,
    borderColor: Color,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "supportBtn_$label")
    val shimmer by infiniteTransition.animateFloat(
        0.3f, 1f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "shimmer"
    )
    val iconBounce by infiniteTransition.animateFloat(
        0f, -3f,
        infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "iconBounce"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VisionColors.Surface.copy(alpha = 0.15f))
            .border(1.dp, borderColor.copy(alpha = shimmer), RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.graphicsLayer { translationY = iconBounce }) {
                iconContent()
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = label, color = VisionColors.OnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  ANIMATED TERMINATE BUTTON
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun AnimatedTerminateButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "terminateAnim")
    val pulseScale by infiniteTransition.animateFloat(
        1f, 1.02f,
        infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "termPulse"
    )
    val borderGlow by infiniteTransition.animateFloat(
        0.2f, 0.6f,
        infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "termGlow"
    )
    val crossRotation by infiniteTransition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "crossSpin"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pulseScale)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, VisionColors.Error.copy(alpha = borderGlow), RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.rotate(crossRotation)) {
                RedCrossIcon(modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Terminate Session",
                color = VisionColors.Error,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  HELPER COMPOSABLES (kept from original)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(VisionColors.WhiteDivider)
    )
}

@Composable
fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = VisionColors.OnSurfaceVariant, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = VisionColors.OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SupportButton(iconContent: @Composable () -> Unit, label: String, borderColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VisionColors.Surface.copy(alpha = 0.15f))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            iconContent()
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = label, color = VisionColors.OnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
