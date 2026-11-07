package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Realistic Thunderstorm Lightning Animation on Jetpack Compose Canvas.
 * Features natural non-aggressive lightning timing, soft cloud-top illumination,
 * smooth flash-and-fade envelope, and branched electric bolts.
 */
@Composable
fun RealisticLightningCanvas(
    animationTimeSeconds: Float,
    config: WeatherAnimationConfig,
    modifier: Modifier = Modifier
) {
    if (!config.animationsEnabled) return

    Canvas(modifier = modifier.fillMaxSize()) {
        drawThunderstormLightning(
            w = size.width,
            h = size.height,
            timeSeconds = animationTimeSeconds,
            highQuality = config.qualityLevel == AnimationQualityLevel.HIGH && !config.batterySaverMode
        )
    }
}

private fun DrawScope.drawThunderstormLightning(
    w: Float,
    h: Float,
    timeSeconds: Float,
    highQuality: Boolean
) {
    // 7.5-second natural cycle with two irregular, non-aggressive flash windows
    val cycle = timeSeconds % 7.5f

    val (flashAlpha, xAnchorFraction) = when {
        // Primary strike + gentle afterglow fade
        cycle in 1.10f..1.25f -> Pair(0.18f, 0.66f)
        cycle in 1.25f..1.65f -> {
            val fade = 1f - ((cycle - 1.25f) / 0.40f)
            Pair(0.24f * fade.coerceIn(0f, 1f), 0.66f)
        }
        // Secondary distant sheet flash ~3.8s later
        cycle in 4.80f..5.25f -> {
            val env = 1f - kotlin.math.abs((cycle - 5.02f) / 0.23f)
            Pair(0.14f * env.coerceIn(0f, 1f), 0.32f)
        }
        else -> Pair(0f, 0.5f)
    }

    if (flashAlpha <= 0.01f) return

    // Soft upper-atmosphere sheet lightning illumination (kept subtle so glass cards stay readable)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE0F2FE).copy(alpha = flashAlpha),
                Color(0xFFA855F7).copy(alpha = flashAlpha * 0.55f),
                Color.Transparent
            )
        ),
        size = Size(w, h * 0.62f)
    )

    // Branched lightning bolt during primary strike peak
    if (cycle in 1.12f..1.52f) {
        val boltAlpha = (flashAlpha * 2.8f).coerceIn(0f, 0.78f)
        val anchorX = w * xAnchorFraction

        val mainBolt = Path().apply {
            moveTo(anchorX, h * 0.06f)
            lineTo(anchorX - w * 0.05f, h * 0.14f)
            lineTo(anchorX + w * 0.02f, h * 0.16f)
            lineTo(anchorX - w * 0.06f, h * 0.26f)
            lineTo(anchorX - w * 0.02f, h * 0.28f)
            lineTo(anchorX - w * 0.09f, h * 0.39f)
        }

        // Outer electric violet-blue glow
        drawPath(
            path = mainBolt,
            color = Color(0xFF38BDF8).copy(alpha = boltAlpha * 0.5f),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
        )

        // Inner bright bolt core
        drawPath(
            path = mainBolt,
            color = Color(0xFFFEF08A).copy(alpha = boltAlpha),
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        )

        if (highQuality) {
            val branchBolt = Path().apply {
                moveTo(anchorX - w * 0.05f, h * 0.14f)
                lineTo(anchorX - w * 0.13f, h * 0.21f)
                lineTo(anchorX - w * 0.16f, h * 0.27f)
            }
            drawPath(
                path = branchBolt,
                color = Color(0xFFBAE6FD).copy(alpha = boltAlpha * 0.7f),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}
