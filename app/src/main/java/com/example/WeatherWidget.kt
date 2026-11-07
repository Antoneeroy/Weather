package com.example

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews

/**
 * Android Home Screen Weather Widget Provider & Updater.
 * Inspired by premium glassmorphic weather widgets:
 * - Transparent blue glassmorphism background with smooth rounded corners
 * - Large current temperature
 * - City name and weather condition
 * - Real hourly forecast row (next 4 hours) with time, weather icons, and temperatures
 * - Tap to open Weather application
 * - Automatic weather refresh via WorkManager and AppWidgetManager
 */
class WeatherWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            WeatherWidgetUpdater.updateSingleWidgetFromStoredState(
                context = context,
                appWidgetManager = appWidgetManager,
                appWidgetId = appWidgetId
            )
        }
    }
}

object WeatherWidgetUpdater {

    private const val PREF_WIDGET_STATE = "weather_widget_shared_state"
    private const val KEY_CITY_NAME = "widget_city_name"
    private const val KEY_TEMP_TEXT = "widget_temp_text"
    private const val KEY_CONDITION_TEXT = "widget_condition_text"
    private const val KEY_HIGH_LOW_TEXT = "widget_high_low_text"
    private const val KEY_UPDATED_TEXT = "widget_updated_text"

    // Hourly keys (Hours 1 through 4)
    private const val KEY_H1_TIME = "widget_h1_time"
    private const val KEY_H1_ICON = "widget_h1_icon"
    private const val KEY_H1_TEMP = "widget_h1_temp"

    private const val KEY_H2_TIME = "widget_h2_time"
    private const val KEY_H2_ICON = "widget_h2_icon"
    private const val KEY_H2_TEMP = "widget_h2_temp"

    private const val KEY_H3_TIME = "widget_h3_time"
    private const val KEY_H3_ICON = "widget_h3_icon"
    private const val KEY_H3_TEMP = "widget_h3_temp"

    private const val KEY_H4_TIME = "widget_h4_time"
    private const val KEY_H4_ICON = "widget_h4_icon"
    private const val KEY_H4_TEMP = "widget_h4_temp"

    /**
     * Pushes live or cached city weather data to SharedPreferences and refreshes all home screen widgets.
     */
    fun updateAllWidgets(
        context: Context,
        city: CityWeatherData,
        temperatureUnit: TemperatureUnit,
        lastUpdatedLabel: String?
    ) {
        val emoji = conditionEmoji(city.condition, city.wmoCode)
        val tempText = formatTemperature(city.currentTempCelsius, temperatureUnit)
        val highText = formatTemperature(city.highTempCelsius, temperatureUnit)
        val lowText = formatTemperature(city.lowTempCelsius, temperatureUnit)
        val conditionLine = "$emoji ${city.condition.displayName} • H: $highText L: $lowText"
        val highLowLine = "High: $highText • Low: $lowText"
        val updatedLine = when {
            city.isLiveData && !lastUpdatedLabel.isNullOrBlank() -> "Live • $lastUpdatedLabel"
            city.isCachedOfflineData && !city.cachedTimestampLabel.isNullOrBlank() -> "Cached • ${city.cachedTimestampLabel}"
            !lastUpdatedLabel.isNullOrBlank() -> "Updated $lastUpdatedLabel"
            else -> "Open-Meteo"
        }

        // Extract next 4 hours from hourly forecast
        val hours = city.hourlyForecast.take(4)
        val h1 = hours.getOrNull(0)
        val h2 = hours.getOrNull(1)
        val h3 = hours.getOrNull(2)
        val h4 = hours.getOrNull(3)

        val prefs = context.getSharedPreferences(PREF_WIDGET_STATE, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_CITY_NAME, city.cityName)
            .putString(KEY_TEMP_TEXT, tempText)
            .putString(KEY_CONDITION_TEXT, conditionLine)
            .putString(KEY_HIGH_LOW_TEXT, highLowLine)
            .putString(KEY_UPDATED_TEXT, updatedLine)
            // Hour 1
            .putString(KEY_H1_TIME, h1?.timeLabel?.ifBlank { "Now" } ?: "Now")
            .putString(KEY_H1_ICON, h1?.let { conditionEmoji(it.condition, it.wmoCode) } ?: emoji)
            .putString(KEY_H1_TEMP, h1?.let { formatTemperature(it.tempCelsius, temperatureUnit) } ?: tempText)
            // Hour 2
            .putString(KEY_H2_TIME, h2?.timeLabel?.ifBlank { "+1h" } ?: "+1h")
            .putString(KEY_H2_ICON, h2?.let { conditionEmoji(it.condition, it.wmoCode) } ?: emoji)
            .putString(KEY_H2_TEMP, h2?.let { formatTemperature(it.tempCelsius, temperatureUnit) } ?: tempText)
            // Hour 3
            .putString(KEY_H3_TIME, h3?.timeLabel?.ifBlank { "+2h" } ?: "+2h")
            .putString(KEY_H3_ICON, h3?.let { conditionEmoji(it.condition, it.wmoCode) } ?: emoji)
            .putString(KEY_H3_TEMP, h3?.let { formatTemperature(it.tempCelsius, temperatureUnit) } ?: tempText)
            // Hour 4
            .putString(KEY_H4_TIME, h4?.timeLabel?.ifBlank { "+3h" } ?: "+3h")
            .putString(KEY_H4_ICON, h4?.let { conditionEmoji(it.condition, it.wmoCode) } ?: emoji)
            .putString(KEY_H4_TEMP, h4?.let { formatTemperature(it.tempCelsius, temperatureUnit) } ?: tempText)
            .apply()

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, WeatherWidgetReceiver::class.java)
        val widgetIds = runCatching { appWidgetManager.getAppWidgetIds(componentName) }
            .getOrDefault(IntArray(0))

