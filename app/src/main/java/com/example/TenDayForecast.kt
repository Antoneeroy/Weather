package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Full 10-Day Forecast Glassmorphic List Component (Today + Next 9 Days).
 */
@Composable
fun TenDayForecastCardList(
    tenDays: List<DailyForecastItem>,
    selectedDayId: String?,
    currentTempCelsius: Int?,
    temperatureUnit: TemperatureUnit,
    animationsEnabled: Boolean,
    onSelectDay: (DailyForecastItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current

    if (tenDays.isEmpty()) {
        EmptyForecastGlassState(modifier = modifier)
        return
    }

    val minOverall = tenDays.minOfOrNull { it.minTempCelsius } ?: 0
    val maxOverall = tenDays.maxOfOrNull { it.maxTempCelsius } ?: 35

    GlassmorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ten_day_rows_card"),
        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = palette.accentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Today & Next 9 Days",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.primaryText
                    )
                }
                Text(
                    text = "Tap any day for full details",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.accentCyan
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            tenDays.forEachIndexed { index, item ->
                val isSelected = selectedDayId == item.id
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isSelected) palette.accentCyan.copy(alpha = 0.14f)
                            else Color.Transparent
                        )
                ) {
                    ForecastDayRowItem(
                        day = item,
                        overallMinTemp = minOverall,
                        overallMaxTemp = maxOverall,
                        temperatureUnit = temperatureUnit,
                        animationsEnabled = animationsEnabled,
                        showExtendedDetails = true,
                        currentTempForToday = if (index == 0) currentTempCelsius else null,
                        onClick = { onSelectDay(item) }
                    )
                }
                if (index < tenDays.lastIndex) {
                    HorizontalDivider(
                        color = palette.cardBorderSubtle,
                        thickness = 0.7.dp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Compact 10-Day Forecast Preview Card for the Home Dashboard with "All 10 Days" navigation action.
 */
@Composable
fun HomeTenDayForecastPreviewCard(
    tenDays: List<DailyForecastItem>,
    currentTempCelsius: Int,
    temperatureUnit: TemperatureUnit,
    animationsEnabled: Boolean,
    onOpenFullForecast: () -> Unit,
    onSelectDayDetail: (DailyForecastItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current

    if (tenDays.isEmpty()) {
        EmptyForecastGlassState(modifier = modifier)
        return
    }

    val minOverall = tenDays.minOfOrNull { it.minTempCelsius } ?: 0
    val maxOverall = tenDays.maxOfOrNull { it.maxTempCelsius } ?: 35

    GlassmorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_ten_day_preview_card"),
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ten_day_forecast_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.primaryText
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onOpenFullForecast)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("view_full_10day_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "All 10 Days",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.accentCyan
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open 10-Day Forecast",
                        tint = palette.accentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            tenDays.take(5).forEachIndexed { index, dayItem ->
                ForecastDayRowItem(
                    day = dayItem,
                    overallMinTemp = minOverall,
                    overallMaxTemp = maxOverall,
                    temperatureUnit = temperatureUnit,
                    animationsEnabled = animationsEnabled,
                    showExtendedDetails = false,
                    currentTempForToday = if (index == 0) currentTempCelsius else null,
                    onClick = { onSelectDayDetail(dayItem) }
                )
                if (index < 4 && index < tenDays.lastIndex) {
                    HorizontalDivider(
                        color = palette.cardBorderSubtle,
                        thickness = 0.7.dp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyForecastGlassState(modifier: Modifier = Modifier) {
    val palette = LocalGlassPalette.current
    GlassmorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("empty_forecast_state_card"),
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CloudQueue,
                contentDescription = null,
                tint = palette.accentCyan,
                modifier = Modifier.size(42.dp)
            )
            Text(
                text = "No Forecast Data Available",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText
            )
            Text(
                text = "Pull or tap Refresh to fetch the 10-day forecast from Open-Meteo.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.secondaryText,
                textAlign = TextAlign.Center
            )
        }
    }
}
