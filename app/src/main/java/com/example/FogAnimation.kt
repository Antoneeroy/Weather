package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin

/**
 * Realistic Atmospheric Fog & Mist Animation on Jetpack Compose Canvas.
 * Renders softly drifting horizontal mist banks that preserve glass card contrast.
 */
@Composable
fun RealisticFogCanvas(
    animationTimeSeconds: Float,
    fogDensity: Float,
    isDarkTheme: Boolean,
    config: WeatherAnimationConfig,
    modifier: Modifier = Modifier
) {
    val bandCount = if (config.qualityLevel == AnimationQualityLevel.HIGH && !config.batterySaverMode) 5 else 3

    Canvas(modifier = modifier.fillMaxSize()) {
        drawFogLayers(
            w = size.width,
            h = size.height,
            timeSeconds = animationTimeSeconds,
            bandCount = bandCount,
            fogDensity = fogDensity,
            isDarkTheme = isDarkTheme
        )
    }
}

private fun DrawScope.drawFogLayers(
    w: Float,
    h: Float,
    timeSeconds: Float,
    bandCount: Int,
    fogDensity: Float,
    isDarkTheme: Boolean
) {
    val baseColor = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFFF8FAFC)
    val maxAlpha = (if (isDarkTheme) 0.16f else 0.28f) * fogDensity.coerceIn(0.4f, 1.0f)

    for (i in 0 until bandCount) {
        val dir = if (i % 2 == 0) 1f else -1f
        val driftOffset = sin(timeSeconds * (0.22f + i * 0.05f) * dir + i * 1.3f) * (w * 0.18f)
        val bandY = h * (0.12f + i * 0.16f)
        val bandHeight = h * 0.18f
        val bandWidth = w * 1.45f
        val leftX = -w * 0.22f + driftOffset

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    baseColor.copy(alpha = maxAlpha),
                    baseColor.copy(alpha = maxAlpha * 0.65f),
                    Color.Transparent
                ),
                startY = bandY,
                endY = bandY + bandHeight
            ),
            topLeft = Offset(leftX, bandY),
            size = Size(bandWidth, bandHeight),
            cornerRadius = CornerRadius(bandHeight * 0.5f, bandHeight * 0.5f)
        )
    }
}
