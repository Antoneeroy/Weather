package com.example

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * All 10 Realistic Weather Background Scenes supported by the Phase 4 Animation Engine.
 */
enum class RealisticWeatherScene(
    val displayName: String,
    val baseCondition: WeatherConditionType
) {
    SUNNY("Sunny", WeatherConditionType.SUNNY),
    PARTLY_CLOUDY("Partly Cloudy", WeatherConditionType.PARTLY_CLOUDY),
    CLOUDY("Cloudy", WeatherConditionType.PARTLY_CLOUDY),
    RAINY("Light Rain", WeatherConditionType.RAINY),
    HEAVY_RAIN("Heavy Rain", WeatherConditionType.RAINY),
    THUNDERSTORM("Thunderstorm", WeatherConditionType.THUNDERSTORM),
    SNOW("Snow", WeatherConditionType.SNOWY),
    FOG("Fog & Mist", WeatherConditionType.PARTLY_CLOUDY),
    NIGHT_SKY("Night Sky", WeatherConditionType.CLEAR_NIGHT),
    CLEAR_MOONLIGHT("Clear Moonlight", WeatherConditionType.CLEAR_NIGHT)
}

/**
 * Computed atmospheric animation parameters derived from real Open-Meteo WMO code & local time.
 */
data class WeatherSceneParameters(
    val scene: RealisticWeatherScene,
    val rainIntensity: Float = 0f,
    val cloudDensity: Float = 0.25f,
    val snowIntensity: Float = 0f,
    val fogDensity: Float = 0f,
    val isGoldenHour: Boolean = false
)

/**
 * Central Realistic Weather Animation Engine (Phase 4).
 * Connects Open-Meteo WMO weather codes, day/night state, and sunrise/sunset golden hours
 * to lifecycle-aware Compose Canvas animations.
 */
object WeatherAnimationEngine {

    /**
     * Resolves the exact [WeatherSceneParameters] from real Open-Meteo weather data or user scene preview.
     */
    fun resolveSceneParameters(
        city: CityWeatherData,
        conditionOverride: WeatherConditionType? = null,
        sceneOverride: RealisticWeatherScene? = null
    ): WeatherSceneParameters {
        val isGoldenHour = detectGoldenHour(city.currentTimeDisplay, city.details.sunriseTime, city.details.sunsetTime)

        if (sceneOverride != null) {
            return parametersForScene(sceneOverride, isGoldenHour)
        }

        if (conditionOverride != null) {
            val mappedScene = when (conditionOverride) {
                WeatherConditionType.SUNNY -> RealisticWeatherScene.SUNNY
                WeatherConditionType.PARTLY_CLOUDY -> RealisticWeatherScene.PARTLY_CLOUDY
                WeatherConditionType.RAINY -> RealisticWeatherScene.RAINY
                WeatherConditionType.CLEAR_NIGHT -> RealisticWeatherScene.NIGHT_SKY
                WeatherConditionType.THUNDERSTORM -> RealisticWeatherScene.THUNDERSTORM
                WeatherConditionType.SNOWY -> RealisticWeatherScene.SNOW
            }
            return parametersForScene(mappedScene, isGoldenHour)
        }

        val wmo = city.wmoCode
        val isNight = city.condition == WeatherConditionType.CLEAR_NIGHT ||
            isNightTimeDisplay(city.currentTimeDisplay, city.details.sunriseTime, city.details.sunsetTime)

        val scene = when (wmo) {
            0 -> if (isNight) RealisticWeatherScene.CLEAR_MOONLIGHT else RealisticWeatherScene.SUNNY
            1 -> if (isNight) RealisticWeatherScene.NIGHT_SKY else RealisticWeatherScene.SUNNY
            2 -> if (isNight) RealisticWeatherScene.NIGHT_SKY else RealisticWeatherScene.PARTLY_CLOUDY
            3 -> RealisticWeatherScene.CLOUDY
            45, 48 -> RealisticWeatherScene.FOG
            51, 53, 56, 61, 80 -> RealisticWeatherScene.RAINY
            55, 57, 63, 65, 66, 67, 81, 82 -> RealisticWeatherScene.HEAVY_RAIN
            71, 73, 75, 77, 85, 86 -> RealisticWeatherScene.SNOW
            95, 96, 99 -> RealisticWeatherScene.THUNDERSTORM
            else -> when (city.condition) {
                WeatherConditionType.SUNNY -> RealisticWeatherScene.SUNNY
                WeatherConditionType.PARTLY_CLOUDY -> RealisticWeatherScene.PARTLY_CLOUDY
                WeatherConditionType.RAINY -> RealisticWeatherScene.RAINY
                WeatherConditionType.CLEAR_NIGHT -> RealisticWeatherScene.NIGHT_SKY
                WeatherConditionType.THUNDERSTORM -> RealisticWeatherScene.THUNDERSTORM
                WeatherConditionType.SNOWY -> RealisticWeatherScene.SNOW
            }
        }

        val rainIntensity = when (wmo) {
            51, 56 -> 0.35f
            53, 61, 80 -> 0.58f
            55, 63, 66, 81 -> 0.82f
            65, 67, 82, 95, 96, 99 -> 1.0f
            else -> if (scene == RealisticWeatherScene.RAINY) 0.58f else if (scene == RealisticWeatherScene.HEAVY_RAIN) 0.95f else 0f
        }

        val snowIntensity = when (wmo) {
            71, 85 -> 0.48f
            73, 77 -> 0.76f
            75, 86 -> 1.0f
            else -> if (scene == RealisticWeatherScene.SNOW) 0.75f else 0f
        }

        val cloudDensity = when (scene) {
            RealisticWeatherScene.SUNNY -> 0.25f
            RealisticWeatherScene.CLEAR_MOONLIGHT -> 0.20f
            RealisticWeatherScene.NIGHT_SKY -> 0.40f
            RealisticWeatherScene.PARTLY_CLOUDY -> 0.62f
            RealisticWeatherScene.CLOUDY -> 0.95f
            RealisticWeatherScene.FOG -> 0.55f
            RealisticWeatherScene.RAINY -> 0.80f
            RealisticWeatherScene.HEAVY_RAIN -> 1.0f
            RealisticWeatherScene.THUNDERSTORM -> 1.0f
            RealisticWeatherScene.SNOW -> 0.65f
        }

        val fogDensity = when (wmo) {
            45 -> 0.72f
            48 -> 0.95f
            else -> if (scene == RealisticWeatherScene.FOG) 0.80f else 0f
        }

        return WeatherSceneParameters(
            scene = scene,
            rainIntensity = rainIntensity,
            cloudDensity = cloudDensity,
            snowIntensity = snowIntensity,
            fogDensity = fogDensity,
            isGoldenHour = isGoldenHour
        )
    }

