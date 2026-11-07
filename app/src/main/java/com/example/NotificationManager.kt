package com.example

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager as SystemNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import java.util.concurrent.TimeUnit

/**
 * Phase 5 Weather Notification Manager & WorkManager Background Scheduler.
 * Supports:
 * - Daily weather summary
 * - Morning weather notification
 * - Rain notification
 * - Severe weather notification
 * - Temperature change notification
 * - Enable/disable settings & user-selectable notification time
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
                "Daily & Morning Weather Summaries",
                SystemNotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily weather forecasts, morning briefings, and temperature trend updates"
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

        // 1. Daily Weather Summary or Immediate Test Summary
        if (settings.dailySummaryNotification || forceSendSummary) {
            postNotification(
                context = context,
                channelId = CHANNEL_DAILY_WEATHER,
                notificationId = NOTIF_ID_DAILY_SUMMARY,
                title = "${city.cityName}: $tempStr • ${city.condition.displayName}",
                body = "High $highStr / Low $lowStr • Rain chance $todayPop% • Scheduled at ${settings.notificationTime.label}"
            )
            postedAny = true
        }

        // 2. Morning Weather Notification
        if (settings.morningWeatherNotification && !forceSendSummary) {
            val sunrise = city.details.sunriseTime
            postNotification(
                context = context,
                channelId = CHANNEL_DAILY_WEATHER,
                notificationId = NOTIF_ID_MORNING_BRIEFING,
                title = "Morning Weather Briefing • ${city.cityName}",
                body = "Currently $tempStr (${city.condition.displayName}). Today's high $highStr, UV Index ${city.details.uvIndex}, Sunrise $sunrise."
            )
            postedAny = true
        }

        // 3. Rain Notification
        val isRainLikely = todayPop >= 50 || city.condition == WeatherConditionType.RAINY
        if (settings.rainNotification && isRainLikely) {
            val precipMm = city.tenDayForecast.firstOrNull()?.precipitationSumMm ?: 0.0
            postNotification(
                context = context,
                channelId = CHANNEL_WEATHER_ALERTS,
                notificationId = NOTIF_ID_RAIN_ALERT,
                title = "Rain Alert for ${city.cityName} ($todayPop% Chance)",
                body = "Precipitation expected today (${String.format("%.1f", precipMm)} mm). Keep an umbrella handy."
            )
            postedAny = true
        }

        // 4. Severe Weather Notification (from WeatherAlertManager)
        if (settings.severeWeatherNotification) {
            val alerts = WeatherAlertManager.evaluateAlertsForCity(city, unit)
            val topAlert = alerts.firstOrNull {
                it.severity == WeatherAlertSeverity.HIGH || it.severity == WeatherAlertSeverity.EXTREME
            }
            if (topAlert != null) {
                postNotification(
                    context = context,
                    channelId = CHANNEL_WEATHER_ALERTS,
                    notificationId = NOTIF_ID_SEVERE_WEATHER,
                    title = "${topAlert.title} (${city.cityName})",
                    body = "${topAlert.summary} [Estimated Open-Meteo Advisory]"
                )
                postedAny = true
            }
        }

        // 5. Temperature Change Notification
        if (settings.temperatureChangeNotification) {
            val tempChangeMsg = WeatherAlertManager.detectSignificantTemperatureChange(city, unit)
            if (tempChangeMsg != null) {
                postNotification(
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

    private fun postNotification(
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
 * updates the local offline cache & home screen widget, and posts configured notifications.
 */
class WeatherBackgroundSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = WeatherPreferences(applicationContext)
        val settings = prefs.settingsFlow.first()
        val selectedCityId = prefs.selectedCityIdFlow.first() ?: settings.defaultCityId
        val savedRecords = prefs.loadSavedCitiesRecords()
        val initialCities = SampleWeatherRepository.getInitialCities()

        val targetCityRecord = savedRecords.find { it.id == selectedCityId }
        val fallbackCity = initialCities.find { it.id == selectedCityId } ?: initialCities.first()

        val cityId = targetCityRecord?.id ?: fallbackCity.id
        val cityName = targetCityRecord?.cityName ?: fallbackCity.cityName
        val regionAndCountry = targetCityRecord?.regionAndCountry ?: fallbackCity.regionAndCountry
        val lat = targetCityRecord?.latitude ?: fallbackCity.latitude
        val lon = targetCityRecord?.longitude ?: fallbackCity.longitude

        val repository = WeatherRepository(applicationContext)
        return when (
            val apiResult = repository.fetchLiveCityWeather(
                id = cityId,
                cityName = cityName,
                regionAndCountry = regionAndCountry,
                latitude = lat,
                longitude = lon,
                isCurrentLocation = false
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
