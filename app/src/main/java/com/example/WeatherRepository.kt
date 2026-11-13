package com.example

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

sealed class WeatherApiResult<out T> {
    data class Success<T>(val data: T) : WeatherApiResult<T>()
    data class NoInternet(val message: String = "No active internet connection detected.") : WeatherApiResult<Nothing>()
    data class ApiError(val message: String) : WeatherApiResult<Nothing>()
}

/**
 * Repository coordinating Open-Meteo Weather Forecast API (current, full-day 24h hourly, and 10-day daily)
 * and Open-Meteo Geocoding API.
 */
class WeatherRepository(
    private val context: Context? = null,
    private val weatherApiService: WeatherApiService = WeatherApiService.create(),
    private val geocodingService: GeocodingService = GeocodingService.create()
) {

    fun isNetworkConnected(): Boolean {
        val ctx = context ?: return true
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun fetchLiveCityWeather(
        id: String,
        cityName: String,
        regionAndCountry: String,
        latitude: Double,
        longitude: Double,
        isCurrentLocation: Boolean = false
    ): WeatherApiResult<CityWeatherData> = withContext(Dispatchers.IO) {
        if (!latitude.isFinite() || !longitude.isFinite() ||
            latitude !in -90.0..90.0 || longitude !in -180.0..180.0
        ) {
            return@withContext WeatherApiResult.ApiError("Invalid location coordinates.")
        }
        if (!isNetworkConnected()) {
            return@withContext WeatherApiResult.NoInternet()
        }

        try {
            val response = weatherApiService.getForecast(
                latitude = latitude,
                longitude = longitude,
                forecastDays = 10
            )
            val mapped = mapResponseToCityWeatherData(
                id = id,
                cityName = cityName,
                regionAndCountry = regionAndCountry,
                latitude = latitude,
                longitude = longitude,
                isCurrentLocation = isCurrentLocation,
                response = response
            )
            WeatherApiResult.Success(mapped)
        } catch (uhe: UnknownHostException) {
            WeatherApiResult.NoInternet("Unable to reach weather servers. Please check your internet connection.")
        } catch (ioe: IOException) {
            WeatherApiResult.NoInternet("Connection interrupted while loading weather. Please try again.")
        } catch (_: Exception) {
            WeatherApiResult.ApiError("Unable to load weather right now. Please try again.")
        }
    }

    suspend fun searchCities(query: String): WeatherApiResult<List<GeocodingLocationDto>> =
        withContext(Dispatchers.IO) {
            val sanitized = query
                .replace(Regex("[\\p{Cntrl}]"), "")
                .trim()
                .take(80)
            if (sanitized.isEmpty()) {
                return@withContext WeatherApiResult.Success(emptyList())
            }
            if (!isNetworkConnected()) {
                return@withContext WeatherApiResult.NoInternet()
            }

            try {
                val response = geocodingService.searchLocations(name = sanitized, count = 10)
                val results = response.results.orEmpty()
                WeatherApiResult.Success(results)
            } catch (uhe: UnknownHostException) {
                WeatherApiResult.NoInternet("Unable to search cities offline. Check your internet connection.")
            } catch (ioe: IOException) {
                WeatherApiResult.NoInternet("Connection timed out while searching cities.")
            } catch (_: Exception) {
                WeatherApiResult.ApiError("Unable to search cities right now. Please try again.")
            }
        }

    fun mapResponseToCityWeatherData(
        id: String,
        cityName: String,
        regionAndCountry: String,
        latitude: Double,
        longitude: Double,
        isCurrentLocation: Boolean,
        response: OpenMeteoForecastResponse
    ): CityWeatherData {
        val currentDto = response.current
        val dailyDto = response.daily
        val hourlyDto = response.hourly

        val currentTemp = currentDto?.temperature2m?.roundToInt() ?: 20
        val feelsLike = currentDto?.apparentTemperature?.roundToInt() ?: currentTemp
        val humidity = currentDto?.relativeHumidity2m ?: 60
        val isDay = currentDto?.isDay ?: 1
        val wmoCode = currentDto?.weatherCode ?: 0

        val conditionType = WmoWeatherCodeMapper.mapToConditionType(wmoCode, isDay)
        val conditionDescription = WmoWeatherCodeMapper.mapToDetailedDescription(wmoCode, isDay)

        val maxTempToday = dailyDto?.temperature2mMax?.firstOrNull()?.roundToInt() ?: (currentTemp + 3)
        val minTempToday = dailyDto?.temperature2mMin?.firstOrNull()?.roundToInt() ?: (currentTemp - 4)

        val windSpeedKmh = currentDto?.windSpeed10m?.roundToInt() ?: 12
        val windDegrees = currentDto?.windDirection10m ?: 0
        val windCompass = WmoWeatherCodeMapper.degreesToCompass(windDegrees)
        val windGustsKmh = currentDto?.windGusts10m?.roundToInt() ?: (windSpeedKmh + 8)

        val pressureHpa = (currentDto?.pressureMsl ?: currentDto?.surfacePressure ?: 1015.0).roundToInt()
        val pressureTrend = when {
            pressureHpa >= 1020 -> "High pressure system ($pressureHpa hPa)"
            pressureHpa <= 1005 -> "Low pressure trough ($pressureHpa hPa)"
            else -> "Normal atmospheric pressure"
        }

        val rawVisibilityMeters = currentDto?.visibilityMeters
            ?: hourlyDto?.visibilityMeters?.firstOrNull { it != null }
            ?: 16000.0
        val visibilityKm = ((rawVisibilityMeters / 1000.0) * 10.0).roundToInt() / 10.0
        val visibilityStatus = when {
            visibilityKm >= 15.0 -> "Crystal clear horizon"
            visibilityKm >= 8.0 -> "Good atmospheric visibility"
            visibilityKm >= 3.0 -> "Moderate haze or mist"
            else -> "Low visibility conditions"
        }

        val rawUv = currentDto?.uvIndex
            ?: dailyDto?.uvIndexMax?.firstOrNull { it != null }
            ?: 3.0
        val uvIndex = rawUv.roundToInt().coerceAtLeast(0)
        val uvCategory = WmoWeatherCodeMapper.uvCategoryLabel(uvIndex)

        val rawSunrise = dailyDto?.sunrise?.firstOrNull()
        val rawSunset = dailyDto?.sunset?.firstOrNull()
        val sunriseFormatted = formatIsoTimeToAmPm(rawSunrise, fallback = "6:30 AM")
        val sunsetFormatted = formatIsoTimeToAmPm(rawSunset, fallback = "5:45 PM")
        val daylightDuration = calculateDaylightDuration(rawSunrise, rawSunset)

        val dewPoint = WmoWeatherCodeMapper.calculateDewPointCelsius(
            currentDto?.temperature2m ?: 20.0,
            humidity
        )

        val currentTimeString = formatIsoTimeToAmPm(currentDto?.time, fallback = "Live")
        val currentDateString = formatIsoDateToReadable(currentDto?.time)
        val offsetHours = response.utcOffsetSeconds / 3600
        val offsetSign = if (offsetHours >= 0) "+" else "-"
        val tzLabel = "${response.timezoneAbbreviation} • GMT$offsetSign${abs(offsetHours)}"

        // Build 10-day forecast rows (Today + Next 9 days) with all 24 hours (12 AM to 11 PM) per day
        val tenDayItems = buildTenDayForecastList(
            currentIsoTime = currentDto?.time,
            dailyDto = dailyDto,
            hourlyDto = hourlyDto,
            defaultHumidity = humidity,
            defaultWindDegrees = windDegrees
        )

        // Build full 24-hour forecast (12 AM to 11 PM) for Today in the city's local timezone
        val todayHourlyFromDaily = tenDayItems.firstOrNull()?.hourlyItemsForDay.orEmpty()
        val hourlyItems = if (todayHourlyFromDaily.size == 24) {
            todayHourlyFromDaily
        } else {
            buildFullDayHourlyForecast(
                currentIsoTime = currentDto?.time,
                firstDailyDateIso = dailyDto?.time?.firstOrNull(),
                currentTemp = currentTemp,
                currentFeelsLike = feelsLike,
                currentCondition = conditionType,
                currentWmoCode = wmoCode,
                currentWindKmh = windSpeedKmh,
                currentWindDegrees = windDegrees,
                currentHumidity = humidity,
                hourlyDto = hourlyDto
            )
        }

        return CityWeatherData(
            id = id,
            cityName = cityName,
            regionAndCountry = regionAndCountry,
            latitude = latitude,
            longitude = longitude,
            timeZoneOffsetLabel = tzLabel,
            currentTimeDisplay = currentTimeString,
            currentDateDisplay = currentDateString,
            currentTempCelsius = currentTemp,
            feelsLikeCelsius = feelsLike,
            highTempCelsius = maxTempToday,
            lowTempCelsius = minTempToday,
            condition = conditionType,
            wmoCode = wmoCode,
            conditionDescription = "$conditionDescription • Wind $windCompass $windSpeedKmh km/h",
            airQualityIndex = 22,
            airQualityLabel = "Live Open-Meteo",
            details = AtmosphericDetails(
                humidityPercent = humidity,
                dewPointCelsius = dewPoint,
                windSpeedKmh = windSpeedKmh,
                windDirection = "$windCompass (${windDegrees}°)",
                windGustKmh = windGustsKmh,
                pressureHpa = pressureHpa,
                pressureTrend = pressureTrend,
                visibilityKm = visibilityKm,
                visibilityStatus = visibilityStatus,
                uvIndex = uvIndex,
                uvCategory = uvCategory,
                sunriseTime = sunriseFormatted,
                sunsetTime = sunsetFormatted,
                daylightDuration = daylightDuration
            ),
            hourlyForecast = hourlyItems,
            tenDayForecast = tenDayItems,
            isCurrentLocation = isCurrentLocation,
            isLiveData = true
        )
    }

    /**
     * Builds all 24 hours (12 AM to 11 PM) of the current day from Open-Meteo hourly arrays,
     * preserving the city's local timezone and highlighting the current hour (`isNow = true`).
     */
    private fun buildFullDayHourlyForecast(
        currentIsoTime: String?,
        firstDailyDateIso: String?,
        currentTemp: Int,
        currentFeelsLike: Int,
        currentCondition: WeatherConditionType,
        currentWmoCode: Int,
        currentWindKmh: Int,
        currentWindDegrees: Int,
        currentHumidity: Int,
        hourlyDto: OpenMeteoHourlyDto?
    ): List<HourlyForecastItem> {
        if (hourlyDto == null || hourlyDto.time.isEmpty()) {
            return SampleWeatherRepository.generate24HoursForDay(
                dayId = "fallback_today",
                minTempCelsius = currentTemp - 4,
                maxTempCelsius = currentTemp + 3,
                condition = currentCondition,
                wmoCode = currentWmoCode,
                rainProbability = 0,
                precipitationSumMm = 0.0,
                windKmh = currentWindKmh,
                humidityPercent = currentHumidity,
                isToday = true
            )
        }

        val targetDatePrefix = currentIsoTime?.take(10)
            ?: firstDailyDateIso
            ?: hourlyDto.time.firstOrNull()?.take(10).orEmpty()

        val dayItems = buildHourlyItemsForSpecificDate(
            dateIsoPrefix = targetDatePrefix,
            currentIsoTime = currentIsoTime,
            hourlyDto = hourlyDto,
            defaultHumidity = currentHumidity,
            defaultWindDegrees = currentWindDegrees
        )

        if (dayItems.size == 24) {
            return dayItems
        }

        // Fallback if hourlyDto has fewer than 24 entries matching targetDatePrefix: take first 24 entries
        val count = hourlyDto.time.size.coerceAtMost(24)
        val currentHourPrefix = currentIsoTime?.take(13)
        val result = mutableListOf<HourlyForecastItem>()

        for (idx in 0 until count) {
            val rawTime = hourlyDto.time.getOrNull(idx).orEmpty()
            val hour24 = parseHour24FromIso(rawTime, fallbackHour = idx % 24)
            val clockLabel = formatHour24ToAmPm(hour24)
            val isCurrentHour = if (currentHourPrefix != null) {
                rawTime.startsWith(currentHourPrefix)
            } else {
                idx == 0
            }

            val temp = hourlyDto.temperature2m.getOrNull(idx)?.roundToInt() ?: currentTemp
            val feelsLike = hourlyDto.apparentTemperature.getOrNull(idx)?.roundToInt() ?: currentFeelsLike
            val code = hourlyDto.weatherCode.getOrNull(idx) ?: currentWmoCode
            val isDayVal = hourlyDto.isDay.getOrNull(idx) ?: if (hour24 in 6..17) 1 else 0
            val cond = WmoWeatherCodeMapper.mapToConditionType(code, isDayVal)
            val shortLabel = WmoWeatherCodeMapper.mapToShortLabel(code, isDayVal)
            val pop = (hourlyDto.precipitationProbability.getOrNull(idx) ?: 0).coerceIn(0, 100)
            val precipMm = ((hourlyDto.precipitationMm.getOrNull(idx) ?: 0.0) * 10.0).roundToInt() / 10.0
            val windKmh = hourlyDto.windSpeed10m.getOrNull(idx)?.roundToInt() ?: currentWindKmh
            val windDeg = hourlyDto.windDirection10m.getOrNull(idx) ?: currentWindDegrees
            val windComp = WmoWeatherCodeMapper.degreesToCompass(windDeg)
            val relHumidity = hourlyDto.relativeHumidity2m.getOrNull(idx) ?: currentHumidity

            result.add(
                HourlyForecastItem(
                    id = "hour_$idx",
                    timeLabel = clockLabel,
                    clockTimeLabel = clockLabel,
                    hour24 = hour24,
                    fullIsoTime = rawTime,
                    tempCelsius = temp,
                    feelsLikeCelsius = feelsLike,
                    condition = cond,
                    wmoCode = code,
                    conditionLabel = shortLabel,
                    popPercent = pop,
                    precipitationMm = precipMm,
                    windSpeedKmh = windKmh,
                    windDirectionDegrees = windDeg,
                    windDirectionCompass = windComp,
                    relativeHumidityPercent = relHumidity,
                    isNow = isCurrentHour
                )
            )
        }

        return result
    }

    private fun buildTenDayForecastList(
        currentIsoTime: String?,
        dailyDto: OpenMeteoDailyDto?,
        hourlyDto: OpenMeteoHourlyDto?,
        defaultHumidity: Int,
        defaultWindDegrees: Int
    ): List<DailyForecastItem> {
        if (dailyDto == null || dailyDto.time.isEmpty()) {
            return emptyList()
        }

        val count = dailyDto.time.size.coerceAtMost(10)
        val items = mutableListOf<DailyForecastItem>()

        for (i in 0 until count) {
            val dateIso = dailyDto.time.getOrNull(i).orEmpty()
            val dayName = if (i == 0) "Today" else formatIsoDayOfWeek(dateIso)
            val dateShort = formatIsoMonthDay(dateIso)
            val fullDateFormatted = formatIsoFullDayAndDate(dateIso)

            val code = dailyDto.weatherCode.getOrNull(i) ?: 0
            val condition = WmoWeatherCodeMapper.mapToConditionType(code, isDay = 1)
            val label = WmoWeatherCodeMapper.mapToShortLabel(code, isDay = 1)

            val maxT = dailyDto.temperature2mMax.getOrNull(i)?.roundToInt() ?: 22
            val minT = dailyDto.temperature2mMin.getOrNull(i)?.roundToInt() ?: 12
            val pop = (dailyDto.precipitationProbabilityMax.getOrNull(i) ?: 0).coerceIn(0, 100)
            val precipSumMm = ((dailyDto.precipitationSumMm.getOrNull(i) ?: 0.0) * 10.0).roundToInt() / 10.0

            val sunriseStr = formatIsoTimeToAmPm(dailyDto.sunrise.getOrNull(i), fallback = "6:30 AM")
            val sunsetStr = formatIsoTimeToAmPm(dailyDto.sunset.getOrNull(i), fallback = "5:45 PM")

            val wind = dailyDto.windSpeed10mMax.getOrNull(i)?.roundToInt() ?: 15
            val uv = dailyDto.uvIndexMax.getOrNull(i)?.roundToInt() ?: 4

            // Extract all 24 hours (12 AM to 11 PM) for this specific date from Open-Meteo hourly data
            val rawDayHourlyItems = buildHourlyItemsForSpecificDate(
                dateIsoPrefix = dateIso,
                currentIsoTime = if (i == 0) currentIsoTime else null,
                hourlyDto = hourlyDto,
                defaultHumidity = defaultHumidity,
                defaultWindDegrees = defaultWindDegrees
            )

            val dayHourlyItems = if (rawDayHourlyItems.size == 24) {
                rawDayHourlyItems
            } else {
                SampleWeatherRepository.generate24HoursForDay(
                    dayId = "day_${dateIso}_$i",
                    minTempCelsius = minT,
                    maxTempCelsius = maxT,
                    condition = condition,
                    wmoCode = code,
                    rainProbability = pop,
                    precipitationSumMm = precipSumMm,
                    windKmh = wind,
                    humidityPercent = defaultHumidity,
                    isToday = i == 0
                )
            }

            val dayAvgHumidity = if (dayHourlyItems.isNotEmpty()) {
                dayHourlyItems.map { it.relativeHumidityPercent }.average().roundToInt()
            } else {
                defaultHumidity
            }

            items.add(
                DailyForecastItem(
                    id = "day_${dateIso}_$i",
                    dayName = dayName,
                    dateShort = dateShort,
                    fullDateFormatted = fullDateFormatted,
                    condition = condition,
                    wmoCode = code,
                    conditionLabel = label,
                    minTempCelsius = minT,
                    maxTempCelsius = maxT,
                    rainProbability = pop,
                    precipitationSumMm = precipSumMm,
                    sunriseTime = sunriseStr,
                    sunsetTime = sunsetStr,
                    windKmh = wind,
                    uvIndex = uv,
                    humidityPercent = dayAvgHumidity,
                    hourlyItemsForDay = dayHourlyItems
                )
            )
        }

        return items
    }

    private fun buildHourlyItemsForSpecificDate(
        dateIsoPrefix: String,
        currentIsoTime: String?,
        hourlyDto: OpenMeteoHourlyDto?,
        defaultHumidity: Int,
        defaultWindDegrees: Int
    ): List<HourlyForecastItem> {
        if (hourlyDto == null || dateIsoPrefix.isBlank()) return emptyList()
        val currentHourPrefix = currentIsoTime?.take(13) // "YYYY-MM-DDTHH"
        val list = mutableListOf<HourlyForecastItem>()

        for (idx in hourlyDto.time.indices) {
            val rawTime = hourlyDto.time[idx]
            if (rawTime.startsWith(dateIsoPrefix)) {
                val hour24 = parseHour24FromIso(rawTime, fallbackHour = list.size % 24)
                val clockLabel = formatHour24ToAmPm(hour24)
                val isCurrentHour = currentHourPrefix != null && rawTime.startsWith(currentHourPrefix)

                val temp = hourlyDto.temperature2m.getOrNull(idx)?.roundToInt() ?: 18
                val feelsLike = hourlyDto.apparentTemperature.getOrNull(idx)?.roundToInt() ?: temp
                val code = hourlyDto.weatherCode.getOrNull(idx) ?: 0
                val isDayVal = hourlyDto.isDay.getOrNull(idx) ?: if (hour24 in 6..17) 1 else 0
                val cond = WmoWeatherCodeMapper.mapToConditionType(code, isDayVal)
                val shortLabel = WmoWeatherCodeMapper.mapToShortLabel(code, isDayVal)
                val pop = (hourlyDto.precipitationProbability.getOrNull(idx) ?: 0).coerceIn(0, 100)
                val precipMm = ((hourlyDto.precipitationMm.getOrNull(idx) ?: 0.0) * 10.0).roundToInt() / 10.0
                val wind = hourlyDto.windSpeed10m.getOrNull(idx)?.roundToInt() ?: 12
                val windDeg = hourlyDto.windDirection10m.getOrNull(idx) ?: defaultWindDegrees
                val windCompass = WmoWeatherCodeMapper.degreesToCompass(windDeg)
                val hum = hourlyDto.relativeHumidity2m.getOrNull(idx) ?: defaultHumidity

                list.add(
                    HourlyForecastItem(
                        id = "dayhour_${dateIsoPrefix}_$hour24",
                        timeLabel = clockLabel,
                        clockTimeLabel = clockLabel,
                        hour24 = hour24,
                        fullIsoTime = rawTime,
                        tempCelsius = temp,
                        feelsLikeCelsius = feelsLike,
                        condition = cond,
                        wmoCode = code,
                        conditionLabel = shortLabel,
                        popPercent = pop,
                        precipitationMm = precipMm,
                        windSpeedKmh = wind,
                        windDirectionDegrees = windDeg,
                        windDirectionCompass = windCompass,
                        relativeHumidityPercent = hum,
                        isNow = isCurrentHour
                    )
                )
            }
        }

        // If currentIsoTime belonged to this date but didn't match an exact hour prefix, mark hour 0 or matching hour
        if (currentIsoTime != null && currentIsoTime.startsWith(dateIsoPrefix) && list.isNotEmpty() && list.none { it.isNow }) {
            val targetHour = parseHour24FromIso(currentIsoTime, fallbackHour = 0)
            return list.map { item ->
                if (item.hour24 == targetHour) item.copy(isNow = true) else item
            }
        }

        return list
    }

    private fun parseHour24FromIso(isoString: String, fallbackHour: Int = 0): Int {
        if (!isoString.contains("T")) return fallbackHour
        return isoString.substringAfter("T").substringBefore(":").toIntOrNull()?.coerceIn(0, 23) ?: fallbackHour
    }

    private fun formatHour24ToAmPm(hour24: Int): String {
        val h = hour24.coerceIn(0, 23)
        val amPm = if (h >= 12) "PM" else "AM"
        val hour12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return "$hour12 $amPm"
    }

    private fun formatIsoTimeToAmPm(isoString: String?, fallback: String): String {
        if (isoString.isNullOrBlank() || !isoString.contains("T")) return fallback
        return try {
            val timePart = isoString.substringAfter("T")
            val parts = timePart.split(":")
            val hour24 = parts.getOrNull(0)?.toIntOrNull() ?: return fallback
            val minute = parts.getOrNull(1)?.take(2) ?: "00"
            val amPm = if (hour24 >= 12) "PM" else "AM"
            val hour12 = when {
                hour24 == 0 -> 12
                hour24 > 12 -> hour24 - 12
                else -> hour24
            }
            "$hour12:$minute $amPm"
        } catch (_: Exception) {
            fallback
        }
    }

    private fun formatIsoDateToReadable(isoString: String?): String {
        if (isoString.isNullOrBlank()) return SampleWeatherRepository.getCurrentDateFormatted()
        return try {
            val datePart = isoString.substringBefore("T")
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsed = parser.parse(datePart) ?: Date()
            SimpleDateFormat("EEEE, MMM d", Locale.US).format(parsed)
        } catch (_: Exception) {
            SampleWeatherRepository.getCurrentDateFormatted()
        }
    }

    private fun formatIsoDayOfWeek(dateIso: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsed = parser.parse(dateIso) ?: return dateIso
            SimpleDateFormat("EEE", Locale.US).format(parsed)
        } catch (_: Exception) {
            dateIso
        }
    }

    private fun formatIsoMonthDay(dateIso: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsed = parser.parse(dateIso) ?: return dateIso
            SimpleDateFormat("MMM d", Locale.US).format(parsed)
        } catch (_: Exception) {
            dateIso
        }
    }

    private fun formatIsoFullDayAndDate(dateIso: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsed = parser.parse(dateIso) ?: return dateIso
            SimpleDateFormat("EEEE, MMMM d", Locale.US).format(parsed)
        } catch (_: Exception) {
            dateIso
        }
    }

    private fun calculateDaylightDuration(sunriseIso: String?, sunsetIso: String?): String {
        if (sunriseIso.isNullOrBlank() || sunsetIso.isNullOrBlank()) return "10h 45m"
        return try {
            val riseTime = sunriseIso.substringAfter("T").split(":")
            val setTime = sunsetIso.substringAfter("T").split(":")
            val riseMinutes = (riseTime[0].toInt() * 60) + riseTime[1].take(2).toInt()
            val setMinutes = (setTime[0].toInt() * 60) + setTime[1].take(2).toInt()
            val diff = (setMinutes - riseMinutes).coerceAtLeast(0)
            val hours = diff / 60
            val mins = diff % 60
            "${hours}h ${String.format(Locale.US, "%02dm", mins)}"
        } catch (_: Exception) {
            "10h 45m"
        }
    }
}