    fun parametersForScene(
        scene: RealisticWeatherScene,
        isGoldenHour: Boolean = false
    ): WeatherSceneParameters {
        return when (scene) {
            RealisticWeatherScene.SUNNY -> WeatherSceneParameters(
                scene = scene,
                cloudDensity = 0.25f,
                isGoldenHour = isGoldenHour
            )
            RealisticWeatherScene.PARTLY_CLOUDY -> WeatherSceneParameters(
                scene = scene,
                cloudDensity = 0.65f,
                isGoldenHour = isGoldenHour
            )
            RealisticWeatherScene.CLOUDY -> WeatherSceneParameters(
                scene = scene,
                cloudDensity = 1.0f
            )
            RealisticWeatherScene.RAINY -> WeatherSceneParameters(
                scene = scene,
                rainIntensity = 0.55f,
                cloudDensity = 0.80f
            )
            RealisticWeatherScene.HEAVY_RAIN -> WeatherSceneParameters(
                scene = scene,
                rainIntensity = 1.0f,
                cloudDensity = 1.0f
            )
            RealisticWeatherScene.THUNDERSTORM -> WeatherSceneParameters(
                scene = scene,
                rainIntensity = 0.95f,
                cloudDensity = 1.0f
            )
            RealisticWeatherScene.SNOW -> WeatherSceneParameters(
                scene = scene,
                snowIntensity = 0.85f,
                cloudDensity = 0.65f
            )
            RealisticWeatherScene.FOG -> WeatherSceneParameters(
                scene = scene,
                fogDensity = 0.85f,
                cloudDensity = 0.55f
            )
            RealisticWeatherScene.NIGHT_SKY -> WeatherSceneParameters(
                scene = scene,
                cloudDensity = 0.45f
            )
            RealisticWeatherScene.CLEAR_MOONLIGHT -> WeatherSceneParameters(
                scene = scene,
                cloudDensity = 0.20f
            )
        }
    }

