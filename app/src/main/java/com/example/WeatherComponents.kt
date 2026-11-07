package com.example

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Reusable Frosted Glassmorphism Card with specular highlight border and translucent gradient fill.
 */
@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalGlassPalette.current
    val fillBrush = if (isHighlighted) {
        Brush.verticalGradient(
            colors = listOf(
                palette.cardHighlightSurface,
                palette.cardSurfaceBottom
            )
        )
    } else {
        palette.glassCardBrush
    }

    val borderBrush = if (isHighlighted) {
        Brush.linearGradient(
            colors = listOf(
                palette.accentCyan.copy(alpha = 0.85f),
                palette.accentPurple.copy(alpha = 0.55f)
            )
        )
    } else {
        palette.glassBorderBrush
    }

    val clickableModifier = if (onClick != null) {
        Modifier
            .clip(shape)
            .clickable(onClick = onClick)
    } else {
        Modifier.clip(shape)
    }

    Box(
        modifier = modifier
            .then(clickableModifier)
            .background(brush = fillBrush, shape = shape)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .padding(contentPadding),
        content = content
    )
}

/**
 * Individual Glassmorphic Weather Information Metric Card
 * Used for Humidity, Wind Speed, Pressure, Visibility, UV Index, Sunrise, and Sunset.
 */
@Composable
fun WeatherMetricGlassCard(
    title: String,
    primaryValue: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    progressFraction: Float? = null,
    modifier: Modifier = Modifier,
    testTagName: String = "metric_card"
) {
    val palette = LocalGlassPalette.current

    GlassmorphicCard(
        modifier = modifier.testTag(testTagName),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = primaryValue,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText
            )

            if (progressFraction != null) {
                val clamped = progressFraction.coerceIn(0.05f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(palette.cardBorderSubtle)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(clamped)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(accentColor, palette.accentPurple)
                                )
                            )
                    )
                }
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.mutedText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Grid of 7 Individual Glass Cards for Atmospheric Weather Information:
 * 1. Humidity
 * 2. Wind Speed
 * 3. Pressure
 * 4. Visibility
 * 5. UV Index
 * 6. Sunrise
 * 7. Sunset
 */
