package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Animation rendering quality tier for Phase 4 Realistic Weather Animation Engine.
 */
enum class AnimationQualityLevel(val label: String, val description: String) {
    LOW("Low Quality", "Reduced particles & simplified shaders for older devices"),
    HIGH("High Quality", "Full particle density, volumetric rays & parallax layers")
}

/**
 * Configuration model for the Phase 4 Weather Animation Engine.
 */
data class WeatherAnimationConfig(
    val animationsEnabled: Boolean = true,
    val qualityLevel: AnimationQualityLevel = AnimationQualityLevel.HIGH,
    val batterySaverMode: Boolean = false
) {
    /**
     * Multiplier (0.35f .. 1.0f) applied to rain, snow, star, and cloud layer counts
     * to optimize CPU/GPU and battery usage on low-end devices or in Battery Saver mode.
     */
    val particleDensityMultiplier: Float
        get() = when {
            !animationsEnabled -> 0.35f
            batterySaverMode -> 0.40f
            qualityLevel == AnimationQualityLevel.LOW -> 0.50f
            else -> 1.0f
        }

    /**
     * Frame step interval in milliseconds (~16ms for 60fps High quality, ~33ms for 30fps Low/Battery Saver).
     */
    val targetFrameIntervalMs: Long
        get() = when {
            batterySaverMode -> 36L
            qualityLevel == AnimationQualityLevel.LOW -> 28L
            else -> 16L
        }
}

/**
 * Reusable Glassmorphic Settings Card for configuring Phase 4 Weather Animation Engine options:
 * - Enable / Disable animations
 * - Low / High animation quality
 * - Battery saver mode
 */
@Composable
fun WeatherAnimationSettingsCard(
    settings: WeatherAppSettings,
    onToggleAnimations: (Boolean) -> Unit,
    onSelectAnimationQuality: (AnimationQualityLevel) -> Unit,
    onToggleBatterySaver: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current

    GlassmorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("animation_engine_settings_card"),
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Realistic Weather Animation Engine",
                style = MaterialTheme.typography.titleMedium,
                color = palette.accentCyan
            )

            // 1. Enable / Disable Animations Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(palette.accentCyan.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Animation,
                            contentDescription = null,
                            tint = palette.accentCyan
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.settings_animations_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.primaryText
                        )
                        Text(
                            text = stringResource(R.string.settings_animations_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondaryText
                        )
                    }
                }

                Switch(
                    checked = settings.animationsEnabled,
                    onCheckedChange = onToggleAnimations,
                    modifier = Modifier.testTag("switch_weather_animations"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = palette.accentCyan,
                        uncheckedThumbColor = palette.secondaryText,
                        uncheckedTrackColor = palette.pillBackground
                    )
                )
            }

            HorizontalDivider(color = palette.cardBorderSubtle)

            // 2. Animation Quality (Low vs. High)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.HighQuality,
                        contentDescription = null,
                        tint = palette.accentPurple
                    )
                    Column {
                        Text(
                            text = "Animation Quality",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.primaryText
                        )
                        Text(
                            text = settings.animationQuality.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondaryText
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimationQualityLevel.entries.forEach { quality ->
                        val isSelected = settings.animationQuality == quality
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) palette.accentCyan.copy(alpha = 0.25f)
                                    else palette.pillBackground
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) palette.accentCyan else palette.cardBorderSubtle,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelectAnimationQuality(quality) }
                                .padding(vertical = 12.dp)
                                .testTag("quality_option_${quality.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = quality.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) palette.primaryText else palette.secondaryText
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = palette.cardBorderSubtle)

            // 3. Battery Saver Mode Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(palette.accentEmerald.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BatterySaver,
                            contentDescription = null,
                            tint = palette.accentEmerald
                        )
                    }
                    Column {
                        Text(
                            text = "Battery Saver Mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.primaryText
                        )
                        Text(
                            text = "Limits particle count, disables secondary bokeh & pauses background frame updates",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondaryText
                        )
                    }
                }

                Switch(
                    checked = settings.batterySaverMode,
                    onCheckedChange = onToggleBatterySaver,
                    modifier = Modifier.testTag("switch_battery_saver"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = palette.accentEmerald,
                        uncheckedThumbColor = palette.secondaryText,
                        uncheckedTrackColor = palette.pillBackground
                    )
                )
            }
        }
    }
}
