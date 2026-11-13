package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun WeatherHomeScreen(
    uiState: WeatherUiState,
    onRefresh: () -> Unit,
    onRequestGpsWeather: () -> Unit,
    onSelectConditionOverride: (WeatherConditionType?) -> Unit,
    onSelectSceneOverride: (RealisticWeatherScene?) -> Unit = {},
    onOpenFullForecast: () -> Unit,
    onOpenCities: () -> Unit,
    onSelectDayDetail: (DailyForecastItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val city = uiState.activeCity
    val unit = uiState.settings.temperatureUnit
    val animationsEnabled = uiState.settings.animationsEnabled
    val effectiveCondition = uiState.effectiveCondition
    val activeScene = uiState.activeSceneParameters.scene

    // Track which day's 24-hour forecast (12 AM to 11 PM) is displayed on the Home Dashboard (defaults to Today)
    var selectedHourlyDayId by remember(city?.id) {
        mutableStateOf(city?.tenDayForecast?.firstOrNull()?.id)
    }
    val activeHourlyDay = city?.tenDayForecast?.find { it.id == selectedHourlyDayId }
        ?: city?.tenDayForecast?.firstOrNull()

    val fullDay24Hours = remember(activeHourlyDay, city?.hourlyForecast) {
        val cityHourly = city?.hourlyForecast.orEmpty()
        when {
            activeHourlyDay != null && activeHourlyDay.hourlyItemsForDay.size == 24 -> {
                activeHourlyDay.hourlyItemsForDay
            }
            cityHourly.size == 24 -> {
                cityHourly
            }
            activeHourlyDay != null -> {
                SampleWeatherRepository.generate24HoursForDay(
                    dayId = activeHourlyDay.id,
                    minTempCelsius = activeHourlyDay.minTempCelsius,
                    maxTempCelsius = activeHourlyDay.maxTempCelsius,
                    condition = activeHourlyDay.condition,
                    wmoCode = activeHourlyDay.wmoCode,
                    rainProbability = activeHourlyDay.rainProbability,
                    precipitationSumMm = activeHourlyDay.precipitationSumMm,
                    windKmh = activeHourlyDay.windKmh,
                    humidityPercent = activeHourlyDay.humidityPercent,
                    isToday = activeHourlyDay.dayName.equals("Today", ignoreCase = true)
                )
            }
            else -> cityHourly
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .testTag("home_dashboard_list"),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 12.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Header Bar with App Name, Logo, GPS Button, City Selector & Refresh Button
            item {
                HomeTopAppBar(
                    cityName = city?.cityName ?: "Select City",
                    hasActiveCity = city != null,
                    isLiveData = city?.isLiveData == true,
                    isCachedOfflineData = city?.isCachedOfflineData == true,
                    cachedTimestampLabel = city?.cachedTimestampLabel,
                    lastUpdatedTime = uiState.lastUpdatedTime,
                    isRefreshing = uiState.isRefreshing,
                    animationsEnabled = animationsEnabled,
                    condition = effectiveCondition,
                    onOpenCities = onOpenCities,
                    onRequestGpsWeather = onRequestGpsWeather,
                    onRefresh = onRefresh
                )
            }

            // 2. Live API Sync State / No Internet / Offline Cache / Error / GPS Status Banner
            item {
                LiveSyncAndGpsStatusSection(
                    syncState = uiState.syncState,
                    isCachedOfflineData = city?.isCachedOfflineData == true,
                    cachedTimestampLabel = city?.cachedTimestampLabel,
                    gpsStatus = uiState.gpsStatus,
                    onRetry = onRefresh,
                    onRequestGps = onRequestGpsWeather,
                    onSearchCityManually = onOpenCities
                )
            }

            // 2b. Phase 5 Estimated Weather Advisories (from real Open-Meteo telemetry)
            if (uiState.activeAlerts.isNotEmpty()) {
                item {
                    WeatherAlertsSection(alerts = uiState.activeAlerts)
                }
            }

            // 3. Interactive 10-Scene Realistic Weather Animation Switcher
            item {
                ConditionPreviewPillStrip(
                    activeScene = activeScene,
                    hasOverride = uiState.sceneOverride != null || uiState.weatherOverride != null,
                    onSelectScene = onSelectSceneOverride,
                    onResetOverride = {
                        onSelectSceneOverride(null)
                        onSelectConditionOverride(null)
                    }
                )
            }

            if (city == null) {
                item {
                    NoCitySelectedGlassCard(
                        onRequestGps = onRequestGpsWeather,
                        onOpenCities = onOpenCities
                    )
                }
            } else {
                // 4. Hero Weather Display (City, Location Icon, Date/Time, Large Temp, Condition, Feels-Like, H/L)
                item {
                    HeroCurrentWeatherSection(
                        city = city,
                        effectiveCondition = effectiveCondition,
                        temperatureUnit = unit,
                        animationsEnabled = animationsEnabled,
                        lastUpdatedTime = uiState.lastUpdatedTime,
                        onCityTap = onOpenCities
                    )
                }

                // 5. Full-Day 24-Hour Horizontal Hourly Forecast Strip (12 AM to 11 PM) + Clickable Detail Panel
                item {
                    HourlyForecastStrip(
                        hourlyItems = fullDay24Hours,
                        temperatureUnit = unit,
                        animationsEnabled = animationsEnabled,
                        title = if (activeHourlyDay != null) {
                            "${activeHourlyDay.dayName} (${activeHourlyDay.dateShort}) • Hourly Forecast"
                        } else {
                            stringResource(R.string.hourly_forecast_title)
                        },
                        subtitleBadge = "12 AM – 11 PM • ${fullDay24Hours.size} Hours",
                        timeZoneLabel = city.timeZoneOffsetLabel,
                        selectedDayLabel = activeHourlyDay?.let { "${it.dayName}, ${it.dateShort}" },
                        tenDays = city.tenDayForecast,
                        selectedDayId = activeHourlyDay?.id,
                        onSelectDay = { clickedDay ->
                            selectedHourlyDayId = clickedDay.id
                        }
                    )
                }

                // 6. 10-Day Forecast Preview Card with button to open full 10-Day screen or clicked day's hourly detail
                item {
                    HomeTenDayForecastPreviewCard(
                        tenDays = city.tenDayForecast,
                        currentTempCelsius = city.currentTempCelsius,
                        temperatureUnit = unit,
                        animationsEnabled = animationsEnabled,
                        onOpenFullForecast = onOpenFullForecast,
                        onSelectDayDetail = { selectedDay ->
                            onSelectDayDetail(selectedDay)
                        }
                    )
                }

                // 7. All 7 Individual Atmospheric Weather Information Glass Cards
                item {
                    WeatherInformationCardsSection(
                        details = city.details,
                        temperatureUnit = unit
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTopAppBar(
    cityName: String,
    hasActiveCity: Boolean,
    isLiveData: Boolean,
    isCachedOfflineData: Boolean,
    cachedTimestampLabel: String?,
    lastUpdatedTime: String?,
    isRefreshing: Boolean,
    animationsEnabled: Boolean,
    condition: WeatherConditionType,
    onOpenCities: () -> Unit,
    onRequestGpsWeather: () -> Unit,
    onRefresh: () -> Unit
) {
    val palette = LocalGlassPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_top_bar"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Logo + Brand Name + Live API / Offline Cache Status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF38BDF8).copy(alpha = 0.28f),
                                Color(0xFFA855F7).copy(alpha = 0.28f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = palette.cardBorderLight,
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                WeatherConditionArtworkIcon(
                    condition = condition,
                    iconSize = 30.dp,
                    animationsEnabled = animationsEnabled
                )
            }

            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val statusIcon = when {
                        isLiveData -> Icons.Filled.Verified
                        isCachedOfflineData -> Icons.Filled.CloudOff
                        else -> Icons.Filled.CloudSync
                    }
                    val statusTint = when {
                        isLiveData -> palette.accentEmerald
                        isCachedOfflineData -> palette.accentGold
                        else -> palette.accentCyan
                    }
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusTint,
                        modifier = Modifier.size(13.dp)
                    )
                    val statusText = when {
                        !hasActiveCity -> "Choose GPS or search a city"
                        isLiveData && lastUpdatedTime != null -> "Live • $lastUpdatedTime"
                        isLiveData -> stringResource(R.string.live_api_badge)
                        isCachedOfflineData && cachedTimestampLabel != null -> "Offline Cache • $cachedTimestampLabel"
                        isCachedOfflineData -> stringResource(R.string.cached_offline_badge)
                        else -> stringResource(R.string.sample_data_badge)
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Right Actions: City Quick Pill + GPS Location Refresh Button + Weather Refresh / Orbital Loader
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .widthIn(max = 136.dp)
                    .clip(CircleShape)
                    .background(palette.pillBackground)
                    .border(1.dp, palette.cardBorderLight, CircleShape)
                    .clickable(onClick = onOpenCities)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("header_city_pill"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = "Selected City",
                    tint = palette.accentCyan,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = cityName,
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onRequestGpsWeather,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(palette.pillBackground)
                    .border(1.dp, palette.cardBorderSubtle, CircleShape)
                    .testTag("gps_location_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = stringResource(R.string.detect_gps_location),
                    tint = palette.accentCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                this@Row.AnimatedVisibility(
                    visible = isRefreshing,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    WeatherOrbitalLoadingIndicator(sizeDp = 34.dp)
                }

                this@Row.AnimatedVisibility(
                    visible = !isRefreshing,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(palette.pillBackground)
                            .border(1.dp, palette.cardBorderSubtle, CircleShape)
                            .testTag("refresh_weather_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.refresh_weather),
                            tint = palette.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveSyncAndGpsStatusSection(
    syncState: WeatherDataSyncState,
    isCachedOfflineData: Boolean,
    cachedTimestampLabel: String?,
    gpsStatus: GpsLocationStatus,
    onRetry: () -> Unit,
    onRequestGps: () -> Unit,
    onSearchCityManually: () -> Unit
) {
    val palette = LocalGlassPalette.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1. Live API Loading / No Internet / Error Card
        when (syncState) {
            is WeatherDataSyncState.Loading -> {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("live_sync_loading_banner"),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WeatherOrbitalLoadingIndicator(sizeDp = 26.dp)
                        Column {
                            Text(
                                text = "Syncing real-time weather with Open-Meteo…",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.primaryText
                            )
                            Text(
                                text = "Fetching current conditions, hourly & 10-day forecast",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.secondaryText
                            )
                        }
                    }
                }
            }

            is WeatherDataSyncState.NoInternet -> {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("no_internet_banner"),
                    shape = RoundedCornerShape(22.dp),
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
                                contentDescription = "No Internet",
                                tint = palette.accentGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = if (isCachedOfflineData) {
                                        stringResource(R.string.cached_offline_badge)
                                    } else {
                                        stringResource(R.string.no_internet_title)
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryText
                                )
                                Text(
                                    text = if (isCachedOfflineData && cachedTimestampLabel != null) {
                                        "Last updated at $cachedTimestampLabel • Showing cached offline weather (not live)"
                                    } else {
                                        syncState.message
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.secondaryText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(palette.accentCyan.copy(alpha = 0.25f))
                                .border(1.dp, palette.accentCyan, CircleShape)
                                .clickable(onClick = onRetry)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("retry_sync_button")
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
                        .testTag("api_error_banner"),
                    shape = RoundedCornerShape(22.dp),
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
                                contentDescription = "API Error",
                                tint = Color(0xFFFB7185),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Weather Update Issue",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryText
                                )
                                Text(
                                    text = syncState.message,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.secondaryText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(palette.accentCyan.copy(alpha = 0.25f))
                                .border(1.dp, palette.accentCyan, CircleShape)
                                .clickable(onClick = onRetry)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("retry_error_button")
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

        // 2. GPS Status Banner (Detecting / Permission Denied / Location Unavailable)
        when (gpsStatus) {
            is GpsLocationStatus.Detecting -> {
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GpsFixed,
                            contentDescription = null,
                            tint = palette.accentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Detecting your current GPS coordinates & city…",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.primaryText
                        )
                    }
                }
            }

            is GpsLocationStatus.PermissionDenied -> {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gps_permission_denied_banner"),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOff,
                                contentDescription = null,
                                tint = palette.accentGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.location_denied_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.secondaryText
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(palette.accentCyan.copy(alpha = 0.22f))
                                    .border(1.dp, palette.accentCyan, CircleShape)
                                    .clickable(onClick = onRequestGps)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MyLocation,
                                    contentDescription = null,
                                    tint = palette.accentCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Grant GPS Permission",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accentCyan
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(palette.pillBackground)
                                    .border(1.dp, palette.cardBorderSubtle, CircleShape)
                                    .clickable(onClick = onSearchCityManually)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = palette.primaryText,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Search City Manually",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.primaryText
                                )
                            }
                        }
                    }
                }
            }

            is GpsLocationStatus.Unavailable -> {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gps_unavailable_banner"),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOff,
                                contentDescription = null,
                                tint = palette.accentGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = gpsStatus.reason,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.secondaryText
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(palette.pillBackground)
                                .border(1.dp, palette.cardBorderSubtle, CircleShape)
                                .clickable(onClick = onSearchCityManually)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Search City",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.accentCyan
                            )
                        }
                    }
                }
            }

            else -> Unit
        }
    }
}

@Composable
private fun ConditionPreviewPillStrip(
    activeScene: RealisticWeatherScene,
    hasOverride: Boolean,
    onSelectScene: (RealisticWeatherScene?) -> Unit,
    onResetOverride: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val scrollState = rememberScrollState()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sky Preview • ${activeScene.displayName}",
                style = MaterialTheme.typography.labelMedium,
                color = palette.secondaryText
            )
            if (hasOverride) {
                Text(
                    text = "Reset to Live Sky",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.accentCyan,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onResetOverride() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("reset_condition_override")
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RealisticWeatherScene.entries.forEach { scene ->
                val isSelected = scene == activeScene
                val bgColor = if (isSelected) {
                    palette.accentCyan.copy(alpha = 0.26f)
                } else {
                    palette.pillBackground
                }
                val borderColor = if (isSelected) {
                    palette.accentCyan
                } else {
                    palette.cardBorderSubtle
                }

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(bgColor)
                        .border(1.dp, borderColor, CircleShape)
                        .clickable { onSelectScene(scene) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("scene_chip_${scene.name}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WeatherConditionArtworkIcon(
                        condition = scene.baseCondition,
                        iconSize = 18.dp,
                        animationsEnabled = false
                    )
                    Text(
                        text = scene.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) palette.primaryText else palette.secondaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCurrentWeatherSection(
    city: CityWeatherData,
    effectiveCondition: WeatherConditionType,
    temperatureUnit: TemperatureUnit,
    animationsEnabled: Boolean,
    lastUpdatedTime: String?,
    onCityTap: () -> Unit
) {
    val palette = LocalGlassPalette.current

    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_weather_card"),
        shape = RoundedCornerShape(34.dp),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Current City Name & Location Icon
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onCityTap)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (city.isCurrentLocation) Icons.Filled.MyLocation else Icons.Filled.LocationOn,
                    contentDescription = "City Location",
                    tint = palette.accentCyan,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = city.cityName,
                    style = MaterialTheme.typography.headlineLarge,
                    color = palette.primaryText,
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = city.regionAndCountry,
                style = MaterialTheme.typography.labelLarge,
                color = palette.accentCyan,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${city.currentDateDisplay}  •  ${city.currentTimeDisplay} (${city.timeZoneOffsetLabel})",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.secondaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Central Weather Icon + Large Temperature Display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                WeatherConditionArtworkIcon(
                    condition = effectiveCondition,
                    iconSize = 86.dp,
                    animationsEnabled = animationsEnabled
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = formatTemperature(city.currentTempCelsius, temperatureUnit),
                    style = MaterialTheme.typography.displayLarge,
                    color = palette.primaryText,
                    modifier = Modifier.testTag("hero_temperature_text")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Weather Condition Label
            Text(
                text = effectiveCondition.displayName,
                style = MaterialTheme.typography.headlineMedium,
                color = palette.accentCyan,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = city.conditionDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.secondaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Feels-Like & High/Low Temperature Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.pillBackground)
                        .border(1.dp, palette.cardBorderSubtle, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.feels_like,
                            formatTemperature(city.feelsLikeCelsius, temperatureUnit)
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.primaryText
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.pillBackground)
                        .border(1.dp, palette.cardBorderSubtle, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.high_low_temp,
                            formatTemperature(city.highTempCelsius, temperatureUnit),
                            formatTemperature(city.lowTempCelsius, temperatureUnit)
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.primaryText
                    )
                }
            }

            if (lastUpdatedTime != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Last updated at $lastUpdatedTime • ${
                        String.format(Locale.US, "%.2f°, %.2f°", city.latitude, city.longitude)
                    }",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.mutedText,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun NoCitySelectedGlassCard(
    onRequestGps: () -> Unit,
    onOpenCities: () -> Unit
) {
    val palette = LocalGlassPalette.current

    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("no_city_selected_card"),
        shape = RoundedCornerShape(30.dp),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(palette.accentCyan.copy(alpha = 0.20f))
                    .border(1.dp, palette.accentCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = "Choose Your Location",
                    tint = palette.accentCyan,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "Choose Your Location",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Detect live weather using your GPS location or search and add any city worldwide.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.secondaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.accentCyan.copy(alpha = 0.24f))
                        .border(1.dp, palette.accentCyan, CircleShape)
                        .clickable(onClick = onRequestGps)
                        .padding(horizontal = 16.dp, vertical = 11.dp)
                        .testTag("empty_home_use_gps_button"),
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
                        .padding(horizontal = 16.dp, vertical = 11.dp)
                        .testTag("empty_home_open_cities_button"),
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
                        text = "Search / Add City",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.primaryText
                    )
                }
            }
        }
    }
}
