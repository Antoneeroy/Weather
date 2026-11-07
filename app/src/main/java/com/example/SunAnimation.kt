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
 * Reduced by ~40% in size for an elegant, refined ambient sky presence that never
 * covers temperature or weather information, while maintaining smooth glow and ray animations
 * and responsive scaling across phone and tablet screen sizes.
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
    val pulse = 0.94f + 0.06f * sin(timeSeconds * 1.4f)
    val driftX = sin(timeSeconds * 0.35f) * 5.dp.toPx()

    // Positioned in the upper-right sky corner so it never covers hero temperature or text
    val sunCenter = Offset(
        x = w * 0.85f + driftX,
        y = h * (if (isGoldenHour) 0.115f else 0.092f)
    )

    // Reduced by ~40% from previous 0.21f width ratio, responsive to min(width, height)
    val baseRadius = (minDim * 0.125f * pulse).coerceIn(28.dp.toPx(), 72.dp.toPx())

    val outerGlowColor = if (isGoldenHour) Color(0x66F97316) else Color(0x55FBBF24)
    val midGlowColor = if (isGoldenHour) Color(0x38FB7185) else Color(0x24F59E0B)
    val coreColor = if (isGoldenHour) Color(0xFFFFEDD5) else Color(0xFFFFFBEB)
    val rimColor = if (isGoldenHour) Color(0xFFFB923C) else Color(0xFFFDE047)

    // 1. Soft ambient solar corona
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

    // 2. Refined rotating volumetric sunlight rays
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
                strokeWidth = (if (i % 2 == 0) 4.dp else 2.5.dp).toPx(),
                cap = StrokeCap.Round
            )
        }
    }

    // 3. Glowing solar photosphere core (~40% smaller and crisp)
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

    // 4. Subtle optical lens-flare bokeh rings (High Quality mode)
    if (highQuality) {
        val flareAlpha = if (isDarkTheme) 0.10f else 0.14f
        drawCircle(
            color = Color(0xFF38BDF8).copy(alpha = flareAlpha),
            radius = 15.dp.toPx(),
            center = Offset(w * 0.62f - driftX * 0.35f, h * 0.18f)
        )
        drawCircle(
            color = (if (isGoldenHour) Color(0xFFFB7185) else Color(0xFFA855F7)).copy(alpha = flareAlpha * 0.8f),
            radius = 24.dp.toPx(),
            center = Offset(w * 0.42f - driftX * 0.55f, h * 0.25f)
        )
    }
}
