package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private data class SnowflakeParticle(
    val xFraction: Float,
    val initialYFraction: Float,
    val radiusDp: Float,
    val fallSpeed: Float,
    val swayAmplitudeDp: Float,
    val swayFrequency: Float,
    val swayPhase: Float,
    val alpha: Float
)

/**
 * Realistic Snowfall Animation on Jetpack Compose Canvas.
 * Renders multi-size crystalline snow particles with natural vertical descent,
 * sinusoidal horizontal wind sway, and intensity variation.
 */
@Composable
fun RealisticSnowCanvas(
    animationTimeSeconds: Float,
    snowIntensity: Float, // 0.4f = light flurries, 0.75f = moderate snow, 1.0f = heavy snow
    config: WeatherAnimationConfig,
    modifier: Modifier = Modifier
) {
    val allFlakes = remember {
        val rng = Random(777)
        List(72) {
            val sizeFactor = rng.nextFloat()
            SnowflakeParticle(
                xFraction = rng.nextFloat(),
                initialYFraction = rng.nextFloat(),
                radiusDp = 1.6f + sizeFactor * 3.8f,
                fallSpeed = 0.032f + sizeFactor * 0.045f,
                swayAmplitudeDp = 10f + rng.nextFloat() * 18f,
                swayFrequency = 0.8f + rng.nextFloat() * 1.1f,
                swayPhase = rng.nextFloat() * 6.28f,
                alpha = 0.35f + sizeFactor * 0.55f
            )
        }
    }

    val activeFlakesCount = (allFlakes.size * snowIntensity.coerceIn(0.35f, 1.0f) * config.particleDensityMultiplier)
        .roundToInt()
        .coerceIn(16, allFlakes.size)

    Canvas(modifier = modifier.fillMaxSize()) {
        drawSnowfallScene(
            w = size.width,
            h = size.height,
            timeSeconds = animationTimeSeconds,
            flakes = allFlakes.take(activeFlakesCount)
        )
    }
}

private fun DrawScope.drawSnowfallScene(
    w: Float,
    h: Float,
    timeSeconds: Float,
    flakes: List<SnowflakeParticle>
) {
    flakes.forEach { flake ->
        val yCycle = (flake.initialYFraction + timeSeconds * flake.fallSpeed) % 1.0f
        val yPos = yCycle * h
        val swayPx = sin(timeSeconds * flake.swayFrequency + flake.swayPhase) * flake.swayAmplitudeDp.dp.toPx()
        val xPos = ((flake.xFraction * w + swayPx) % w).let { if (it < 0f) it + w else it }
        val r = flake.radiusDp.dp.toPx()

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = flake.alpha),
                    Color(0xFFBAE6FD).copy(alpha = flake.alpha * 0.35f),
                    Color.Transparent
                ),
                center = Offset(xPos, yPos),
                radius = r * 1.6f
            ),
            radius = r * 1.6f,
            center = Offset(xPos, yPos)
        )
    }
}