@Composable
fun WeatherInformationCardsSection(
    details: AtmosphericDetails,
    temperatureUnit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val dewPointFormatted = formatTemperature(details.dewPointCelsius, temperatureUnit)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.weather_details_title),
            style = MaterialTheme.typography.titleLarge,
            color = palette.primaryText,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Row 1: Humidity & Wind Speed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WeatherMetricGlassCard(
                title = stringResource(R.string.card_humidity),
                primaryValue = "${details.humidityPercent}%",
                subtitle = "Dew point is $dewPointFormatted right now",
                icon = Icons.Filled.WaterDrop,
                accentColor = Color(0xFF38BDF8),
                progressFraction = details.humidityPercent / 100f,
                modifier = Modifier.weight(1f),
                testTagName = "card_humidity"
            )
            WeatherMetricGlassCard(
                title = stringResource(R.string.card_wind_speed),
                primaryValue = "${details.windSpeedKmh} km/h",
                subtitle = "${details.windDirection} • Gusts ${details.windGustKmh} km/h",
                icon = Icons.Filled.Air,
                accentColor = Color(0xFF818CF8),
                progressFraction = (details.windSpeedKmh / 60f).coerceIn(0.1f, 1f),
                modifier = Modifier.weight(1f),
                testTagName = "card_wind_speed"
            )
        }

        // Row 2: Pressure & Visibility
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WeatherMetricGlassCard(
                title = stringResource(R.string.card_pressure),
                primaryValue = "${details.pressureHpa} hPa",
                subtitle = details.pressureTrend,
                icon = Icons.Filled.Compress,
                accentColor = Color(0xFFA855F7),
                progressFraction = ((details.pressureHpa - 980f) / 60f).coerceIn(0.1f, 1f),
                modifier = Modifier.weight(1f),
                testTagName = "card_pressure"
            )
            WeatherMetricGlassCard(
                title = stringResource(R.string.card_visibility),
                primaryValue = "${details.visibilityKm} km",
                subtitle = details.visibilityStatus,
                icon = Icons.Filled.Visibility,
                accentColor = Color(0xFF34D399),
                progressFraction = (details.visibilityKm.toFloat() / 20f).coerceIn(0.1f, 1f),
                modifier = Modifier.weight(1f),
                testTagName = "card_visibility"
            )
        }

        // Row 3: UV Index & Sunrise
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WeatherMetricGlassCard(
                title = stringResource(R.string.card_uv_index),
                primaryValue = "${details.uvIndex} (${details.uvCategory})",
                subtitle = if (details.uvIndex >= 6) "Sun protection recommended" else "Low solar UV risk today",
                icon = Icons.Filled.WbSunny,
                accentColor = Color(0xFFFBBF24),
                progressFraction = (details.uvIndex / 11f).coerceIn(0.08f, 1f),
                modifier = Modifier.weight(1f),
                testTagName = "card_uv_index"
            )
            WeatherMetricGlassCard(
                title = stringResource(R.string.card_sunrise),
                primaryValue = details.sunriseTime,
                subtitle = "Dawn horizon • Daylight ${details.daylightDuration}",
                icon = Icons.Filled.WbSunny,
                accentColor = Color(0xFFF59E0B),
                progressFraction = 0.62f,
                modifier = Modifier.weight(1f),
                testTagName = "card_sunrise"
            )
        }

        // Row 4: Dedicated Sunset Glass Card with Solar Horizon Arc visualization
        SunsetSolarArcGlassCard(
            sunriseTime = details.sunriseTime,
            sunsetTime = details.sunsetTime,
            daylightDuration = details.daylightDuration,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SunsetSolarArcGlassCard(
    sunriseTime: String,
    sunsetTime: String,
    daylightDuration: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current

    GlassmorphicCard(
        modifier = modifier.testTag("card_sunset"),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF43F5E).copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WbTwilight,
                            contentDescription = stringResource(R.string.card_sunset),
                            tint = Color(0xFFFB7185),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.card_sunset).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.secondaryText
                        )
                        Text(
                            text = sunsetTime,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Sunrise: $sunriseTime",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.accentGold
                    )
                    Text(
                        text = "Daylight: $daylightDuration",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.mutedText
                    )
                }
            }

            // Custom Solar Path Arc
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                val w = size.width
                val h = size.height
                val horizonY = h * 0.82f

                // Dashed horizon line
                drawLine(
                    color = palette.cardBorderLight,
                    start = Offset(12.dp.toPx(), horizonY),
                    end = Offset(w - 12.dp.toPx(), horizonY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )

                // Solar trajectory arc
                drawArc(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0xFFFBBF24),
                            Color(0xFFFB7185),
                            Color(0xFFA855F7)
                        )
                    ),
                    startAngle = 195f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(w * 0.08f, h * 0.14f),
                    size = Size(w * 0.84f, h * 1.35f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Glowing sun position along the arc
                val sunOrbCenter = Offset(w * 0.72f, h * 0.34f)
                drawCircle(
                    color = Color(0x55FB7185),
                    radius = 12.dp.toPx(),
                    center = sunOrbCenter
                )
                drawCircle(
                    color = Color(0xFFFDE047),
                    radius = 6.dp.toPx(),
                    center = sunOrbCenter
                )
            }
        }
    }
}

/**
 * Floating Glassmorphic Bottom Navigation Bar respecting Android navigationBars WindowInsets.
 */
@Composable
fun WeatherGlassBottomNavigationBar(
    currentDestination: WeatherDestination,
    onNavigate: (WeatherDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val navShape = RoundedCornerShape(30.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(navShape)
                .background(palette.navBarSurface)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            palette.cardBorderLight,
                            palette.cardBorderSubtle
                        )
                    ),
                    shape = navShape
                )
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .testTag("bottom_navigation_bar"),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WeatherDestination.entries.forEach { destination ->
                val selected = destination == currentDestination
                val pillWidth by animateDpAsState(
                    targetValue = if (selected) 88.dp else 64.dp,
                    animationSpec = tween(250),
                    label = "nav_pill_w"
                )
                val pillBg by animateColorAsState(
                    targetValue = if (selected) palette.accentCyan.copy(alpha = 0.22f) else Color.Transparent,
                    animationSpec = tween(250),
                    label = "nav_pill_bg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (selected) palette.accentCyan else palette.secondaryText,
                    animationSpec = tween(250),
                    label = "nav_content_color"
                )

                val icon = when (destination) {
                    WeatherDestination.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                    WeatherDestination.FORECAST -> if (selected) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth
                    WeatherDestination.CITIES -> if (selected) Icons.Filled.LocationCity else Icons.Outlined.LocationCity
                    WeatherDestination.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
                }

                Column(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .width(pillWidth)
                        .clip(RoundedCornerShape(22.dp))
                        .background(pillBg)
                        .clickable { onNavigate(destination) }
                        .padding(vertical = 8.dp)
                        .testTag("nav_tab_${destination.route}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = destination.label,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
