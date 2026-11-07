package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Categories of weather advisories evaluated by [WeatherAlertManager] in Phase 5.
 */
enum class WeatherAlertCategory(val displayName: String) {
    HEAVY_RAIN("Heavy Rain Advisory"),
    THUNDERSTORM("Thunderstorm Advisory"),
    EXTREME_TEMPERATURE("Extreme Temperature Advisory"),
    HIGH_WIND("High Wind Speed Advisory"),
    SNOW("Snow & Winter Advisory"),
    HEAVY_PRECIPITATION("Heavy Precipitation Advisory")
}

/**
 * Severity levels for estimated meteorological advisories.
 */
enum class WeatherAlertSeverity(val badgeText: String, val badgeColor: Color) {
    MODERATE("MODERATE", Color(0xFFFBBF24)),
    HIGH("HIGH", Color(0xFFF97316)),
    EXTREME("EXTREME", Color(0xFFEF4444))
}

/**
 * Represents an estimated weather alert derived from actual Open-Meteo meteorological metrics.
 * Always distinguishes estimated advisories from official government warnings.
 */
data class WeatherAlertItem(
    val id: String,
    val category: WeatherAlertCategory,
    val severity: WeatherAlertSeverity,
    val title: String,
    val summary: String,
    val metricTriggerDetail: String,
    val recommendedAction: String,
    val attributionLabel: String = "Estimated Weather Alert • Based on Open-Meteo API Telemetry (Not an Official Government Warning)"
)

/**
 * Phase 5 Weather Alert System.
 * Evaluates actual Open-Meteo current, hourly, and daily forecast telemetry for:
 * 1. Heavy rain
 * 2. Thunderstorm
 * 3. Extreme temperature
 * 4. High wind speed
 * 5. Snow
 * 6. Heavy precipitation
 *
 * Explicitly labels all alerts as estimated meteorological advisories rather than government warnings.
 */
object WeatherAlertManager {

