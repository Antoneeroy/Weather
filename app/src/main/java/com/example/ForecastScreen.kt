package com.example

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Dedicated Premium 10-Day Forecast Screen (Phase 3–5).
 * Connects to ForecastDetailScreen when a user taps any forecast day row,
 * and allows switching the 24-hour forecast strip to any of the 10 days.
 */
@Composable
fun ForecastScreen(
    uiState: WeatherUiState,
    onSelectDay: (DailyForecastItem) -> Unit,
    onCloseDayDetail: () -> Unit,
    onRefresh: () -> Unit,
    onBackToHome: () -> Unit,
    onOpenCities: () -> Unit = {},
    onRequestGps: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val city = uiState.activeCity
    if (city == null) {
        BackHandler(onBack = onBackToHome)
        val palette = LocalGlassPalette.current
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .testTag("forecast_no_city_card"),
                shape = RoundedCornerShape(28.dp),
                contentPadding = PaddingValues(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = palette.accentCyan,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "No Location Selected",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Select a city or detect your GPS location to view the 10-day and 24-hour forecast.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.secondaryText,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(palette.accentCyan.copy(alpha = 0.24f))
                                .border(1.dp, palette.accentCyan, CircleShape)
                                .clickable(onClick = onRequestGps)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MyLocation,
                                contentDescription = null,
                                tint = palette.accentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Use My GPS",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentCyan
                            )
                        }
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(palette.pillBackground)
                                .border(1.dp, palette.cardBorderLight, CircleShape)
                                .clickable(onClick = onOpenCities)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = palette.primaryText,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Cities & Search",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.primaryText
                            )
                        }
                    }
                }
            }
        }
        return
    }

    val unit = uiState.settings.temperatureUnit
    val animationsEnabled = uiState.settings.animationsEnabled
    val tenDays = city.tenDayForecast

    val minOverall = tenDays.minOfOrNull { it.minTempCelsius } ?: 0
    val maxOverall = tenDays.maxOfOrNull { it.maxTempCelsius } ?: 35
    val detailDay = uiState.selectedDayForDetail

    // Tracks which day's 24-hour forecast (12 AM to 11 PM) is previewed on the main 10-Day screen (defaults to Today)
    var selectedHourlyPreviewDayId by remember(city.id, detailDay?.id) {
        mutableStateOf(detailDay?.id ?: tenDays.firstOrNull()?.id)
    }
    val activeHourlyPreviewDay = tenDays.find { it.id == selectedHourlyPreviewDayId }
        ?: tenDays.firstOrNull()

    val hourlyItemsToShow = remember(activeHourlyPreviewDay, city.hourlyForecast) {
        if (activeHourlyPreviewDay != null) {
            if (activeHourlyPreviewDay.hourlyItemsForDay.isNotEmpty()) {
                activeHourlyPreviewDay.hourlyItemsForDay
            } else {
                SampleWeatherRepository.generate24HoursForDay(
                    dayId = activeHourlyPreviewDay.id,
                    minTempCelsius = activeHourlyPreviewDay.minTempCelsius,
                    maxTempCelsius = activeHourlyPreviewDay.maxTempCelsius,
                    condition = activeHourlyPreviewDay.condition,
                    wmoCode = activeHourlyPreviewDay.wmoCode,
                    rainProbability = activeHourlyPreviewDay.rainProbability,
                    precipitationSumMm = activeHourlyPreviewDay.precipitationSumMm,
                    windKmh = activeHourlyPreviewDay.windKmh,
                    humidityPercent = activeHourlyPreviewDay.humidityPercent,
                    isToday = activeHourlyPreviewDay.dayName.equals("Today", ignoreCase = true)
                )
            }
        } else {
            city.hourlyForecast
        }
    }

    val hourlyTitle = if (activeHourlyPreviewDay != null) {
        "${activeHourlyPreviewDay.dayName} (${activeHourlyPreviewDay.dateShort}) • Hourly Forecast"
    } else {
        stringResource(R.string.hourly_forecast_title)
    }

    val hourlyBadge = "12 AM – 11 PM • ${hourlyItemsToShow.size} Hours"

    // If Day Detail Screen is active, render ForecastDetailScreen with back handling
    AnimatedContent(
        targetState = uiState.isShowingDayDetailScreen && detailDay != null,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "forecast_detail_switch"
    ) { showDetail ->
        if (showDetail && detailDay != null) {
            ForecastDetailScreen(
                cityName = city.cityName,
                regionAndCountry = city.regionAndCountry,
                day = detailDay,
                overallMinTemp = minOverall,
                overallMaxTemp = maxOverall,
                temperatureUnit = unit,
                animationsEnabled = animationsEnabled,
                onBack = onCloseDayDetail,
                tenDays = tenDays,
                onSelectAnotherDay = { clickedDay ->
                    selectedHourlyPreviewDayId = clickedDay.id
                    onSelectDay(clickedDay)
                },
                modifier = modifier
            )
        } else {
            BackHandler(onBack = onBackToHome)

            val palette = LocalGlassPalette.current

            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                        .testTag("ten_day_forecast_screen_list"),
                    contentPadding = PaddingValues(
                        start = 18.dp,
                        end = 18.dp,
                        top = 14.dp,
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Header with City Name, Timezone, and Refresh / Loading Indicator
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.ten_day_forecast_title),
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = palette.primaryText
                                )
                                val dataBadge = when {
                                    city.isLiveData -> "Live Open-Meteo"
                                    city.isCachedOfflineData -> "Offline Cached (${city.cachedTimestampLabel ?: "Saved"})"
                                    else -> city.timeZoneOffsetLabel
                                }
                                Text(
                                    text = "${city.cityName} • ${city.regionAndCountry} ($dataBadge)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.secondaryText
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(44.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    this@Row.AnimatedVisibility(
                                        visible = uiState.isRefreshing,
                                        enter = fadeIn(),
                                        exit = fadeOut()
                                    ) {
                                        WeatherOrbitalLoadingIndicator(sizeDp = 34.dp)
                                    }

                                    this@Row.AnimatedVisibility(
                                        visible = !uiState.isRefreshing,
                                        enter = fadeIn(),
                                        exit = fadeOut()
                                    ) {
                                        IconButton(
                                            onClick = onRefresh,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(palette.pillBackground)
                                                .border(1.dp, palette.cardBorderLight, CircleShape)
                                                .testTag("forecast_refresh_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Refresh,
                                                contentDescription = stringResource(R.string.refresh_weather),
                                                tint = palette.accentCyan
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(palette.pillBackground)
                                        .border(1.dp, palette.cardBorderLight, RoundedCornerShape(15.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarMonth,
                                        contentDescription = null,
                                        tint = palette.accentCyan
                                    )
                                }
                            }
                        }
                    }

                    // 2. Live API Sync Error / No Internet State with Retry Button
                    item {
                        ForecastSyncBanner(
                            syncState = uiState.syncState,
                            isCachedOfflineData = city.isCachedOfflineData,
                            cachedTimestamp = city.cachedTimestampLabel,
                            onRetry = onRefresh
                        )
                    }

                    // 2b. Active Weather Alerts (if any)
                    if (uiState.activeAlerts.isNotEmpty()) {
                        item {
                            WeatherAlertsSection(alerts = uiState.activeAlerts)
                        }
                    }

                    // 3. Day Selector Strip for Hourly Forecast + 24-Hour Horizontal Scrolling Strip
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (tenDays.isNotEmpty()) {
                                val chipScrollState = rememberScrollState()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(chipScrollState)
                                        .testTag("forecast_day_hourly_chip_row"),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isNext24Selected = selectedHourlyPreviewDayId == null
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (isNext24Selected) palette.accentCyan.copy(alpha = 0.25f)
                                                else palette.pillBackground
                                            )
                                            .border(
                                                width = if (isNext24Selected) 1.5.dp else 1.dp,
                                                color = if (isNext24Selected) palette.accentCyan else palette.cardBorderSubtle,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedHourlyPreviewDayId = null }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Next 24 Hours",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isNext24Selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isNext24Selected) palette.primaryText else palette.secondaryText
                                        )
                                    }

                                    tenDays.forEach { dayItem ->
                                        val isDaySelected = selectedHourlyPreviewDayId == dayItem.id
                                        Row(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(
                                                    if (isDaySelected) palette.accentCyan.copy(alpha = 0.25f)
                                                    else palette.pillBackground
                                                )
                                                .border(
                                                    width = if (isDaySelected) 1.5.dp else 1.dp,
                                                    color = if (isDaySelected) palette.accentCyan else palette.cardBorderSubtle,
                                                    shape = CircleShape
                                                )
                                                .clickable { selectedHourlyPreviewDayId = dayItem.id }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                                .testTag("hourly_day_chip_${dayItem.id}"),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            WeatherConditionArtworkIcon(
                                                condition = dayItem.condition,
                                                iconSize = 16.dp,
                                                animationsEnabled = false
                                            )
                                            Text(
                                                text = "${dayItem.dayName} (${dayItem.dateShort})",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isDaySelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isDaySelected) palette.primaryText else palette.secondaryText
                                            )
                                        }
                                    }
                                }
                            }

                            HourlyForecastStrip(
                                hourlyItems = hourlyItemsToShow,
                                temperatureUnit = unit,
                                animationsEnabled = animationsEnabled,
                                title = hourlyTitle,
                                subtitleBadge = hourlyBadge,
                                timeZoneLabel = city.timeZoneOffsetLabel,
                                selectedDayLabel = activeHourlyPreviewDay?.let { "${it.dayName}, ${it.dateShort}" }
                            )
                        }
                    }

                    // 4. Full 10-Day Forecast List (Today + Next 9 Days)
                    item {
                        TenDayForecastCardList(
                            tenDays = tenDays,
                            selectedDayId = selectedHourlyPreviewDayId ?: detailDay?.id,
                            currentTempCelsius = city.currentTempCelsius,
                            temperatureUnit = unit,
                            animationsEnabled = animationsEnabled,
                            onSelectDay = { clickedDay ->
                                selectedHourlyPreviewDayId = clickedDay.id
                                onSelectDay(clickedDay)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastSyncBanner(
    syncState: WeatherDataSyncState,
    isCachedOfflineData: Boolean,
    cachedTimestamp: String?,
    onRetry: () -> Unit
) {
    val palette = LocalGlassPalette.current

    when (syncState) {
        is WeatherDataSyncState.Loading -> {
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WeatherOrbitalLoadingIndicator(sizeDp = 24.dp)
                    Text(
                        text = "Loading real 10-day & 24-hour forecast from Open-Meteo…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.primaryText
                    )
                }
            }
        }

        is WeatherDataSyncState.NoInternet -> {
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("forecast_no_internet_card"),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = palette.accentGold
                        )
                        Column {
                            Text(
                                text = if (isCachedOfflineData) {
                                    "Offline Mode • Showing Cached Forecast"
                                } else {
                                    stringResource(R.string.no_internet_title)
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryText
                            )
                            Text(
                                text = if (isCachedOfflineData && cachedTimestamp != null) {
                                    "Last updated at $cachedTimestamp • Connect to internet to refresh"
                                } else {
                                    syncState.message
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.secondaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(palette.accentCyan.copy(alpha = 0.25f))
                            .border(1.dp, palette.accentCyan, CircleShape)
                            .clickable(onClick = onRetry)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("forecast_retry_button")
                    ) {
                        Text(
                            text = stringResource(R.string.retry_action),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentCyan
                        )
                    }
                }
            }
        }

        is WeatherDataSyncState.Error -> {
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("forecast_error_card"),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFFB7185)
                        )
                        Text(
                            text = syncState.message,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryText
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(palette.accentCyan.copy(alpha = 0.25f))
                            .border(1.dp, palette.accentCyan, CircleShape)
                            .clickable(onClick = onRetry)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.retry_action),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentCyan
                        )
                    }
                }
            }
        }

        is WeatherDataSyncState.Success -> Unit
    }
}