        for (widgetId in widgetIds) {
            updateSingleWidgetFromStoredState(context, appWidgetManager, widgetId)
        }
    }

    fun updateSingleWidgetFromStoredState(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefs = context.getSharedPreferences(PREF_WIDGET_STATE, Context.MODE_PRIVATE)
        val cityName = prefs.getString(KEY_CITY_NAME, "San Francisco") ?: "San Francisco"
        val tempText = prefs.getString(KEY_TEMP_TEXT, "21°C") ?: "21°C"
        val conditionText = prefs.getString(KEY_CONDITION_TEXT, "☀️ Partly Cloudy") ?: "☀️ Partly Cloudy"
        val highLowText = prefs.getString(KEY_HIGH_LOW_TEXT, "High: 23°C • Low: 13°C") ?: "High: 23°C • Low: 13°C"
        val updatedText = prefs.getString(KEY_UPDATED_TEXT, "Live Open-Meteo") ?: "Live Open-Meteo"

        val h1Time = prefs.getString(KEY_H1_TIME, "Now") ?: "Now"
        val h1Icon = prefs.getString(KEY_H1_ICON, "⛅") ?: "⛅"
        val h1Temp = prefs.getString(KEY_H1_TEMP, "21°") ?: "21°"

        val h2Time = prefs.getString(KEY_H2_TIME, "+1h") ?: "+1h"
        val h2Icon = prefs.getString(KEY_H2_ICON, "☀️") ?: "☀️"
        val h2Temp = prefs.getString(KEY_H2_TEMP, "22°") ?: "22°"

        val h3Time = prefs.getString(KEY_H3_TIME, "+2h") ?: "+2h"
        val h3Icon = prefs.getString(KEY_H3_ICON, "⛅") ?: "⛅"
        val h3Temp = prefs.getString(KEY_H3_TEMP, "23°") ?: "23°"

        val h4Time = prefs.getString(KEY_H4_TIME, "+3h") ?: "+3h"
        val h4Icon = prefs.getString(KEY_H4_ICON, "🌧️") ?: "🌧️"
        val h4Temp = prefs.getString(KEY_H4_TEMP, "20°") ?: "20°"

        val views = RemoteViews(context.packageName, R.layout.weather_widget_layout).apply {
            setTextViewText(R.id.widget_city_name, cityName)
            setTextViewText(R.id.widget_temperature_text, tempText)
            setTextViewText(R.id.widget_condition_text, conditionText)
            setTextViewText(R.id.widget_high_low_text, highLowText)
            setTextViewText(R.id.widget_updated_text, updatedText)

            // Hourly pills
            setTextViewText(R.id.widget_h1_time, h1Time)
            setTextViewText(R.id.widget_h1_icon, h1Icon)
            setTextViewText(R.id.widget_h1_temp, h1Temp)

            setTextViewText(R.id.widget_h2_time, h2Time)
            setTextViewText(R.id.widget_h2_icon, h2Icon)
            setTextViewText(R.id.widget_h2_temp, h2Temp)

            setTextViewText(R.id.widget_h3_time, h3Time)
            setTextViewText(R.id.widget_h3_icon, h3Icon)
            setTextViewText(R.id.widget_h3_temp, h3Temp)

            setTextViewText(R.id.widget_h4_time, h4Time)
            setTextViewText(R.id.widget_h4_icon, h4Icon)
            setTextViewText(R.id.widget_h4_temp, h4Temp)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, pendingFlags)
        views.setOnClickPendingIntent(R.id.widget_root_container, pendingIntent)

        runCatching {
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    /**
     * Requests the launcher to pin the Weather Widget on Android 8.0+ (API 26+).
     */
    fun requestPinWeatherWidget(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (!appWidgetManager.isRequestPinAppWidgetSupported) return false
        val provider = ComponentName(context, WeatherWidgetReceiver::class.java)
        return runCatching {
            appWidgetManager.requestPinAppWidget(provider, null, null)
        }.getOrDefault(false)
    }

    fun conditionEmoji(condition: WeatherConditionType, wmoCode: Int): String {
        return when {
            wmoCode in listOf(95, 96, 99) || condition == WeatherConditionType.THUNDERSTORM -> "⛈️"
            wmoCode in listOf(71, 73, 75, 77, 85, 86) || condition == WeatherConditionType.SNOWY -> "❄️"
            wmoCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82) || condition == WeatherConditionType.RAINY -> "🌧️"
            wmoCode in listOf(45, 48) -> "🌫️"
            condition == WeatherConditionType.CLEAR_NIGHT -> "🌙"
            condition == WeatherConditionType.PARTLY_CLOUDY -> "⛅"
            else -> "☀️"
        }
    }
}
