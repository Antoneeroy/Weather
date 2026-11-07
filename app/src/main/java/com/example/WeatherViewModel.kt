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
                // Restore cached city snapshots & custom saved cities
                val cachedMap = prefs.getAllCachedSnapshots()
                val savedRecords = prefs.loadSavedCitiesRecords()
                if (cachedMap.isNotEmpty() || savedRecords.isNotEmpty()) {
                    _uiState.update { current ->
                        val baseCities = current.cities.map { city ->
                            cachedMap[city.id]?.weatherData ?: city
                        }.toMutableList()

                        savedRecords.forEach { rec ->
                            if (baseCities.none { it.id == rec.id }) {
                                val cachedCity = cachedMap[rec.id]?.weatherData
                                if (cachedCity != null) {
                                    baseCities.add(cachedCity)
                                } else {
                                    val template = current.cities.first()
                                    baseCities.add(
                                        template.copy(
                                            id = rec.id,
                                            cityName = rec.cityName,
                                            regionAndCountry = rec.regionAndCountry,
                                            latitude = rec.latitude,
                                            longitude = rec.longitude,
                                            isFavorite = rec.isFavorite,
                                            isLiveData = false
                                        )
                                    )
                                }
                            }
                        }
                        current.copy(cities = baseCities)
                    }
                }
            }

            viewModelScope.launch {
                prefs.settingsFlow.collect { loadedSettings ->
                    _uiState.update { current ->
                        current.copy(settings = loadedSettings)
                    }
                    appContext?.let { ctx ->
                        WeatherNotificationHelper.scheduleOrUpdateBackgroundWork(ctx, loadedSettings)
                    }
                }
            }

            viewModelScope.launch {
                prefs.favoriteCityIdsFlow.collect { favIds ->
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

            viewModelScope.launch {
                prefs.selectedCityIdFlow.collect { savedCityId ->
                    if (!savedCityId.isNullOrBlank()) {
                        _uiState.update { current ->
                            if (current.cities.any { it.id == savedCityId }) {
                                current.copy(selectedCityId = savedCityId)
                            } else {
                                current
                            }
                        }
                    }
                }
            }
        }

        if (autoFetchOnInit) {
            viewModelScope.launch {
                if (locationManager?.hasLocationPermission() == true) {
                    detectGpsLocationAndFetchWeather()
                } else {
                    refreshActiveCityWeather()
                }
            }
        }
    }

    fun refreshActiveCityWeather() {
        val currentCity = _uiState.value.activeCity
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

    fun setDefaultCity(cityId: String) {
        updateSettings { it.copy(defaultCityId = cityId) }
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

    fun addDiscoverableCity(city: CityWeatherData) {
        _uiState.update { state ->
            if (state.cities.any { it.id == city.id }) {
                state.copy(selectedCityId = city.id)
            } else {
                state.copy(
                    cities = state.cities + city,
                    selectedCityId = city.id
                )
            }
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

    fun removeCity(cityId: String) {
        _uiState.update { state ->
            if (state.cities.size <= 1) return@update state
            val remaining = state.cities.filterNot { it.id == cityId }
            val nextSelected = if (state.selectedCityId == cityId) remaining.first().id else state.selectedCityId
            state.copy(cities = remaining, selectedCityId = nextSelected)
        }
        viewModelScope.launch {
            weatherPreferences?.saveCustomCitiesList(_uiState.value.cities, _uiState.value.favoriteCityIds)
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
        appContext?.let { ctx ->
            WeatherWidgetUpdater.updateAllWidgets(
                context = ctx,
                city = _uiState.value.activeCity,
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
        val posted = WeatherNotificationHelper.evaluateAndSendNotifications(
            context = ctx,
            city = _uiState.value.activeCity,
            settings = _uiState.value.settings,
            forceSendSummary = true
        )
        _uiState.update {
            it.copy(
                notificationStatusMessage = if (posted) {
                    "Live weather notification sent for ${_uiState.value.activeCity.cityName}."
                } else {
                    "Grant notification permission to receive weather notifications."
                }
            )
        }
        return posted
    }

    fun pinOrSyncHomeScreenWidget() {
        val ctx = appContext ?: return
        WeatherWidgetUpdater.updateAllWidgets(
            context = ctx,
            city = _uiState.value.activeCity,
            temperatureUnit = _uiState.value.settings.temperatureUnit,
            lastUpdatedLabel = _uiState.value.lastUpdatedTime
        )
        val pinned = WeatherWidgetUpdater.requestPinWeatherWidget(ctx)
        _uiState.update {
            it.copy(
                notificationStatusMessage = if (pinned) {
                    "Widget pin request sent to Android launcher & synced with ${_uiState.value.activeCity.cityName}."
                } else {
                    "Widget data synced for ${_uiState.value.activeCity.cityName}. Add the Weather widget from your launcher."
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
