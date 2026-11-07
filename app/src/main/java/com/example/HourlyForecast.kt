package com.example

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Full-Day 24-Hour Horizontal Scrolling Forecast (12 AM to 11 PM) with Clickable Hourly Cards
 * and an interactive Detailed Hourly Weather Panel.
 *
 * Displays all 24 hours of the selected day without missing hours:
 * - 12 AM to 11 PM time label
 * - Weather icon
 * - Hourly temperature (°C / °F)
 * - Hourly weather condition
 * - Rain probability (%)
 * - Wind speed (km/h)
 *
 * Clicking any hourly card highlights the selected hour, smoothly scrolls so the selected hour
 * is automatically visible, and updates the Detailed Hourly Weather Panel with:
 * - Selected time
 * - Temperature
 * - Feels-like temperature
 * - Weather condition
 * - Rain probability
 * - Precipitation
 * - Humidity
 * - Wind speed
 * - Wind direction
 */
@Composable
fun HourlyForecastStrip(
    hourlyItems: List<HourlyForecastItem>,
    temperatureUnit: TemperatureUnit,
    animationsEnabled: Boolean,
    title: String = stringResource(R.string.hourly_forecast_title),
    subtitleBadge: String = "12 AM – 11 PM • 24 Hours",
    timeZoneLabel: String? = null,
    selectedDayLabel: String? = null,
    tenDays: List<DailyForecastItem> = emptyList(),
    selectedDayId: String? = null,
    onSelectDay: ((DailyForecastItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val listState = rememberLazyListState()

    // Determine initial hour index: prefer the current hour (`isNow`) if present, else 0 (12 AM)
    val defaultHourIndex = remember(hourlyItems) {
        val nowIdx = hourlyItems.indexOfFirst { it.isNow }
        if (nowIdx >= 0) nowIdx else 0
    }

    var selectedHourIndex by remember(hourlyItems) {
        mutableIntStateOf(defaultHourIndex)
    }

    val safeSelectedIndex = selectedHourIndex.coerceIn(0, (hourlyItems.size - 1).coerceAtLeast(0))
    val selectedHourItem = hourlyItems.getOrNull(safeSelectedIndex)

    // Smoothly scroll so the selected hour is automatically visible whenever selection or city/day changes
    LaunchedEffect(safeSelectedIndex, hourlyItems) {
        if (hourlyItems.isNotEmpty() && safeSelectedIndex in hourlyItems.indices) {
            val targetScrollIndex = (safeSelectedIndex - 1).coerceAtLeast(0)
            listState.animateScrollToItem(index = targetScrollIndex)
        }
    }

    GlassmorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hourly_forecast_card"),
        contentPadding = PaddingValues(vertical = 18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Title + 12 AM - 11 PM Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = palette.accentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                        val subText = buildString {
                            append("12 AM to 11 PM")
                            if (!selectedDayLabel.isNullOrBlank()) {
                                append(" • $selectedDayLabel")
                            }
                            if (!timeZoneLabel.isNullOrBlank()) {
                                append(" ($timeZoneLabel)")
                            }
                        }
                        Text(
                            text = subText,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondaryText
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.accentCyan.copy(alpha = 0.16f))
                        .border(1.dp, palette.accentCyan.copy(alpha = 0.45f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = subtitleBadge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.accentCyan
                    )
                }
            }

            // Optional Day Selector Pills when provided on Home Dashboard
            if (tenDays.isNotEmpty() && onSelectDay != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val dayScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(dayScrollState)
                        .padding(horizontal = 16.dp)
                        .testTag("home_hourly_day_selector"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tenDays.forEach { dayItem ->
                        val isDaySelected = dayItem.id == selectedDayId
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isDaySelected) palette.accentCyan.copy(alpha = 0.25f)
                                    else palette.pillBackground
                                )
                                .border(
                                    width = if (isDaySelected) 1.4.dp else 1.dp,
                                    color = if (isDaySelected) palette.accentCyan else palette.cardBorderSubtle,
                                    shape = CircleShape
                                )
                                .clickable { onSelectDay(dayItem) }
                                .padding(horizontal = 11.dp, vertical = 6.dp)
                                .testTag("home_hourly_day_chip_${dayItem.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            WeatherConditionArtworkIcon(
                                condition = dayItem.condition,
                                iconSize = 15.dp,
                                animationsEnabled = false
                            )
                            Text(
                                text = "${dayItem.dayName} (${dayItem.dateShort})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isDaySelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isDaySelected) palette.primaryText else palette.secondaryText
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (hourlyItems.isEmpty()) {
                Text(
                    text = "Hourly forecast data is currently unavailable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.mutedText,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                )
            } else {
                // Horizontal scrolling row for all 24 hours (12 AM to 11 PM)
                LazyRow(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hourly_forecast_row"),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(
                        items = hourlyItems,
                        key = { _, item -> item.id }
                    ) { index, item ->
                        val isSelected = index == safeSelectedIndex
                        HourlyForecastPill(
                            item = item,
                            isSelected = isSelected,
                            temperatureUnit = temperatureUnit,
                            animationsEnabled = animationsEnabled,
                            onClick = {
                                selectedHourIndex = index
                            }
                        )
                    }
                }

                // Detailed Hourly Weather Panel for the selected hour
                if (selectedHourItem != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        color = palette.cardBorderSubtle
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    AnimatedContent(
                        targetState = selectedHourItem,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "selected_hour_detail_panel"
                    ) { activeHour ->
                        SelectedHourDetailPanel(
                            item = activeHour,
                            hourIndex = safeSelectedIndex,
                            totalHours = hourlyItems.size,
                            temperatureUnit = temperatureUnit,
                            animationsEnabled = animationsEnabled,
                            selectedDayLabel = selectedDayLabel,
                            timeZoneLabel = timeZoneLabel,
                            onPreviousHour = {
                                if (safeSelectedIndex > 0) {
                                    selectedHourIndex = safeSelectedIndex - 1
                                }
                            },
                            onNextHour = {
                                if (safeSelectedIndex < hourlyItems.lastIndex) {
                                    selectedHourIndex = safeSelectedIndex + 1
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HourlyForecastPill(
    item: HourlyForecastItem,
    isSelected: Boolean,
    temperatureUnit: TemperatureUnit,
    animationsEnabled: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val pillShape = RoundedCornerShape(24.dp)

    val containerBrush = when {
        isSelected -> Brush.verticalGradient(
            colors = listOf(
                palette.accentCyan.copy(alpha = 0.42f),
                palette.accentPurple.copy(alpha = 0.30f)
            )
        )
        item.isNow -> Brush.verticalGradient(
            colors = listOf(
                palette.accentCyan.copy(alpha = 0.22f),
                palette.cardSurfaceBottom.copy(alpha = 0.14f)
            )
        )
        else -> Brush.verticalGradient(
            colors = listOf(
                palette.cardSurfaceTop.copy(alpha = 0.15f),
                palette.cardSurfaceBottom.copy(alpha = 0.06f)
            )
        )
    }

    val borderColor = when {
        isSelected -> palette.accentCyan
        item.isNow -> palette.accentCyan.copy(alpha = 0.65f)
        else -> palette.cardBorderSubtle
    }

    val borderWidth = when {
        isSelected -> 2.dp
        item.isNow -> 1.3.dp
        else -> 1.dp
    }

    val displayClockLabel = item.clockTimeLabel.ifBlank { item.timeLabel }

    Column(
        modifier = Modifier
            .width(102.dp)
            .clip(pillShape)
            .background(brush = containerBrush, shape = pillShape)
            .border(width = borderWidth, color = borderColor, shape = pillShape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .testTag("hourly_item_$displayClockLabel"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Selected / Current Hour Top Indicator Badge
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    when {
                        isSelected -> palette.accentCyan.copy(alpha = 0.28f)
                        item.isNow -> palette.accentEmerald.copy(alpha = 0.24f)
                        else -> Color.Transparent
                    }
                )
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Text(
                text = when {
                    isSelected && item.isNow -> "SELECTED • NOW"
                    isSelected -> "SELECTED"
                    item.isNow -> "NOW"
                    else -> String.format(Locale.US, "%02d:00", item.hour24)
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected || item.isNow) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> palette.primaryText
                    item.isNow -> palette.accentEmerald
                    else -> palette.mutedText
                },
                maxLines = 1
            )
        }

        // 1. Time Label (12 AM to 11 PM)
        Text(
            text = displayClockLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected || item.isNow) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isSelected) palette.primaryText else if (item.isNow) palette.accentCyan else palette.secondaryText,
            maxLines = 1
        )

        // 2. Weather Icon
        WeatherConditionArtworkIcon(
            condition = item.condition,
            iconSize = 34.dp,
            animationsEnabled = animationsEnabled && isSelected
        )

        // 3. Hourly Temperature
        Text(
            text = formatTemperature(item.tempCelsius, temperatureUnit),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.primaryText
        )

        // 4. Hourly Weather Condition Label
        Text(
            text = item.conditionLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) palette.primaryText else palette.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        // 5. Rain Probability (%)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.WaterDrop,
                contentDescription = "Rain Probability",
                tint = palette.accentCyan,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = "${item.popPercent}%",
                style = MaterialTheme.typography.labelSmall,
                color = if (item.popPercent > 0) palette.accentCyan else palette.mutedText,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }

        // 6. Wind Speed (km/h)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Air,
                contentDescription = "Wind Speed",
                tint = palette.secondaryText,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = "${item.windSpeedKmh} km/h",
                style = MaterialTheme.typography.labelSmall,
                color = palette.mutedText,
                maxLines = 1
            )
        }
    }
}

/**
 * Detailed Hourly Weather Panel displayed when the user clicks any of the 24 hourly forecast cards.
 * Shows all 9 required meteorological fields:
 * 1. Selected time
 * 2. Temperature
 * 3. Feels-like temperature
 * 4. Weather condition
 * 5. Rain probability
 * 6. Precipitation
 * 7. Humidity
 * 8. Wind speed
 * 9. Wind direction
 */
@Composable
private fun SelectedHourDetailPanel(
    item: HourlyForecastItem,
    hourIndex: Int,
    totalHours: Int,
    temperatureUnit: TemperatureUnit,
    animationsEnabled: Boolean,
    selectedDayLabel: String?,
    timeZoneLabel: String?,
    onPreviousHour: () -> Unit,
    onNextHour: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val panelShape = RoundedCornerShape(24.dp)
    val displayClockLabel = item.clockTimeLabel.ifBlank { item.timeLabel }
    val formatted24h = String.format(Locale.US, "%02d:00", item.hour24)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        palette.accentCyan.copy(alpha = 0.16f),
                        palette.accentPurple.copy(alpha = 0.12f),
                        palette.cardSurfaceBottom.copy(alpha = 0.18f)
                    )
                )
            )
            .border(1.2.dp, palette.accentCyan.copy(alpha = 0.55f), panelShape)
            .padding(16.dp)
            .testTag("hourly_detail_panel"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Bar: Selected Time + Prev/Next Hour Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.TouchApp,
                    contentDescription = null,
                    tint = palette.accentCyan,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Selected Time: $displayClockLabel ($formatted24h)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText,
                            modifier = Modifier.testTag("hourly_detail_selected_time")
                        )
                        if (item.isNow) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(palette.accentEmerald.copy(alpha = 0.22f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Current Hour",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accentEmerald
                                )
                            }
                        }
                    }
                    val metaLine = buildString {
                        append("Hour ${hourIndex + 1} of $totalHours")
                        if (!selectedDayLabel.isNullOrBlank()) {
                            append(" • $selectedDayLabel")
                        }
                        if (!timeZoneLabel.isNullOrBlank()) {
                            append(" • $timeZoneLabel")
                        }
                    }
                    Text(
                        text = metaLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondaryText
                    )
                }
            }

            // Step Previous / Next Hour Buttons (auto-scrolls the 24-hour LazyRow)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onPreviousHour,
                    enabled = hourIndex > 0,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(palette.pillBackground)
                        .border(1.dp, palette.cardBorderSubtle, CircleShape)
                        .testTag("hourly_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Hour",
                        tint = if (hourIndex > 0) palette.primaryText else palette.mutedText,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onNextHour,
                    enabled = hourIndex < totalHours - 1,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(palette.pillBackground)
                        .border(1.dp, palette.cardBorderSubtle, CircleShape)
                        .testTag("hourly_next_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Hour",
                        tint = if (hourIndex < totalHours - 1) palette.primaryText else palette.mutedText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Primary Summary Row: Weather Icon + Temperature + Feels-Like Temperature + Weather Condition
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.pillBackground)
                .border(1.dp, palette.cardBorderSubtle, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WeatherConditionArtworkIcon(
                    condition = item.condition,
                    iconSize = 48.dp,
                    animationsEnabled = animationsEnabled
                )
                Column {
                    Text(
                        text = formatTemperature(item.tempCelsius, temperatureUnit),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText,
                        modifier = Modifier.testTag("hourly_detail_temperature")
                    )
                    Text(
                        text = "Feels like ${formatTemperature(item.feelsLikeCelsius, temperatureUnit)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.accentCyan,
                        modifier = Modifier.testTag("hourly_detail_feels_like")
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Weather Condition",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.mutedText
                )
                Text(
                    text = item.conditionLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText,
                    textAlign = TextAlign.End,
                    modifier = Modifier.testTag("hourly_detail_condition")
                )
                Text(
                    text = item.condition.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.secondaryText
                )
            }
        }

        // 6-Metric Grid: Rain Probability, Precipitation, Humidity, Wind Speed, Wind Direction, Feels-Like
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HourlyDetailMetricChip(
                    icon = Icons.Filled.WaterDrop,
                    label = "Rain Probability",
                    value = "${item.popPercent}%",
                    accentColor = palette.accentCyan,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hourly_detail_rain_prob")
                )
                HourlyDetailMetricChip(
                    icon = Icons.Filled.Grain,
                    label = "Precipitation",
                    value = "${String.format(Locale.US, "%.1f", item.precipitationMm)} mm",
                    accentColor = Color(0xFF60A5FA),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hourly_detail_precipitation")
                )
                HourlyDetailMetricChip(
                    icon = Icons.Filled.WaterDrop,
                    label = "Humidity",
                    value = "${item.relativeHumidityPercent}%",
                    accentColor = palette.accentEmerald,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hourly_detail_humidity")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HourlyDetailMetricChip(
                    icon = Icons.Filled.Air,
                    label = "Wind Speed",
                    value = "${item.windSpeedKmh} km/h",
                    accentColor = Color(0xFF818CF8),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hourly_detail_wind_speed")
                )
                HourlyDetailMetricChip(
                    icon = Icons.Filled.Explore,
                    label = "Wind Direction",
                    value = "${item.windDirectionCompass} (${item.windDirectionDegrees}°)",
                    accentColor = palette.accentGold,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hourly_detail_wind_direction")
                )
                HourlyDetailMetricChip(
                    icon = Icons.Filled.Thermostat,
                    label = "Feels-Like",
                    value = formatTemperature(item.feelsLikeCelsius, temperatureUnit),
                    accentColor = palette.accentPurple,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hourly_detail_feels_like_chip")
                )
            }
        }
    }
}

@Composable
private fun HourlyDetailMetricChip(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.pillBackground)
            .border(1.dp, palette.cardBorderSubtle, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = palette.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = palette.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
