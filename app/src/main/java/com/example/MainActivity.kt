package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val weatherViewModel: WeatherViewModel = viewModel(
                factory = WeatherViewModel.provideFactory(context)
            )
            val uiState by weatherViewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()

            val effectiveDarkTheme = when (uiState.settings.themeMode) {
                ThemeModeOption.DARK -> true
                ThemeModeOption.LIGHT -> false
                ThemeModeOption.AUTO_SYSTEM -> systemDark
            }

            LaunchedEffect(effectiveDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (effectiveDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (effectiveDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
            }

            WeatherAppRoot(
                uiState = uiState,
                effectiveDarkTheme = effectiveDarkTheme,
                systemIsDark = systemDark,
                viewModel = weatherViewModel
            )
        }
    }
}

@Composable
fun WeatherAppRoot(
    uiState: WeatherUiState,
    viewModel: WeatherViewModel,
    effectiveDarkTheme: Boolean = uiState.settings.isDarkTheme,
    systemIsDark: Boolean = true
) {
    val context = LocalContext.current
    var currentDestination by rememberSaveable { mutableStateOf(WeatherDestination.HOME) }
    var hasRequestedInitialPermission by rememberSaveable { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.detectGpsLocationAndFetchWeather()
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.dispatchTestWeatherNotification()
        } else {
            viewModel.toggleNotificationsMaster(false)
        }
    }

    val requestOrSendNotification = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !WeatherNotificationHelper.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.dispatchTestWeatherNotification()
        }
    }

    val requestOrDetectGpsLocation = {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (hasPerm) {
            viewModel.detectGpsLocationAndFetchWeather()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    // Request foreground location permission once on initial app launch
    LaunchedEffect(Unit) {
        WeatherNotificationHelper.ensureNotificationChannels(context)
        if (!hasRequestedInitialPermission) {
            hasRequestedInitialPermission = true
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

            if (!hasPerm) {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            }
        }
    }

    WeatherTheme(darkTheme = effectiveDarkTheme) {
        WeatherAnimatedBackground(
            sceneParams = uiState.activeSceneParameters,
            isDarkTheme = effectiveDarkTheme,
            animationConfig = uiState.settings.toAnimationConfig()
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    WeatherGlassBottomNavigationBar(
                        currentDestination = currentDestination,
                        onNavigate = { destination ->
                            if (destination != WeatherDestination.FORECAST) {
                                viewModel.closeDayDetailScreen()
                            }
                            currentDestination = destination
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = innerPadding.calculateTopPadding()),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Crossfade(
                        targetState = currentDestination,
                        animationSpec = tween(durationMillis = 280),
                        label = "screen_crossfade"
                    ) { destination ->
                        when (destination) {
                            WeatherDestination.HOME -> {
                                WeatherHomeScreen(
                                    uiState = uiState,
                                    onRefresh = viewModel::triggerRefresh,
                                    onRequestGpsWeather = requestOrDetectGpsLocation,
                                    onSelectConditionOverride = viewModel::setWeatherConditionOverride,
                                    onSelectSceneOverride = viewModel::setWeatherSceneOverride,
                                    onOpenFullForecast = {
                                        viewModel.closeDayDetailScreen()
                                        currentDestination = WeatherDestination.FORECAST
                                    },
                                    onOpenCities = {
                                        currentDestination = WeatherDestination.CITIES
                                    },
                                    onSelectDayDetail = { day ->
                                        viewModel.selectDayForDetail(day, openDetailScreen = true)
                                        currentDestination = WeatherDestination.FORECAST
                                    }
                                )
                            }

                            WeatherDestination.FORECAST -> {
                                ForecastScreen(
                                    uiState = uiState,
                                    onSelectDay = { day ->
                                        viewModel.selectDayForDetail(day, openDetailScreen = true)
                                    },
                                    onCloseDayDetail = viewModel::closeDayDetailScreen,
                                    onRefresh = viewModel::triggerRefresh,
                                    onBackToHome = {
                                        currentDestination = WeatherDestination.HOME
                                    }
                                )
                            }

                            WeatherDestination.CITIES -> {
                                CitiesScreen(
                                    uiState = uiState,
                                    onSelectCity = viewModel::selectCity,
                                    onSearchQueryChange = viewModel::updateSearchQuery,
                                    onExecuteCitySearch = viewModel::executeImmediateCitySearch,
                                    onSelectGeocodedLocation = { geoDto ->
                                        viewModel.selectGeocodedLocation(
                                            location = geoDto,
                                            onCompleteNavigateHome = {
                                                currentDestination = WeatherDestination.HOME
                                            }
                                        )
                                    },
                                    onRequestGpsLocation = requestOrDetectGpsLocation,
                                    onAddCity = viewModel::addDiscoverableCity,
                                    onRemoveCity = viewModel::removeCity,
                                    onNavigateHome = {
                                        currentDestination = WeatherDestination.HOME
                                    },
                                    onToggleFavoriteCity = viewModel::toggleFavoriteCity,
                                    onSetDefaultCity = viewModel::setDefaultCity
                                )
                            }

                            WeatherDestination.SETTINGS -> {
                                SettingsScreen(
                                    settings = uiState.settings,
                                    onToggleDarkTheme = viewModel::toggleDarkTheme,
                                    onSelectTempUnit = viewModel::setTemperatureUnit,
                                    onToggleAnimations = viewModel::toggleAnimations,
                                    onSelectAnimationQuality = viewModel::setAnimationQuality,
                                    onToggleBatterySaver = viewModel::toggleBatterySaverMode,
                                    onBackToHome = {
                                        currentDestination = WeatherDestination.HOME
                                    },
                                    savedCities = uiState.cities,
                                    notificationStatusMessage = uiState.notificationStatusMessage,
                                    onSelectThemeMode = { mode ->
                                        viewModel.setThemeMode(mode, systemIsDark)
                                    },
                                    onToggleNotificationsMaster = { enabled ->
                                        viewModel.toggleNotificationsMaster(enabled)
                                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            !WeatherNotificationHelper.hasNotificationPermission(context)
                                        ) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    },
                                    onToggleDailySummaryNotif = viewModel::toggleDailySummaryNotification,
                                    onToggleMorningWeatherNotif = viewModel::toggleMorningWeatherNotification,
                                    onToggleRainNotif = viewModel::toggleRainNotification,
                                    onToggleSevereWeatherNotif = viewModel::toggleSevereWeatherNotification,
                                    onToggleTempChangeNotif = viewModel::toggleTemperatureChangeNotification,
                                    onSelectNotificationTime = viewModel::setNotificationTime,
                                    onSendTestNotification = { requestOrSendNotification() },
                                    onSelectDefaultCity = viewModel::setDefaultCity,
                                    onSelectRefreshInterval = viewModel::setRefreshInterval,
                                    onPinOrUpdateWidget = viewModel::pinOrSyncHomeScreenWidget
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
