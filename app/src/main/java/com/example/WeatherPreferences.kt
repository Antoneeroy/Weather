package com.example

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.weatherDataStore by preferencesDataStore(name = "weather_phase5_preferences")

/**
 * Theme appearance options for Phase 5 Settings & Customization.
 */
enum class ThemeModeOption(val label: String, val subtitle: String) {
    DARK("Dark Theme", "Deep celestial blue & purple glassmorphism"),
    LIGHT("Light Theme", "Luminous daylight sky glassmorphism"),
    AUTO_SYSTEM("Automatic", "Follow Android system Dark / Light mode")
}

/**
 * Scheduled background refresh interval options for WorkManager.
 */
enum class RefreshIntervalOption(val label: String, val intervalMinutes: Long) {
    EVERY_30_MIN("Every 30 Minutes", 30L),
    EVERY_1_HOUR("Every 1 Hour", 60L),
    EVERY_3_HOURS("Every 3 Hours", 180L),
    EVERY_6_HOURS("Every 6 Hours", 360L),
    MANUAL_ONLY("Manual Refresh Only", 0L)
}

/**
 * User-selectable daily notification time options.
 */
enum class NotificationTimeOption(val label: String, val hour24: Int, val minute: Int) {
    TIME_07_00("7:00 AM", 7, 0),
    TIME_08_00("8:00 AM", 8, 0),
    TIME_09_00("9:00 AM", 9, 0),
    TIME_18_00("6:00 PM", 18, 0),
    TIME_20_00("8:00 PM", 20, 0)
}

/**
 * Lightweight persisted descriptor for user-added cities.
 */
data class SavedCityRecord(
    val id: String,
    val cityName: String,
    val regionAndCountry: String,
    val latitude: Double,
    val longitude: Double,
    val isFavorite: Boolean = false
)

/**
 * Snapshot stored in offline cache when a live Open-Meteo request succeeds.
 */
data class CachedCityWeatherSnapshot(
    val cityId: String,
    val cachedAtFormatted: String,
    val cachedAtEpochMillis: Long,
    val weatherData: CityWeatherData
)

/**
 * Persistent preferences and offline weather cache manager (Phase 5).
 */
class WeatherPreferences(private val context: Context) {

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode_option")
        val KEY_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val KEY_TEMP_UNIT = stringPreferencesKey("temp_unit")
        val KEY_ANIMATIONS = booleanPreferencesKey("animations_enabled")
        val KEY_ANIM_QUALITY = stringPreferencesKey("animation_quality")
        val KEY_BATTERY_SAVER = booleanPreferencesKey("battery_saver_mode")

        // Phase 5 Notification Settings
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_NOTIFY_DAILY_SUMMARY = booleanPreferencesKey("notify_daily_summary")
        val KEY_NOTIFY_MORNING = booleanPreferencesKey("notify_morning_weather")
        val KEY_NOTIFY_RAIN = booleanPreferencesKey("notify_rain_alert")
        val KEY_NOTIFY_SEVERE = booleanPreferencesKey("notify_severe_weather")
        val KEY_NOTIFY_TEMP_CHANGE = booleanPreferencesKey("notify_temp_change")
        val KEY_NOTIFICATION_TIME = stringPreferencesKey("notification_time_option")

        // Phase 5 City & Refresh Settings
        val KEY_SELECTED_CITY_ID = stringPreferencesKey("selected_city_id")
        val KEY_DEFAULT_CITY_ID = stringPreferencesKey("default_city_id")
        val KEY_FAVORITE_CITY_IDS = stringSetPreferencesKey("favorite_city_ids")
        val KEY_REFRESH_INTERVAL = stringPreferencesKey("refresh_interval_option")
        val KEY_SAVED_CUSTOM_CITIES_JSON = stringPreferencesKey("saved_custom_cities_json")
        val KEY_OFFLINE_WEATHER_CACHE_JSON = stringPreferencesKey("offline_weather_cache_json")

        private val moshi: Moshi by lazy {
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
        }

        private val savedCityListType = Types.newParameterizedType(
            List::class.java,
            SavedCityRecord::class.java
        )
        private val savedCitiesAdapter by lazy {
            moshi.adapter<List<SavedCityRecord>>(savedCityListType)
        }

