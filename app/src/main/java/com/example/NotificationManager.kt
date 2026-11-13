package com.example

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager as SystemNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Android Weather Notification Widget Manager & WorkManager Background Scheduler.
 *
 * Implements a real Android notification widget with custom RemoteViews layout:
 * - Current city name
 * - Current temperature
 * - Weather condition & emoji icon
 * - High and low temperature
 * - Next 4 hours forecast with hourly temperature and weather icons
 * - Last updated timestamp
 * - Automatic background updates via WorkManager
 * - Enable/disable toggles & selectable update schedules in Settings
 * - Strict Android POST_NOTIFICATIONS permission checks
 */
object WeatherNotificationHelper {

    const val CHANNEL_DAILY_WEATHER = "weather_daily_summary_channel"
    const val CHANNEL_WEATHER_ALERTS = "weather_severe_alerts_channel"

    private const val NOTIF_ID_DAILY_SUMMARY = 4001
    private const val NOTIF_ID_MORNING_BRIEFING = 4002
    private const val NOTIF_ID_RAIN_ALERT = 4003
    private const val NOTIF_ID_SEVERE_WEATHER = 4004
    private const val NOTIF_ID_TEMP_CHANGE = 4005

    private const val WORK_NAME_PERIODIC_WEATHER_SYNC = "weather_phase5_periodic_sync_work"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val sysMgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as? SystemNotificationManager
                ?: return

            val dailyChannel = NotificationChannel(
                CHANNEL_DAILY_WEATHER,
                "Weather Forecast & Notification Widget",
                SystemNotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Live weather status, temperature, and hourly forecast widget notifications"
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_WEATHER_ALERTS,
                "Rain & Severe Weather Advisories",
                SystemNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Estimated rain, thunderstorm, high wind, and extreme temperature notifications"
            }

