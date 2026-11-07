package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private data class NightStarSpec(
    val xFraction: Float,
    val yFraction: Float,
    val radiusDp: Float,
    val twinkleSpeed: Float,
    val phaseOffset: Float,
    val baseAlpha: Float,
    val tintIndex: Int
)

/**
 * Realistic Night Sky & Clear Moonlight Animation on Jetpack Compose Canvas.
 * Renders twinkling multi-size starfield, luminous moon with soft celestial halo,
 * and periodic shooting star trails in High Quality mode.
 */
@Composable
fun RealisticNightSkyCanvas(
    animationTimeSeconds: Float,
    isClearMoonlight: Boolean,
    config: WeatherAnimationConfig,
    modifier: Modifier = Modifier
) {
    val allStars = remember {
        val rng = Random(404)
        List(75) {
            NightStarSpec(
                xFraction = rng.nextFloat(),
                yFraction = rng.nextFloat() * 0.68f,
                radiusDp = 1.0f + rng.nextFloat() * 2.4f,
                twinkleSpeed = 1.2f + rng.nextFloat() * 2.2f,
                phaseOffset = rng.nextFloat() * (2f * PI.toFloat()),
                baseAlpha = 0.35f + rng.nextFloat() * 0.65f,
                tintIndex = rng.nextInt(3)
            )
        }
    }

    val activeStarCount = (allStars.size * config.particleDensityMultiplier)
        .roundToInt()
        .coerceIn(22, allStars.size)

    Canvas(modifier = modifier.fillMaxSize()) {
        drawNightSkyScene(
            w = size.width,
            h = size.height,
            timeSeconds = animationTimeSeconds,
            stars = allStars.take(activeStarCount),
            isClearMoonlight = isClearMoonlight,
            highQuality = config.qualityLevel == AnimationQualityLevel.HIGH && !config.batterySaverMode
        )
    }
}

private fun DrawScope.drawNightSkyScene(
    w: Float,
    h: Float,
    timeSeconds: Float,
    stars: List<NightStarSpec>,
    isClearMoonlight: Boolean,
    highQuality: Boolean
) {
    // 1. Twinkling Starfield
    stars.forEach { star ->
        val wave = (sin(timeSeconds * star.twinkleSpeed + star.phaseOffset) + 1f) * 0.5f
        val alpha = (star.baseAlpha * (0.4f + 0.6f * wave)).coerceIn(0.10f, 1.0f)
        val pos = Offset(star.xFraction * w, star.yFraction * h)
        val r = star.radiusDp.dp.toPx()

        val starColor = when (star.tintIndex) {
            0 -> Color(0xFFF8FAFC)
            1 -> Color(0xFFBAE6FD)
            else -> Color(0xFFDDD6FE)
        }

        drawCircle(
            color = starColor.copy(alpha = alpha),
            radius = r,
            center = pos
        )

        if (highQuality && star.radiusDp > 2.3f) {
            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = alpha * 0.28f),
                radius = r * 2.7f,
                center = pos
            )
        }
    }

    // 2. Periodic subtle shooting star across upper sky (every ~12 seconds in High Quality)
    if (highQuality) {
        val cyclePos = (timeSeconds % 12f)
        if (cyclePos in 2.0f..2.85f) {
            val p = (cyclePos - 2.0f) / 0.85f
            val headX = w * (0.78f - p * 0.45f)
            val headY = h * (0.09f + p * 0.16f)
            val tailX = headX + 46.dp.toPx()
            val tailY = headY - 18.dp.toPx()
            val meteorAlpha = sin(p * PI.toFloat()).coerceIn(0f, 1f) * 0.75f

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = meteorAlpha),
                        Color(0xFF38BDF8).copy(alpha = meteorAlpha * 0.4f),
                        Color.Transparent
                    ),
                    start = Offset(headX, headY),
                    end = Offset(tailX, tailY)
                ),
                start = Offset(headX, headY),
                end = Offset(tailX, tailY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }

    // 3. Glowing Moon with soft halo
    val moonCenter = Offset(w * 0.80f, h * 0.15f)
    val moonRadius = (if (isClearMoonlight) 42.dp else 36.dp).toPx()
    val haloScale = if (isClearMoonlight) 3.5f else 2.8f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x5538BDF8),
                Color(0x26818CF8),
                Color.Transparent
            ),
            center = moonCenter,
            radius = moonRadius * haloScale
        ),
        radius = moonRadius * haloScale,
        center = moonCenter
    )

    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFFFFBEB), Color(0xFFE0F2FE)),
            start = Offset(moonCenter.x - moonRadius, moonCenter.y - moonRadius),
            end = Offset(moonCenter.x + moonRadius, moonCenter.y + moonRadius)
        ),
        radius = moonRadius,
        center = moonCenter
    )

    if (!isClearMoonlight) {
        // Crescent shadow cutout for standard starlit night
        drawCircle(
            color = Color(0xFF080E24),
            radius = moonRadius * 0.85f,
            center = Offset(moonCenter.x - moonRadius * 0.34f, moonCenter.y - moonRadius * 0.22f)
        )
    } else {
        // Subtle lunar mare craters for full Clear Moonlight mode
        drawCircle(
            color = Color(0x2464748B),
            radius = moonRadius * 0.22f,
            center = Offset(moonCenter.x - moonRadius * 0.25f, moonCenter.y - moonRadius * 0.12f)
        )
        drawCircle(
            color = Color(0x1F64748B),
            radius = moonRadius * 0.16f,
            center = Offset(moonCenter.x + moonRadius * 0.22f, moonCenter.y + moonRadius * 0.20f)
        )
    }
}