    fun evaluateAlertsForCity(
        city: CityWeatherData,
        temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS
    ): List<WeatherAlertItem> {
        val alerts = mutableListOf<WeatherAlertItem>()
        val todayForecast = city.tenDayForecast.firstOrNull()
        val wmo = city.wmoCode
        val maxPopToday = maxOf(
            todayForecast?.rainProbability ?: 0,
            city.hourlyForecast.take(12).maxOfOrNull { it.popPercent } ?: 0
        )
        val precipSumMm = todayForecast?.precipitationSumMm ?: 0.0
        val maxHourlyPrecipMm = city.hourlyForecast.take(12).maxOfOrNull { it.precipitationMm } ?: 0.0

        // 1. Thunderstorm Check (WMO 95, 96, 99 or Thunderstorm condition)
        if (wmo in listOf(95, 96, 99) || city.condition == WeatherConditionType.THUNDERSTORM) {
            val isHail = wmo in listOf(96, 99)
            alerts.add(
                WeatherAlertItem(
                    id = "alert_thunderstorm_${city.id}",
                    category = WeatherAlertCategory.THUNDERSTORM,
                    severity = if (isHail) WeatherAlertSeverity.EXTREME else WeatherAlertSeverity.HIGH,
                    title = if (isHail) "Estimated Thunderstorm & Hail Advisory" else "Estimated Thunderstorm Activity",
                    summary = "Convective storm cells detected in ${city.cityName} with lightning potential and gusty downdrafts.",
                    metricTriggerDetail = "Open-Meteo WMO Code $wmo (${city.conditionDescription}) • Wind gusts up to ${city.details.windGustKmh} km/h",
                    recommendedAction = "Seek indoor shelter during active lightning and avoid open elevated areas."
                )
            )
        }

        // 2. Heavy Rain Check (WMO 63, 65, 81, 82 or daily rain sum >= 10mm)
        val isRainWmo = wmo in listOf(63, 65, 66, 67, 81, 82)
        if (isRainWmo || (city.condition == WeatherConditionType.RAINY && precipSumMm >= 8.0)) {
            val heavy = wmo in listOf(65, 67, 82) || precipSumMm >= 15.0
            alerts.add(
                WeatherAlertItem(
                    id = "alert_heavy_rain_${city.id}",
                    category = WeatherAlertCategory.HEAVY_RAIN,
                    severity = if (heavy) WeatherAlertSeverity.HIGH else WeatherAlertSeverity.MODERATE,
                    title = if (heavy) "Estimated Heavy Rain Advisory" else "Estimated Active Rainfall Advisory",
                    summary = "Sustained rainfall detected across ${city.cityName} with reduced road visibility.",
                    metricTriggerDetail = String.format(
                        Locale.US,
                        "Daily Liquid Sum: %.1f mm • Peak Hourly: %.1f mm • Visibility: %.1f km",
                        precipSumMm,
                        maxHourlyPrecipMm,
                        city.details.visibilityKm
                    ),
                    recommendedAction = "Carry waterproof gear and allow extra braking distance on wet roads."
                )
            )
        }

        // 3. Snow Check (WMO 71, 73, 75, 77, 85, 86 or SNOWY condition)
        val isSnowWmo = wmo in listOf(71, 73, 75, 77, 85, 86)
        if (isSnowWmo || city.condition == WeatherConditionType.SNOWY) {
            val heavySnow = wmo in listOf(75, 86) || precipSumMm >= 10.0
            alerts.add(
                WeatherAlertItem(
                    id = "alert_snow_${city.id}",
                    category = WeatherAlertCategory.SNOW,
                    severity = if (heavySnow) WeatherAlertSeverity.HIGH else WeatherAlertSeverity.MODERATE,
                    title = if (heavySnow) "Estimated Heavy Snowfall Advisory" else "Estimated Snow & Winter Advisory",
                    summary = "Snowfall and sub-freezing pavement temperatures indicated for ${city.cityName}.",
                    metricTriggerDetail = String.format(
                        Locale.US,
                        "WMO Code $wmo • Temp: %s • Precip Equivalent: %.1f mm",
                        formatTemperature(city.currentTempCelsius, temperatureUnit),
                        precipSumMm
                    ),
                    recommendedAction = "Dress in thermal layers and watch for icy surfaces on bridges and walkways."
                )
            )
        }

        // 4. Extreme Temperature Check (High >= 34°C or Low <= 0°C)
        val maxTempC = maxOf(city.currentTempCelsius, city.highTempCelsius)
        val minTempC = minOf(city.currentTempCelsius, city.lowTempCelsius)
        if (maxTempC >= 34) {
            alerts.add(
                WeatherAlertItem(
                    id = "alert_extreme_heat_${city.id}",
                    category = WeatherAlertCategory.EXTREME_TEMPERATURE,
                    severity = if (maxTempC >= 38) WeatherAlertSeverity.EXTREME else WeatherAlertSeverity.HIGH,
                    title = "Estimated Extreme Heat Advisory",
                    summary = "Elevated daytime temperatures and thermal heat index in ${city.cityName}.",
                    metricTriggerDetail = "Peak High: ${formatTemperature(maxTempC, temperatureUnit)} • Feels Like: ${formatTemperature(city.feelsLikeCelsius, temperatureUnit)} • UV Index: ${city.details.uvIndex}",
                    recommendedAction = "Stay hydrated, limit strenuous midday sun exposure, and wear UV protection."
                )
            )
        } else if (minTempC <= 0) {
            alerts.add(
                WeatherAlertItem(
                    id = "alert_extreme_cold_${city.id}",
                    category = WeatherAlertCategory.EXTREME_TEMPERATURE,
                    severity = if (minTempC <= -8) WeatherAlertSeverity.HIGH else WeatherAlertSeverity.MODERATE,
                    title = "Estimated Freezing Temperature Advisory",
                    summary = "Sub-zero temperatures and frost risk detected for ${city.cityName}.",
                    metricTriggerDetail = "Minimum Temp: ${formatTemperature(minTempC, temperatureUnit)} • Feels Like: ${formatTemperature(city.feelsLikeCelsius, temperatureUnit)}",
                    recommendedAction = "Protect exposed pipes, plants, and wear insulated cold-weather clothing."
                )
            )
        }

        // 5. High Wind Speed Check (Wind >= 28 km/h or Gusts >= 40 km/h)
        val windSpeed = city.details.windSpeedKmh
        val windGust = city.details.windGustKmh
        if (windSpeed >= 28 || windGust >= 40) {
            alerts.add(
                WeatherAlertItem(
                    id = "alert_high_wind_${city.id}",
                    category = WeatherAlertCategory.HIGH_WIND,
                    severity = if (windSpeed >= 45 || windGust >= 60) WeatherAlertSeverity.HIGH else WeatherAlertSeverity.MODERATE,
                    title = "Estimated High Wind Speed Advisory",
                    summary = "Strong surface winds and gusts measured in ${city.cityName}.",
                    metricTriggerDetail = "Sustained Wind: $windSpeed km/h (${city.details.windDirection}) • Wind Gusts: $windGust km/h",
                    recommendedAction = "Secure loose outdoor items and use caution when driving high-profile vehicles."
                )
            )
        }

        // 6. Heavy Precipitation Probability Check (POP >= 75% or Precipitation Sum >= 9.0 mm when not already covered)
        if (maxPopToday >= 75 || precipSumMm >= 9.0) {
            alerts.add(
                WeatherAlertItem(
                    id = "alert_heavy_precip_${city.id}",
                    category = WeatherAlertCategory.HEAVY_PRECIPITATION,
                    severity = if (maxPopToday >= 90 || precipSumMm >= 15.0) WeatherAlertSeverity.HIGH else WeatherAlertSeverity.MODERATE,
                    title = "Estimated High Precipitation Probability ($maxPopToday%)",
                    summary = "Open-Meteo forecast models indicate a $maxPopToday% probability of significant precipitation within 24 hours.",
                    metricTriggerDetail = String.format(
                        Locale.US,
                        "Max Rain Probability: %d%% • 24h Total Accumulation: %.1f mm",
                        maxPopToday,
                        precipSumMm
                    ),
                    recommendedAction = "Check the hourly forecast timeline before commuting or planning outdoor activities."
                )
            )
        }

        return alerts
    }

