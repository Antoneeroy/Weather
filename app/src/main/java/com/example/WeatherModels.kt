package com.example

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

enum class WeatherConditionType(
    val displayName: String,
    val subtitle: String
) {
    SUNNY("Sunny", "Golden sunshine & clear skies"),
    PARTLY_CLOUDY("Partly Cloudy", "Filtered sun with drifting clouds"),
    RAINY("Rain Showers", "Cool rain & atmospheric mist"),
    CLEAR_NIGHT("Clear Night", "Starlit sky & calm evening breeze"),
    THUNDERSTORM("Thunderstorm", "Electric storm clouds & heavy rain"),
    SNOWY("Alpine Snow", "Crisp air & softly falling snow")
}

enum class TemperatureUnit(val symbol: String, val label: String) {
    CELSIUS("°C", "Celsius (°C)"),
    FAHRENHEIT("°F", "Fahrenheit (°F)")
}

enum class WeatherDestination(val route: String, val label: String, val titleRes: Int) {
    HOME("home", "Home", R.string.nav_home),
    FORECAST("forecast", "10-Day", R.string.nav_forecast),
    CITIES("cities", "Cities", R.string.nav_cities),
    SETTINGS("settings", "Settings", R.string.nav_settings)
}

/**
 * Represents the live network synchronization state for Phase 2–5.
 */
sealed class WeatherDataSyncState {
    data object Loading : WeatherDataSyncState()
    data class Success(val updatedAtTime: String) : WeatherDataSyncState()
    data class NoInternet(val message: String) : WeatherDataSyncState()
    data class Error(val message: String) : WeatherDataSyncState()
}

/**
 * Represents the GPS location detection state.
 */
sealed class GpsLocationStatus {
    data object Idle : GpsLocationStatus()
    data object Detecting : GpsLocationStatus()
    data class Active(val cityName: String) : GpsLocationStatus()
    data object PermissionDenied : GpsLocationStatus()
    data class Unavailable(val reason: String) : GpsLocationStatus()
}

data class AtmosphericDetails(
    val humidityPercent: Int,
    val dewPointCelsius: Int,
    val windSpeedKmh: Int,
    val windDirection: String,
    val windGustKmh: Int,
    val pressureHpa: Int,
    val pressureTrend: String,
    val visibilityKm: Double,
    val visibilityStatus: String,
    val uvIndex: Int,
    val uvCategory: String,
    val sunriseTime: String,
    val sunsetTime: String,
    val daylightDuration: String
)

data class CityWeatherData(
    val id: String,
    val cityName: String,
    val regionAndCountry: String,
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194,
    val timeZoneOffsetLabel: String,
    val currentTimeDisplay: String,
    val currentDateDisplay: String,
    val currentTempCelsius: Int,
    val feelsLikeCelsius: Int,
    val highTempCelsius: Int,
    val lowTempCelsius: Int,
    val condition: WeatherConditionType,
    val wmoCode: Int = 0,
    val conditionDescription: String,
    val airQualityIndex: Int,
    val airQualityLabel: String,
    val details: AtmosphericDetails,
    val hourlyForecast: List<HourlyForecastItem>,
    val tenDayForecast: List<DailyForecastItem>,
    val isCurrentLocation: Boolean = false,
    val isLiveData: Boolean = false,
    val isCachedOfflineData: Boolean = false,
    val cachedTimestampLabel: String? = null,
    val isFavorite: Boolean = false
)

data class WeatherAppSettings(
    val themeMode: ThemeModeOption = ThemeModeOption.DARK,
    val isDarkTheme: Boolean = true,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val animationsEnabled: Boolean = true,
    val animationQuality: AnimationQualityLevel = AnimationQualityLevel.HIGH,
    val batterySaverMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val dailySummaryNotification: Boolean = true,
    val morningWeatherNotification: Boolean = true,
    val rainNotification: Boolean = true,
    val severeWeatherNotification: Boolean = true,
    val temperatureChangeNotification: Boolean = true,
    val notificationTime: NotificationTimeOption = NotificationTimeOption.TIME_08_00,
    val defaultCityId: String = "",
    val refreshInterval: RefreshIntervalOption = RefreshIntervalOption.EVERY_1_HOUR
) {
    fun toAnimationConfig(): WeatherAnimationConfig = WeatherAnimationConfig(
        animationsEnabled = animationsEnabled,
        qualityLevel = animationQuality,
        batterySaverMode = batterySaverMode
    )
}

data class WeatherUiState(
    val cities: List<CityWeatherData> = emptyList(),
    val selectedCityId: String? = null,
    val favoriteCityIds: Set<String> = emptySet(),
    val weatherOverride: WeatherConditionType? = null,
    val sceneOverride: RealisticWeatherScene? = null,
    val settings: WeatherAppSettings = WeatherAppSettings(),
    val isRefreshing: Boolean = false,
    val syncState: WeatherDataSyncState = WeatherDataSyncState.Success("Ready"),
    val lastUpdatedTime: String? = null,
    val gpsStatus: GpsLocationStatus = GpsLocationStatus.Idle,
    val searchQuery: String = "",
    val isSearchingCities: Boolean = false,
    val geocodingSuggestions: List<GeocodingLocationDto> = emptyList(),
    val searchErrorMessage: String? = null,
    val selectedDayForDetail: DailyForecastItem? = null,
    val isShowingDayDetailScreen: Boolean = false,
    val notificationStatusMessage: String? = null
) {
    val activeCity: CityWeatherData?
        get() = selectedCityId?.let { id -> cities.find { it.id == id } } ?: cities.firstOrNull()

    val activeSceneParameters: WeatherSceneParameters
        get() = activeCity?.let { city ->
            WeatherAnimationEngine.resolveSceneParameters(
                city = city,
                conditionOverride = weatherOverride,
                sceneOverride = sceneOverride
            )
        } ?: WeatherAnimationEngine.resolveDefaultSceneParameters(
            conditionOverride = weatherOverride,
            sceneOverride = sceneOverride
        )

    val effectiveCondition: WeatherConditionType
        get() = sceneOverride?.baseCondition ?: weatherOverride ?: activeCity?.condition ?: WeatherConditionType.SUNNY

    val activeAlerts: List<WeatherAlertItem>
        get() = activeCity?.let { city ->
            WeatherAlertManager.evaluateAlertsForCity(
                city = city,
                temperatureUnit = settings.temperatureUnit
            )
        } ?: emptyList()
}

