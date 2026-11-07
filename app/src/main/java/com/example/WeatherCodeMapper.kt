package com.example

import androidx.compose.ui.graphics.Color
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Specific WMO Weather Condition Category for accurate icons, labels, and temperature bar gradients.
 */
enum class WmoDetailedCategory(
    val title: String,
    val subtitle: String
) {
    CLEAR_SKY("Clear Sky", "Unobstructed solar irradiance & clear horizon"),
    MAINLY_CLEAR("Mainly Clear", "Predominantly clear sky with minimal high wisps"),
    PARTLY_CLOUDY("Partly Cloudy", "Scattered cumulus clouds & mild sunbreaks"),
    OVERCAST("Overcast", "Full stratocumulus cloud canopy"),
    FOG("Fog & Mist", "Suspended atmospheric water droplets reducing visibility"),
    DRIZZLE("Drizzle", "Fine, uniform light precipitation from low stratus"),
    FREEZING_DRIZZLE("Freezing Drizzle", "Supercooled drizzle freezing on contact"),
    RAIN("Rain", "Steady frontal or convective liquid precipitation"),
    FREEZING_RAIN("Freezing Rain", "Sub-zero surface glaze rain conditions"),
    RAIN_SHOWERS("Rain Showers", "Passing convective rain showers"),
    SNOW("Snow", "Crystalline snowfall accumulation"),
    SNOW_SHOWERS("Snow Showers", "Intermittent convective snow bursts"),
    THUNDERSTORM("Thunderstorm", "Deep cumulonimbus storm with lightning & thunder")
}

/**
 * Maps Open-Meteo WMO Weather Interpretation Codes (0..99) to app condition types,
 * detailed categories, labels, and condition-specific temperature bar color gradients.
 */
object WmoWeatherCodeMapper {

    fun mapToDetailedCategory(wmoCode: Int): WmoDetailedCategory {
        return when (wmoCode) {
            0 -> WmoDetailedCategory.CLEAR_SKY
            1 -> WmoDetailedCategory.MAINLY_CLEAR
            2 -> WmoDetailedCategory.PARTLY_CLOUDY
            3 -> WmoDetailedCategory.OVERCAST
            45, 48 -> WmoDetailedCategory.FOG
            51, 53, 55 -> WmoDetailedCategory.DRIZZLE
            56, 57 -> WmoDetailedCategory.FREEZING_DRIZZLE
            61, 63, 65 -> WmoDetailedCategory.RAIN
            66, 67 -> WmoDetailedCategory.FREEZING_RAIN
            71, 73, 75, 77 -> WmoDetailedCategory.SNOW
            80, 81, 82 -> WmoDetailedCategory.RAIN_SHOWERS
            85, 86 -> WmoDetailedCategory.SNOW_SHOWERS
            95, 96, 99 -> WmoDetailedCategory.THUNDERSTORM
            else -> WmoDetailedCategory.PARTLY_CLOUDY
        }
    }

    fun mapToConditionType(wmoCode: Int, isDay: Int = 1): WeatherConditionType {
        return when (wmoCode) {
            0, 1 -> if (isDay == 1) WeatherConditionType.SUNNY else WeatherConditionType.CLEAR_NIGHT
            2, 3, 45, 48 -> WeatherConditionType.PARTLY_CLOUDY
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherConditionType.RAINY
            71, 73, 75, 77, 85, 86 -> WeatherConditionType.SNOWY
            95, 96, 99 -> WeatherConditionType.THUNDERSTORM
            else -> if (isDay == 1) WeatherConditionType.PARTLY_CLOUDY else WeatherConditionType.CLEAR_NIGHT
        }
    }

    fun mapToShortLabel(wmoCode: Int, isDay: Int = 1): String {
        return when (wmoCode) {
            0 -> if (isDay == 1) "Clear Sky" else "Clear Night"
            1 -> if (isDay == 1) "Mainly Clear" else "Mostly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45 -> "Foggy"
            48 -> "Rime Fog"
            51 -> "Light Drizzle"
            53 -> "Moderate Drizzle"
            55 -> "Dense Drizzle"
            56, 57 -> "Freezing Drizzle"
            61 -> "Light Rain"
            63 -> "Moderate Rain"
            65 -> "Heavy Rain"
            66, 67 -> "Freezing Rain"
            71 -> "Light Snow"
            73 -> "Moderate Snow"
            75 -> "Heavy Snow"
            77 -> "Snow Grains"
            80 -> "Light Showers"
            81 -> "Rain Showers"
            82 -> "Heavy Showers"
            85 -> "Snow Showers"
            86 -> "Heavy Snow Showers"
            95 -> "Thunderstorm"
            96, 99 -> "Severe Storm & Hail"
            else -> if (isDay == 1) "Partly Cloudy" else "Clear Night"
        }
    }

