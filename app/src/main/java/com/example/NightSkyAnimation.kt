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
import kotlin.math.min
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
 * Realistic Night Sky & Moon Animation on Jetpack Compose Canvas.
 * Inspired by authentic astronomical imagery:
 * - Realistic detailed full moon with clean white and silver natural surface textures
 * - Visible natural lunar craters (Tycho, Copernicus, Oceanus Procellarum, Mare Tranquillitatis)
 * - Soft, natural multi-stage ethereal glow around the moon
 * - Deep dark navy night sky background with twinkling stars and subtle shooting stars
 * - Balanced, non-cartoon, non-oversized moon size responsive across all screen sizes
 * - Smooth breathing animation
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
        List(85) {
            NightStarSpec(
                xFraction = rng.nextFloat(),
                yFraction = rng.nextFloat() * 0.72f,
                radiusDp = 0.8f + rng.nextFloat() * 2.2f,
                twinkleSpeed = 1.1f + rng.nextFloat() * 2.4f,
                phaseOffset = rng.nextFloat() * (2f * PI.toFloat()),
                baseAlpha = 0.35f + rng.nextFloat() * 0.65f,
                tintIndex = rng.nextInt(3)
            )
        }
    }

    val activeStarCount = (allStars.size * config.particleDensityMultiplier)
        .roundToInt()
        .coerceIn(24, allStars.size)

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
    val minDim = min(w, h)

    // 1. Twinkling Starfield in Deep Navy Sky
    stars.forEach { star ->
        val wave = (sin(timeSeconds * star.twinkleSpeed + star.phaseOffset) + 1f) * 0.5f
        val alpha = (star.baseAlpha * (0.35f + 0.65f * wave)).coerceIn(0.12f, 1.0f)
        val pos = Offset(star.xFraction * w, star.yFraction * h)
        val r = star.radiusDp.dp.toPx()

        val starColor = when (star.tintIndex) {
            0 -> Color(0xFFF8FAFC)
            1 -> Color(0xFFBAE6FD)
            else -> Color(0xFFE2E8F0)
        }

        drawCircle(
            color = starColor.copy(alpha = alpha),
            radius = r,
            center = pos
        )

        if (highQuality && star.radiusDp > 2.0f) {
            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = alpha * 0.22f),
                radius = r * 2.5f,
                center = pos
            )
        }
    }

    // 2. Shooting star trail across upper atmosphere (every ~12 seconds in High Quality)
    if (highQuality) {
        val cyclePos = (timeSeconds % 12f)
        if (cyclePos in 2.0f..2.85f) {
            val p = (cyclePos - 2.0f) / 0.85f
            val headX = w * (0.76f - p * 0.42f)
            val headY = h * (0.08f + p * 0.15f)
            val tailX = headX + 44.dp.toPx()
            val tailY = headY - 16.dp.toPx()
            val meteorAlpha = sin(p * PI.toFloat()).coerceIn(0f, 1f) * 0.75f

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = meteorAlpha),
                        Color(0xFF93C5FD).copy(alpha = meteorAlpha * 0.45f),
                        Color.Transparent
                    ),
                    start = Offset(headX, headY),
                    end = Offset(tailX, tailY)
                ),
                start = Offset(headX, headY),
                end = Offset(tailX, tailY),
                strokeWidth = 1.8.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }

    // 3. Realistic Detailed Moon & Multi-Layer Ethereal Lunar Glow
    // Positioned neatly in the upper sky (w * 0.82, h * 0.10) to never overlap hero text
    val pulse = 0.97f + 0.03f * sin(timeSeconds * 0.8f)
    val driftX = sin(timeSeconds * 0.25f) * 3.dp.toPx()
    val moonCenter = Offset(w * 0.82f + driftX, h * 0.098f)

    // Balanced realistic moon size (~32dp - 56dp depending on screen size, not oversized)
    val moonRadius = (minDim * 0.085f * pulse).coerceIn(30.dp.toPx(), 54.dp.toPx())

    // A. Outer Ethereal Atmosphere Glow (Soft cyan-silver radiance)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x38BAE6FD),
                Color(0x2038BDF8),
                Color(0x0C1E3A8A),
                Color.Transparent
            ),
            center = moonCenter,
            radius = moonRadius * 3.4f
        ),
        radius = moonRadius * 3.4f,
        center = moonCenter
    )

    // B. Inner Lunar Corona Glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x66F8FAFC),
                Color(0x33CBD5E1),
                Color.Transparent
            ),
            center = moonCenter,
            radius = moonRadius * 1.55f
        ),
        radius = moonRadius * 1.55f,
        center = moonCenter
    )

    // C. Lunar Disc Base (Clean white, pearl and silver-gray gradient)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFF), // Brilliant sub-solar point
                Color(0xFFF1F5F9), // Pearl highlands
                Color(0xFFE2E8F0), // Silver surface
                Color(0xFFCBD5E1)  // Cool limb
            ),
            center = Offset(moonCenter.x - moonRadius * 0.15f, moonCenter.y - moonRadius * 0.15f),
            radius = moonRadius * 1.15f
        ),
        radius = moonRadius,
        center = moonCenter
    )

    // D. Realistic Lunar Maria (Dark basaltic plains: Sea of Tranquility, Ocean of Storms, Sea of Serenity)
    // 1. Oceanus Procellarum / Mare Imbrium (Upper-left western mare)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x5294A3B8),
                Color(0x3864748B),
                Color.Transparent
            ),
            center = Offset(moonCenter.x - moonRadius * 0.36f, moonCenter.y - moonRadius * 0.28f),
            radius = moonRadius * 0.42f
        ),
        radius = moonRadius * 0.42f,
        center = Offset(moonCenter.x - moonRadius * 0.36f, moonCenter.y - moonRadius * 0.28f)
    )

    // 2. Mare Serenitatis & Mare Tranquillitatis (Upper-right central mare)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x4D64748B),
                Color(0x2B475569),
                Color.Transparent
            ),
            center = Offset(moonCenter.x + moonRadius * 0.18f, moonCenter.y - moonRadius * 0.16f),
            radius = moonRadius * 0.34f
        ),
        radius = moonRadius * 0.34f,
        center = Offset(moonCenter.x + moonRadius * 0.18f, moonCenter.y - moonRadius * 0.16f)
    )

    // 3. Mare Crisium (Distinct circular dark basin near eastern limb)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x59475569),
                Color(0x22334155),
                Color.Transparent
            ),
            center = Offset(moonCenter.x + moonRadius * 0.54f, moonCenter.y - moonRadius * 0.08f),
            radius = moonRadius * 0.16f
        ),
        radius = moonRadius * 0.16f,
        center = Offset(moonCenter.x + moonRadius * 0.54f, moonCenter.y - moonRadius * 0.08f)
    )

    // 4. Mare Fecunditatis & Mare Nectaris (Lower-right south-eastern mare)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x4464748B),
                Color(0x1F475569),
                Color.Transparent
            ),
            center = Offset(moonCenter.x + moonRadius * 0.24f, moonCenter.y + moonRadius * 0.28f),
            radius = moonRadius * 0.28f
        ),
        radius = moonRadius * 0.28f,
        center = Offset(moonCenter.x + moonRadius * 0.24f, moonCenter.y + moonRadius * 0.28f)
    )

    // 5. Mare Nubium & Mare Humorum (Lower-left southern mare)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x4064748B),
                Color(0x1A334155),
                Color.Transparent
            ),
            center = Offset(moonCenter.x - moonRadius * 0.22f, moonCenter.y + moonRadius * 0.34f),
            radius = moonRadius * 0.25f
        ),
        radius = moonRadius * 0.25f,
        center = Offset(moonCenter.x - moonRadius * 0.22f, moonCenter.y + moonRadius * 0.34f)
    )

    // E. Prominent Impact Craters with Bright Ejecta Rays (Tycho & Copernicus)
    // Tycho Crater in southern highlands (high albedo bright center with ray system)
    val tychoCenter = Offset(moonCenter.x - moonRadius * 0.08f, moonCenter.y + moonRadius * 0.62f)
    val tychoRadius = moonRadius * 0.07f

    // Tycho bright ejecta rays
    val rayAngles = listOf(20f, 65f, 115f, 160f, 210f, 275f, 330f)
    rayAngles.forEach { deg ->
        val rad = deg * (PI.toFloat() / 180f)
        val rayLen = moonRadius * 0.45f
        drawLine(
            color = Color(0x38FFFFFF),
            start = tychoCenter,
            end = Offset(
                tychoCenter.x + kotlin.math.cos(rad) * rayLen,
                tychoCenter.y + sin(rad) * rayLen
            ),
            strokeWidth = 1.2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // Tycho crater rim and central peak
    drawCircle(
        color = Color(0x55334155),
        radius = tychoRadius,
        center = tychoCenter
    )
    drawCircle(
        color = Color(0xFFFFFFFF),
        radius = tychoRadius * 0.5f,
        center = tychoCenter
    )

    // Copernicus Crater (prominent crater in Mare Imbrium region)
    val copernicusCenter = Offset(moonCenter.x - moonRadius * 0.28f, moonCenter.y + moonRadius * 0.04f)
    drawCircle(
        color = Color(0x44334155),
        radius = moonRadius * 0.085f,
        center = copernicusCenter
    )
    drawCircle(
        color = Color(0xE6FFFFFF),
        radius = moonRadius * 0.04f,
        center = copernicusCenter
    )

    // Kepler Crater
    val keplerCenter = Offset(moonCenter.x - moonRadius * 0.52f, moonCenter.y + moonRadius * 0.06f)
    drawCircle(
        color = Color(0xD9FFFFFF),
        radius = moonRadius * 0.035f,
        center = keplerCenter
    )

    // F. Delicate spherical limb shading (Moon Terminator & 3D curvature)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color(0x050F172A),
                Color(0x28020617)
            ),
            center = Offset(moonCenter.x - moonRadius * 0.20f, moonCenter.y - moonRadius * 0.20f),
            radius = moonRadius * 1.02f
        ),
        radius = moonRadius,
        center = moonCenter
    )

    // Fine atmospheric limb rim (clean crisp lunar edge)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color(0x1EBAE6FD),
                Color(0x66FFFFFF)
            ),
            center = moonCenter,
            radius = moonRadius
        ),
        radius = moonRadius,
        center = moonCenter
    )

    // If standard night sky (not full moonlight mode), render subtle crescent earthshine phase
    if (!isClearMoonlight) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xD9060B1E),
                    Color(0xB3080E24),
                    Color.Transparent
                ),
                center = Offset(moonCenter.x - moonRadius * 0.38f, moonCenter.y - moonRadius * 0.24f),
                radius = moonRadius * 0.90f
            ),
            radius = moonRadius * 0.88f,
            center = Offset(moonCenter.x - moonRadius * 0.38f, moonCenter.y - moonRadius * 0.24f)
        )
    }
}
