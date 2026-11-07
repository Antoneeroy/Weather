package com.example

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Dedicated Forecast Detail Screen opened when a user taps any forecast day row.
 * Prominently displays the clicked day's full 24-hour hourly forecast right below the hero summary,
 * plus interactive 10-day switching and detailed meteorological metrics.
 */
@Composable
fun ForecastDetailScreen(
    cityName: String,
    regionAndCountry: String,
    day: DailyForecastItem,
    overallMinTemp: Int,
    overallMaxTemp: Int,
    temperatureUnit: TemperatureUnit,
    animationsEnabled: Boolean,
    onBack: () -> Unit,
    tenDays: List<DailyForecastItem> = emptyList(),
    onSelectAnotherDay: (DailyForecastItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val palette = LocalGlassPalette.current
    val detailedCategory = WmoWeatherCodeMapper.mapToDetailedCategory(day.wmoCode)

    // Ensure the clicked day always has a full 24-hour forecast list ready to display
    val dayHourlyForecast = remember(day) {
        if (day.hourlyItemsForDay.isNotEmpty()) {
            day.hourlyItemsForDay
        } else {
            SampleWeatherRepository.generate24HoursForDay(
                dayId = day.id,
                minTempCelsius = day.minTempCelsius,
                maxTempCelsius = day.maxTempCelsius,
                condition = day.condition,
                wmoCode = day.wmoCode,
                rainProbability = day.rainProbability,
                precipitationSumMm = day.precipitationSumMm,
                windKmh = day.windKmh,
                humidityPercent = day.humidityPercent,
                isToday = day.dayName.equals("Today", ignoreCase = true)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("forecast_detail_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 14.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Navigation Bar with Back Button & Selected Date
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(palette.pillBackground)
                            .border(1.dp, palette.cardBorderLight, CircleShape)
                            .clickable(onClick = onBack)
                            .testTag("forecast_detail_back_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Forecast",
                            tint = palette.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${day.dayName} • ${day.fullDateFormatted}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                        Text(
                            text = "$cityName, $regionAndCountry",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondaryText
                        )
                    }
                }
            }

            // 1b. Interactive 10-Day Switcher Strip (tap any day to view its hourly forecast)
            if (tenDays.size > 1) {
                item {
                    val dayScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(dayScrollState)
                            .testTag("detail_day_selector_strip"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tenDays.forEach { candidateDay ->
                            val isSelected = candidateDay.id == day.id
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (isSelected) palette.accentCyan.copy(alpha = 0.26f)
                                        else palette.pillBackground
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) palette.accentCyan else palette.cardBorderSubtle,
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .clickable { onSelectAnotherDay(candidateDay) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("detail_day_pill_${candidateDay.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                WeatherConditionArtworkIcon(
                                    condition = candidateDay.condition,
                                    iconSize = 20.dp,
                                    animationsEnabled = false
                                )
                                Column {
                                    Text(
                                        text = candidateDay.dayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) palette.primaryText else palette.secondaryText
                                    )
                                    Text(
                                        text = candidateDay.dateShort,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) palette.accentCyan else palette.mutedText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Clicked Day's Full 24-Hour Hourly Forecast (Prominently right at the top!)
            item {
                HourlyForecastStrip(
                    hourlyItems = dayHourlyForecast,
                    temperatureUnit = temperatureUnit,
                    animationsEnabled = animationsEnabled,
                    title = "${day.dayName} (${day.dateShort}) • Hourly Forecast",
                    subtitleBadge = "12 AM – 11 PM • ${dayHourlyForecast.size} Hours",
                    selectedDayLabel = "${day.dayName}, ${day.dateShort}"
                )
            }

            // 3. Hero Day Summary Card (Weather Icon, Condition, Max/Min Temp, Range Bar)
            item {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forecast_detail_hero_card"),
                    isHighlighted = true,
                    shape = RoundedCornerShape(30.dp),
                    contentPadding = PaddingValues(22.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                tint = palette.accentCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = day.fullDateFormatted,
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.accentCyan
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            WeatherConditionArtworkIcon(
                                condition = day.condition,
                                iconSize = 76.dp,
                                animationsEnabled = animationsEnabled
                            )
                            Spacer(modifier = Modifier.width(18.dp))
                            Column {
                                Text(
                                    text = formatTemperature(day.maxTempCelsius, temperatureUnit),
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryText
                                )
                                Text(
                                    text = "Low ${formatTemperature(day.minTempCelsius, temperatureUnit)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.secondaryText
                                )
                            }
                        }

                        Text(
                            text = day.conditionLabel,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = detailedCategory.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondaryText,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Scaled Temperature Bar within 10-Day context
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Daily Min: ${formatTemperature(day.minTempCelsius, temperatureUnit)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.secondaryText
                                )
                                Text(
                                    text = "Daily Max: ${formatTemperature(day.maxTempCelsius, temperatureUnit)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.primaryText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            ConditionScaledTemperatureRangeBar(
                                dayMinCelsius = day.minTempCelsius,
                                dayMaxCelsius = day.maxTempCelsius,
                                overallMinCelsius = overallMinTemp,
                                overallMaxCelsius = overallMaxTemp,
                                condition = day.condition,
                                wmoCode = day.wmoCode,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 4. Detailed Metrics Grid: Rain Probability, Precipitation (mm), Humidity, Wind Speed, UV, Sunrise
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WeatherMetricGlassCard(
                            title = "Rain Probability",
                            primaryValue = "${day.rainProbability}%",
                            subtitle = if (day.rainProbability >= 50) "High chance of precipitation" else "Low to moderate rain risk",
                            icon = Icons.Filled.WaterDrop,
                            accentColor = Color(0xFF38BDF8),
                            progressFraction = day.rainProbability / 100f,
                            modifier = Modifier.weight(1f),
                            testTagName = "detail_rain_prob_card"
                        )
                        WeatherMetricGlassCard(
                            title = "Precipitation",
                            primaryValue = "${String.format(Locale.US, "%.1f", day.precipitationSumMm)} mm",
                            subtitle = "Total daily liquid equivalent sum",
                            icon = Icons.Filled.Grain,
                            accentColor = Color(0xFF60A5FA),
                            progressFraction = (day.precipitationSumMm.toFloat() / 20f).coerceIn(0.05f, 1f),
                            modifier = Modifier.weight(1f),
                            testTagName = "detail_precip_mm_card"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WeatherMetricGlassCard(
                            title = "Humidity",
                            primaryValue = "${day.humidityPercent}%",
                            subtitle = "Average daily relative humidity",
                            icon = Icons.Filled.WaterDrop,
                            accentColor = Color(0xFF34D399),
                            progressFraction = day.humidityPercent / 100f,
                            modifier = Modifier.weight(1f),
                            testTagName = "detail_humidity_card"
                        )
                        WeatherMetricGlassCard(
                            title = "Wind Speed",
                            primaryValue = "${day.windKmh} km/h",
                            subtitle = "Maximum daily 10m wind speed",
                            icon = Icons.Filled.Air,
                            accentColor = Color(0xFF818CF8),
                            progressFraction = (day.windKmh / 60f).coerceIn(0.08f, 1f),
                            modifier = Modifier.weight(1f),
                            testTagName = "detail_wind_card"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WeatherMetricGlassCard(
                            title = "UV Index",
                            primaryValue = "${day.uvIndex} (${WmoWeatherCodeMapper.uvCategoryLabel(day.uvIndex)})",
                            subtitle = if (day.uvIndex >= 6) "Sun protection advised" else "Moderate to low UV",
                            icon = Icons.Filled.WbSunny,
                            accentColor = Color(0xFFFBBF24),
                            progressFraction = (day.uvIndex / 11f).coerceIn(0.08f, 1f),
                            modifier = Modifier.weight(1f),
                            testTagName = "detail_uv_card"
                        )
                        WeatherMetricGlassCard(
                            title = "Sunrise",
                            primaryValue = day.sunriseTime,
                            subtitle = "Sunset at ${day.sunsetTime}",
                            icon = Icons.Filled.WbSunny,
                            accentColor = Color(0xFFF59E0B),
                            progressFraction = 0.6f,
                            modifier = Modifier.weight(1f),
                            testTagName = "detail_sunrise_card"
                        )
                    }
                }
            }

            // 5. Sunrise & Sunset Solar Arc Card
            item {
                SunsetSolarArcGlassCard(
                    sunriseTime = day.sunriseTime,
                    sunsetTime = day.sunsetTime,
                    daylightDuration = "Sunrise ${day.sunriseTime} → Sunset ${day.sunsetTime}"
                )
            }
        }
    }
}