            sysMgr.createNotificationChannel(dailyChannel)
            sysMgr.createNotificationChannel(alertsChannel)
        }
    }

    /**
     * Schedules or cancels WorkManager periodic background weather updates based on user settings.
     */
    fun scheduleOrUpdateBackgroundWork(
        context: Context,
        settings: WeatherAppSettings
    ) {
        val workManager = runCatching { WorkManager.getInstance(context) }.getOrNull() ?: return

        if (settings.refreshInterval == RefreshIntervalOption.MANUAL_ONLY && !settings.notificationsEnabled) {
            workManager.cancelUniqueWork(WORK_NAME_PERIODIC_WEATHER_SYNC)
            return
        }

        val intervalMinutes = if (settings.refreshInterval.intervalMinutes >= 15L) {
            settings.refreshInterval.intervalMinutes
        } else {
            60L
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val initialDelayMinutes = computeInitialDelayMinutes(settings.notificationTime)

        val periodicRequest = PeriodicWorkRequestBuilder<WeatherBackgroundSyncWorker>(
            intervalMinutes,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelayMinutes.coerceAtMost(intervalMinutes / 2), TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_PERIODIC_WEATHER_SYNC,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    private fun computeInitialDelayMinutes(notificationTime: NotificationTimeOption): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, notificationTime.hour24)
            set(Calendar.MINUTE, notificationTime.minute)
            set(Calendar.SECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val diffMillis = (target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
        return (diffMillis / (60_000L)).coerceIn(1L, 120L)
    }

    /**
     * Evaluates current live weather against user notification preferences and posts enabled notifications.
     */
    fun evaluateAndSendNotifications(
        context: Context,
        city: CityWeatherData,
        settings: WeatherAppSettings,
        forceSendSummary: Boolean = false
    ): Boolean {
        if (!settings.notificationsEnabled && !forceSendSummary) return false
        if (!hasNotificationPermission(context)) return false

        ensureNotificationChannels(context)
        val unit = settings.temperatureUnit
        val tempStr = formatTemperature(city.currentTempCelsius, unit)
        val highStr = formatTemperature(city.highTempCelsius, unit)
        val lowStr = formatTemperature(city.lowTempCelsius, unit)
        val todayPop = city.tenDayForecast.firstOrNull()?.rainProbability ?: 0
        var postedAny = false

        // 1. Custom RemoteViews Weather Notification Widget (Compact + Expanded with 4-hour forecast)
        if (settings.dailySummaryNotification || forceSendSummary) {
            postCustomWeatherWidgetNotification(
                context = context,
                city = city,
                unit = unit,
                notificationId = NOTIF_ID_DAILY_SUMMARY
            )
            postedAny = true
        }

        // 2. Morning Weather Briefing
        if (settings.morningWeatherNotification && !forceSendSummary) {
            val sunrise = city.details.sunriseTime
            postStandardNotification(
                context = context,
                channelId = CHANNEL_DAILY_WEATHER,
                notificationId = NOTIF_ID_MORNING_BRIEFING,
                title = "Morning Briefing • ${city.cityName}",
                body = "Currently $tempStr (${city.condition.displayName}). High $highStr, UV ${city.details.uvIndex}, Sunrise $sunrise."
            )
            postedAny = true
        }

        // 3. Rain Alert
        val isRainLikely = todayPop >= 50 || city.condition == WeatherConditionType.RAINY
        if (settings.rainNotification && isRainLikely) {
            val precipMm = city.tenDayForecast.firstOrNull()?.precipitationSumMm ?: 0.0
            postStandardNotification(
                context = context,
                channelId = CHANNEL_WEATHER_ALERTS,
                notificationId = NOTIF_ID_RAIN_ALERT,
                title = "Rain Alert: ${city.cityName} ($todayPop% Chance)",
                body = "Precipitation expected (${String.format(Locale.US, "%.1f", precipMm)} mm). Keep an umbrella handy."
            )
            postedAny = true
        }

        // 4. Severe Weather Notification
        if (settings.severeWeatherNotification) {
            val alerts = WeatherAlertManager.evaluateAlertsForCity(city, unit)
            val topAlert = alerts.firstOrNull {
                it.severity == WeatherAlertSeverity.HIGH || it.severity == WeatherAlertSeverity.EXTREME
            }
            if (topAlert != null) {
                postStandardNotification(
                    context = context,
                    channelId = CHANNEL_WEATHER_ALERTS,
                    notificationId = NOTIF_ID_SEVERE_WEATHER,
                    title = "${topAlert.title} (${city.cityName})",
                    body = "${topAlert.summary} (Estimated Forecast Advisory)"
                )
                postedAny = true
            }
        }

        // 5. Temperature Change Notification
        if (settings.temperatureChangeNotification) {
            val tempChangeMsg = WeatherAlertManager.detectSignificantTemperatureChange(city, unit)
            if (tempChangeMsg != null) {
                postStandardNotification(
                    context = context,
                    channelId = CHANNEL_DAILY_WEATHER,
                    notificationId = NOTIF_ID_TEMP_CHANGE,
                    title = "Temperature Shift in ${city.cityName}",
                    body = tempChangeMsg
                )
                postedAny = true
            }
        }

        return postedAny
    }

    /**
     * Posts a rich custom RemoteViews weather notification widget showing city, temp, high/low,
     * weather condition icon, and next 4 hours forecast.
     */
    private fun postCustomWeatherWidgetNotification(
        context: Context,
        city: CityWeatherData,
        unit: TemperatureUnit,
        notificationId: Int
    ) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val emoji = WeatherWidgetUpdater.conditionEmoji(city.condition, city.wmoCode)
        val tempText = formatTemperature(city.currentTempCelsius, unit)
        val highText = formatTemperature(city.highTempCelsius, unit)
        val lowText = formatTemperature(city.lowTempCelsius, unit)
        val summaryLine = "${city.condition.displayName} • H: $highText L: $lowText"
        val updatedTime = city.currentTimeDisplay

        // 1. Compact View
        val compactView = RemoteViews(context.packageName, R.layout.notification_weather_compact).apply {
            setTextViewText(R.id.notif_weather_icon, emoji)
            setTextViewText(R.id.notif_city_name, city.cityName)
            setTextViewText(R.id.notif_condition_summary, summaryLine)
            setTextViewText(R.id.notif_temperature_text, tempText)
            setTextViewText(R.id.notif_updated_time, updatedTime)
        }

        // 2. Expanded View with next 4 hours forecast
        val hours = city.hourlyForecast.take(4)
        val h1 = hours.getOrNull(0)
        val h2 = hours.getOrNull(1)
        val h3 = hours.getOrNull(2)
        val h4 = hours.getOrNull(3)
        val todayPop = city.tenDayForecast.firstOrNull()?.rainProbability ?: 0

        val expandedView = RemoteViews(context.packageName, R.layout.notification_weather_expanded).apply {
            setTextViewText(R.id.notif_exp_weather_icon, emoji)
            setTextViewText(R.id.notif_exp_city_name, city.cityName)
            setTextViewText(R.id.notif_exp_condition_summary, summaryLine)
            setTextViewText(R.id.notif_exp_temperature_text, tempText)
            setTextViewText(R.id.notif_exp_rain_prob, "💧 Rain Chance: $todayPop%")
            setTextViewText(R.id.notif_exp_updated_text, "Live • $updatedTime")

            // Hour 1
            setTextViewText(R.id.notif_h1_time, h1?.timeLabel?.ifBlank { "Now" } ?: "Now")
            setTextViewText(R.id.notif_h1_icon, h1?.let { WeatherWidgetUpdater.conditionEmoji(it.condition, it.wmoCode) } ?: emoji)
            setTextViewText(R.id.notif_h1_temp, h1?.let { formatTemperature(it.tempCelsius, unit) } ?: tempText)

            // Hour 2
            setTextViewText(R.id.notif_h2_time, h2?.timeLabel?.ifBlank { "+1h" } ?: "+1h")
            setTextViewText(R.id.notif_h2_icon, h2?.let { WeatherWidgetUpdater.conditionEmoji(it.condition, it.wmoCode) } ?: emoji)
            setTextViewText(R.id.notif_h2_temp, h2?.let { formatTemperature(it.tempCelsius, unit) } ?: tempText)

            // Hour 3
            setTextViewText(R.id.notif_h3_time, h3?.timeLabel?.ifBlank { "+2h" } ?: "+2h")
            setTextViewText(R.id.notif_h3_icon, h3?.let { WeatherWidgetUpdater.conditionEmoji(it.condition, it.wmoCode) } ?: emoji)
            setTextViewText(R.id.notif_h3_temp, h3?.let { formatTemperature(it.tempCelsius, unit) } ?: tempText)

            // Hour 4
            setTextViewText(R.id.notif_h4_time, h4?.timeLabel?.ifBlank { "+3h" } ?: "+3h")
            setTextViewText(R.id.notif_h4_icon, h4?.let { WeatherWidgetUpdater.conditionEmoji(it.condition, it.wmoCode) } ?: emoji)
            setTextViewText(R.id.notif_h4_temp, h4?.let { formatTemperature(it.tempCelsius, unit) } ?: tempText)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_WEATHER)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setCustomContentView(compactView)
            .setCustomBigContentView(expandedView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    private fun postStandardNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String
    ) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                if (channelId == CHANNEL_WEATHER_ALERTS) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}

/**
 * WorkManager CoroutineWorker that fetches real Open-Meteo weather in the background,
 * updates the local offline cache & home screen widget, and posts configured notification widgets.
 */
class WeatherBackgroundSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val presetIds = setOf(
            "sf_us", "tokyo_jp", "london_uk", "dubai_ae", "zermatt_ch",
            "nyc_us", "singapore_sg", "paris_fr"
        )
        val prefs = WeatherPreferences(applicationContext)
        val settings = prefs.settingsFlow.first()
        val selectedCityId = prefs.selectedCityIdFlow.first()?.takeIf { it !in presetIds }
        val savedRecords = prefs.loadSavedCitiesRecords().filterNot { it.id in presetIds }

        val targetId = selectedCityId?.takeIf { it.isNotBlank() }
            ?: settings.defaultCityId.takeIf { it.isNotBlank() && it !in presetIds }

        val savedRecord = savedRecords.find { it.id == targetId } ?: savedRecords.firstOrNull()
            ?: return Result.success()

        val cityId = savedRecord.id
        val cityName = savedRecord.cityName
        val regionAndCountry = savedRecord.regionAndCountry
        val lat = savedRecord.latitude
        val lon = savedRecord.longitude

        val repository = WeatherRepository(applicationContext)
        return when (
            val apiResult = repository.fetchLiveCityWeather(
                id = cityId,
                cityName = cityName,
                regionAndCountry = regionAndCountry,
                latitude = lat,
                longitude = lon,
                isCurrentLocation = cityId == "gps_current_location"
            )
        ) {
            is WeatherApiResult.Success -> {
                val liveCity = apiResult.data
                val nowStamp = SampleWeatherRepository.getCurrentTimeFormatted()
                prefs.cacheLiveCityWeather(liveCity, nowStamp)
                WeatherWidgetUpdater.updateAllWidgets(
                    context = applicationContext,
                    city = liveCity,
                    temperatureUnit = settings.temperatureUnit,
                    lastUpdatedLabel = nowStamp
                )
                WeatherNotificationHelper.evaluateAndSendNotifications(
                    context = applicationContext,
                    city = liveCity,
                    settings = settings,
                    forceSendSummary = false
                )
                Result.success()
            }

            is WeatherApiResult.NoInternet -> Result.retry()
            is WeatherApiResult.ApiError -> Result.retry()
        }
    }
}
