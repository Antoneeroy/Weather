package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    private fun createTestCity(
        id: String = "geo_test_paris",
        cityName: String = "Paris",
        regionAndCountry: String = "Île-de-France, France",
        latitude: Double = 48.8566,
        longitude: Double = 2.3522
    ): CityWeatherData {
        val tenDays = (0 until 10).map { idx ->
            val dayId = "${id}_d$idx"
            val hours = SampleWeatherRepository.generate24HoursForDay(
                dayId = dayId,
                minTempCelsius = 9 + (idx % 3),
                maxTempCelsius = 17 + (idx % 3),
                condition = WeatherConditionType.SUNNY,
                wmoCode = 1,
                rainProbability = 10,
                precipitationSumMm = 0.0,
                windKmh = 13,
                humidityPercent = 60,
                isToday = idx == 0
            )
            DailyForecastItem(
                id = dayId,
                dayName = if (idx == 0) "Today" else "Day $idx",
                dateShort = "Nov ${6 + idx}",
                fullDateFormatted = "Nov ${6 + idx}, 2026",
                condition = WeatherConditionType.SUNNY,
                wmoCode = 1,
                conditionLabel = "Mainly Clear",
                minTempCelsius = 9 + (idx % 3),
                maxTempCelsius = 17 + (idx % 3),
                rainProbability = 10,
                precipitationSumMm = 0.0,
                sunriseTime = "7:47 AM",
                sunsetTime = "5:21 PM",
                windKmh = 13,
                uvIndex = 4,
                humidityPercent = 60,
                hourlyItemsForDay = hours
            )
        }
        return CityWeatherData(
            id = id,
            cityName = cityName,
            regionAndCountry = regionAndCountry,
            latitude = latitude,
            longitude = longitude,
            timeZoneOffsetLabel = "CET",
            currentTimeDisplay = "2:00 PM",
            currentDateDisplay = "Nov 6, 2026",
            currentTempCelsius = 15,
            feelsLikeCelsius = 14,
            highTempCelsius = 17,
            lowTempCelsius = 9,
            condition = WeatherConditionType.SUNNY,
            wmoCode = 1,
            conditionDescription = "Mainly Clear",
            airQualityIndex = 26,
            airQualityLabel = "Good",
            details = AtmosphericDetails(
                humidityPercent = 60,
                dewPointCelsius = 7,
                windSpeedKmh = 13,
                windDirection = "W",
                windGustKmh = 20,
                pressureHpa = 1020,
                pressureTrend = "Steady",
                visibilityKm = 18.0,
                visibilityStatus = "Clear",
                uvIndex = 4,
                uvCategory = "Moderate",
                sunriseTime = "7:47 AM",
                sunsetTime = "5:21 PM",
                daylightDuration = "9h 34m"
            ),
            tenDayForecast = tenDays,
            hourlyForecast = tenDays.first().hourlyItemsForDay
        )
    }

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Weather", appName)
    }

    @Test
    fun `wmo weather code mapper supports all Phase 3 condition categories and gradients`() {
        assertEquals(WmoDetailedCategory.CLEAR_SKY, WmoWeatherCodeMapper.mapToDetailedCategory(0))
        assertEquals(WmoDetailedCategory.MAINLY_CLEAR, WmoWeatherCodeMapper.mapToDetailedCategory(1))
        assertEquals(WmoDetailedCategory.PARTLY_CLOUDY, WmoWeatherCodeMapper.mapToDetailedCategory(2))
        assertEquals(WmoDetailedCategory.OVERCAST, WmoWeatherCodeMapper.mapToDetailedCategory(3))
        assertEquals(WmoDetailedCategory.FOG, WmoWeatherCodeMapper.mapToDetailedCategory(45))
        assertEquals(WmoDetailedCategory.DRIZZLE, WmoWeatherCodeMapper.mapToDetailedCategory(53))
        assertEquals(WmoDetailedCategory.FREEZING_DRIZZLE, WmoWeatherCodeMapper.mapToDetailedCategory(56))
        assertEquals(WmoDetailedCategory.RAIN, WmoWeatherCodeMapper.mapToDetailedCategory(63))
        assertEquals(WmoDetailedCategory.FREEZING_RAIN, WmoWeatherCodeMapper.mapToDetailedCategory(66))
        assertEquals(WmoDetailedCategory.SNOW, WmoWeatherCodeMapper.mapToDetailedCategory(75))
        assertEquals(WmoDetailedCategory.RAIN_SHOWERS, WmoWeatherCodeMapper.mapToDetailedCategory(81))
        assertEquals(WmoDetailedCategory.SNOW_SHOWERS, WmoWeatherCodeMapper.mapToDetailedCategory(85))
        assertEquals(WmoDetailedCategory.THUNDERSTORM, WmoWeatherCodeMapper.mapToDetailedCategory(95))

        val rainGradient = WmoWeatherCodeMapper.getConditionGradientColors(WeatherConditionType.RAINY, 63)
        assertEquals(3, rainGradient.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `repository parses 10-day forecast and full-day 24-hour (12 AM to 11 PM) hourly forecast from OpenMeteo`() = runTest {
        val thirtyHoursTimes = (0 until 30).map { idx ->
            val day = 6 + (idx / 24)
            val hour = idx % 24
            String.format("2026-11-%02dT%02d:00", day, hour)
        }

        val tenDaysDates = (6..15).map { day ->
            String.format("2026-11-%02d", day)
        }

        val fakeForecastApi = object : WeatherApiService {
            override suspend fun getForecast(
                latitude: Double,
                longitude: Double,
                current: String,
                hourly: String,
                daily: String,
                timezone: String,
                forecastDays: Int
            ): OpenMeteoForecastResponse {
                return OpenMeteoForecastResponse(
                    latitude = latitude,
                    longitude = longitude,
                    timezone = "Europe/Paris",
                    timezoneAbbreviation = "CET",
                    utcOffsetSeconds = 3600,
                    current = OpenMeteoCurrentDto(
                        time = "2026-11-06T02:00",
                        temperature2m = 18.4,
                        relativeHumidity2m = 62,
                        apparentTemperature = 17.6,
                        isDay = 1,
                        weatherCode = 0,
                        surfacePressure = 1019.2,
                        pressureMsl = 1021.0,
                        windSpeed10m = 14.3,
                        windDirection10m = 270,
                        windGusts10m = 22.0,
                        visibilityMeters = 20000.0,
                        uvIndex = 4.2
                    ),
                    hourly = OpenMeteoHourlyDto(
                        time = thirtyHoursTimes,
                        temperature2m = List(30) { 15.0 + (it % 6) },
                        apparentTemperature = List(30) { 14.0 + (it % 6) },
                        relativeHumidity2m = List(30) { 65 + (it % 10) },
                        precipitationProbability = List(30) { (it * 5) % 100 },
                        precipitationMm = List(30) { if (it % 3 == 0) 1.4 else 0.0 },
                        weatherCode = List(30) { if (it % 4 == 0) 61 else 2 },
                        windSpeed10m = List(30) { 12.0 + (it % 8) },
                        windDirection10m = List(30) { (180 + it * 10) % 360 },
                        visibilityMeters = List(30) { 18000.0 },
                        uvIndex = List(30) { 3.0 },
                        isDay = List(30) { 1 }
                    ),
                    daily = OpenMeteoDailyDto(
                        time = tenDaysDates,
                        weatherCode = listOf(0, 1, 2, 3, 45, 51, 61, 66, 75, 95),
                        temperature2mMax = List(10) { 20.0 + it },
                        temperature2mMin = List(10) { 10.0 + (it % 4) },
                        precipitationProbabilityMax = List(10) { (it * 10).coerceAtMost(95) },
                        precipitationSumMm = List(10) { it * 1.5 },
                        sunrise = tenDaysDates.map { "${it}T07:45" },
                        sunset = tenDaysDates.map { "${it}T17:22" },
                        uvIndexMax = List(10) { 4.5 },
                        windSpeed10mMax = List(10) { 18.0 + it }
                    )
                )
            }
        }

        val repository = WeatherRepository(
            context = null,
            weatherApiService = fakeForecastApi
        )

        val liveWeatherResult = repository.fetchLiveCityWeather(
            id = "paris_fr",
            cityName = "Paris",
            regionAndCountry = "Île-de-France, France",
            latitude = 48.8534,
            longitude = 2.3488,
            isCurrentLocation = false
        )

        assertTrue(liveWeatherResult is WeatherApiResult.Success)
        val data = (liveWeatherResult as WeatherApiResult.Success).data
        assertTrue(data.isLiveData)
        // Verify full-day 24 hourly items (12 AM to 11 PM without missing hours)
        assertEquals(24, data.hourlyForecast.size)
        assertEquals("12 AM", data.hourlyForecast.first().clockTimeLabel)
        assertEquals(0, data.hourlyForecast.first().hour24)
        assertEquals("11 PM", data.hourlyForecast.last().clockTimeLabel)
        assertEquals(23, data.hourlyForecast.last().hour24)
        // Current time is 02:00 (2 AM), so index 2 is marked isNow
        assertTrue(data.hourlyForecast[2].isNow)
        assertEquals(16, data.hourlyForecast[2].feelsLikeCelsius)
        assertEquals("SSW", data.hourlyForecast[2].windDirectionCompass)

        // Verify 10 daily forecast rows (Today + Next 9 days)
        assertEquals(10, data.tenDayForecast.size)
        assertEquals("Today", data.tenDayForecast.first().dayName)
        assertEquals("7:45 AM", data.tenDayForecast.first().sunriseTime)
        assertEquals("5:22 PM", data.tenDayForecast.first().sunsetTime)
        assertEquals(6.0, data.tenDayForecast[4].precipitationSumMm, 0.01)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `repository handles no internet IOException gracefully`() = runTest {
        val offlineApi = object : WeatherApiService {
            override suspend fun getForecast(
                latitude: Double,
                longitude: Double,
                current: String,
                hourly: String,
                daily: String,
                timezone: String,
                forecastDays: Int
            ): OpenMeteoForecastResponse {
                throw IOException("Network unreachable")
            }
        }

        val repo = WeatherRepository(
            context = null,
            weatherApiService = offlineApi
        )

        val res = repo.fetchLiveCityWeather(
            id = "sf_us",
            cityName = "San Francisco",
            regionAndCountry = "California, US",
            latitude = 37.77,
            longitude = -122.41
        )
        assertTrue(res is WeatherApiResult.NoInternet)
    }

    @Test
    fun `viewModel opens and closes ForecastDetailScreen for selected day`() {
        val vm = WeatherViewModel(appContext = null, autoFetchOnInit = false)
        assertTrue(vm.uiState.value.cities.isEmpty())
        assertNull(vm.uiState.value.activeCity)

        val sampleCity = createTestCity()
        vm.addSavedCity(sampleCity)
        val firstDay = vm.uiState.value.activeCity!!.tenDayForecast.first()
        assertNotNull(firstDay)

        vm.selectDayForDetail(firstDay, openDetailScreen = true)
        assertTrue(vm.uiState.value.isShowingDayDetailScreen)
        assertEquals(firstDay.id, vm.uiState.value.selectedDayForDetail?.id)

        vm.closeDayDetailScreen()
        assertFalse(vm.uiState.value.isShowingDayDetailScreen)
    }

    @Test
    fun `weather animation engine resolves all 10 realistic scenes from WMO codes and day or night state`() {
        val baseCity = createTestCity()

        // 1. Sunny (daytime WMO 0)
        val sunnyParams = WeatherAnimationEngine.resolveSceneParameters(
            city = baseCity.copy(
                wmoCode = 0,
                condition = WeatherConditionType.SUNNY,
                currentTimeDisplay = "1:00 PM"
            )
        )
        assertEquals(RealisticWeatherScene.SUNNY, sunnyParams.scene)
        assertEquals(0.25f, sunnyParams.cloudDensity, 0.01f)

        // 2. Partly Cloudy (daytime WMO 2 + golden hour near sunset)
        val partlyCloudyParams = WeatherAnimationEngine.resolveSceneParameters(
            city = baseCity.copy(
                wmoCode = 2,
                condition = WeatherConditionType.PARTLY_CLOUDY,
                currentTimeDisplay = baseCity.details.sunsetTime
            )
        )
        assertEquals(RealisticWeatherScene.PARTLY_CLOUDY, partlyCloudyParams.scene)
        assertTrue(partlyCloudyParams.cloudDensity > 0.3f)
        assertTrue(partlyCloudyParams.isGoldenHour)

        // 3. Cloudy / Overcast (WMO 3)
        val cloudyParams = WeatherAnimationEngine.resolveSceneParameters(
            city = baseCity.copy(wmoCode = 3, condition = WeatherConditionType.PARTLY_CLOUDY)
        )
        assertEquals(RealisticWeatherScene.CLOUDY, cloudyParams.scene)
        assertTrue(cloudyParams.cloudDensity >= 0.85f)

        // 4. Rainy (light/moderate rain WMO 61)
        val rainyParams = WeatherAnimationEngine.resolveSceneParameters(
            city = baseCity.copy(wmoCode = 61, condition = WeatherConditionType.RAINY)
        )
        assertEquals(RealisticWeatherScene.RAINY, rainyParams.scene)
        assertTrue(rainyParams.rainIntensity in 0.35f..0.75f)

        // 5. Heavy Rain (WMO 65 / 82)
        val heavyRainParams = WeatherAnimationEngine.resolveSceneParameters(
            city = baseCity.copy(wmoCode = 65, condition = WeatherConditionType.RAINY)
        )
        assertEquals(RealisticWeatherScene.HEAVY_RAIN, heavyRainParams.scene)
        assertTrue(heavyRainParams.rainIntensity >= 0.9f)

        // 6. Thunderstorm (WMO 95)
        val stormParams = WeatherAnimationEngine.resolveSceneParameters(
            city = baseCity.copy(wmoCode = 95, condition = WeatherConditionType.THUNDERSTORM)
        )
        assertEquals(RealisticWeatherScene.THUNDERSTORM, stormParams.scene)
        assertTrue(stormParams.rainIntensity >= 0.85f)
        assertEquals(1.0f, stormParams.cloudDensity, 0.01f)

        // 7. Snow (WMO 75)
        val snowParams = WeatherAnimationEngine.resolveSceneParameters(
            city = baseCity.copy(wmoCode = 75, condition = WeatherConditionType.SNOWY)
        )
        assertEquals(RealisticWeatherScene.SNOW, snowParams.scene)
        assertTrue(snowParams.snowIntensity >= 0.85f)

        // 8. Fog (WMO 45)
        val fogParams = WeatherAnimationEngine.resolveSceneParameters(
            city = baseCity.copy(wmoCode = 45, condition = WeatherConditionType.PARTLY_CLOUDY)
        )
        assertEquals(RealisticWeatherScene.FOG, fogParams.scene)
        assertTrue(fogParams.fogDensity >= 0.7f)

        // 9. Clear Moonlight (nighttime WMO 0)
        val moonlightParams = WeatherAnimationEngine.resolveSceneParameters(
            city = baseCity.copy(
                wmoCode = 0,
                condition = WeatherConditionType.CLEAR_NIGHT,
                currentTimeDisplay = "11:30 PM"
            )
        )
        assertEquals(RealisticWeatherScene.CLEAR_MOONLIGHT, moonlightParams.scene)
        assertEquals(0.20f, moonlightParams.cloudDensity, 0.01f)

        // 10. Night Sky (nighttime WMO 2)
        val nightSkyParams = WeatherAnimationEngine.resolveSceneParameters(
            city = baseCity.copy(
                wmoCode = 2,
                condition = WeatherConditionType.CLEAR_NIGHT,
                currentTimeDisplay = "11:30 PM"
            )
        )
        assertEquals(RealisticWeatherScene.NIGHT_SKY, nightSkyParams.scene)
        assertEquals(0.40f, nightSkyParams.cloudDensity, 0.01f)
    }

    @Test
    fun `viewModel updates animation quality, battery saver mode, and scene override`() {
        val vm = WeatherViewModel(appContext = null, autoFetchOnInit = false)

        // Default is HIGH quality and batterySaverMode = false
        assertEquals(AnimationQualityLevel.HIGH, vm.uiState.value.settings.animationQuality)
        assertFalse(vm.uiState.value.settings.batterySaverMode)
        assertEquals(1.0f, vm.uiState.value.settings.toAnimationConfig().particleDensityMultiplier, 0.01f)

        // Switch to LOW quality
        vm.setAnimationQuality(AnimationQualityLevel.LOW)
        assertEquals(AnimationQualityLevel.LOW, vm.uiState.value.settings.animationQuality)
        assertEquals(0.50f, vm.uiState.value.settings.toAnimationConfig().particleDensityMultiplier, 0.01f)

        // Enable Battery Saver mode
        vm.toggleBatterySaverMode(true)
        assertTrue(vm.uiState.value.settings.batterySaverMode)
        assertEquals(0.40f, vm.uiState.value.settings.toAnimationConfig().particleDensityMultiplier, 0.01f)

        // Override scene to SNOW and verify activeSceneParameters
        vm.setWeatherSceneOverride(RealisticWeatherScene.SNOW)
        assertEquals(RealisticWeatherScene.SNOW, vm.uiState.value.activeSceneParameters.scene)

        // Reset override to live API weather
        vm.setWeatherSceneOverride(null)
        assertEquals(null, vm.uiState.value.sceneOverride)
    }

    @Test
    fun `every day in 10-day forecast has its own 24-hour hourly forecast when clicked`() {
        val vm = WeatherViewModel(appContext = null, autoFetchOnInit = false)
        vm.addSavedCity(createTestCity())
        val tenDays = vm.uiState.value.activeCity!!.tenDayForecast
        assertEquals(10, tenDays.size)

        tenDays.forEach { dayItem ->
            vm.selectDayForDetail(dayItem, openDetailScreen = true)
            val selected = vm.uiState.value.selectedDayForDetail
            assertNotNull(selected)
            assertEquals(dayItem.id, selected?.id)
            assertEquals(24, selected?.hourlyItemsForDay?.size)
        }
    }

    @Test
    fun `weather alert manager evaluates real meteorological advisories and distinguishes from official warnings`() {
        val baseCity = createTestCity()

        // 1. Thunderstorm + High Wind + Heavy Rain
        val stormCity = baseCity.copy(
            condition = WeatherConditionType.THUNDERSTORM,
            wmoCode = 95,
            details = baseCity.details.copy(windSpeedKmh = 38, windGustKmh = 52),
            tenDayForecast = listOf(
                baseCity.tenDayForecast.first().copy(rainProbability = 90, precipitationSumMm = 18.5)
            )
        )
        val stormAlerts = WeatherAlertManager.evaluateAlertsForCity(stormCity, TemperatureUnit.CELSIUS)
        assertTrue(stormAlerts.any { it.category == WeatherAlertCategory.THUNDERSTORM })
        assertTrue(stormAlerts.any { it.category == WeatherAlertCategory.HIGH_WIND })
        assertTrue(stormAlerts.any { it.category == WeatherAlertCategory.HEAVY_PRECIPITATION })
        assertTrue(stormAlerts.all { it.attributionLabel.contains("Not an Official Government Warning") })

        // 2. Snow + Freezing Temperature
        val snowCity = baseCity.copy(
            condition = WeatherConditionType.SNOWY,
            wmoCode = 75,
            currentTempCelsius = -4,
            lowTempCelsius = -9,
            highTempCelsius = 0
        )
        val snowAlerts = WeatherAlertManager.evaluateAlertsForCity(snowCity, TemperatureUnit.CELSIUS)
        assertTrue(snowAlerts.any { it.category == WeatherAlertCategory.SNOW })
        assertTrue(snowAlerts.any { it.category == WeatherAlertCategory.EXTREME_TEMPERATURE })
    }

    @Test
    fun `phase 5 viewModel manages empty initial saved locations, empty popular cities, favorite cities, default city, clear all, theme modes, notifications, and widget`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = WeatherViewModel(appContext = null, autoFetchOnInit = false)

        // Initial 5 preset saved & favorite locations and Popular World Cities are cleaned (both empty)
        assertTrue(SampleWeatherRepository.getInitialCities().isEmpty())
        assertTrue(SampleWeatherRepository.discoverableCities.isEmpty())
        assertTrue(vm.uiState.value.cities.isEmpty())
        assertTrue(vm.uiState.value.favoriteCityIds.isEmpty())
        assertNull(vm.uiState.value.activeCity)
        assertNull(vm.uiState.value.selectedCityId)

        // Add searched/user cities
        val paris = createTestCity(id = "geo_paris", cityName = "Paris")
        val mumbai = createTestCity(
            id = "geo_mumbai",
            cityName = "Mumbai",
            regionAndCountry = "Maharashtra, India",
            latitude = 19.0760,
            longitude = 72.8777
        )
        vm.addSavedCity(paris)
        vm.addSavedCity(mumbai)
        assertEquals(2, vm.uiState.value.cities.size)

        // Toggle favorite city
        assertFalse(vm.uiState.value.favoriteCityIds.contains(paris.id))
        vm.toggleFavoriteCity(paris.id)
        assertTrue(vm.uiState.value.favoriteCityIds.contains(paris.id))

        // Set default startup city
        vm.setDefaultCity(mumbai.id)
        assertEquals(mumbai.id, vm.uiState.value.settings.defaultCityId)

        // Remove individual city down to 1, then down to 0
        vm.removeCity(paris.id)
        assertEquals(1, vm.uiState.value.cities.size)
        vm.removeCity(mumbai.id)
        assertTrue(vm.uiState.value.cities.isEmpty())
        assertNull(vm.uiState.value.activeCity)

        // Re-add and verify clearAllSavedCities
        vm.addSavedCity(paris)
        vm.addSavedCity(mumbai)
        vm.clearAllSavedCities()
        assertTrue(vm.uiState.value.cities.isEmpty())
        assertTrue(vm.uiState.value.favoriteCityIds.isEmpty())
        assertNull(vm.uiState.value.selectedCityId)

        // Theme modes (Dark, Light, Auto)
        vm.setThemeMode(ThemeModeOption.LIGHT, systemIsDark = true)
        assertEquals(ThemeModeOption.LIGHT, vm.uiState.value.settings.themeMode)
        assertFalse(vm.uiState.value.settings.isDarkTheme)

        vm.setThemeMode(ThemeModeOption.AUTO_SYSTEM, systemIsDark = true)
        assertEquals(ThemeModeOption.AUTO_SYSTEM, vm.uiState.value.settings.themeMode)
        assertTrue(vm.uiState.value.settings.isDarkTheme)

        // Notification time & refresh interval
        vm.setNotificationTime(NotificationTimeOption.TIME_07_00)
        assertEquals(NotificationTimeOption.TIME_07_00, vm.uiState.value.settings.notificationTime)

        vm.setRefreshInterval(RefreshIntervalOption.EVERY_3_HOURS)
        assertEquals(RefreshIntervalOption.EVERY_3_HOURS, vm.uiState.value.settings.refreshInterval)

        // Verify notification channels & widget updater run cleanly on Android context
        WeatherNotificationHelper.ensureNotificationChannels(context)
        WeatherWidgetUpdater.updateAllWidgets(
            context = context,
            city = paris,
            temperatureUnit = TemperatureUnit.CELSIUS,
            lastUpdatedLabel = "10:30 AM"
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `repository validates coordinates and sanitizes search queries`() = runTest {
        var capturedQuery: String? = null
        val fakeForecastApi = object : WeatherApiService {
            override suspend fun getForecast(
                latitude: Double,
                longitude: Double,
                current: String,
                hourly: String,
                daily: String,
                timezone: String,
                forecastDays: Int
            ): OpenMeteoForecastResponse {
                throw AssertionError("Forecast API should not be called for invalid coordinates")
            }
        }
        val fakeGeocodingApi = object : GeocodingService {
            override suspend fun searchLocations(
                name: String,
                count: Int,
                language: String,
                format: String
            ): GeocodingSearchResponse {
                capturedQuery = name
                return GeocodingSearchResponse(results = emptyList())
            }
        }

        val repo = WeatherRepository(
            context = null,
            weatherApiService = fakeForecastApi,
            geocodingService = fakeGeocodingApi
        )

        val nanResult = repo.fetchLiveCityWeather("bad1", "Bad", "Nowhere", Double.NaN, 10.0)
        assertTrue(nanResult is WeatherApiResult.ApiError)

        val outOfRangeLat = repo.fetchLiveCityWeather("bad2", "Bad", "Nowhere", 95.0, 10.0)
        assertTrue(outOfRangeLat is WeatherApiResult.ApiError)

        val outOfRangeLon = repo.fetchLiveCityWeather("bad3", "Bad", "Nowhere", 45.0, -185.0)
        assertTrue(outOfRangeLon is WeatherApiResult.ApiError)

        val searchResult = repo.searchCities("  Tokyo\n\r\t  ")
        assertTrue(searchResult is WeatherApiResult.Success)
        assertEquals("Tokyo", capturedQuery)

        val emptyControlSearch = repo.searchCities("\u0000\n\t  ")
        assertTrue(emptyControlSearch is WeatherApiResult.Success)
        assertTrue((emptyControlSearch as WeatherApiResult.Success).data.isEmpty())
    }

    @Test
    fun `release metadata, package identity, permission denial, and widget status flows work cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("com.aistudio.weather.glsmph", context.packageName)
        assertEquals("Weather", context.getString(R.string.app_name))
        assertEquals("Version 5.1.0", context.getString(R.string.settings_app_version))
        assertTrue(context.getString(R.string.about_developer_desc).contains("Antonee"))
        assertTrue(
            context.getString(R.string.privacy_policy_url_label)
                .contains("https://github.com/Antoneeroy/Weather/blob/main/PRIVACY_POLICY.md")
        )

        val vm = WeatherViewModel(appContext = context, autoFetchOnInit = false)

        // Location permission denied state
        vm.onLocationPermissionDenied()
        assertTrue(vm.uiState.value.gpsStatus is GpsLocationStatus.PermissionDenied)

        // Add city and verify widget sync updates status message
        val testCity = createTestCity()
        vm.addSavedCity(testCity)
        vm.pinOrSyncHomeScreenWidget()
        assertNotNull(vm.uiState.value.notificationStatusMessage)
        assertTrue(vm.uiState.value.notificationStatusMessage!!.contains("Widget", ignoreCase = true))
    }
}

