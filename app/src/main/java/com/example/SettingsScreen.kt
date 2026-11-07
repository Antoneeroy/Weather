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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Public User Settings Screen.
 *
 * Clean, consumer-friendly interface strictly structured for public everyday users:
 * 1. App Information (Weather App logo, app name, version number)
 * 2. Appearance (Dark Theme, Light Theme, Automatic Theme)
 * 3. Weather Preferences (Celsius / Fahrenheit, Default City, Weather Refresh)
 * 4. Notifications (Daily Weather, Rain Alerts, Severe Weather Alerts)
 * 5. Privacy Policy & Data Safety
 * 6. Terms & Conditions
 * 7. About Developer
 * 8. Copyright Information
 *
 * Free of internal developer text, APK build guides, signing instructions, or technical reports.
 */
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
    var showPrivacyPolicyDetails by rememberSaveable { mutableStateOf(false) }
    var showTermsDetails by rememberSaveable { mutableStateOf(false) }

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
                top = 16.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. App Information (Logo, App Name, Version)
            item {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_info_card"),
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Weather App Logo
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF38BDF8).copy(alpha = 0.35f),
                                            Color(0xFFA855F7).copy(alpha = 0.35f)
                                        )
                                    )
                                )
                                .border(1.dp, palette.cardBorderLight, RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            WeatherConditionArtworkIcon(
                                condition = WeatherConditionType.PARTLY_CLOUDY,
                                iconSize = 38.dp,
                                animationsEnabled = settings.animationsEnabled
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryText
                            )
                            Text(
                                text = stringResource(R.string.settings_app_version),
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.accentCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Simple, accurate & beautiful weather forecasting",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryText
                            )
                        }
                    }
                }
            }

            // 2. Appearance (Dark Theme, Light Theme, Automatic Theme)
            item {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("appearance_settings_card"),
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
                                imageVector = Icons.Filled.Palette,
                                contentDescription = null,
                                tint = palette.accentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Appearance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryText
                            )
                        }

                        Text(
                            text = "Choose your preferred color theme or match your phone's system appearance.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondaryText
                        )

                        // 3 Theme Options (Dark, Light, Automatic)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeModeOption.entries.forEach { mode ->
                                val isSelected = settings.themeMode == mode
                                val modeIcon = when (mode) {
                                    ThemeModeOption.DARK -> Icons.Filled.DarkMode
                                    ThemeModeOption.LIGHT -> Icons.Filled.LightMode
                                    ThemeModeOption.AUTO_SYSTEM -> Icons.Filled.BrightnessAuto
                                }
                                val modeLabel = when (mode) {
                                    ThemeModeOption.DARK -> "Dark"
                                    ThemeModeOption.LIGHT -> "Light"
                                    ThemeModeOption.AUTO_SYSTEM -> "Automatic"
                                }

                                Row(
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
                                        .padding(vertical = 12.dp, horizontal = 4.dp)
                                        .testTag("theme_mode_${mode.name}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = modeIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) palette.accentCyan else palette.secondaryText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = modeLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) palette.primaryText else palette.secondaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Realistic Weather Animation Engine (Weather Animations, Battery Saver Mode, Animation Quality: High/Low)
            item {
                WeatherAnimationSettingsCard(
                    settings = settings,
                    onToggleAnimations = onToggleAnimations,
                    onSelectAnimationQuality = onSelectAnimationQuality,
                    onToggleBatterySaver = onToggleBatterySaver
                )
            }

            // 3. Weather Preferences (Celsius / Fahrenheit, Default City, Weather Refresh)
            item {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("weather_preferences_card"),
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
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                tint = palette.accentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Weather Preferences",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryText
                            )
                        }

                        // A. Temperature Unit (Celsius / Fahrenheit)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Thermostat,
                                    contentDescription = null,
                                    tint = palette.accentPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Temperature Unit",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.primaryText
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TemperatureUnit.entries.forEach { unit ->
                                    val isSelected = settings.temperatureUnit == unit
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) palette.accentCyan.copy(alpha = 0.25f)
                                                else palette.pillBackground
                                            )
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) palette.accentCyan else palette.cardBorderSubtle,
                                                shape = CircleShape
                                            )
                                            .clickable { onSelectTempUnit(unit) }
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                            .testTag("temp_unit_${unit.name}")
                                    ) {
                                        Text(
                                            text = unit.symbol,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) palette.primaryText else palette.secondaryText
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = palette.cardBorderSubtle)

                        // B. Default City
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationCity,
                                contentDescription = null,
                                tint = palette.accentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Default City",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.primaryText
                                )
                                Text(
                                    text = "Select your home location shown when opening the app",
                                    style = MaterialTheme.typography.bodySmall,
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
                                                width = if (isDefault) 1.4.dp else 1.dp,
                                                color = if (isDefault) palette.accentCyan else palette.cardBorderSubtle,
                                                shape = CircleShape
                                            )
                                            .clickable { onSelectDefaultCity(city.id) }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                            .testTag("default_city_option_${city.id}")
                                    ) {
                                        Text(
                                            text = city.cityName,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isDefault) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isDefault) palette.primaryText else palette.secondaryText
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = palette.cardBorderSubtle)

                        // C. Weather Refresh Interval
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = palette.accentEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Weather Refresh",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.primaryText
                                )
                                Text(
                                    text = "How frequently weather updates in background: ${settings.refreshInterval.label}",
                                    style = MaterialTheme.typography.bodySmall,
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
                                            width = if (isSelected) 1.4.dp else 1.dp,
                                            color = if (isSelected) palette.accentCyan else palette.cardBorderSubtle,
                                            shape = CircleShape
                                        )
                                        .clickable { onSelectRefreshInterval(interval) }
                                        .padding(horizontal = 13.dp, vertical = 7.dp)
                                        .testTag("refresh_interval_${interval.name}")
                                ) {
                                    Text(
                                        text = interval.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) palette.primaryText else palette.secondaryText
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = palette.cardBorderSubtle)

                        // D. Home Screen Widget Sync / Pin Action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(palette.accentCyan.copy(alpha = 0.16f))
                                .border(1.dp, palette.accentCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .clickable(onClick = onPinOrUpdateWidget)
                                .padding(horizontal = 14.dp, vertical = 11.dp)
                                .testTag("pin_weather_widget_button"),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Widgets,
                                contentDescription = null,
                                tint = palette.accentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add or Update Home Screen Widget",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentCyan
                            )
                        }
                    }
                }
            }

            // 4. Notifications (Daily Weather, Rain Alerts, Severe Weather Alerts)
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = palette.accentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Notifications",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryText
                            )
                        }

                        // Master Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Notifications",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryText
                                )
                                Text(
                                    text = "Receive helpful forecasts and weather advisories",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryText
                                )
                            }
                            Switch(
                                checked = settings.notificationsEnabled,
                                onCheckedChange = onToggleNotificationsMaster,
                                modifier = Modifier.testTag("notifications_master_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = palette.accentCyan
                                )
                            )
                        }

                        if (settings.notificationsEnabled) {
                            HorizontalDivider(color = palette.cardBorderSubtle)

                            // 1. Daily Weather
                            PublicNotificationToggleRow(
                                title = "Daily Weather Summary",
                                subtitle = "Morning forecast delivered at your chosen time",
                                checked = settings.dailySummaryNotification,
                                onCheckedChange = onToggleDailySummaryNotif,
                                testTag = "daily_summary_switch"
                            )

                            // 2. Rain Alerts
                            PublicNotificationToggleRow(
                                title = "Rain Alerts",
                                subtitle = "Heads-up notification when rain is expected",
                                checked = settings.rainNotification,
                                onCheckedChange = onToggleRainNotif,
                                testTag = "rain_alert_switch"
                            )

                            // 3. Severe Weather Alerts
                            PublicNotificationToggleRow(
                                title = "Severe Weather Alerts",
                                subtitle = "Advisories for storms, heavy precipitation & extreme temps",
                                checked = settings.severeWeatherNotification,
                                onCheckedChange = onToggleSevereWeatherNotif,
                                testTag = "severe_weather_switch"
                            )

                            HorizontalDivider(color = palette.cardBorderSubtle)

                            // Send Test Notification
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(palette.pillBackground)
                                    .border(1.dp, palette.cardBorderSubtle, RoundedCornerShape(14.dp))
                                    .clickable(onClick = onSendTestNotification)
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("send_test_notification_button"),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Send Test Weather Notification",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.accentCyan
                                )
                            }

                            if (!notificationStatusMessage.isNullOrBlank()) {
                                Text(
                                    text = notificationStatusMessage,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.accentEmerald
                                )
                            }
                        }
                    }
                }
            }

            // 5. Privacy Policy & Data Safety
            item {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("privacy_policy_card"),
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPrivacyPolicyDetails = !showPrivacyPolicyDetails },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = palette.accentEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.privacy_policy_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryText
                                )
                            }
                            Icon(
                                imageVector = if (showPrivacyPolicyDetails) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = palette.secondaryText
                            )
                        }

                        Text(
                            text = stringResource(R.string.privacy_policy_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondaryText
                        )

                        AnimatedVisibility(visible = showPrivacyPolicyDetails) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(palette.pillBackground)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "• Location Privacy: Your GPS location is used strictly on-device to retrieve accurate local weather data. Your coordinates are never sent to private advertising servers, never stored, and never sold.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.primaryText
                                )
                                Text(
                                    text = "• Manual City Search: You can freely use all features without granting GPS permission by searching for any city worldwide.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryText
                                )
                                Text(
                                    text = "• No Tracking or Ads: This application does not contain advertising SDKs, tracking cookies, or third-party behavioral analytics.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryText
                                )
                                Text(
                                    text = stringResource(R.string.open_meteo_attribution),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.accentCyan
                                )
                            }
                        }
                    }
                }
            }

            // 6. Terms & Conditions
            item {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("terms_card"),
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTermsDetails = !showTermsDetails },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Description,
                                    contentDescription = null,
                                    tint = palette.accentPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.terms_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryText
                                )
                            }
                            Icon(
                                imageVector = if (showTermsDetails) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = palette.secondaryText
                            )
                        }

                        Text(
                            text = stringResource(R.string.terms_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondaryText
                        )

                        AnimatedVisibility(visible = showTermsDetails) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(palette.pillBackground)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "• Weather data and forecasts are gathered from Open-Meteo. While we strive for high precision, forecasts may vary based on rapid atmospheric shifts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryText
                                )
                                Text(
                                    text = "• Severe weather notifications provide estimated alerts based on meteorological API criteria. For critical emergency situations, always consult official regional meteorological services.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryText
                                )
                            }
                        }
                    }
                }
            }

            // 7. About Developer
            item {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("about_developer_card"),
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = palette.accentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.about_developer_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryText
                            )
                        }
                        Text(
                            text = stringResource(R.string.about_developer_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondaryText
                        )
                    }
                }
            }

            // 8. Copyright Information
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Copyright,
                            contentDescription = null,
                            tint = palette.mutedText,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.copyright_text),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.mutedText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicNotificationToggleRow(
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
                style = MaterialTheme.typography.bodySmall,
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
