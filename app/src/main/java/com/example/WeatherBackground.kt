package com.example

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Dynamic Animated Background container for Phase 4.
 * Supports all 10 realistic weather scenes with smooth color transitions and
 * lifecycle-aware particle/cloud/sun/moon/lightning/fog Canvas layers.
 */
@Composable
fun WeatherAnimatedBackground(
    sceneParams: WeatherSceneParameters,
    isDarkTheme: Boolean,
    animationConfig: WeatherAnimationConfig,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val targetColors = getGradientStopsForScene(
        scene = sceneParams.scene,
        isGoldenHour = sceneParams.isGoldenHour,
        isDark = isDarkTheme
    )

    val topColor by animateColorAsState(
        targetValue = targetColors[0],
        animationSpec = tween(durationMillis = 850),
        label = "bg_top"
    )
    val midColor by animateColorAsState(
        targetValue = targetColors[1],
        animationSpec = tween(durationMillis = 850),
        label = "bg_mid"
    )
    val bottomColor by animateColorAsState(
        targetValue = targetColors[2],
        animationSpec = tween(durationMillis = 850),
        label = "bg_bottom"
    )
    val ambientGlowColor by animateColorAsState(
        targetValue = targetColors[3],
        animationSpec = tween(durationMillis = 850),
        label = "bg_glow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(topColor, midColor, bottomColor)
                )
            )
            .drawBehind {
                // Ambient glassmorphism depth spheres
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ambientGlowColor.copy(alpha = if (isDarkTheme) 0.26f else 0.20f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.16f, size.height * 0.24f),
                        radius = size.width * 0.65f
                    ),
                    radius = size.width * 0.65f,
                    center = Offset(size.width * 0.16f, size.height * 0.24f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7C3AED).copy(alpha = if (isDarkTheme) 0.18f else 0.13f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.72f),
                        radius = size.width * 0.70f
                    ),
                    radius = size.width * 0.70f,
                    center = Offset(size.width * 0.88f, size.height * 0.72f)
                )
            }
    ) {
        WeatherAnimationEngineLayer(
            sceneParams = sceneParams,
            isDarkTheme = isDarkTheme,
            config = animationConfig,
            modifier = Modifier.fillMaxSize()
        )
        content()
    }
}

