package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Realistic Sun & Sunlight Ray Animation on Jetpack Compose Canvas.
 * Reduced by 50% in size for a neat, elegant sky presence that never overlaps
 * temperature, city name, or weather information on the main Home Dashboard.
 * Maintains smooth breathing pulse glow and rotating sunlight rays while remaining
 * responsive across all Android phone and tablet screen sizes.
 */
@Composable
fun RealisticSunCanvas(
    animationTimeSeconds: Float,
    isGoldenHour: Boolean,
    isDarkTheme: Boolean,
    config: WeatherAnimationConfig,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRealisticSunlight(
            w = size.width,
            h = size.height,
            timeSeconds = animationTimeSeconds,
            isGoldenHour = isGoldenHour,
            isDarkTheme = isDarkTheme,
            highQuality = config.qualityLevel == AnimationQualityLevel.HIGH && !config.batterySaverMode
        )
    }
}

private fun DrawScope.drawRealisticSunlight(
    w: Float,
    h: Float,
    timeSeconds: Float,
    isGoldenHour: Boolean,
    isDarkTheme: Boolean,
    highQuality: Boolean
) {
    val minDim = min(w, h)

    // Subtle breathing pulse and gentle horizontal drift
    val pulse = 0.95f + 0.05f * sin(timeSeconds * 1.4f)
    val driftX = sin(timeSeconds * 0.35f) * 3.5.dp.toPx()

    // Positioned neatly in the upper-right sky corner to avoid temperature, city name, or cards
    val sunCenter = Offset(
        x = w * 0.86f + driftX,
        y = h * (if (isGoldenHour) 0.105f else 0.085f)
    )

    // Reduced by 50% (from 0.125f to 0.0625f, clamped between 16.dp and 36.dp), responsive across all screens
    val baseRadius = (minDim * 0.0625f * pulse).coerceIn(16.dp.toPx(), 36.dp.toPx())

    val outerGlowColor = if (isGoldenHour) Color(0x66F97316) else Color(0x55FBBF24)
    val midGlowColor = if (isGoldenHour) Color(0x38FB7185) else Color(0x24F59E0B)
    val coreColor = if (isGoldenHour) Color(0xFFFFEDD5) else Color(0xFFFFFBEB)
    val rimColor = if (isGoldenHour) Color(0xFFFB923C) else Color(0xFFFDE047)

    // 1. Soft ambient outer glowing circle (proportionally reduced by 50%)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                outerGlowColor,
                midGlowColor,
                Color(0x0A38BDF8),
                Color.Transparent
            ),
            center = sunCenter,
            radius = baseRadius * 2.15f
        ),
        radius = baseRadius * 2.15f,
        center = sunCenter
    )

    // 2. Proportionally scaled rotating sunlight rays
    val rayCount = if (highQuality) 12 else 8
    val rotationDeg = (timeSeconds * 6f) % 360f
    rotate(degrees = rotationDeg, pivot = sunCenter) {
        for (i in 0 until rayCount) {
            val angleRad = (i * (360f / rayCount)) * (PI.toFloat() / 180f)
            val innerR = baseRadius * 0.60f
            val outerR = baseRadius * (1.26f + (i % 2) * 0.24f)
            val start = Offset(
                sunCenter.x + cos(angleRad) * innerR,
                sunCenter.y + sin(angleRad) * innerR
            )
            val end = Offset(
                sunCenter.x + cos(angleRad) * outerR,
                sunCenter.y + sin(angleRad) * outerR
            )
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        rimColor.copy(alpha = 0.38f),
                        Color.Transparent
                    ),
                    start = start,
                    end = end
                ),
                start = start,
                end = end,
                strokeWidth = (if (i % 2 == 0) 2.dp else 1.25.dp).toPx(),
                cap = StrokeCap.Round
            )
        }
    }

    // 3. Glowing solar core (proportionally scaled down 50%)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                coreColor,
                rimColor,
                outerGlowColor,
                Color.Transparent
            ),
            center = sunCenter,
            radius = baseRadius * 0.68f
        ),
        radius = baseRadius * 0.68f,
        center = sunCenter
    )

    // 4. Subtle optical lens-flare bokeh rings (proportionally reduced)
    if (highQuality) {
        val flareAlpha = if (isDarkTheme) 0.08f else 0.12f
        drawCircle(
            color = Color(0xFF38BDF8).copy(alpha = flareAlpha),
            radius = 8.dp.toPx(),
            center = Offset(w * 0.64f - driftX * 0.35f, h * 0.15f)
        )
        drawCircle(
            color = (if (isGoldenHour) Color(0xFFFB7185) else Color(0xFFA855F7)).copy(alpha = flareAlpha * 0.8f),
            radius = 12.dp.toPx(),
            center = Offset(w * 0.48f - driftX * 0.55f, h * 0.20f)
        )
    }
}
