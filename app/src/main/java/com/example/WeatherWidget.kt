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
 * Phase 5 Android Home Screen Weather Widget Receiver & Updater.
 * Displays:
 * - City name
 * - Current temperature
 * - Weather condition & icon
 * - High / Low temperature
 * - Last updated time
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
        val conditionLine = "$emoji ${city.condition.displayName}"
        val highLowLine = "H: $highText  •  L: $lowText"
        val updatedLine = when {
            city.isLiveData && !lastUpdatedLabel.isNullOrBlank() -> "Live • $lastUpdatedLabel"
            city.isCachedOfflineData && !city.cachedTimestampLabel.isNullOrBlank() -> "Cached • ${city.cachedTimestampLabel}"
            !lastUpdatedLabel.isNullOrBlank() -> "Updated $lastUpdatedLabel"
            else -> "Open-Meteo Weather"
        }

        val prefs = context.getSharedPreferences(PREF_WIDGET_STATE, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_CITY_NAME, city.cityName)
            .putString(KEY_TEMP_TEXT, tempText)
            .putString(KEY_CONDITION_TEXT, conditionLine)
            .putString(KEY_HIGH_LOW_TEXT, highLowLine)
            .putString(KEY_UPDATED_TEXT, updatedLine)
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
        val highLowText = prefs.getString(KEY_HIGH_LOW_TEXT, "H: 23°C  •  L: 13°C") ?: "H: 23°C  •  L: 13°C"
        val updatedText = prefs.getString(KEY_UPDATED_TEXT, "Tap to view 10-day forecast") ?: "Tap to view 10-day forecast"

        val views = RemoteViews(context.packageName, R.layout.weather_widget_layout).apply {
            setTextViewText(R.id.widget_city_name, cityName)
            setTextViewText(R.id.widget_temperature_text, tempText)
            setTextViewText(R.id.widget_condition_text, conditionText)
            setTextViewText(R.id.widget_high_low_text, highLowText)
            setTextViewText(R.id.widget_updated_text, updatedText)
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

    private fun conditionEmoji(condition: WeatherConditionType, wmoCode: Int): String {
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
