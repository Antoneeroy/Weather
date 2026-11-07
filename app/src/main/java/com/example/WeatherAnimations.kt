package com.example

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom High-Craft Vector/Canvas Weather Condition Icon with optional subtle motion.
 */
@Composable
fun WeatherConditionArtworkIcon(
    condition: WeatherConditionType,
    modifier: Modifier = Modifier,
    iconSize: Dp = 44.dp,
    animationsEnabled: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon_anim")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sun_spin"
    )
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloud_bob"
    )

    val effectiveAngle = if (animationsEnabled) angle else 0f
    val effectiveBob = if (animationsEnabled) floatOffset else 0f

    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val bobPx = effectiveBob.dp.toPx()

        when (condition) {
            WeatherConditionType.SUNNY -> {
                drawSunArtwork(
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.28f,
                    rotationDegrees = effectiveAngle
                )
            }

            WeatherConditionType.PARTLY_CLOUDY -> {
                drawSunArtwork(
                    center = Offset(w * 0.64f, h * 0.36f),
                    radius = w * 0.22f,
                    rotationDegrees = effectiveAngle
                )
                drawFrostedCloudArtwork(
                    centerX = w * 0.46f,
                    centerY = h * 0.60f + bobPx * 0.5f,
                    cloudWidth = w * 0.72f
                )
            }

            WeatherConditionType.RAINY -> {
                drawFrostedCloudArtwork(
                    centerX = w * 0.50f,
                    centerY = h * 0.44f + bobPx * 0.4f,
                    cloudWidth = w * 0.76f
                )
                val drops = listOf(
                    Offset(w * 0.34f, h * 0.72f),
                    Offset(w * 0.50f, h * 0.78f),
                    Offset(w * 0.66f, h * 0.72f)
                )
                drops.forEach { start ->
                    drawLine(
                        color = Color(0xFF38BDF8),
                        start = start,
                        end = Offset(start.x - w * 0.05f, start.y + h * 0.14f),
                        strokeWidth = (w * 0.065f).coerceAtLeast(3f),
                        cap = StrokeCap.Round
                    )
                }
            }

            WeatherConditionType.CLEAR_NIGHT -> {
                val moonCenter = Offset(w * 0.52f, h * 0.50f + bobPx * 0.4f)
                val r = w * 0.30f
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFEF08A), Color(0xFF38BDF8))
                    ),
                    radius = r,
                    center = moonCenter
                )
                drawCircle(
                    color = Color(0xFF0F172A),
                    radius = r * 0.82f,
                    center = Offset(moonCenter.x - r * 0.36f, moonCenter.y - r * 0.26f)
                )
                drawCircle(
                    color = Color(0xFFFDE047),
                    radius = w * 0.045f,
                    center = Offset(w * 0.24f, h * 0.26f)
                )
                drawCircle(
                    color = Color(0xFFBAE6FD),
                    radius = w * 0.035f,
                    center = Offset(w * 0.78f, h * 0.24f)
                )
            }

            WeatherConditionType.THUNDERSTORM -> {
                drawFrostedCloudArtwork(
                    centerX = w * 0.50f,
                    centerY = h * 0.42f + bobPx * 0.4f,
                    cloudWidth = w * 0.76f,
                    isStormy = true
                )
                val bolt = Path().apply {
                    moveTo(w * 0.53f, h * 0.58f)
                    lineTo(w * 0.41f, h * 0.75f)
                    lineTo(w * 0.52f, h * 0.75f)
                    lineTo(w * 0.45f, h * 0.92f)
                }
                drawPath(
                    path = bolt,
                    color = Color(0xFFFACC15),
                    style = Stroke(width = (w * 0.07f).coerceAtLeast(3f), cap = StrokeCap.Round)
                )
            }

            WeatherConditionType.SNOWY -> {
                drawFrostedCloudArtwork(
                    centerX = w * 0.50f,
                    centerY = h * 0.44f + bobPx * 0.4f,
                    cloudWidth = w * 0.74f
                )
                val flakes = listOf(
                    Offset(w * 0.34f, h * 0.76f),
                    Offset(w * 0.50f, h * 0.82f),
                    Offset(w * 0.66f, h * 0.76f)
                )
                flakes.forEach { center ->
                    drawCircle(
                        color = Color(0xFFE0F2FE),
                        radius = (w * 0.048f).coerceAtLeast(2.5f),
                        center = center
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawSunArtwork(
    center: Offset,
    radius: Float,
    rotationDegrees: Float
) {
    rotate(degrees = rotationDegrees, pivot = center) {
        for (i in 0 until 8) {
            val angle = i * 45f * (PI.toFloat() / 180f)
            val start = Offset(
                center.x + cos(angle) * (radius * 1.22f),
                center.y + sin(angle) * (radius * 1.22f)
            )
            val end = Offset(
                center.x + cos(angle) * (radius * 1.52f),
                center.y + sin(angle) * (radius * 1.52f)
            )
            drawLine(
                color = Color(0xFFFBBF24),
                start = start,
                end = end,
                strokeWidth = (radius * 0.22f).coerceAtLeast(2.5f),
                cap = StrokeCap.Round
            )
        }
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFEF08A), Color(0xFFF59E0B)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawFrostedCloudArtwork(
    centerX: Float,
    centerY: Float,
    cloudWidth: Float,
    isStormy: Boolean = false
) {
    val cloudHeight = cloudWidth * 0.48f
    val topColor = if (isStormy) Color(0xFF94A3B8) else Color(0xFFF8FAFC)
    val bottomColor = if (isStormy) Color(0xFF475569) else Color(0xFFBAE6FD)

    drawCircle(
        brush = Brush.verticalGradient(listOf(topColor, bottomColor)),
        radius = cloudHeight * 0.56f,
        center = Offset(centerX - cloudWidth * 0.12f, centerY - cloudHeight * 0.18f)
    )
    drawCircle(
        brush = Brush.verticalGradient(listOf(topColor, bottomColor)),
        radius = cloudHeight * 0.44f,
        center = Offset(centerX + cloudWidth * 0.16f, centerY - cloudHeight * 0.08f)
    )
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(topColor, bottomColor)),
        topLeft = Offset(centerX - cloudWidth * 0.46f, centerY - cloudHeight * 0.22f),
        size = Size(cloudWidth * 0.92f, cloudHeight * 0.58f),
        cornerRadius = CornerRadius(cloudHeight * 0.32f, cloudHeight * 0.32f)
    )
}

/**
 * Glassmorphic Orbital Radar Loading Animation for refresh states.
 */
@Composable
fun WeatherOrbitalLoadingIndicator(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 42.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbital_loader")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbital_rotation"
    )

    Box(modifier = modifier.size(sizeDp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension * 0.42f
            drawCircle(
                color = Color(0x3338BDF8),
                radius = r,
                style = Stroke(width = 3.dp.toPx())
            )
            rotate(rotation) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xFF38BDF8),
                            Color(0xFFA855F7)
                        )
                    ),
                    startAngle = 0f,
                    sweepAngle = 240f,
                    useCenter = false,
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}