    private fun detectGoldenHour(currentTime: String, sunrise: String, sunset: String): Boolean {
        val currentMins = parseAmPmMinutes(currentTime) ?: return false
        val riseMins = parseAmPmMinutes(sunrise) ?: 390
        val setMins = parseAmPmMinutes(sunset) ?: 1050
        return kotlin.math.abs(currentMins - riseMins) <= 50 ||
            kotlin.math.abs(currentMins - setMins) <= 50
    }

    private fun isNightTimeDisplay(currentTime: String, sunrise: String, sunset: String): Boolean {
        val currentMins = parseAmPmMinutes(currentTime) ?: return false
        val riseMins = parseAmPmMinutes(sunrise) ?: 390
        val setMins = parseAmPmMinutes(sunset) ?: 1050
        return currentMins < riseMins || currentMins > setMins
    }

    private fun parseAmPmMinutes(timeStr: String): Int? {
        val clean = timeStr.trim().uppercase()
        if (!clean.contains("AM") && !clean.contains("PM")) return null
        return try {
            val isPm = clean.contains("PM")
            val hm = clean.replace("AM", "").replace("PM", "").trim().split(":")
            var h = hm[0].trim().toInt()
            val m = hm.getOrNull(1)?.trim()?.take(2)?.toIntOrNull() ?: 0
            if (isPm && h < 12) h += 12
            if (!isPm && h == 12) h = 0
            h * 60 + m
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Lifecycle-aware animation time state in seconds.
 * Automatically pauses frame updates when the Android Activity enters background (ON_STOP)
 * or when animations are disabled in Settings, preventing battery drain and CPU usage.
 */
@Composable
fun rememberLifecycleAwareAnimationTime(config: WeatherAnimationConfig): Float {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isAppInForeground by remember { mutableStateOf(true) }
    var elapsedSeconds by remember { mutableFloatStateOf(4.2f) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> isAppInForeground = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isAppInForeground = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(config.animationsEnabled, config.qualityLevel, config.batterySaverMode, isAppInForeground) {
        if (!config.animationsEnabled || !isAppInForeground) {
            return@LaunchedEffect
        }
        var lastFrameNanos = 0L
        val stepIntervalMs = config.targetFrameIntervalMs
        while (isActive && config.animationsEnabled && isAppInForeground) {
            withFrameNanos { frameNanos ->
                if (lastFrameNanos != 0L) {
                    val deltaSec = ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
                    elapsedSeconds = (elapsedSeconds + deltaSec) % 3600f
                }
                lastFrameNanos = frameNanos
            }
            if (stepIntervalMs > 16L) {
                delay(stepIntervalMs - 16L)
            }
        }
    }

    return elapsedSeconds
}

/**
 * Renders the active [WeatherSceneParameters] with smooth crossfade transitions between weather states.
 */
@Composable
fun WeatherAnimationEngineLayer(
    sceneParams: WeatherSceneParameters,
    isDarkTheme: Boolean,
    config: WeatherAnimationConfig,
    modifier: Modifier = Modifier
) {
    val animationTimeSeconds = rememberLifecycleAwareAnimationTime(config)

    Crossfade(
        targetState = sceneParams,
        animationSpec = tween(durationMillis = 750),
        modifier = modifier.fillMaxSize(),
        label = "weather_scene_engine_crossfade"
    ) { params ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (params.scene) {
                RealisticWeatherScene.SUNNY -> {
                    RealisticSunCanvas(
                        animationTimeSeconds = animationTimeSeconds,
                        isGoldenHour = params.isGoldenHour,
                        isDarkTheme = isDarkTheme,
                        config = config
                    )
                    RealisticCloudCanvas(
                        animationTimeSeconds = animationTimeSeconds,
                        cloudDensity = params.cloudDensity,
                        isStormClouds = false,
                        isNightClouds = false,
                        isDarkTheme = isDarkTheme,
                        config = config
                    )
                }

                RealisticWeatherScene.PARTLY_CLOUDY -> {
                    RealisticSunCanvas(
                        animationTimeSeconds = animationTimeSeconds,
                        isGoldenHour = params.isGoldenHour,
                        isDarkTheme = isDarkTheme,
                        config = config
                    )
                    RealisticCloudCanvas(
                        animationTimeSeconds = animationTimeSeconds,
                        cloudDensity = params.cloudDensity,
                        isStormClouds = false,
                        isNightClouds = false,
                        isDarkTheme = isDarkTheme,
                        config = config
                    )
                }

                RealisticWeatherScene.CLOUDY -> {
                    RealisticCloudCanvas(
                        animationTimeSeconds = animationTimeSeconds,
                        cloudDensity = params.cloudDensity,
                        isStormClouds = false,
                        isNightClouds = false,
                        isDarkTheme = isDarkTheme,
                        config = config
                    )
                }

                RealisticWeatherScene.RAINY -> {
                    RealisticCloudCanvas(
                        animationTimeSeconds = animationTimeSeconds,
                        cloudDensity = params.cloudDensity,
                        isStormClouds = false,
                        isNightClouds = false,
                        isDarkTheme = isDarkTheme,
                        config = config
                    )
                    RealisticRainCanvas(
                        animationTimeSeconds = animationTimeSeconds,
                        rainIntensity = params.rainIntensity,
                        isHeavyRain = false,
                        config = config
                    )
                }

                RealisticWeatherScene.HEAVY_RAIN -> {
                    RealisticCloudCanvas(
                        animationTimeSeconds = animationTimeSeconds,
                        cloudDensity = params.cloudDensity,
                        isStormClouds = true,
                        isNightClouds = false,
                        isDarkTheme = isDarkTheme,
                        config = config
                    )
                    RealisticRainCanvas(
                        animationTimeSeconds = animationTimeSeconds,
                        rainIntensity = params.rainIntensity,
                        isHeavyRain = true,
                        config = config
                    )
                }

                RealisticWeatherScene.THUNDERSTORM -> {
                    RealisticCloudCanvas(
                        animationTimeSeconds = animationTimeSeconds,
                        cloudDensity = params.cloudDensity,
                        isStormClouds = true,
                        isNightClouds = false,
                        isDarkTheme = isDarkTheme,
                        config = config
                    )
                    RealisticLightningCanvas(
                        animationTimeSeconds = animationTimeSeconds,
                        config = config
                    )
                    RealisticRainCanvas(
                        animationTimeSeconds = animationTimeSeconds,
                        rainIntensity = params.rainIntensity,
                        isHeavyRain = true,
                        config = config
                    )
                }

                RealisticWeatherScene.SNOW -> {
                    RealisticCloudCanvas(
                        animationTimeSeconds = animationTimeSeconds,
                        cloudDensity = params.cloudDensity,
                        isStormClouds = false,
                        isNightClouds = false,
                        isDarkTheme = isDarkTheme,
                        config = config
                    )
                    RealisticSnowCanvas(
                        animationTimeSeconds = animationTimeSeconds,
                        snowIntensity = params.snowIntensity,
                        config = config
                    )
                }

                RealisticWeatherScene.FOG -> {
                    RealisticCloudCanvas(
                        animationTimeSeconds = animationTimeSeconds,
                        cloudDensity = params.cloudDensity,
                        isStormClouds = false,
                        isNightClouds = false,
                        isDarkTheme = isDarkTheme,
                        config = config
                    )
                    RealisticFogCanvas(
                        animationTimeSeconds = animationTimeSeconds,
                        fogDensity = params.fogDensity,
                        isDarkTheme = isDarkTheme,
                        config = config
                    )
                }

                RealisticWeatherScene.NIGHT_SKY -> {
                    RealisticNightSkyCanvas(
                        animationTimeSeconds = animationTimeSeconds,
                        isClearMoonlight = false,
                        config = config
                    )
                    RealisticCloudCanvas(
                        animationTimeSeconds = animationTimeSeconds,
                        cloudDensity = params.cloudDensity,
                        isStormClouds = false,
                        isNightClouds = true,
                        isDarkTheme = isDarkTheme,
                        config = config
                    )
                }

                RealisticWeatherScene.CLEAR_MOONLIGHT -> {
                    RealisticNightSkyCanvas(
                        animationTimeSeconds = animationTimeSeconds,
                        isClearMoonlight = true,
                        config = config
                    )
                    RealisticCloudCanvas(
                        animationTimeSeconds = animationTimeSeconds,
                        cloudDensity = params.cloudDensity,
                        isStormClouds = false,
                        isNightClouds = true,
                        isDarkTheme = isDarkTheme,
                        config = config
                    )
                }
            }
        }
    }
}