    /**
     * Detects significant temperature changes between current temperature and upcoming 24-hour / next-day highs.
     */
    fun detectSignificantTemperatureChange(
        city: CityWeatherData,
        temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS
    ): String? {
        val today = city.tenDayForecast.getOrNull(0) ?: return null
        val tomorrow = city.tenDayForecast.getOrNull(1) ?: return null
        val diff = tomorrow.maxTempCelsius - today.maxTempCelsius
        return when {
            diff >= 4 -> "Warming trend: Tomorrow's high rises by +${diff}°C to ${formatTemperature(tomorrow.maxTempCelsius, temperatureUnit)}."
            diff <= -4 -> "Cooling trend: Tomorrow's high drops by ${diff}°C to ${formatTemperature(tomorrow.maxTempCelsius, temperatureUnit)}."
            else -> null
        }
    }
}

@Composable
fun WeatherAlertsSection(
    alerts: List<WeatherAlertItem>,
    modifier: Modifier = Modifier
) {
    if (alerts.isEmpty()) return

    val palette = LocalGlassPalette.current
    var isExpanded by rememberSaveable { mutableStateOf(true) }

    GlassmorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weather_alerts_section_card"),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B).copy(alpha = 0.22f))
                            .border(1.dp, Color(0xFFF59E0B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = "Weather Advisories",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Estimated Weather Advisories (${alerts.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                        Text(
                            text = "Derived from real Open-Meteo telemetry • Not an official government warning",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accentGold
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse alerts" else "Expand alerts",
                    tint = palette.secondaryText
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    alerts.forEachIndexed { index, alert ->
                        WeatherAlertItemRow(alert = alert)
                        if (index < alerts.lastIndex) {
                            HorizontalDivider(color = palette.cardBorderSubtle)
                        }
                    }

                    // Explicit disclaimer distinguishing estimated API advisories from government warnings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.pillBackground)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = palette.accentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Note: These advisories are estimated automatically from Open-Meteo numerical weather metrics and are distinct from official national meteorological agency warnings.",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondaryText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherAlertItemRow(alert: WeatherAlertItem) {
    val palette = LocalGlassPalette.current
    val icon: ImageVector = when (alert.category) {
        WeatherAlertCategory.HEAVY_RAIN -> Icons.Filled.Umbrella
        WeatherAlertCategory.THUNDERSTORM -> Icons.Filled.Thunderstorm
        WeatherAlertCategory.EXTREME_TEMPERATURE -> Icons.Filled.Thermostat
        WeatherAlertCategory.HIGH_WIND -> Icons.Filled.Air
        WeatherAlertCategory.SNOW -> Icons.Filled.AcUnit
        WeatherAlertCategory.HEAVY_PRECIPITATION -> Icons.Filled.Bolt
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weather_alert_item_${alert.category.name}"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = alert.severity.badgeColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(alert.severity.badgeColor.copy(alpha = 0.22f))
                    .border(1.dp, alert.severity.badgeColor, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "ESTIMATED • ${alert.severity.badgeText}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = alert.severity.badgeColor
                )
            }
        }

        Text(
            text = alert.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.primaryText
        )

        Text(
            text = alert.metricTriggerDetail,
            style = MaterialTheme.typography.labelMedium,
            color = palette.accentCyan
        )

        Spacer(modifier = Modifier.height(1.dp))

        Text(
            text = "Guidance: ${alert.recommendedAction}",
            style = MaterialTheme.typography.labelMedium,
            color = palette.secondaryText
        )
    }
}