fun formatTemperature(tempCelsius: Int, unit: TemperatureUnit, includeUnitSymbol: Boolean = true): String {
    val value = when (unit) {
        TemperatureUnit.CELSIUS -> tempCelsius
        TemperatureUnit.FAHRENHEIT -> ((tempCelsius * 9.0 / 5.0) + 32.0).toInt()
    }
    return if (includeUnitSymbol) "$value${unit.symbol}" else "$value°"
}

object SampleWeatherRepository {

    fun getCurrentDateFormatted(): String {
        val formatter = SimpleDateFormat("EEEE, MMM d", Locale.US)
        return formatter.format(Date())
    }

    fun getCurrentTimeFormatted(): String {
        val formatter = SimpleDateFormat("h:mm a", Locale.US)
        return formatter.format(Date())
    }

    /**
     * Generates a realistic 24-hour (`12 AM` to `11 PM`) hourly forecast specifically for a given forecast day,
     * following a natural diurnal temperature curve between [minTempCelsius] and [maxTempCelsius]
     * and reflecting that specific day's weather condition, WMO code, rain probability, and wind speed.
     */
    fun generate24HoursForDay(
        dayId: String,
        minTempCelsius: Int,
        maxTempCelsius: Int,
        condition: WeatherConditionType,
        wmoCode: Int,
        rainProbability: Int,
        precipitationSumMm: Double,
        windKmh: Int,
        humidityPercent: Int,
        isToday: Boolean = false
    ): List<HourlyForecastItem> {
        val hourLabels = listOf(
            "12 AM", "1 AM", "2 AM", "3 AM", "4 AM", "5 AM",
            "6 AM", "7 AM", "8 AM", "9 AM", "10 AM", "11 AM",
            "12 PM", "1 PM", "2 PM", "3 PM", "4 PM", "5 PM",
            "6 PM", "7 PM", "8 PM", "9 PM", "10 PM", "11 PM"
        )
        val tempSpan = (maxTempCelsius - minTempCelsius).coerceAtLeast(1)

        return hourLabels.mapIndexed { hour24, hourLabel ->
            // Peak temperature around 2 PM – 3 PM (hour 14-15), minimum around 5 AM
            val phase = ((hour24 - 5).toDouble() / 24.0) * 2.0 * PI
            val normalizedCurve = ((sin(phase - PI / 2.0) + 1.0) / 2.0).coerceIn(0.0, 1.0)
            val hourlyTemp = (minTempCelsius + (tempSpan * normalizedCurve)).roundToInt()
            val hourlyFeelsLike = hourlyTemp + when {
                windKmh >= 24 -> -2
                humidityPercent >= 80 && hourlyTemp >= 25 -> 2
                else -> -1
            }

            val isDaylight = hour24 in 6..17
            val hourCondition = when {
                !isDaylight && condition == WeatherConditionType.SUNNY -> WeatherConditionType.CLEAR_NIGHT
                else -> condition
            }
            val shortLabel = WmoWeatherCodeMapper.mapToShortLabel(wmoCode, if (isDaylight) 1 else 0)

            val popVariation = when (hour24 % 4) {
                0 -> 0
                1 -> 5
                2 -> -5
                else -> 3
            }
            val hourlyPop = if (rainProbability == 0) 0 else (rainProbability + popVariation).coerceIn(5, 100)
            val hourlyPrecip = if (hourlyPop >= 40 && precipitationSumMm > 0.0) {
                ((precipitationSumMm / 8.0) * (0.7 + (hour24 % 3) * 0.3) * 10.0).roundToInt() / 10.0
            } else {
                0.0
            }

            val hourlyWind = (windKmh - 4 + (hour24 % 5) * 2).coerceAtLeast(4)
            val windDeg = (240 + (hour24 * 10)) % 360
            val windComp = WmoWeatherCodeMapper.degreesToCompass(windDeg)
            val hourlyHumidity = (humidityPercent + if (isDaylight) -4 else 5).coerceIn(25, 99)

            HourlyForecastItem(
                id = "${dayId}_h$hour24",
                timeLabel = hourLabel,
                clockTimeLabel = hourLabel,
                hour24 = hour24,
                fullIsoTime = String.format(Locale.US, "%02d:00", hour24),
                tempCelsius = hourlyTemp,
                feelsLikeCelsius = hourlyFeelsLike,
                condition = hourCondition,
                wmoCode = wmoCode,
                conditionLabel = shortLabel,
                popPercent = hourlyPop,
                precipitationMm = hourlyPrecip,
                windSpeedKmh = hourlyWind,
                windDirectionDegrees = windDeg,
                windDirectionCompass = windComp,
                relativeHumidityPercent = hourlyHumidity,
                isNow = isToday && hour24 == 14
            )
        }
    }

    fun getInitialCities(): List<CityWeatherData> = emptyList()

    val discoverableCities: List<CityWeatherData> = emptyList()
}
