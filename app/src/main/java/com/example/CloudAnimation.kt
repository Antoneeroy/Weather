package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.roundToInt

private data class CloudLayerSpec(
    val id: Int,
    val initialXFraction: Float,
    val yFraction: Float,
    val widthFraction: Float,
    val speedPerSecond: Float,
    val opacityScale: Float
)

/**
 * Multi-layered Volumetric Cloud Animation on Jetpack Compose Canvas.
 * Supports light fair-weather clouds, dense overcast canopies, dark storm clouds, and moonlit night clouds.
 */
@Composable
fun RealisticCloudCanvas(
    animationTimeSeconds: Float,
    cloudDensity: Float, // 0.25f = few clouds, 0.6f = partly cloudy, 1.0f = overcast/storm
    isStormClouds: Boolean,
    isNightClouds: Boolean,
    isDarkTheme: Boolean,
    config: WeatherAnimationConfig,
    modifier: Modifier = Modifier
) {
    val cloudLayers = remember {
        listOf(
            CloudLayerSpec(0, 0.12f, 0.08f, 0.64f, 0.014f, 1.0f),
            CloudLayerSpec(1, 0.68f, 0.14f, 0.54f, -0.011f, 0.86f),
            CloudLayerSpec(2, 0.35f, 0.21f, 0.72f, 0.009f, 0.75f),
            CloudLayerSpec(3, 0.82f, 0.28f, 0.58f, -0.008f, 0.64f),
            CloudLayerSpec(4, 0.18f, 0.35f, 0.66f, 0.006f, 0.52f),
            CloudLayerSpec(5, 0.55f, 0.05f, 0.78f, 0.012f, 0.80f)
        )
    }

    val activeCloudCount = (cloudLayers.size * cloudDensity.coerceIn(0.3f, 1f) * config.particleDensityMultiplier)
        .roundToInt()
        .coerceIn(2, cloudLayers.size)

    Canvas(modifier = modifier.fillMaxSize()) {
        drawCloudLayers(
            w = size.width,
            h = size.height,
            timeSeconds = animationTimeSeconds,
            layers = cloudLayers.take(activeCloudCount),
            cloudDensity = cloudDensity,
            isStormClouds = isStormClouds,
            isNightClouds = isNightClouds,
            isDarkTheme = isDarkTheme
        )
    }
}

private fun DrawScope.drawCloudLayers(
    w: Float,
    h: Float,
    timeSeconds: Float,
    layers: List<CloudLayerSpec>,
    cloudDensity: Float,
    isStormClouds: Boolean,
    isNightClouds: Boolean,
    isDarkTheme: Boolean
) {
    val baseOpacity = when {
        isStormClouds -> 0.30f
        isNightClouds -> 0.14f
        isDarkTheme -> 0.20f
        else -> 0.34f
    } * (0.65f + cloudDensity * 0.35f)

    val topTint = when {
        isStormClouds -> Color(0xFF64748B)
        isNightClouds -> Color(0xFF94A3B8)
        else -> Color(0xFFF8FAFC)
    }

    val bottomTint = when {
        isStormClouds -> Color(0xFF1E293B)
        isNightClouds -> Color(0xFF334155)
        else -> Color(0xFFBAE6FD)
    }

    layers.forEach { spec ->
        val rawTravel = (spec.initialXFraction + timeSeconds * spec.speedPerSecond) % 1.5f
        val normalizedX = if (rawTravel < -0.25f) rawTravel + 1.5f else rawTravel
        val centerX = (normalizedX - 0.15f) * w
        val centerY = h * spec.yFraction
        val cloudW = w * spec.widthFraction
        val cloudH = cloudW * 0.34f
        val layerAlpha = (baseOpacity * spec.opacityScale).coerceIn(0.05f, 0.48f)

        // Base soft pill cloud body
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    topTint.copy(alpha = layerAlpha),
                    bottomTint.copy(alpha = layerAlpha * 0.45f)
                ),
                startY = centerY - cloudH * 0.5f,
                endY = centerY + cloudH * 0.5f
            ),
            topLeft = Offset(centerX - cloudW * 0.5f, centerY),
            size = Size(cloudW, cloudH * 0.58f),
            cornerRadius = CornerRadius(cloudH * 0.32f, cloudH * 0.32f)
        )

        // Left cumulus dome
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    topTint.copy(alpha = layerAlpha * 0.95f),
                    bottomTint.copy(alpha = layerAlpha * 0.15f)
                ),
                center = Offset(centerX - cloudW * 0.16f, centerY + cloudH * 0.04f),
                radius = cloudH * 0.48f
            ),
            radius = cloudH * 0.48f,
            center = Offset(centerX - cloudW * 0.16f, centerY + cloudH * 0.04f)
        )

        // Central-right cumulus dome
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    topTint.copy(alpha = layerAlpha * 0.85f),
                    bottomTint.copy(alpha = layerAlpha * 0.12f)
                ),
                center = Offset(centerX + cloudW * 0.14f, centerY + cloudH * 0.10f),
                radius = cloudH * 0.40f
            ),
            radius = cloudH * 0.40f,
            center = Offset(centerX + cloudW * 0.14f, centerY + cloudH * 0.10f)
        )
    }
}