private fun getGradientStopsForScene(
    scene: RealisticWeatherScene,
    isGoldenHour: Boolean,
    isDark: Boolean
): List<Color> {
    if (isGoldenHour && (scene == RealisticWeatherScene.SUNNY || scene == RealisticWeatherScene.PARTLY_CLOUDY)) {
        return if (isDark) {
            listOf(
                Color(0xFF1E1B4B), // Dusk indigo zenith
                Color(0xFF3B1D5A), // Sunset violet-amber transition
                Color(0xFF0F172A), // Deep horizon slate
                Color(0xFFF97316)  // Warm golden-hour glow
            )
        } else {
            listOf(
                Color(0xFF93C5FD),
                Color(0xFFFED7AA),
                Color(0xFFFFF7ED),
                Color(0xFFFB923C)
            )
        }
    }

    return if (isDark) {
        when (scene) {
            RealisticWeatherScene.SUNNY -> listOf(
                Color(0xFF0B1938), // Deep sapphire sky
                Color(0xFF1D2B64), // Royal indigo-blue
                Color(0xFF090D1A), // Obsidian base
                Color(0xFF38BDF8)  // Cyan solar glow
            )
            RealisticWeatherScene.PARTLY_CLOUDY -> listOf(
                Color(0xFF0F172A), // Celestial slate blue
                Color(0xFF1E1B4B), // Deep cosmic indigo-purple
                Color(0xFF070A14), // Midnight black-blue
                Color(0xFF60A5FA)  // Soft sky glow
            )
            RealisticWeatherScene.CLOUDY -> listOf(
                Color(0xFF111827), // Overcast charcoal-slate
                Color(0xFF1E293B), // Layered steel blue
                Color(0xFF090D16), // Dark slate base
                Color(0xFF94A3B8)  // Silver cloud glow
            )
            RealisticWeatherScene.RAINY -> listOf(
                Color(0xFF091326), // Rain navy
                Color(0xFF162447), // Cobalt slate
                Color(0xFF050811), // Deep abyssal base
                Color(0xFF0EA5E9)  // Cyan raindrop glow
            )
            RealisticWeatherScene.HEAVY_RAIN -> listOf(
                Color(0xFF060C1A), // Deep downpour navy
                Color(0xFF0F1E36), // Dark tempest blue
                Color(0xFF04060C), // Pitch storm base
                Color(0xFF0284C7)  // Intense azure glow
            )
            RealisticWeatherScene.THUNDERSTORM -> listOf(
                Color(0xFF0D0B1E), // Electric dark violet
                Color(0xFF241748), // Thunderhead purple-indigo
                Color(0xFF06070E), // Pitch storm base
                Color(0xFFC084FC)  // Lightning violet glow
            )
            RealisticWeatherScene.SNOW -> listOf(
                Color(0xFF0D1B2A), // Glacial midnight blue
                Color(0xFF1B3A5C), // Frosted steel blue
                Color(0xFF08101D), // Deep winter base
                Color(0xFF7DD3FC)  // Ice crystal cyan
            )
            RealisticWeatherScene.FOG -> listOf(
                Color(0xFF111827), // Misty slate top
                Color(0xFF273549), // Diffused fog blue-gray
                Color(0xFF0B111E), // Deep mist base
                Color(0xFFCBD5E1)  // Soft vapor glow
            )
            RealisticWeatherScene.NIGHT_SKY -> listOf(
                Color(0xFF050711), // Pure cosmos black-blue
                Color(0xFF1B1440), // Nebula deep purple
                Color(0xFF04060D), // Midnight base
                Color(0xFFA855F7)  // Amethyst starlight glow
            )
            RealisticWeatherScene.CLEAR_MOONLIGHT -> listOf(
                Color(0xFF061026), // Lunar sapphire night
                Color(0xFF16264C), // Moonlight indigo
                Color(0xFF050814), // Deep nocturnal horizon
                Color(0xFF7DD3FC)  // Luminous lunar cyan
            )
        }
    } else {
        when (scene) {
            RealisticWeatherScene.SUNNY -> listOf(
                Color(0xFF7DD3FC),
                Color(0xFFBAE6FD),
                Color(0xFFE0F2FE),
                Color(0xFFFDE047)
            )
            RealisticWeatherScene.PARTLY_CLOUDY -> listOf(
                Color(0xFF93C5FD),
                Color(0xFFC7D2FE),
                Color(0xFFEFF6FF),
                Color(0xFF60A5FA)
            )
            RealisticWeatherScene.CLOUDY -> listOf(
                Color(0xFF94A3B8),
                Color(0xFFCBD5E1),
                Color(0xFFF1F5F9),
                Color(0xFF64748B)
            )
            RealisticWeatherScene.RAINY -> listOf(
                Color(0xFF94A3B8),
                Color(0xFFBAE6FD),
                Color(0xFFE2E8F0),
                Color(0xFF38BDF8)
            )
            RealisticWeatherScene.HEAVY_RAIN -> listOf(
                Color(0xFF64748B),
                Color(0xFF94A3B8),
                Color(0xFFCBD5E1),
                Color(0xFF0284C7)
            )
            RealisticWeatherScene.THUNDERSTORM -> listOf(
                Color(0xFFA78BFA),
                Color(0xFFC4B5FD),
                Color(0xFFEDE9FE),
                Color(0xFF7C3AED)
            )
            RealisticWeatherScene.SNOW -> listOf(
                Color(0xFFBAE6FD),
                Color(0xFFE0F2FE),
                Color(0xFFF8FAFC),
                Color(0xFF0284C7)
            )
            RealisticWeatherScene.FOG -> listOf(
                Color(0xFFCBD5E1),
                Color(0xFFE2E8F0),
                Color(0xFFF8FAFC),
                Color(0xFF94A3B8)
            )
            RealisticWeatherScene.NIGHT_SKY -> listOf(
                Color(0xFFA5B4FC),
                Color(0xFFDDD6FE),
                Color(0xFFF3E8FF),
                Color(0xFF818CF8)
            )
            RealisticWeatherScene.CLEAR_MOONLIGHT -> listOf(
                Color(0xFF93C5FD),
                Color(0xFFDBEAFE),
                Color(0xFFF0F9FF),
                Color(0xFF38BDF8)
            )
        }
    }
}
