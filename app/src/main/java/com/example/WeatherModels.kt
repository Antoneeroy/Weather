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
    val refreshInterval: RefreshIntervalOption = RefreshIntervalOption.EVERY_1_HOUR,
    val defaultCityId: String = "sf_us"
) {
    fun toAnimationConfig(): WeatherAnimationConfig = WeatherAnimationConfig(
        animationsEnabled = animationsEnabled,
        qualityLevel = animationQuality,
        batterySaverMode = batterySaverMode
    )
}

data class WeatherUiState(
    val cities: List<CityWeatherData> = SampleWeatherRepository.getInitialCities(),
    val selectedCityId: String = "sf_us",
    val favoriteCityIds: Set<String> = setOf("sf_us"),
    val weatherOverride: WeatherConditionType? = null,
    val sceneOverride: RealisticWeatherScene? = null,
    val settings: WeatherAppSettings = WeatherAppSettings(),
    val isRefreshing: Boolean = false,
    val syncState: WeatherDataSyncState = WeatherDataSyncState.Loading,
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
    val activeCity: CityWeatherData
        get() = cities.find { it.id == selectedCityId } ?: cities.first()

    val activeSceneParameters: WeatherSceneParameters
        get() = WeatherAnimationEngine.resolveSceneParameters(
            city = activeCity,
            conditionOverride = weatherOverride,
            sceneOverride = sceneOverride
        )

    val effectiveCondition: WeatherConditionType
        get() = sceneOverride?.baseCondition ?: weatherOverride ?: activeCity.condition

    val activeAlerts: List<WeatherAlertItem>
        get() = WeatherAlertManager.evaluateAlertsForCity(
            city = activeCity,
            temperatureUnit = settings.temperatureUnit
        )
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

    private fun createDailyForecastWith24Hours(
        id: String,
        dayName: String,
        dateShort: String,
        fullDateFormatted: String,
        condition: WeatherConditionType,
        wmoCode: Int,
        conditionLabel: String,
        minTempCelsius: Int,
        maxTempCelsius: Int,
        rainProbability: Int,
        precipitationSumMm: Double,
        sunriseTime: String,
        sunsetTime: String,
        windKmh: Int,
        uvIndex: Int,
        humidityPercent: Int
    ): DailyForecastItem {
        val isToday = dayName.equals("Today", ignoreCase = true)
        return DailyForecastItem(
            id = id,
            dayName = dayName,
            dateShort = dateShort,
            fullDateFormatted = fullDateFormatted,
            condition = condition,
            wmoCode = wmoCode,
            conditionLabel = conditionLabel,
            minTempCelsius = minTempCelsius,
            maxTempCelsius = maxTempCelsius,
            rainProbability = rainProbability,
            precipitationSumMm = precipitationSumMm,
            sunriseTime = sunriseTime,
            sunsetTime = sunsetTime,
            windKmh = windKmh,
            uvIndex = uvIndex,
            humidityPercent = humidityPercent,
            hourlyItemsForDay = generate24HoursForDay(
                dayId = id,
                minTempCelsius = minTempCelsius,
                maxTempCelsius = maxTempCelsius,
                condition = condition,
                wmoCode = wmoCode,
                rainProbability = rainProbability,
                precipitationSumMm = precipitationSumMm,
                windKmh = windKmh,
                humidityPercent = humidityPercent,
                isToday = isToday
            )
        )
    }

    private fun buildDefault24Hours(
        prefix: String,
        minTemp: Int,
        maxTemp: Int,
        baseCondition: WeatherConditionType,
        baseWmo: Int,
        basePop: Int,
        basePrecipSum: Double,
        baseWind: Int,
        baseHumidity: Int
    ): List<HourlyForecastItem> {
        return generate24HoursForDay(
            dayId = prefix,
            minTempCelsius = minTemp,
            maxTempCelsius = maxTemp,
            condition = baseCondition,
            wmoCode = baseWmo,
            rainProbability = basePop,
            precipitationSumMm = basePrecipSum,
            windKmh = baseWind,
            humidityPercent = baseHumidity,
            isToday = true
        )
    }

    fun getInitialCities(): List<CityWeatherData> {
        val todayStr = getCurrentDateFormatted()
        val sfHours = buildDefault24Hours("sf", 13, 23, WeatherConditionType.PARTLY_CLOUDY, 2, 10, 0.0, 19, 68)
        val tokyoHours = buildDefault24Hours("ty", 11, 19, WeatherConditionType.CLEAR_NIGHT, 0, 5, 0.0, 12, 54)
        val londonHours = buildDefault24Hours("ln", 8, 14, WeatherConditionType.RAINY, 61, 80, 5.4, 26, 88)
        val dubaiHours = buildDefault24Hours("db", 24, 34, WeatherConditionType.SUNNY, 0, 0, 0.0, 14, 41)
        val zermattHours = buildDefault24Hours("zm", -8, 2, WeatherConditionType.SNOWY, 73, 75, 9.5, 15, 82)

        return listOf(
            CityWeatherData(
                id = "sf_us",
                cityName = "San Francisco",
                regionAndCountry = "California, United States",
                latitude = 37.7749,
                longitude = -122.4194,
                timeZoneOffsetLabel = "PST • GMT-8",
                currentTimeDisplay = "5:28 PM",
                currentDateDisplay = todayStr,
                currentTempCelsius = 21,
                feelsLikeCelsius = 20,
                highTempCelsius = 23,
                lowTempCelsius = 13,
                condition = WeatherConditionType.PARTLY_CLOUDY,
                wmoCode = 2,
                conditionDescription = "Partly Cloudy • Coastal Breeze",
                airQualityIndex = 24,
                airQualityLabel = "Good (AQI 24)",
                isCurrentLocation = false,
                isLiveData = false,
                isFavorite = true,
                details = AtmosphericDetails(
                    humidityPercent = 68,
                    dewPointCelsius = 14,
                    windSpeedKmh = 19,
                    windDirection = "WNW (290°)",
                    windGustKmh = 28,
                    pressureHpa = 1018,
                    pressureTrend = "Steady • High pressure ridge",
                    visibilityKm = 16.0,
                    visibilityStatus = "Crystal clear horizon",
                    uvIndex = 4,
                    uvCategory = "Moderate",
                    sunriseTime = "6:42 AM",
                    sunsetTime = "5:08 PM",
                    daylightDuration = "10h 26m"
                ),
                hourlyForecast = sfHours,
                tenDayForecast = listOf(
                    createDailyForecastWith24Hours("sf_d1", "Today", "Nov 6", "Friday, Nov 6", WeatherConditionType.PARTLY_CLOUDY, 2, "Partly Cloudy", 13, 23, 10, 0.0, "6:42 AM", "5:08 PM", 19, 4, 68),
                    createDailyForecastWith24Hours("sf_d2", "Sat", "Nov 7", "Saturday, Nov 7", WeatherConditionType.SUNNY, 0, "Clear Sky", 14, 25, 5, 0.0, "6:43 AM", "5:07 PM", 14, 6, 55),
                    createDailyForecastWith24Hours("sf_d3", "Sun", "Nov 8", "Sunday, Nov 8", WeatherConditionType.SUNNY, 1, "Mainly Clear", 14, 24, 10, 0.0, "6:44 AM", "5:06 PM", 16, 6, 58),
                    createDailyForecastWith24Hours("sf_d4", "Mon", "Nov 9", "Monday, Nov 9", WeatherConditionType.RAINY, 63, "Moderate Rain", 11, 18, 80, 8.4, "6:45 AM", "5:05 PM", 29, 2, 86),
                    createDailyForecastWith24Hours("sf_d5", "Tue", "Nov 10", "Tuesday, Nov 10", WeatherConditionType.RAINY, 80, "Rain Showers", 10, 17, 65, 4.2, "6:46 AM", "5:04 PM", 24, 3, 82),
                    createDailyForecastWith24Hours("sf_d6", "Wed", "Nov 11", "Wednesday, Nov 11", WeatherConditionType.PARTLY_CLOUDY, 2, "Partly Cloudy", 12, 20, 20, 0.3, "6:47 AM", "5:03 PM", 18, 4, 70),
                    createDailyForecastWith24Hours("sf_d7", "Thu", "Nov 12", "Thursday, Nov 12", WeatherConditionType.SUNNY, 0, "Clear Sky", 13, 22, 5, 0.0, "6:48 AM", "5:02 PM", 12, 5, 60),
                    createDailyForecastWith24Hours("sf_d8", "Fri", "Nov 13", "Friday, Nov 13", WeatherConditionType.PARTLY_CLOUDY, 3, "Overcast", 12, 21, 15, 0.0, "6:49 AM", "5:01 PM", 15, 5, 64),
                    createDailyForecastWith24Hours("sf_d9", "Sat", "Nov 14", "Saturday, Nov 14", WeatherConditionType.THUNDERSTORM, 95, "Thunderstorm", 10, 16, 90, 14.6, "6:50 AM", "5:01 PM", 38, 1, 92),
                    createDailyForecastWith24Hours("sf_d10", "Sun", "Nov 15", "Sunday, Nov 15", WeatherConditionType.PARTLY_CLOUDY, 2, "Partly Cloudy", 11, 19, 25, 0.8, "6:51 AM", "5:00 PM", 22, 4, 72)
                )
            ),
            CityWeatherData(
                id = "tokyo_jp",
                cityName = "Tokyo",
                regionAndCountry = "Kanto, Japan",
                latitude = 35.6762,
                longitude = 139.6503,
                timeZoneOffsetLabel = "JST • GMT+9",
                currentTimeDisplay = "10:28 AM",
                currentDateDisplay = todayStr,
                currentTempCelsius = 16,
                feelsLikeCelsius = 15,
                highTempCelsius = 19,
                lowTempCelsius = 11,
                condition = WeatherConditionType.CLEAR_NIGHT,
                wmoCode = 0,
                conditionDescription = "Starlit Metropolis • Crisp Air",
                airQualityIndex = 18,
                airQualityLabel = "Excellent (AQI 18)",
                isCurrentLocation = false,
                isLiveData = false,
                details = AtmosphericDetails(
                    humidityPercent = 54,
                    dewPointCelsius = 7,
                    windSpeedKmh = 12,
                    windDirection = "NNE (25°)",
                    windGustKmh = 18,
                    pressureHpa = 1024,
                    pressureTrend = "Rising • Clear dome",
                    visibilityKm = 20.0,
                    visibilityStatus = "Unlimited visibility",
                    uvIndex = 1,
                    uvCategory = "Low",
                    sunriseTime = "6:09 AM",
                    sunsetTime = "4:41 PM",
                    daylightDuration = "10h 32m"
                ),
                hourlyForecast = tokyoHours,
                tenDayForecast = listOf(
                    createDailyForecastWith24Hours("ty_d1", "Today", "Nov 6", "Friday, Nov 6", WeatherConditionType.CLEAR_NIGHT, 0, "Clear Sky", 11, 19, 5, 0.0, "6:09 AM", "4:41 PM", 12, 5, 54),
                    createDailyForecastWith24Hours("ty_d2", "Sat", "Nov 7", "Saturday, Nov 7", WeatherConditionType.SUNNY, 0, "Clear Sky", 12, 21, 0, 0.0, "6:10 AM", "4:40 PM", 10, 5, 48),
                    createDailyForecastWith24Hours("ty_d3", "Sun", "Nov 8", "Sunday, Nov 8", WeatherConditionType.PARTLY_CLOUDY, 2, "Partly Cloudy", 11, 18, 20, 0.2, "6:11 AM", "4:39 PM", 14, 4, 60),
                    createDailyForecastWith24Hours("ty_d4", "Mon", "Nov 9", "Monday, Nov 9", WeatherConditionType.RAINY, 61, "Light Rain", 9, 15, 75, 6.5, "6:12 AM", "4:39 PM", 22, 2, 84),
                    createDailyForecastWith24Hours("ty_d5", "Tue", "Nov 10", "Tuesday, Nov 10", WeatherConditionType.SUNNY, 1, "Mainly Clear", 10, 19, 10, 0.0, "6:13 AM", "4:38 PM", 18, 5, 52),
                    createDailyForecastWith24Hours("ty_d6", "Wed", "Nov 11", "Wednesday, Nov 11", WeatherConditionType.SUNNY, 0, "Clear Sky", 11, 20, 5, 0.0, "6:14 AM", "4:37 PM", 11, 5, 50),
                    createDailyForecastWith24Hours("ty_d7", "Thu", "Nov 12", "Thursday, Nov 12", WeatherConditionType.PARTLY_CLOUDY, 2, "Partly Cloudy", 10, 18, 15, 0.0, "6:15 AM", "4:36 PM", 13, 4, 58),
                    createDailyForecastWith24Hours("ty_d8", "Fri", "Nov 13", "Friday, Nov 13", WeatherConditionType.RAINY, 80, "Rain Showers", 9, 16, 60, 3.8, "6:16 AM", "4:36 PM", 20, 2, 78),
                    createDailyForecastWith24Hours("ty_d9", "Sat", "Nov 14", "Saturday, Nov 14", WeatherConditionType.SUNNY, 0, "Clear Sky", 8, 17, 5, 0.0, "6:17 AM", "4:35 PM", 15, 4, 49),
                    createDailyForecastWith24Hours("ty_d10", "Sun", "Nov 15", "Sunday, Nov 15", WeatherConditionType.SUNNY, 0, "Clear Sky", 9, 18, 0, 0.0, "6:18 AM", "4:34 PM", 10, 5, 47)
                )
            ),
            CityWeatherData(
                id = "london_uk",
                cityName = "London",
                regionAndCountry = "Greater London, United Kingdom",
                latitude = 51.5074,
                longitude = -0.1278,
                timeZoneOffsetLabel = "GMT • UTC+0",
                currentTimeDisplay = "1:28 AM",
                currentDateDisplay = todayStr,
                currentTempCelsius = 11,
                feelsLikeCelsius = 9,
                highTempCelsius = 14,
                lowTempCelsius = 8,
                condition = WeatherConditionType.RAINY,
                wmoCode = 61,
                conditionDescription = "Light Rain Showers • Misty",
                airQualityIndex = 31,
                airQualityLabel = "Good (AQI 31)",
                isCurrentLocation = false,
                isLiveData = false,
                details = AtmosphericDetails(
                    humidityPercent = 88,
                    dewPointCelsius = 9,
                    windSpeedKmh = 26,
                    windDirection = "SW (225°)",
                    windGustKmh = 41,
                    pressureHpa = 1006,
                    pressureTrend = "Falling • Atlantic front",
                    visibilityKm = 7.5,
                    visibilityStatus = "Moderate mist in basin",
                    uvIndex = 2,
                    uvCategory = "Low",
                    sunriseTime = "7:04 AM",
                    sunsetTime = "4:24 PM",
                    daylightDuration = "9h 20m"
                ),
                hourlyForecast = londonHours,
                tenDayForecast = listOf(
                    createDailyForecastWith24Hours("ln_d1", "Today", "Nov 6", "Friday, Nov 6", WeatherConditionType.RAINY, 61, "Light Rain", 8, 14, 85, 5.4, "7:04 AM", "4:24 PM", 26, 2, 88),
                    createDailyForecastWith24Hours("ln_d2", "Sat", "Nov 7", "Saturday, Nov 7", WeatherConditionType.RAINY, 53, "Moderate Drizzle", 7, 13, 70, 3.1, "7:06 AM", "4:22 PM", 22, 2, 84),
                    createDailyForecastWith24Hours("ln_d3", "Sun", "Nov 8", "Sunday, Nov 8", WeatherConditionType.PARTLY_CLOUDY, 2, "Partly Cloudy", 6, 12, 25, 0.4, "7:07 AM", "4:20 PM", 16, 3, 74),
                    createDailyForecastWith24Hours("ln_d4", "Mon", "Nov 9", "Monday, Nov 9", WeatherConditionType.THUNDERSTORM, 95, "Thunderstorm", 7, 13, 90, 12.2, "7:09 AM", "4:19 PM", 38, 1, 91),
                    createDailyForecastWith24Hours("ln_d5", "Tue", "Nov 10", "Tuesday, Nov 10", WeatherConditionType.PARTLY_CLOUDY, 3, "Overcast", 5, 11, 20, 0.0, "7:11 AM", "4:17 PM", 14, 2, 72),
                    createDailyForecastWith24Hours("ln_d6", "Wed", "Nov 11", "Wednesday, Nov 11", WeatherConditionType.RAINY, 63, "Moderate Rain", 8, 13, 80, 7.8, "7:13 AM", "4:16 PM", 28, 1, 86),
                    createDailyForecastWith24Hours("ln_d7", "Thu", "Nov 12", "Thursday, Nov 12", WeatherConditionType.PARTLY_CLOUDY, 45, "Foggy", 9, 14, 30, 0.6, "7:14 AM", "4:14 PM", 19, 2, 78),
                    createDailyForecastWith24Hours("ln_d8", "Fri", "Nov 13", "Friday, Nov 13", WeatherConditionType.SUNNY, 1, "Mainly Clear", 4, 11, 10, 0.0, "7:16 AM", "4:13 PM", 11, 3, 66),
                    createDailyForecastWith24Hours("ln_d9", "Sat", "Nov 14", "Saturday, Nov 14", WeatherConditionType.PARTLY_CLOUDY, 2, "Partly Cloudy", 6, 12, 20, 0.2, "7:18 AM", "4:11 PM", 15, 2, 71),
                    createDailyForecastWith24Hours("ln_d10", "Sun", "Nov 15", "Sunday, Nov 15", WeatherConditionType.RAINY, 81, "Rain Showers", 8, 13, 75, 4.9, "7:20 AM", "4:10 PM", 31, 1, 85)
                )
            ),
            CityWeatherData(
                id = "dubai_ae",
                cityName = "Dubai",
                regionAndCountry = "Dubai Emirate, UAE",
                latitude = 25.2048,
                longitude = 55.2708,
                timeZoneOffsetLabel = "GST • GMT+4",
                currentTimeDisplay = "5:28 AM",
                currentDateDisplay = todayStr,
                currentTempCelsius = 31,
                feelsLikeCelsius = 33,
                highTempCelsius = 34,
                lowTempCelsius = 24,
                condition = WeatherConditionType.SUNNY,
                wmoCode = 0,
                conditionDescription = "Abundant Sunshine • Warm",
                airQualityIndex = 42,
                airQualityLabel = "Good (AQI 42)",
                isCurrentLocation = false,
                isLiveData = false,
                details = AtmosphericDetails(
                    humidityPercent = 41,
                    dewPointCelsius = 16,
                    windSpeedKmh = 14,
                    windDirection = "NW (315°)",
                    windGustKmh = 21,
                    pressureHpa = 1014,
                    pressureTrend = "Steady • Thermal high",
                    visibilityKm = 18.0,
                    visibilityStatus = "Clear desert horizon",
                    uvIndex = 8,
                    uvCategory = "Very High",
                    sunriseTime = "6:27 AM",
                    sunsetTime = "5:34 PM",
                    daylightDuration = "11h 07m"
                ),
                hourlyForecast = dubaiHours,
                tenDayForecast = listOf(
                    createDailyForecastWith24Hours("db_d1", "Today", "Nov 6", "Friday, Nov 6", WeatherConditionType.SUNNY, 0, "Clear Sky", 24, 34, 0, 0.0, "6:27 AM", "5:34 PM", 14, 8, 41),
                    createDailyForecastWith24Hours("db_d2", "Sat", "Nov 7", "Saturday, Nov 7", WeatherConditionType.SUNNY, 0, "Clear Sky", 24, 35, 0, 0.0, "6:28 AM", "5:33 PM", 16, 8, 39),
                    createDailyForecastWith24Hours("db_d3", "Sun", "Nov 8", "Sunday, Nov 8", WeatherConditionType.SUNNY, 0, "Clear Sky", 23, 33, 0, 0.0, "6:29 AM", "5:33 PM", 15, 8, 43),
                    createDailyForecastWith24Hours("db_d4", "Mon", "Nov 9", "Monday, Nov 9", WeatherConditionType.PARTLY_CLOUDY, 2, "Partly Cloudy", 23, 32, 5, 0.0, "6:29 AM", "5:32 PM", 18, 7, 46),
                    createDailyForecastWith24Hours("db_d5", "Tue", "Nov 10", "Tuesday, Nov 10", WeatherConditionType.SUNNY, 0, "Clear Sky", 22, 33, 0, 0.0, "6:30 AM", "5:32 PM", 14, 8, 42),
                    createDailyForecastWith24Hours("db_d6", "Wed", "Nov 11", "Wednesday, Nov 11", WeatherConditionType.SUNNY, 0, "Clear Sky", 22, 32, 0, 0.0, "6:31 AM", "5:31 PM", 12, 7, 40),
                    createDailyForecastWith24Hours("db_d7", "Thu", "Nov 12", "Thursday, Nov 12", WeatherConditionType.SUNNY, 0, "Clear Sky", 21, 31, 0, 0.0, "6:32 AM", "5:31 PM", 15, 7, 38),
                    createDailyForecastWith24Hours("db_d8", "Fri", "Nov 13", "Friday, Nov 13", WeatherConditionType.SUNNY, 1, "Mainly Clear", 21, 31, 5, 0.0, "6:32 AM", "5:30 PM", 19, 7, 44),
                    createDailyForecastWith24Hours("db_d9", "Sat", "Nov 14", "Saturday, Nov 14", WeatherConditionType.PARTLY_CLOUDY, 2, "Partly Cloudy", 20, 30, 10, 0.0, "6:33 AM", "5:30 PM", 17, 6, 48),
                    createDailyForecastWith24Hours("db_d10", "Sun", "Nov 15", "Sunday, Nov 15", WeatherConditionType.SUNNY, 0, "Clear Sky", 20, 31, 0, 0.0, "6:34 AM", "5:30 PM", 13, 7, 41)
                )
            ),
            CityWeatherData(
                id = "zermatt_ch",
                cityName = "Zermatt",
                regionAndCountry = "Valais Alps, Switzerland",
                latitude = 46.0207,
                longitude = 7.7491,
                timeZoneOffsetLabel = "CET • GMT+1",
                currentTimeDisplay = "2:28 AM",
                currentDateDisplay = todayStr,
                currentTempCelsius = -2,
                feelsLikeCelsius = -6,
                highTempCelsius = 2,
                lowTempCelsius = -8,
                condition = WeatherConditionType.SNOWY,
                wmoCode = 73,
                conditionDescription = "Alpine Powder Snow • Calm",
                airQualityIndex = 9,
                airQualityLabel = "Pristine (AQI 9)",
                isCurrentLocation = false,
                isLiveData = false,
                details = AtmosphericDetails(
                    humidityPercent = 82,
                    dewPointCelsius = -5,
                    windSpeedKmh = 15,
                    windDirection = "ENE (68°)",
                    windGustKmh = 24,
                    pressureHpa = 1019,
                    pressureTrend = "Steady • Alpine trough",
                    visibilityKm = 6.2,
                    visibilityStatus = "Light falling snow crystal haze",
                    uvIndex = 3,
                    uvCategory = "Moderate",
                    sunriseTime = "7:21 AM",
                    sunsetTime = "5:02 PM",
                    daylightDuration = "9h 41m"
                ),
                hourlyForecast = zermattHours,
                tenDayForecast = listOf(
                    createDailyForecastWith24Hours("zm_d1", "Today", "Nov 6", "Friday, Nov 6", WeatherConditionType.SNOWY, 73, "Moderate Snow", -8, 2, 80, 9.5, "7:21 AM", "5:02 PM", 15, 3, 82),
                    createDailyForecastWith24Hours("zm_d2", "Sat", "Nov 7", "Saturday, Nov 7", WeatherConditionType.SNOWY, 75, "Heavy Snow", -10, -1, 90, 16.2, "7:22 AM", "5:01 PM", 22, 2, 88),
                    createDailyForecastWith24Hours("zm_d3", "Sun", "Nov 8", "Sunday, Nov 8", WeatherConditionType.SUNNY, 0, "Clear Sky", -9, 3, 10, 0.0, "7:24 AM", "5:00 PM", 10, 5, 54),
                    createDailyForecastWith24Hours("zm_d4", "Mon", "Nov 9", "Monday, Nov 9", WeatherConditionType.PARTLY_CLOUDY, 2, "Partly Cloudy", -7, 4, 20, 0.0, "7:25 AM", "4:59 PM", 12, 4, 61),
                    createDailyForecastWith24Hours("zm_d5", "Tue", "Nov 10", "Tuesday, Nov 10", WeatherConditionType.SNOWY, 85, "Snow Showers", -8, 1, 70, 5.4, "7:26 AM", "4:58 PM", 18, 2, 79),
                    createDailyForecastWith24Hours("zm_d6", "Wed", "Nov 11", "Wednesday, Nov 11", WeatherConditionType.SNOWY, 75, "Heavy Snow", -11, -2, 85, 12.8, "7:28 AM", "4:56 PM", 25, 2, 84),
                    createDailyForecastWith24Hours("zm_d7", "Thu", "Nov 12", "Thursday, Nov 12", WeatherConditionType.CLEAR_NIGHT, 1, "Mainly Clear", -12, 0, 5, 0.0, "7:29 AM", "4:55 PM", 8, 4, 50),
                    createDailyForecastWith24Hours("zm_d8", "Fri", "Nov 13", "Friday, Nov 13", WeatherConditionType.SUNNY, 0, "Clear Sky", -8, 3, 10, 0.0, "7:30 AM", "4:54 PM", 11, 5, 52),
                    createDailyForecastWith24Hours("zm_d9", "Sat", "Nov 14", "Saturday, Nov 14", WeatherConditionType.PARTLY_CLOUDY, 2, "Partly Cloudy", -6, 4, 25, 0.5, "7:32 AM", "4:53 PM", 14, 3, 64),
                    createDailyForecastWith24Hours("zm_d10", "Sun", "Nov 15", "Sunday, Nov 15", WeatherConditionType.SNOWY, 71, "Light Snow", -9, 1, 75, 6.1, "7:33 AM", "4:52 PM", 19, 2, 81)
                )
            )
        )
    }

    val discoverableCities: List<CityWeatherData> by lazy {
        val todayStr = getCurrentDateFormatted()
        val initial = getInitialCities()
        listOf(
            CityWeatherData(
                id = "nyc_us",
                cityName = "New York",
                regionAndCountry = "New York, United States",
                latitude = 40.7128,
                longitude = -74.0060,
                timeZoneOffsetLabel = "EST • GMT-5",
                currentTimeDisplay = "8:28 PM",
                currentDateDisplay = todayStr,
                currentTempCelsius = 15,
                feelsLikeCelsius = 14,
                highTempCelsius = 18,
                lowTempCelsius = 9,
                condition = WeatherConditionType.CLEAR_NIGHT,
                wmoCode = 0,
                conditionDescription = "Clear Evening • Hudson Breeze",
                airQualityIndex = 28,
                airQualityLabel = "Good (AQI 28)",
                details = AtmosphericDetails(
                    humidityPercent = 61,
                    dewPointCelsius = 8,
                    windSpeedKmh = 17,
                    windDirection = "NW (315°)",
                    windGustKmh = 26,
                    pressureHpa = 1021,
                    pressureTrend = "Rising",
                    visibilityKm = 16.0,
                    visibilityStatus = "Clear skyline",
                    uvIndex = 3,
                    uvCategory = "Moderate",
                    sunriseTime = "6:33 AM",
                    sunsetTime = "4:46 PM",
                    daylightDuration = "10h 13m"
                ),
                hourlyForecast = initial.first().hourlyForecast,
                tenDayForecast = initial.first().tenDayForecast
            ),
            CityWeatherData(
                id = "singapore_sg",
                cityName = "Singapore",
                regionAndCountry = "Marina Bay, Singapore",
                latitude = 1.3521,
                longitude = 103.8198,
                timeZoneOffsetLabel = "SGT • GMT+8",
                currentTimeDisplay = "9:28 AM",
                currentDateDisplay = todayStr,
                currentTempCelsius = 29,
                feelsLikeCelsius = 34,
                highTempCelsius = 32,
                lowTempCelsius = 26,
                condition = WeatherConditionType.THUNDERSTORM,
                wmoCode = 95,
                conditionDescription = "Tropical Thunderstorm • Humid",
                airQualityIndex = 35,
                airQualityLabel = "Good (AQI 35)",
                details = AtmosphericDetails(
                    humidityPercent = 84,
                    dewPointCelsius = 25,
                    windSpeedKmh = 22,
                    windDirection = "SSE (160°)",
                    windGustKmh = 36,
                    pressureHpa = 1009,
                    pressureTrend = "Convective low",
                    visibilityKm = 9.0,
                    visibilityStatus = "Tropical rain curtain",
                    uvIndex = 9,
                    uvCategory = "Very High",
                    sunriseTime = "6:54 AM",
                    sunsetTime = "6:58 PM",
                    daylightDuration = "12h 04m"
                ),
                hourlyForecast = initial[2].hourlyForecast,
                tenDayForecast = initial[2].tenDayForecast
            ),
            CityWeatherData(
                id = "paris_fr",
                cityName = "Paris",
                regionAndCountry = "Île-de-France, France",
                latitude = 48.8566,
                longitude = 2.3522,
                timeZoneOffsetLabel = "CET • GMT+1",
                currentTimeDisplay = "2:28 AM",
                currentDateDisplay = todayStr,
                currentTempCelsius = 13,
                feelsLikeCelsius = 12,
                highTempCelsius = 16,
                lowTempCelsius = 8,
                condition = WeatherConditionType.PARTLY_CLOUDY,
                wmoCode = 2,
                conditionDescription = "Soft Clouds • Mild Autumn",
                airQualityIndex = 22,
                airQualityLabel = "Good (AQI 22)",
                details = AtmosphericDetails(
                    humidityPercent = 74,
                    dewPointCelsius = 8,
                    windSpeedKmh = 14,
                    windDirection = "W (270°)",
                    windGustKmh = 21,
                    pressureHpa = 1016,
                    pressureTrend = "Steady",
                    visibilityKm = 14.5,
                    visibilityStatus = "Clear",
                    uvIndex = 3,
                    uvCategory = "Moderate",
                    sunriseTime = "7:46 AM",
                    sunsetTime = "5:22 PM",
                    daylightDuration = "9h 36m"
                ),
                hourlyForecast = initial[0].hourlyForecast,
                tenDayForecast = initial[0].tenDayForecast
            )
        )
    }
}
