package com.example

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    settings: WeatherAppSettings,
    onToggleDarkTheme: (Boolean) -> Unit,
    onSelectTempUnit: (TemperatureUnit) -> Unit,
    onToggleAnimations: (Boolean) -> Unit,
    onSelectAnimationQuality: (AnimationQualityLevel) -> Unit,
    onToggleBatterySaver: (Boolean) -> Unit,
    onBackToHome: () -> Unit,
    savedCities: List<CityWeatherData> = emptyList(),
    notificationStatusMessage: String? = null,
    onSelectThemeMode: (ThemeModeOption) -> Unit = {},
    onToggleNotificationsMaster: (Boolean) -> Unit = {},
    onToggleDailySummaryNotif: (Boolean) -> Unit = {},
    onToggleMorningWeatherNotif: (Boolean) -> Unit = {},
    onToggleRainNotif: (Boolean) -> Unit = {},
    onToggleSevereWeatherNotif: (Boolean) -> Unit = {},
    onToggleTempChangeNotif: (Boolean) -> Unit = {},
    onSelectNotificationTime: (NotificationTimeOption) -> Unit = {},
    onSendTestNotification: () -> Unit = {},
    onSelectDefaultCity: (String) -> Unit = {},
    onSelectRefreshInterval: (RefreshIntervalOption) -> Unit = {},
    onPinOrUpdateWidget: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToHome)

    val palette = LocalGlassPalette.current
    var showApkBuildGuide by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .testTag("settings_screen_list"),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 14.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = palette.primaryText
                    )
                    Text(
                        text = "Customize theme mode, units, animations, WorkManager notifications, default city & widget",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.secondaryText
                    )
                }
            }

            // 1. Appearance Card (Dark / Light / Automatic Theme + Quick Switch)
            item {
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_appearance_section),
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.accentCyan
                        )

                        // Theme Mode 3-way selector (Dark / Light / Automatic)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeModeOption.entries.forEach { mode ->
                                val isSelected = settings.themeMode == mode
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
                                        .clickable { onSelectThemeMode(mode) }
                                        .padding(vertical = 12.dp, horizontal = 6.dp)
                                        .testTag("theme_mode_${mode.name}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) palette.primaryText else palette.secondaryText
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = palette.cardBorderSubtle)

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
                                        .background(palette.accentPurple.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (settings.isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                        contentDescription = null,
                                        tint = palette.accentPurple
                                    )
                                }
                                Column {
                                    Text(
                                        text = stringResource(R.string.settings_dark_theme_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = palette.primaryText
                                    )
                                    Text(
                                        text = settings.themeMode.subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.secondaryText
                                    )
                                }
                            }

                            Switch(
                                checked = settings.isDarkTheme,
                                onCheckedChange = onToggleDarkTheme,
                                modifier = Modifier.testTag("switch_dark_theme"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = palette.accentCyan,
                                    uncheckedThumbColor = palette.secondaryText,
                                    uncheckedTrackColor = palette.pillBackground
                                )
                            )
                        }
                    }
                }
            }

            // 2. Celsius / Fahrenheit Selection Card
            item {
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Thermostat,
                                contentDescription = null,
                                tint = palette.accentGold
                            )
                            Text(
                                text = stringResource(R.string.settings_temp_unit_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.primaryText
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TemperatureUnit.entries.forEach { unitOption ->
                                val isSelected = settings.temperatureUnit == unitOption
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            if (isSelected) palette.accentCyan.copy(alpha = 0.25f)
                                            else palette.pillBackground
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) palette.accentCyan else palette.cardBorderSubtle,
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .clickable { onSelectTempUnit(unitOption) }
                                        .padding(vertical = 14.dp)
                                        .testTag("unit_option_${unitOption.name}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = unitOption.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) palette.primaryText else palette.secondaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Phase 4 Realistic Weather Animation Engine Settings Card
            item {
                WeatherAnimationSettingsCard(
                    settings = settings,
                    onToggleAnimations = onToggleAnimations,
                    onSelectAnimationQuality = onSelectAnimationQuality,
                    onToggleBatterySaver = onToggleBatterySaver
                )
            }

            // 4. Phase 5 Weather Notifications & Scheduled Alerts Card
            item {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notifications_settings_card"),
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(palette.accentCyan.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.NotificationsActive,
                                        contentDescription = null,
                                        tint = palette.accentCyan
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Weather Notifications",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = palette.primaryText
                                    )
                                    Text(
                                        text = "Scheduled via Android WorkManager",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.secondaryText
                                    )
                                }
                            }

                            Switch(
                                checked = settings.notificationsEnabled,
                                onCheckedChange = onToggleNotificationsMaster,
                                modifier = Modifier.testTag("switch_notifications_master"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = palette.accentCyan,
                                    uncheckedThumbColor = palette.secondaryText,
                                    uncheckedTrackColor = palette.pillBackground
                                )
                            )
                        }

                        AnimatedVisibility(visible = settings.notificationsEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                HorizontalDivider(color = palette.cardBorderSubtle)

                                NotificationToggleRow(
                                    title = "Daily Weather Summary",
                                    subtitle = "High/low temperatures, sky condition & rain probability",
                                    checked = settings.dailySummaryNotification,
                                    onCheckedChange = onToggleDailySummaryNotif,
                                    testTag = "switch_notif_daily_summary"
                                )

                                NotificationToggleRow(
                                    title = "Morning Weather Briefing",
                                    subtitle = "Morning temperature, UV index & sunrise forecast",
                                    checked = settings.morningWeatherNotification,
                                    onCheckedChange = onToggleMorningWeatherNotif,
                                    testTag = "switch_notif_morning"
                                )

                                NotificationToggleRow(
                                    title = "Rain Notification",
                                    subtitle = "Alerts when rain probability ≥ 50% or active rainfall",
                                    checked = settings.rainNotification,
                                    onCheckedChange = onToggleRainNotif,
                                    testTag = "switch_notif_rain"
                                )

                                NotificationToggleRow(
                                    title = "Severe Weather Notification",
                                    subtitle = "Thunderstorm, heavy rain, snow, high wind & extreme temperatures",
                                    checked = settings.severeWeatherNotification,
                                    onCheckedChange = onToggleSevereWeatherNotif,
                                    testTag = "switch_notif_severe"
                                )

                                NotificationToggleRow(
                                    title = "Temperature Change Notification",
                                    subtitle = "Notifies when tomorrow's high shifts by ≥ 4°C",
                                    checked = settings.temperatureChangeNotification,
                                    onCheckedChange = onToggleTempChangeNotif,
                                    testTag = "switch_notif_temp_change"
                                )

                                HorizontalDivider(color = palette.cardBorderSubtle)

                                // User-selectable notification delivery time
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccessTime,
                                        contentDescription = null,
                                        tint = palette.accentGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Daily Notification Time (${settings.notificationTime.label})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primaryText
                                    )
                                }

                                val timeScroll = rememberScrollState()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(timeScroll),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    NotificationTimeOption.entries.forEach { timeOption ->
                                        val isSelected = settings.notificationTime == timeOption
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) palette.accentCyan.copy(alpha = 0.25f)
                                                    else palette.pillBackground
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) palette.accentCyan else palette.cardBorderSubtle,
                                                    shape = CircleShape
                                                )
                                                .clickable { onSelectNotificationTime(timeOption) }
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                                .testTag("notif_time_${timeOption.name}")
                                        ) {
                                            Text(
                                                text = timeOption.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) palette.primaryText else palette.secondaryText
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Send Test Notification Now Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(palette.accentCyan.copy(alpha = 0.22f))
                                .border(1.dp, palette.accentCyan, RoundedCornerShape(16.dp))
                                .clickable(onClick = onSendTestNotification)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("send_test_notification_button"),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = null,
                                tint = palette.accentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Send Live Weather Notification Now",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentCyan
                            )
                        }

                        if (!notificationStatusMessage.isNullOrBlank()) {
                            Text(
                                text = notificationStatusMessage,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.accentEmerald
                            )
                        }
                    }
                }
            }

            // 5. Default City & Background Refresh Interval Card
            item {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_and_default_city_card"),
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationCity,
                                contentDescription = null,
                                tint = palette.accentCyan
                            )
                            Column {
                                Text(
                                    text = "Default Startup City",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.primaryText
                                )
                                Text(
                                    text = "Primary location used for background updates & widget",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.secondaryText
                                )
                            }
                        }

                        if (savedCities.isNotEmpty()) {
                            val cityScroll = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(cityScroll),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                savedCities.forEach { city ->
                                    val isDefault = settings.defaultCityId == city.id
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (isDefault) palette.accentCyan.copy(alpha = 0.25f)
                                                else palette.pillBackground
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isDefault) palette.accentCyan else palette.cardBorderSubtle,
                                                shape = CircleShape
                                            )
                                            .clickable { onSelectDefaultCity(city.id) }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                            .testTag("default_city_option_${city.id}")
                                    ) {
                                        Text(
                                            text = city.cityName,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isDefault) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isDefault) palette.primaryText else palette.secondaryText
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = palette.cardBorderSubtle)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = palette.accentPurple
                            )
                            Column {
                                Text(
                                    text = "Background Refresh Interval",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.primaryText
                                )
                                Text(
                                    text = "Current: ${settings.refreshInterval.label}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.secondaryText
                                )
                            }
                        }

                        val intervalScroll = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(intervalScroll),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RefreshIntervalOption.entries.forEach { interval ->
                                val isSelected = settings.refreshInterval == interval
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) palette.accentCyan.copy(alpha = 0.25f)
                                            else palette.pillBackground
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) palette.accentCyan else palette.cardBorderSubtle,
                                            shape = CircleShape
                                        )
                                        .clickable { onSelectRefreshInterval(interval) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                        .testTag("refresh_interval_${interval.name}")
                                ) {
                                    Text(
                                        text = interval.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) palette.primaryText else palette.secondaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Android Home Screen Weather Widget Card
            item {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_widget_settings_card"),
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Widgets,
                                contentDescription = null,
                                tint = palette.accentEmerald
                            )
                            Column {
                                Text(
                                    text = "Android Home Screen Weather Widget",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.primaryText
                                )
                                Text(
                                    text = "Shows City, Temperature, Condition Icon, High/Low & Last Updated Time",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.secondaryText
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(palette.accentEmerald.copy(alpha = 0.20f))
                                .border(1.dp, palette.accentEmerald, RoundedCornerShape(16.dp))
                                .clickable(onClick = onPinOrUpdateWidget)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("pin_weather_widget_button"),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Widgets,
                                contentDescription = null,
                                tint = palette.accentEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Pin / Sync Home Screen Weather Widget",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentEmerald
                            )
                        }
                    }
                }
            }

            // 7. About Application & APK Build Guide Section
            item {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_info_section_card"),
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = palette.accentCyan
                            )
                            Text(
                                text = stringResource(R.string.settings_about_section),
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.primaryText
                            )
                        }

                        Text(
                            text = stringResource(R.string.settings_app_version),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentCyan
                        )

                        Text(
                            text = "Application ID: com.aistudio.weather.glsmph • Version 5.0.0 (Code 5)",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryText
                        )

                        Text(
                            text = stringResource(R.string.settings_architecture),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondaryText
                        )

                        HorizontalDivider(color = palette.cardBorderSubtle)

                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = palette.accentEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Production-Ready Phase 1–5 Complete",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.primaryText
                                )
                                Text(
                                    text = stringResource(R.string.settings_roadmap),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.mutedText
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(palette.pillBackground)
                                .border(1.dp, palette.cardBorderSubtle, RoundedCornerShape(14.dp))
                                .clickable { showApkBuildGuide = !showApkBuildGuide }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("toggle_apk_build_guide_button"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Build,
                                    contentDescription = null,
                                    tint = palette.accentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "APK Build, Signing & Installation Guide",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryText
                                )
                            }
                            Text(
                                text = if (showApkBuildGuide) "Hide" else "View Steps",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.accentCyan
                            )
                        }

                        AnimatedVisibility(visible = showApkBuildGuide) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(palette.pillBackground)
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "• Debug APK: gradle :app:assembleDebug\n  Output: app/build/outputs/apk/debug/app-debug.apk",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.primaryText
                                )
                                Text(
                                    text = "• Release APK: gradle :app:assembleRelease\n  Output: app/build/outputs/apk/release/app-release.apk",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.primaryText
                                )
                                Text(
                                    text = "• Install on Device: Enable USB Debugging or transfer APK to Android phone and tap to install.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.secondaryText
                                )
                            }
                        }

                        HorizontalDivider(color = palette.cardBorderSubtle)

                        // Privacy Policy, Data Safety & Attribution Card Row
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(palette.pillBackground)
                                .border(1.dp, palette.cardBorderSubtle, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = palette.accentEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.privacy_policy_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryText
                                )
                            }
                            Text(
                                text = stringResource(R.string.privacy_policy_desc),
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.secondaryText
                            )
                            Text(
                                text = "• Location: Only requested when 'Use My GPS' is tapped or during setup. Used exclusively to query weather coordinates. Never logged, sold or shared.",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.mutedText
                            )
                            Text(
                                text = "• Manual Use: Full functionality is available without location permission by searching any city.",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.mutedText
                            )
                            Text(
                                text = stringResource(R.string.open_meteo_attribution),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.accentCyan
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val palette = LocalGlassPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = palette.primaryText
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = palette.secondaryText
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.accentCyan,
                uncheckedThumbColor = palette.secondaryText,
                uncheckedTrackColor = palette.pillBackground
            )
        )
    }
}
