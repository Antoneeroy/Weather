package com.example

/**
 * Full-Day 24-Hour Hourly Forecast Data Model (`12 AM` to `11 PM`).
 * Includes all meteorological fields required for the clickable Hourly Detail Panel:
 * - Selected time (`timeLabel`, `clockTimeLabel`, `fullIsoTime`)
 * - Temperature (`tempCelsius`)
 * - Feels-like temperature (`feelsLikeCelsius`)
 * - Weather condition (`condition`, `wmoCode`, `conditionLabel`)
 * - Rain probability (`popPercent`)
 * - Precipitation (`precipitationMm`)
 * - Humidity (`relativeHumidityPercent`)
 * - Wind speed (`windSpeedKmh`)
 * - Wind direction (`windDirectionDegrees`, `windDirectionCompass`)
 */
data class HourlyForecastItem(
    val id: String,
    val timeLabel: String,
    val clockTimeLabel: String = timeLabel,
    val fullIsoTime: String = "",
    val hour24: Int = 0,
    val tempCelsius: Int,
    val feelsLikeCelsius: Int = tempCelsius,
    val condition: WeatherConditionType,
    val wmoCode: Int = 0,
    val conditionLabel: String = condition.displayName,
    val popPercent: Int,
    val precipitationMm: Double = 0.0,
    val windSpeedKmh: Int = 10,
    val windDirectionDegrees: Int = 315,
    val windDirectionCompass: String = "NW (315°)",
    val relativeHumidityPercent: Int = 60,
    val isNow: Boolean = false
)

/**
 * 10-Day Daily Forecast Data Model (Today + Next 9 Days).
 * Contains `hourlyItemsForDay` with all 24 hours (`12 AM` to `11 PM`) for that specific day.
 */
data class DailyForecastItem(
    val id: String,
    val dayName: String,
    val dateShort: String,
    val fullDateFormatted: String = dateShort,
    val condition: WeatherConditionType,
    val wmoCode: Int = 0,
    val conditionLabel: String,
    val minTempCelsius: Int,
    val maxTempCelsius: Int,
    val rainProbability: Int,
    val precipitationSumMm: Double = 0.0,
    val sunriseTime: String = "6:35 AM",
    val sunsetTime: String = "5:25 PM",
    val windKmh: Int,
    val uvIndex: Int,
    val humidityPercent: Int,
    val hourlyItemsForDay: List<HourlyForecastItem> = emptyList()
)