    fun mapToDetailedDescription(wmoCode: Int, isDay: Int = 1): String {
        return when (wmoCode) {
            0 -> if (isDay == 1) "Clear Sky • Abundant Sunshine" else "Clear Starlit Sky"
            1 -> if (isDay == 1) "Mainly Clear • Bright" else "Mostly Clear Night"
            2 -> "Partly Cloudy • Scattered Clouds"
            3 -> "Overcast • Layered Cloud Deck"
            45, 48 -> "Atmospheric Fog & Mist"
            51, 53, 55 -> "Light to Moderate Drizzle"
            56, 57 -> "Freezing Drizzle"
            61 -> "Slight Rain Showers"
            63 -> "Moderate Rain"
            65 -> "Heavy Rain"
            66, 67 -> "Freezing Rain"
            71 -> "Slight Snowfall"
            73 -> "Moderate Snowfall"
            75, 77 -> "Heavy Alpine Snow"
            80, 81 -> "Scattered Rain Showers"
            82 -> "Intense Rain Showers"
            85, 86 -> "Snow Showers"
            95 -> "Thunderstorm Activity"
            96, 99 -> "Severe Thunderstorm with Hail"
            else -> "Variable Sky Conditions"
        }
    }

    /**
     * Returns condition-specific gradient colors for the 10-Day Temperature Range Bar.
     */
    fun getConditionGradientColors(condition: WeatherConditionType, wmoCode: Int = -1): List<Color> {
        return when {
            wmoCode == 3 || wmoCode == 45 || wmoCode == 48 -> listOf(
                Color(0xFF64748B),
                Color(0xFF94A3B8),
                Color(0xFF38BDF8)
            )
            wmoCode == 66 || wmoCode == 67 || wmoCode == 56 || wmoCode == 57 -> listOf(
                Color(0xFF06B6D4),
                Color(0xFF3B82F6),
                Color(0xFF8B5CF6)
            )
            condition == WeatherConditionType.SUNNY -> listOf(
                Color(0xFF38BDF8),
                Color(0xFFFBBF24),
                Color(0xFFF97316)
            )
            condition == WeatherConditionType.PARTLY_CLOUDY -> listOf(
                Color(0xFF38BDF8),
                Color(0xFF818CF8),
                Color(0xFFFBBF24)
            )
            condition == WeatherConditionType.RAINY -> listOf(
                Color(0xFF0284C7),
                Color(0xFF38BDF8),
                Color(0xFF6366F1)
            )
            condition == WeatherConditionType.THUNDERSTORM -> listOf(
                Color(0xFF6366F1),
                Color(0xFFA855F7),
                Color(0xFFFACC15)
            )
            condition == WeatherConditionType.SNOWY -> listOf(
                Color(0xFFBAE6FD),
                Color(0xFF7DD3FC),
                Color(0xFF38BDF8)
            )
            condition == WeatherConditionType.CLEAR_NIGHT -> listOf(
                Color(0xFF38BDF8),
                Color(0xFF818CF8),
                Color(0xFFA855F7)
            )
            else -> listOf(
                Color(0xFF38BDF8),
                Color(0xFFA855F7),
                Color(0xFFFBBF24)
            )
        }
    }

    fun degreesToCompass(degrees: Int): String {
        val directions = arrayOf(
            "N", "NNE", "NE", "ENE",
            "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW",
            "W", "WNW", "NW", "NNW"
        )
        val normalized = ((degrees % 360) + 360) % 360
        val index = ((normalized / 22.5) + 0.5).toInt() % 16
        return directions[index]
    }

    fun uvCategoryLabel(uv: Int): String {
        return when {
            uv <= 2 -> "Low"
            uv in 3..5 -> "Moderate"
            uv in 6..7 -> "High"
            uv in 8..10 -> "Very High"
            else -> "Extreme"
        }
    }

    fun calculateDewPointCelsius(tempC: Double, humidityPercent: Int): Int {
        val rh = humidityPercent.coerceIn(1, 100) / 100.0
        val a = 17.27
        val b = 237.7
        val alpha = ((a * tempC) / (b + tempC)) + ln(rh)
        val dewPoint = (b * alpha) / (a - alpha)
        return dewPoint.roundToInt()
    }
}
