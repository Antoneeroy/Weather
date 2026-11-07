package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Horizontal Temperature Range Bar scaled to the 10-day forecast overall min/max range
 * and colored using condition-specific gradients from WmoWeatherCodeMapper.
 */
@Composable
fun ConditionScaledTemperatureRangeBar(
    dayMinCelsius: Int,
    dayMaxCelsius: Int,
    overallMinCelsius: Int,
    overallMaxCelsius: Int,
    condition: WeatherConditionType,
    wmoCode: Int = -1,
    currentTempCelsius: Int? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val totalSpan = (overallMaxCelsius - overallMinCelsius).coerceAtLeast(1).toFloat()
    val startFraction = ((dayMinCelsius - overallMinCelsius) / totalSpan).coerceIn(0f, 0.90f)
    val endFraction = ((dayMaxCelsius - overallMinCelsius) / totalSpan)
        .coerceIn(startFraction + 0.08f, 1f)

    val gradientColors = WmoWeatherCodeMapper.getConditionGradientColors(condition, wmoCode)

    Canvas(
        modifier = modifier
            .height(8.dp)
            .testTag("temperature_range_bar")
    ) {
        val trackW = size.width
        val trackH = size.height
        val corner = CornerRadius(trackH / 2f, trackH / 2f)

        // Background track
        drawRoundRect(
            color = palette.cardBorderSubtle,
            size = Size(trackW, trackH),
            cornerRadius = corner
        )

        val barStartX = trackW * startFraction
        val barWidth = (trackW * (endFraction - startFraction)).coerceAtLeast(trackH)

        // Condition-scaled temperature range fill
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = gradientColors,
                startX = barStartX,
                endX = barStartX + barWidth
            ),
            topLeft = Offset(barStartX, 0f),
            size = Size(barWidth, trackH),
            cornerRadius = corner
        )

        // Optional current temperature indicator dot (for Today)
        if (currentTempCelsius != null) {
            val currentFraction = ((currentTempCelsius - overallMinCelsius) / totalSpan)
                .coerceIn(startFraction, endFraction)
            val dotX = (trackW * currentFraction).coerceIn(trackH / 2f, trackW - trackH / 2f)
            drawCircle(
                color = Color(0xFF0F172A),
                radius = (trackH * 0.72f),
                center = Offset(dotX, trackH / 2f)
            )
            drawCircle(
                color = Color.White,
                radius = (trackH * 0.48f),
                center = Offset(dotX, trackH / 2f)
            )
        }
    }
}

/**
 * Rain Probability & Precipitation Amount (mm) Badge.
 */
@Composable
fun PrecipitationProbabilityBadge(
    rainProbabilityPercent: Int,
    precipitationMm: Double,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(palette.accentCyan.copy(alpha = 0.16f))
            .border(0.8.dp, palette.accentCyan.copy(alpha = 0.45f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.WaterDrop,
            contentDescription = "Rain Probability",
            tint = palette.accentCyan,
            modifier = Modifier.size(11.dp)
        )
        val text = if (precipitationMm > 0.0) {
            "$rainProbabilityPercent% • ${String.format(Locale.US, "%.1f", precipitationMm)}mm"
        } else {
            "$rainProbabilityPercent%"
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = palette.accentCyan,
            maxLines = 1
        )
    }
}

/**
 * Interactive 10-Day Forecast Row Component displaying:
 * - Day name & date
 * - Weather condition icon & WMO condition label
 * - Rain probability % & precipitation sum in mm
 * - Sunrise & sunset micro-summary (on expanded/full row)
 * - Minimum & Maximum temperatures
 * - Condition-colored horizontal temperature range bar
 */
@Composable
fun ForecastDayRowItem(
    day: DailyForecastItem,
    overallMinTemp: Int,
    overallMaxTemp: Int,
    temperatureUnit: TemperatureUnit,
    animationsEnabled: Boolean,
    showExtendedDetails: Boolean = false,
    currentTempForToday: Int? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val isToday = day.dayName.equals("Today", ignoreCase = true)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp)
            .testTag("forecast_row_${day.dayName}"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Day Name, Date & Condition Label
            Column(modifier = Modifier.width(82.dp)) {
                Text(
                    text = day.dayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isToday) palette.accentCyan else palette.primaryText,
                    maxLines = 1
                )
                Text(
                    text = day.dateShort,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.mutedText,
                    maxLines = 1
                )
                Text(
                    text = day.conditionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 2. Weather Condition Icon + Rain Probability / mm
            Column(
                modifier = Modifier.width(72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                WeatherConditionArtworkIcon(
                    condition = day.condition,
                    iconSize = 30.dp,
                    animationsEnabled = animationsEnabled
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.WaterDrop,
                        contentDescription = null,
                        tint = palette.accentCyan,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "${day.rainProbability}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (day.rainProbability > 0) palette.accentCyan else palette.mutedText
                    )
                }
            }

            // 3. Minimum Temperature
            Text(
                text = formatTemperature(day.minTempCelsius, temperatureUnit, includeUnitSymbol = false),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.secondaryText,
                modifier = Modifier.width(34.dp)
            )

            // 4. Scaled Condition-Colored Temperature Range Bar
            ConditionScaledTemperatureRangeBar(
                dayMinCelsius = day.minTempCelsius,
                dayMaxCelsius = day.maxTempCelsius,
                overallMinCelsius = overallMinTemp,
                overallMaxCelsius = overallMaxTemp,
                condition = day.condition,
                wmoCode = day.wmoCode,
                currentTempCelsius = if (isToday) currentTempForToday else null,
                modifier = Modifier.weight(1f)
            )

            // 5. Maximum Temperature
            Text(
                text = formatTemperature(day.maxTempCelsius, temperatureUnit, includeUnitSymbol = false),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText,
                modifier = Modifier.width(36.dp)
            )
        }

        // Extended sub-row for 10-Day Forecast Screen showing Precipitation (mm), Sunrise & Sunset
        if (showExtendedDetails) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WbSunny,
                            contentDescription = "Sunrise",
                            tint = palette.accentGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = day.sunriseTime,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.mutedText
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WbTwilight,
                            contentDescription = "Sunset",
                            tint = Color(0xFFFB7185),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = day.sunsetTime,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.mutedText
                        )
                    }
                }

                Text(
                    text = "Precip: ${String.format(Locale.US, "%.1f", day.precipitationSumMm)} mm",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (day.precipitationSumMm > 0.0) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (day.precipitationSumMm > 0.0) palette.accentCyan else palette.mutedText
                )
            }
        }
    }
}
