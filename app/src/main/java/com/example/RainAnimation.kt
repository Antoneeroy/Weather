package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.random.Random

private data class RealisticRainStreak(
    val xFraction: Float,
    val initialYFraction: Float,
    val speedMultiplier: Float,
    val lengthDp: Float,
    val strokeWidthDp: Float,
    val baseAlpha: Float,
    val windSlant: Float,
    val layerDepth: Int // 0 = background mist drop, 1 = mid rain, 2 = foreground fast streak
)

private data class RainSplashRipple(
    val xFraction: Float,
    val yFraction: Float,
    val phaseOffset: Float,
    val maxRadiusDp: Float
)

/**
 * Realistic Canvas Rain Animation supporting light drizzle, moderate rain, and heavy downpours
 * with multi-depth raindrop streaks, natural wind angle, and surface splash ripples.
 */
@Composable
fun RealisticRainCanvas(
    animationTimeSeconds: Float,
    rainIntensity: Float, // 0.25f = light drizzle, 0.6f = moderate rain, 1.0f = heavy rain/storm
    isHeavyRain: Boolean,
    config: WeatherAnimationConfig,
    modifier: Modifier = Modifier
) {
    val allStreaks = remember {
        val rng = Random(2026)
        List(110) { idx ->
            val depth = when {
                idx < 45 -> 0
                idx < 85 -> 1
                else -> 2
            }
            RealisticRainStreak(
                xFraction = rng.nextFloat(),
                initialYFraction = rng.nextFloat(),
                speedMultiplier = when (depth) {
                    0 -> 0.55f + rng.nextFloat() * 0.35f
                    1 -> 0.85f + rng.nextFloat() * 0.45f
                    else -> 1.25f + rng.nextFloat() * 0.55f
                },
                lengthDp = when (depth) {
                    0 -> 12f + rng.nextFloat() * 10f
                    1 -> 20f + rng.nextFloat() * 16f
                    else -> 30f + rng.nextFloat() * 22f
                },
                strokeWidthDp = when (depth) {
                    0 -> 1.0f
                    1 -> 1.6f
                    else -> 2.3f
                },
                baseAlpha = when (depth) {
                    0 -> 0.16f + rng.nextFloat() * 0.14f
                    1 -> 0.28f + rng.nextFloat() * 0.22f
                    else -> 0.42f + rng.nextFloat() * 0.28f
                },
                windSlant = -0.12f - rng.nextFloat() * 0.08f,
                layerDepth = depth
            )
        }
    }

    val splashRipples = remember {
        val rng = Random(909)
        List(16) {
            RainSplashRipple(
                xFraction = 0.06f + rng.nextFloat() * 0.88f,
                yFraction = 0.22f + rng.nextFloat() * 0.72f,
                phaseOffset = rng.nextFloat(),
                maxRadiusDp = 6f + rng.nextFloat() * 8f
            )
        }
    }

    val activeDropCount = (allStreaks.size * rainIntensity.coerceIn(0.3f, 1.0f) * config.particleDensityMultiplier)
        .roundToInt()
        .coerceIn(14, allStreaks.size)

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRainScene(
            w = size.width,
            h = size.height,
            timeSeconds = animationTimeSeconds,
            streaks = allStreaks.take(activeDropCount),
            ripples = if (config.qualityLevel == AnimationQualityLevel.HIGH && !config.batterySaverMode) {
                splashRipples
            } else {
                emptyList()
            },
            isHeavyRain = isHeavyRain,
            rainIntensity = rainIntensity
        )
    }
}

private fun DrawScope.drawRainScene(
    w: Float,
    h: Float,
    timeSeconds: Float,
    streaks: List<RealisticRainStreak>,
    ripples: List<RainSplashRipple>,
    isHeavyRain: Boolean,
    rainIntensity: Float
) {
    val speedScale = if (isHeavyRain) 0.95f else 0.68f
    val windFactor = if (isHeavyRain) 1.35f else 0.90f
    val primaryDropColor = if (isHeavyRain) Color(0xFF7DD3FC) else Color(0xFFBAE6FD)

    streaks.forEach { drop ->
        val cycle = (drop.initialYFraction + timeSeconds * drop.speedMultiplier * speedScale) % 1.0f
        val startY = cycle * h
        val slant = drop.windSlant * windFactor
        val rawX = (drop.xFraction + cycle * slant) % 1.0f
        val startX = (if (rawX < 0f) rawX + 1f else rawX) * w

        val lengthPx = drop.lengthDp.dp.toPx() * (if (isHeavyRain) 1.28f else 1.0f)
        val endX = startX + lengthPx * slant
        val endY = startY + lengthPx

        val effectiveAlpha = (drop.baseAlpha * (0.65f + rainIntensity * 0.35f)).coerceIn(0.08f, 0.85f)

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    primaryDropColor.copy(alpha = effectiveAlpha * 0.5f),
                    Color.White.copy(alpha = effectiveAlpha)
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY)
            ),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = drop.strokeWidthDp.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // Subtle raindrop impact ripples on glass planes in High Quality mode
    ripples.forEach { ripple ->
        val rippleProgress = ((timeSeconds * 0.9f + ripple.phaseOffset) % 1.0f)
        val radiusPx = ripple.maxRadiusDp.dp.toPx() * rippleProgress
        val alpha = ((1f - rippleProgress) * 0.26f * rainIntensity).coerceIn(0f, 0.3f)
        if (alpha > 0.02f) {
            val center = Offset(ripple.xFraction * w, ripple.yFraction * h)
            drawOval(
                color = primaryDropColor.copy(alpha = alpha),
                topLeft = Offset(center.x - radiusPx, center.y - radiusPx * 0.45f),
                size = Size(radiusPx * 2f, radiusPx * 0.9f),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}