        private val cachedSnapshotMapType = Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            CachedCityWeatherSnapshot::class.java
        )
        private val cachedSnapshotsAdapter by lazy {
            moshi.adapter<Map<String, CachedCityWeatherSnapshot>>(cachedSnapshotMapType)
        }
    }

    val settingsFlow: Flow<WeatherAppSettings> = context.weatherDataStore.data.map { prefs ->
        val themeModeName = prefs[KEY_THEME_MODE] ?: ThemeModeOption.DARK.name
        val themeMode = runCatching { ThemeModeOption.valueOf(themeModeName) }
            .getOrDefault(ThemeModeOption.DARK)
        val dark = prefs[KEY_DARK_THEME] ?: (themeMode != ThemeModeOption.LIGHT)
        val unitName = prefs[KEY_TEMP_UNIT] ?: TemperatureUnit.CELSIUS.name
        val unit = runCatching { TemperatureUnit.valueOf(unitName) }
            .getOrDefault(TemperatureUnit.CELSIUS)
        val anim = prefs[KEY_ANIMATIONS] ?: true
        val qualityName = prefs[KEY_ANIM_QUALITY] ?: AnimationQualityLevel.HIGH.name
        val quality = runCatching { AnimationQualityLevel.valueOf(qualityName) }
            .getOrDefault(AnimationQualityLevel.HIGH)
        val batterySaver = prefs[KEY_BATTERY_SAVER] ?: false

        val notificationsEnabled = prefs[KEY_NOTIFICATIONS_ENABLED] ?: true
        val dailySummary = prefs[KEY_NOTIFY_DAILY_SUMMARY] ?: true
        val morningWeather = prefs[KEY_NOTIFY_MORNING] ?: true
        val rainAlert = prefs[KEY_NOTIFY_RAIN] ?: true
        val severeAlert = prefs[KEY_NOTIFY_SEVERE] ?: true
        val tempChange = prefs[KEY_NOTIFY_TEMP_CHANGE] ?: true
        val notifyTimeName = prefs[KEY_NOTIFICATION_TIME] ?: NotificationTimeOption.TIME_08_00.name
        val notifyTime = runCatching { NotificationTimeOption.valueOf(notifyTimeName) }
            .getOrDefault(NotificationTimeOption.TIME_08_00)

        val defaultCityId = prefs[KEY_DEFAULT_CITY_ID] ?: ""
        val refreshIntervalName = prefs[KEY_REFRESH_INTERVAL] ?: RefreshIntervalOption.EVERY_1_HOUR.name
        val refreshInterval = runCatching { RefreshIntervalOption.valueOf(refreshIntervalName) }
            .getOrDefault(RefreshIntervalOption.EVERY_1_HOUR)

        WeatherAppSettings(
            themeMode = themeMode,
            isDarkTheme = dark,
            temperatureUnit = unit,
            animationsEnabled = anim,
            animationQuality = quality,
            batterySaverMode = batterySaver,
            notificationsEnabled = notificationsEnabled,
            dailySummaryNotification = dailySummary,
            morningWeatherNotification = morningWeather,
            rainNotification = rainAlert,
            severeWeatherNotification = severeAlert,
            temperatureChangeNotification = tempChange,
            notificationTime = notifyTime,
            defaultCityId = defaultCityId,
            refreshInterval = refreshInterval
        )
    }

    val favoriteCityIdsFlow: Flow<Set<String>> = context.weatherDataStore.data.map { prefs ->
        prefs[KEY_FAVORITE_CITY_IDS] ?: emptySet()
    }

    val selectedCityIdFlow: Flow<String?> = context.weatherDataStore.data.map { prefs ->
        prefs[KEY_SELECTED_CITY_ID] ?: prefs[KEY_DEFAULT_CITY_ID]?.takeIf { it.isNotBlank() }
    }

    suspend fun saveSettings(settings: WeatherAppSettings) {
        context.weatherDataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = settings.themeMode.name
            prefs[KEY_DARK_THEME] = settings.isDarkTheme
            prefs[KEY_TEMP_UNIT] = settings.temperatureUnit.name
            prefs[KEY_ANIMATIONS] = settings.animationsEnabled
            prefs[KEY_ANIM_QUALITY] = settings.animationQuality.name
            prefs[KEY_BATTERY_SAVER] = settings.batterySaverMode
            prefs[KEY_NOTIFICATIONS_ENABLED] = settings.notificationsEnabled
            prefs[KEY_NOTIFY_DAILY_SUMMARY] = settings.dailySummaryNotification
            prefs[KEY_NOTIFY_MORNING] = settings.morningWeatherNotification
            prefs[KEY_NOTIFY_RAIN] = settings.rainNotification
            prefs[KEY_NOTIFY_SEVERE] = settings.severeWeatherNotification
            prefs[KEY_NOTIFY_TEMP_CHANGE] = settings.temperatureChangeNotification
            prefs[KEY_NOTIFICATION_TIME] = settings.notificationTime.name
            prefs[KEY_DEFAULT_CITY_ID] = settings.defaultCityId
            prefs[KEY_REFRESH_INTERVAL] = settings.refreshInterval.name
        }
    }

    suspend fun saveSelectedCityId(cityId: String?) {
        context.weatherDataStore.edit { prefs ->
            if (cityId.isNullOrBlank()) {
                prefs.remove(KEY_SELECTED_CITY_ID)
            } else {
                prefs[KEY_SELECTED_CITY_ID] = cityId
            }
        }
    }

    suspend fun saveFavoriteCityIds(favoriteIds: Set<String>) {
        context.weatherDataStore.edit { prefs ->
            prefs[KEY_FAVORITE_CITY_IDS] = favoriteIds
        }
    }

    suspend fun saveCustomCitiesList(cities: List<CityWeatherData>, favoriteIds: Set<String>) {
        val records = cities.map { city ->
            SavedCityRecord(
                id = city.id,
                cityName = city.cityName,
                regionAndCountry = city.regionAndCountry,
                latitude = city.latitude,
                longitude = city.longitude,
                isFavorite = favoriteIds.contains(city.id)
            )
        }
        val json = runCatching { savedCitiesAdapter.toJson(records) }.getOrNull() ?: return
        context.weatherDataStore.edit { prefs ->
            prefs[KEY_SAVED_CUSTOM_CITIES_JSON] = json
        }
    }

    suspend fun loadSavedCitiesRecords(): List<SavedCityRecord> {
        val prefs = context.weatherDataStore.data.first()
        val json = prefs[KEY_SAVED_CUSTOM_CITIES_JSON] ?: return emptyList()
        return runCatching { savedCitiesAdapter.fromJson(json).orEmpty() }
            .getOrDefault(emptyList())
    }

    /**
     * Caches a live Open-Meteo [CityWeatherData] response for offline viewing.
     */
    suspend fun cacheLiveCityWeather(
        city: CityWeatherData,
        timestampFormatted: String
    ) {
        val prefs = context.weatherDataStore.data.first()
        val existingJson = prefs[KEY_OFFLINE_WEATHER_CACHE_JSON]
        val currentMap = if (!existingJson.isNullOrBlank()) {
            runCatching { cachedSnapshotsAdapter.fromJson(existingJson).orEmpty().toMutableMap() }
                .getOrDefault(mutableMapOf())
        } else {
            mutableMapOf()
        }

        currentMap[city.id] = CachedCityWeatherSnapshot(
            cityId = city.id,
            cachedAtFormatted = timestampFormatted,
            cachedAtEpochMillis = System.currentTimeMillis(),
            weatherData = city.copy(
                isLiveData = false,
                isCachedOfflineData = true,
                cachedTimestampLabel = timestampFormatted
            )
        )

        val updatedJson = runCatching { cachedSnapshotsAdapter.toJson(currentMap) }.getOrNull() ?: return
        context.weatherDataStore.edit { mutPrefs ->
            mutPrefs[KEY_OFFLINE_WEATHER_CACHE_JSON] = updatedJson
        }
    }

    /**
     * Loads a previously cached [CachedCityWeatherSnapshot] when the device has no internet connection.
     * Ensures `isLiveData = false` and `isCachedOfflineData = true` so cached data is never shown as live.
     */
    suspend fun getCachedCityWeather(cityId: String): CachedCityWeatherSnapshot? {
        val prefs = context.weatherDataStore.data.first()
        val existingJson = prefs[KEY_OFFLINE_WEATHER_CACHE_JSON] ?: return null
        val map = runCatching { cachedSnapshotsAdapter.fromJson(existingJson).orEmpty() }
            .getOrDefault(emptyMap())
        val snapshot = map[cityId] ?: return null
        return snapshot.copy(
            weatherData = snapshot.weatherData.copy(
                isLiveData = false,
                isCachedOfflineData = true,
                cachedTimestampLabel = snapshot.cachedAtFormatted
            )
        )
    }

    /**
     * Loads all cached city snapshots so saved cities immediately have their last cached weather offline.
     */
    suspend fun getAllCachedSnapshots(): Map<String, CachedCityWeatherSnapshot> {
        val prefs = context.weatherDataStore.data.first()
        val existingJson = prefs[KEY_OFFLINE_WEATHER_CACHE_JSON] ?: return emptyMap()
        return runCatching { cachedSnapshotsAdapter.fromJson(existingJson).orEmpty() }
            .getOrDefault(emptyMap())
    }
}
