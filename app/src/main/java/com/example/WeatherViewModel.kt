package com.example

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val appContext: Context? = null,
    private val repository: WeatherRepository = WeatherRepository(appContext),
    private val locationManager: LocationManager? = appContext?.let { LocationManager(it) },
    private val weatherPreferences: WeatherPreferences? = appContext?.let { WeatherPreferences(it) },
    autoFetchOnInit: Boolean = appContext != null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        WeatherUiState(
            syncState = if (autoFetchOnInit) {
                WeatherDataSyncState.Loading
            } else {
                WeatherDataSyncState.Success("Ready")
            }
        )
    )
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var searchDebounceJob: Job? = null

    companion object {
        private val INITIAL_FIVE_CITY_IDS = setOf(
            "sf_us", "tokyo_jp", "london_uk", "dubai_ae", "zermatt_ch",
            "nyc_us", "singapore_sg", "paris_fr"
        )

        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appCtx = context.applicationContext
                    return WeatherViewModel(
                        appContext = appCtx,
                        repository = WeatherRepository(appCtx),
                        locationManager = LocationManager(appCtx),
                        weatherPreferences = WeatherPreferences(appCtx),
                        autoFetchOnInit = true
                    ) as T
                }
            }
    }

    init {
        weatherPreferences?.let { prefs ->
            viewModelScope.launch {
                val cachedMap = prefs.getAllCachedSnapshots()
                val savedRecords = prefs.loadSavedCitiesRecords()
                    .filterNot { it.id in INITIAL_FIVE_CITY_IDS }
                val favIds = (prefs.favoriteCityIdsFlow.firstOrNull() ?: emptySet())
                    .filterNot { it in INITIAL_FIVE_CITY_IDS }
                    .toSet()
                val rawSettings = prefs.settingsFlow.firstOrNull() ?: WeatherAppSettings()
                val loadedSettings = if (rawSettings.defaultCityId in INITIAL_FIVE_CITY_IDS) {
                    rawSettings.copy(defaultCityId = "")
                } else {
                    rawSettings
                }
                val savedSelectedId = prefs.selectedCityIdFlow.firstOrNull()
                    ?.takeIf { it !in INITIAL_FIVE_CITY_IDS }
                    ?: loadedSettings.defaultCityId.takeIf { it.isNotBlank() }

                val restoredCities = savedRecords.map { rec ->
                    cachedMap[rec.id]?.weatherData?.copy(isFavorite = favIds.contains(rec.id))
                        ?: createMinimalCityFromRecord(rec, favIds.contains(rec.id))
                }

                val resolvedSelectedId = savedSelectedId?.takeIf { id ->
                    restoredCities.any { it.id == id }
                } ?: restoredCities.firstOrNull()?.id

                _uiState.update { current ->
                    val target = restoredCities.find { it.id == resolvedSelectedId } ?: restoredCities.firstOrNull()
                    current.copy(
                        cities = restoredCities,
                        selectedCityId = resolvedSelectedId,
                        favoriteCityIds = favIds,
                        settings = loadedSettings,
                        selectedDayForDetail = target?.tenDayForecast?.firstOrNull()
                    )
                }

                if (autoFetchOnInit) {
                    if (locationManager?.hasLocationPermission() == true) {
                        detectGpsLocationAndFetchWeather()
                    } else if (_uiState.value.activeCity != null) {
                        refreshActiveCityWeather()
                    } else {
                        _uiState.update {
                            it.copy(
                                isRefreshing = false,
                                syncState = WeatherDataSyncState.Success("Ready")
                            )
                        }
                    }
                }
            }

            viewModelScope.launch {
                prefs.settingsFlow.collect { loadedSettings ->
                    val sanitized = if (loadedSettings.defaultCityId in INITIAL_FIVE_CITY_IDS) {
                        loadedSettings.copy(defaultCityId = "")
                    } else {
                        loadedSettings
                    }
                    _uiState.update { current ->
                        current.copy(settings = sanitized)
                    }
                    appContext?.let { ctx ->
                        WeatherNotificationHelper.scheduleOrUpdateBackgroundWork(ctx, sanitized)
                    }
                }
            }

            viewModelScope.launch {
                prefs.favoriteCityIdsFlow.collect { rawFavIds ->
                    val favIds = rawFavIds.filterNot { it in INITIAL_FIVE_CITY_IDS }.toSet()
                    _uiState.update { current ->
                        current.copy(
                            favoriteCityIds = favIds,
                            cities = current.cities.map { c ->
                                c.copy(isFavorite = favIds.contains(c.id))
                            }
                        )
                    }
                }
            }
        } ?: run {
            if (autoFetchOnInit) {
                viewModelScope.launch {
                    if (locationManager?.hasLocationPermission() == true) {
                        detectGpsLocationAndFetchWeather()
                    } else {
                        _uiState.update {
                            it.copy(
                                isRefreshing = false,
                                syncState = WeatherDataSyncState.Success("Ready")
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createMinimalCityFromRecord(rec: SavedCityRecord, isFavorite: Boolean): CityWeatherData {
        return CityWeatherData(
            id = rec.id,
            cityName = rec.cityName,
            regionAndCountry = rec.regionAndCountry,
            latitude = rec.latitude,
            longitude = rec.longitude,
            timeZoneOffsetLabel = "UTC",
            currentTimeDisplay = "Syncing…",
            currentDateDisplay = SampleWeatherRepository.getCurrentDateFormatted(),
            currentTempCelsius = 0,
            feelsLikeCelsius = 0,
            highTempCelsius = 0,
            lowTempCelsius = 0,
            condition = WeatherConditionType.PARTLY_CLOUDY,
            wmoCode = 2,
            conditionDescription = "Syncing live forecast…",
            airQualityIndex = 0,
            airQualityLabel = "Open-Meteo",
            details = AtmosphericDetails(
                humidityPercent = 0,
                dewPointCelsius = 0,
                windSpeedKmh = 0,
                windDirection = "--",
                windGustKmh = 0,
                pressureHpa = 1013,
                pressureTrend = "Syncing",
                visibilityKm = 10.0,
                visibilityStatus = "Syncing",
                uvIndex = 0,
                uvCategory = "Low",
                sunriseTime = "--:--",
                sunsetTime = "--:--",
                daylightDuration = "--"
            ),
            hourlyForecast = emptyList(),
            tenDayForecast = emptyList(),
            isCurrentLocation = rec.id == "gps_current_location",
            isLiveData = false,
            isFavorite = isFavorite
        )
    }

    fun refreshActiveCityWeather() {
        val currentCity = _uiState.value.activeCity
        if (currentCity == null) {
            if (locationManager?.hasLocationPermission() == true) {
                detectGpsLocationAndFetchWeather()
            } else {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        syncState = WeatherDataSyncState.Success("Ready")
                    )
                }
            }
            return
        }
        fetchLiveWeatherForCity(
            id = currentCity.id,
            cityName = currentCity.cityName,
            regionAndCountry = currentCity.regionAndCountry,
            latitude = currentCity.latitude,
            longitude = currentCity.longitude,
            isCurrentLocation = currentCity.isCurrentLocation,
            makeActive = true
        )
    }

    fun triggerRefresh() {
        refreshActiveCityWeather()
    }

    fun detectGpsLocationAndFetchWeather() {
        val locMgr = locationManager
        if (locMgr == null) {
            refreshActiveCityWeather()
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshing = true,
                    gpsStatus = GpsLocationStatus.Detecting,
                    syncState = WeatherDataSyncState.Loading
                )
            }

            when (val locResult = locMgr.fetchCurrentDeviceLocation()) {
                is LocationFetchResult.Success -> {
                    val gpsCityId = "gps_current_location"
                    _uiState.update {
                        it.copy(gpsStatus = GpsLocationStatus.Active(locResult.cityName))
                    }
                    fetchLiveWeatherForCity(
                        id = gpsCityId,
                        cityName = locResult.cityName,
                        regionAndCountry = locResult.regionAndCountry,
                        latitude = locResult.latitude,
                        longitude = locResult.longitude,
                        isCurrentLocation = true,
                        makeActive = true
                    )
                }

                is LocationFetchResult.PermissionDenied -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            gpsStatus = GpsLocationStatus.PermissionDenied
                        )
                    }
                    refreshActiveCityWeather()
                }

                is LocationFetchResult.LocationDisabled -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            gpsStatus = GpsLocationStatus.Unavailable(
                                "Location Services (GPS) are turned off on this device."
                            )
                        )
                    }
                    refreshActiveCityWeather()
                }

                is LocationFetchResult.LocationUnavailable -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            gpsStatus = GpsLocationStatus.Unavailable(locResult.message)
                        )
                    }
                    refreshActiveCityWeather()
                }
            }
        }
    }

    fun onLocationPermissionDenied() {
        _uiState.update {
            it.copy(gpsStatus = GpsLocationStatus.PermissionDenied)
        }
        if (_uiState.value.syncState is WeatherDataSyncState.Loading) {
            refreshActiveCityWeather()
        }
    }

    private fun fetchLiveWeatherForCity(
        id: String,
        cityName: String,
        regionAndCountry: String,
        latitude: Double,
        longitude: Double,
        isCurrentLocation: Boolean,
        makeActive: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshing = true,
                    syncState = WeatherDataSyncState.Loading
                )
            }

            when (
                val apiResult = repository.fetchLiveCityWeather(
                    id = id,
                    cityName = cityName,
                    regionAndCountry = regionAndCountry,
                    latitude = latitude,
                    longitude = longitude,
                    isCurrentLocation = isCurrentLocation
                )
            ) {
                is WeatherApiResult.Success -> {
                    val nowStamp = SampleWeatherRepository.getCurrentTimeFormatted()
                    val isFav = _uiState.value.favoriteCityIds.contains(id)
                    val liveCity = apiResult.data.copy(
                        isLiveData = true,
                        isCachedOfflineData = false,
                        cachedTimestampLabel = nowStamp,
                        isFavorite = isFav
                    )

                    _uiState.update { state ->
                        val updatedList = if (state.cities.any { it.id == id }) {
                            state.cities.map { existing ->
                                if (existing.id == id) liveCity else existing
                            }
                        } else {
                            if (isCurrentLocation) {
                                listOf(liveCity) + state.cities.filterNot { it.isCurrentLocation }
                            } else {
                                state.cities + liveCity
                            }
                        }

                        val updatedSelectedDay = if (makeActive && !state.isShowingDayDetailScreen) {
                            liveCity.tenDayForecast.firstOrNull()
                        } else if (state.selectedDayForDetail != null) {
                            liveCity.tenDayForecast.find { it.dayName == state.selectedDayForDetail.dayName }
                                ?: liveCity.tenDayForecast.firstOrNull()
                        } else {
                            liveCity.tenDayForecast.firstOrNull()
                        }

                        state.copy(
                            cities = updatedList,
                            selectedCityId = if (makeActive) id else state.selectedCityId,
                            weatherOverride = if (makeActive) null else state.weatherOverride,
                            sceneOverride = if (makeActive) null else state.sceneOverride,
                            selectedDayForDetail = updatedSelectedDay,
                            isRefreshing = false,
                            syncState = WeatherDataSyncState.Success(nowStamp),
                            lastUpdatedTime = nowStamp
                        )
                    }

                    // Persist offline cache, saved cities, and update widget
                    weatherPreferences?.let { prefs ->
                        prefs.cacheLiveCityWeather(liveCity, nowStamp)
                        if (makeActive) {
                            prefs.saveSelectedCityId(id)
                        }
                        prefs.saveCustomCitiesList(_uiState.value.cities, _uiState.value.favoriteCityIds)
                    }

                    appContext?.let { ctx ->
                        WeatherWidgetUpdater.updateAllWidgets(
                            context = ctx,
                            city = liveCity,
                            temperatureUnit = _uiState.value.settings.temperatureUnit,
                            lastUpdatedLabel = nowStamp
                        )
                    }
                }

                is WeatherApiResult.NoInternet -> {
                    // Check if we have a cached offline snapshot for this city
                    val cachedSnapshot = weatherPreferences?.getCachedCityWeather(id)
                    _uiState.update { state ->
                        val updatedCities = if (cachedSnapshot != null) {
                            state.cities.map { existing ->
                                if (existing.id == id) {
                                    cachedSnapshot.weatherData.copy(
                                        isLiveData = false,
                                        isCachedOfflineData = true,
                                        cachedTimestampLabel = cachedSnapshot.cachedAtFormatted
                                    )
                                } else {
                                    existing
                                }
                            }
                        } else {
                            state.cities.map { existing ->
                                if (existing.id == id) {
                                    existing.copy(isLiveData = false)
                                } else {
                                    existing
                                }
                            }
                        }

                        state.copy(
                            cities = updatedCities,
                            isRefreshing = false,
                            lastUpdatedTime = cachedSnapshot?.cachedAtFormatted ?: state.lastUpdatedTime,
                            syncState = WeatherDataSyncState.NoInternet(apiResult.message)
                        )
                    }
                }

                is WeatherApiResult.ApiError -> {
                    _uiState.update { state ->
                        state.copy(
                            isRefreshing = false,
                            syncState = WeatherDataSyncState.Error(apiResult.message)
                        )
                    }
                }
            }
        }
    }

    fun selectCity(cityId: String) {
        val target = _uiState.value.cities.find { it.id == cityId } ?: return
        _uiState.update {
            it.copy(
                selectedCityId = cityId,
                weatherOverride = null,
                sceneOverride = null,
                selectedDayForDetail = target.tenDayForecast.firstOrNull(),
                isShowingDayDetailScreen = false
            )
        }
        viewModelScope.launch {
            weatherPreferences?.saveSelectedCityId(cityId)
        }
        fetchLiveWeatherForCity(
            id = target.id,
            cityName = target.cityName,
            regionAndCountry = target.regionAndCountry,
            latitude = target.latitude,
            longitude = target.longitude,
            isCurrentLocation = target.isCurrentLocation,
            makeActive = true
        )
    }

    fun toggleFavoriteCity(cityId: String) {
        _uiState.update { state ->
            val updatedFavs = if (state.favoriteCityIds.contains(cityId)) {
                state.favoriteCityIds - cityId
            } else {
                state.favoriteCityIds + cityId
            }
            state.copy(
                favoriteCityIds = updatedFavs,
                cities = state.cities.map { city ->
                    if (city.id == cityId) city.copy(isFavorite = updatedFavs.contains(cityId)) else city
                }
            )
        }
        viewModelScope.launch {
            weatherPreferences?.saveFavoriteCityIds(_uiState.value.favoriteCityIds)
            weatherPreferences?.saveCustomCitiesList(_uiState.value.cities, _uiState.value.favoriteCityIds)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                searchErrorMessage = null
            )
        }
        searchDebounceJob?.cancel()

        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _uiState.update {
                it.copy(
                    isSearchingCities = false,
                    geocodingSuggestions = emptyList(),
                    searchErrorMessage = null
                )
            }
            return
        }

        searchDebounceJob = viewModelScope.launch {
            delay(320L)
            performGeocodingCitySearch(trimmed)
        }
    }

    fun executeImmediateCitySearch() {
        val trimmed = _uiState.value.searchQuery.trim()
        if (trimmed.isEmpty()) {
            _uiState.update {
                it.copy(searchErrorMessage = "Please enter a city name to search.")
            }
            return
        }
        searchDebounceJob?.cancel()
        viewModelScope.launch {
            performGeocodingCitySearch(trimmed)
        }
    }

    private suspend fun performGeocodingCitySearch(query: String) {
        _uiState.update {
            it.copy(
                isSearchingCities = true,
                searchErrorMessage = null
            )
        }
        when (val result = repository.searchCities(query)) {
            is WeatherApiResult.Success -> {
                val list = result.data
                _uiState.update {
                    it.copy(
                        isSearchingCities = false,
                        geocodingSuggestions = list,
                        searchErrorMessage = if (list.isEmpty()) {
                            "No matching cities found for \"$query\". Check spelling and try again."
                        } else {
                            null
                        }
                    )
                }
            }

            is WeatherApiResult.NoInternet -> {
                _uiState.update {
                    it.copy(
                        isSearchingCities = false,
                        geocodingSuggestions = emptyList(),
                        searchErrorMessage = result.message
                    )
                }
            }

            is WeatherApiResult.ApiError -> {
                _uiState.update {
                    it.copy(
                        isSearchingCities = false,
                        geocodingSuggestions = emptyList(),
                        searchErrorMessage = result.message
                    )
                }
            }
        }
    }

    fun selectGeocodedLocation(
        location: GeocodingLocationDto,
        onCompleteNavigateHome: (() -> Unit)? = null
    ) {
        val cityKey = location.uniqueCityKey
        _uiState.update {
            it.copy(
                searchQuery = "",
                geocodingSuggestions = emptyList(),
                searchErrorMessage = null,
                isShowingDayDetailScreen = false
            )
        }
        onCompleteNavigateHome?.invoke()
        fetchLiveWeatherForCity(
            id = cityKey,
            cityName = location.name,
            regionAndCountry = location.formattedRegionAndCountry,
            latitude = location.latitude,
            longitude = location.longitude,
            isCurrentLocation = false,
            makeActive = true
        )
    }

    fun addSavedCity(city: CityWeatherData) {
        val exists = _uiState.value.cities.any { it.id == city.id }
        if (!exists) {
            _uiState.update { state ->
                state.copy(
                    cities = state.cities + city,
                    selectedCityId = city.id,
                    selectedDayForDetail = city.tenDayForecast.firstOrNull(),
                    isShowingDayDetailScreen = false
                )
            }
            viewModelScope.launch {
                weatherPreferences?.saveSelectedCityId(city.id)
                weatherPreferences?.saveCustomCitiesList(_uiState.value.cities, _uiState.value.favoriteCityIds)
            }
        } else {
            selectCity(city.id)
        }
        fetchLiveWeatherForCity(
            id = city.id,
            cityName = city.cityName,
            regionAndCountry = city.regionAndCountry,
            latitude = city.latitude,
            longitude = city.longitude,
            isCurrentLocation = false,
            makeActive = true
        )
    }

    fun setDefaultCity(cityId: String) {
        updateSettings { it.copy(defaultCityId = cityId) }
        if (_uiState.value.cities.any { it.id == cityId }) {
            selectCity(cityId)
        }
    }

    fun removeCity(cityId: String) {
        _uiState.update { state ->
            val remaining = state.cities.filterNot { it.id == cityId }
            val nextSelected = if (state.selectedCityId == cityId) {
                remaining.firstOrNull()?.id
            } else {
                state.selectedCityId
            }
            val nextDetailDay = remaining.find { it.id == nextSelected }?.tenDayForecast?.firstOrNull()
            state.copy(
                cities = remaining,
                selectedCityId = nextSelected,
                favoriteCityIds = state.favoriteCityIds - cityId,
                selectedDayForDetail = nextDetailDay,
                isShowingDayDetailScreen = false
            )
        }
        viewModelScope.launch {
            weatherPreferences?.saveSelectedCityId(_uiState.value.selectedCityId)
            weatherPreferences?.saveFavoriteCityIds(_uiState.value.favoriteCityIds)
            weatherPreferences?.saveCustomCitiesList(_uiState.value.cities, _uiState.value.favoriteCityIds)
        }
    }

    fun clearAllSavedCities() {
        _uiState.update { state ->
            state.copy(
                cities = emptyList(),
                selectedCityId = null,
                favoriteCityIds = emptySet(),
                selectedDayForDetail = null,
                isShowingDayDetailScreen = false
            )
        }
        if (_uiState.value.settings.defaultCityId.isNotBlank()) {
            updateSettings { it.copy(defaultCityId = "") }
        }
        viewModelScope.launch {
            weatherPreferences?.saveSelectedCityId(null)
            weatherPreferences?.saveFavoriteCityIds(emptySet())
            weatherPreferences?.saveCustomCitiesList(emptyList(), emptySet())
        }
    }

    fun setWeatherConditionOverride(condition: WeatherConditionType?) {
        _uiState.update {
            it.copy(
                weatherOverride = condition,
                sceneOverride = null
            )
        }
    }

    fun setWeatherSceneOverride(scene: RealisticWeatherScene?) {
        _uiState.update {
            it.copy(
                sceneOverride = scene,
                weatherOverride = null
            )
        }
    }

    fun setThemeMode(mode: ThemeModeOption, systemIsDark: Boolean = true) {
        val resolvedDark = when (mode) {
            ThemeModeOption.DARK -> true
            ThemeModeOption.LIGHT -> false
            ThemeModeOption.AUTO_SYSTEM -> systemIsDark
        }
        updateSettings {
            it.copy(
                themeMode = mode,
                isDarkTheme = resolvedDark
            )
        }
    }

    fun toggleDarkTheme(isDark: Boolean) {
        updateSettings {
            it.copy(
                themeMode = if (isDark) ThemeModeOption.DARK else ThemeModeOption.LIGHT,
                isDarkTheme = isDark
            )
        }
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        updateSettings { it.copy(temperatureUnit = unit) }
        val active = _uiState.value.activeCity ?: return
        appContext?.let { ctx ->
            WeatherWidgetUpdater.updateAllWidgets(
                context = ctx,
                city = active,
                temperatureUnit = unit,
                lastUpdatedLabel = _uiState.value.lastUpdatedTime
            )
        }
    }

    fun toggleAnimations(enabled: Boolean) {
        updateSettings { it.copy(animationsEnabled = enabled) }
    }

    fun setAnimationQuality(quality: AnimationQualityLevel) {
        updateSettings { it.copy(animationQuality = quality) }
    }

    fun toggleBatterySaverMode(enabled: Boolean) {
        updateSettings { it.copy(batterySaverMode = enabled) }
    }

    fun toggleNotificationsMaster(enabled: Boolean) {
        updateSettings { it.copy(notificationsEnabled = enabled) }
    }

    fun toggleDailySummaryNotification(enabled: Boolean) {
        updateSettings { it.copy(dailySummaryNotification = enabled) }
    }

    fun toggleMorningWeatherNotification(enabled: Boolean) {
        updateSettings { it.copy(morningWeatherNotification = enabled) }
    }

    fun toggleRainNotification(enabled: Boolean) {
        updateSettings { it.copy(rainNotification = enabled) }
    }

    fun toggleSevereWeatherNotification(enabled: Boolean) {
        updateSettings { it.copy(severeWeatherNotification = enabled) }
    }

    fun toggleTemperatureChangeNotification(enabled: Boolean) {
        updateSettings { it.copy(temperatureChangeNotification = enabled) }
    }

    fun setNotificationTime(timeOption: NotificationTimeOption) {
        updateSettings { it.copy(notificationTime = timeOption) }
    }

    fun setRefreshInterval(intervalOption: RefreshIntervalOption) {
        updateSettings { it.copy(refreshInterval = intervalOption) }
    }

    fun dispatchTestWeatherNotification(): Boolean {
        val ctx = appContext ?: return false
        val active = _uiState.value.activeCity
        if (active == null) {
            _uiState.update {
                it.copy(notificationStatusMessage = "Add or detect a city first to send a weather notification.")
            }
            return false
        }
        val posted = WeatherNotificationHelper.evaluateAndSendNotifications(
            context = ctx,
            city = active,
            settings = _uiState.value.settings,
            forceSendSummary = true
        )
        _uiState.update {
            it.copy(
                notificationStatusMessage = if (posted) {
                    "Live weather notification sent for ${active.cityName}."
                } else {
                    "Grant notification permission to receive weather notifications."
                }
            )
        }
        return posted
    }

    fun pinOrSyncHomeScreenWidget() {
        val ctx = appContext ?: return
        val active = _uiState.value.activeCity
        if (active != null) {
            WeatherWidgetUpdater.updateAllWidgets(
                context = ctx,
                city = active,
                temperatureUnit = _uiState.value.settings.temperatureUnit,
                lastUpdatedLabel = _uiState.value.lastUpdatedTime
            )
        }
        val pinned = WeatherWidgetUpdater.requestPinWeatherWidget(ctx)
        _uiState.update {
            it.copy(
                notificationStatusMessage = when {
                    pinned && active != null ->
                        "Widget pin request sent to Android launcher & synced with ${active.cityName}."
                    pinned ->
                        "Widget pin request sent to Android launcher."
                    active != null ->
                        "Widget data synced for ${active.cityName}. Add the Weather widget from your launcher."
                    else ->
                        "Add the Weather widget from your launcher, then add or detect a city."
                }
            )
        }
    }

    fun selectDayForDetail(day: DailyForecastItem?, openDetailScreen: Boolean = true) {
        _uiState.update {
            it.copy(
                selectedDayForDetail = day,
                isShowingDayDetailScreen = day != null && openDetailScreen
            )
        }
    }

    fun closeDayDetailScreen() {
        _uiState.update {
            it.copy(isShowingDayDetailScreen = false)
        }
    }

    private fun updateSettings(transform: (WeatherAppSettings) -> WeatherAppSettings) {
        _uiState.update { current ->
            val updated = transform(current.settings)
            current.copy(settings = updated)
        }
        val latest = _uiState.value.settings
        viewModelScope.launch {
            weatherPreferences?.saveSettings(latest)
            appContext?.let { ctx ->
                WeatherNotificationHelper.scheduleOrUpdateBackgroundWork(ctx, latest)
            }
        }
    }
}
