package com.example

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun CitiesScreen(
    uiState: WeatherUiState,
    onSelectCity: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onExecuteCitySearch: () -> Unit,
    onSelectGeocodedLocation: (GeocodingLocationDto) -> Unit,
    onRequestGpsLocation: () -> Unit,
    onAddCity: (CityWeatherData) -> Unit,
    onRemoveCity: (String) -> Unit,
    onNavigateHome: () -> Unit,
    onToggleFavoriteCity: (String) -> Unit = {},
    onSetDefaultCity: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateHome)

    val palette = LocalGlassPalette.current
    val focusManager = LocalFocusManager.current
    val unit = uiState.settings.temperatureUnit
    val animationsEnabled = uiState.settings.animationsEnabled

    val availableToAdd = SampleWeatherRepository.discoverableCities.filter { discoverable ->
        uiState.cities.none { it.id == discoverable.id }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .testTag("cities_screen_list"),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 14.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header + Detect GPS Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.cities_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = palette.primaryText
                        )
                        Text(
                            text = stringResource(R.string.cities_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondaryText
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(palette.accentCyan.copy(alpha = 0.22f))
                            .border(1.dp, palette.accentCyan, CircleShape)
                            .clickable {
                                onRequestGpsLocation()
                                onNavigateHome()
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("cities_use_gps_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = stringResource(R.string.detect_gps_location),
                            tint = palette.accentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.detect_gps_location),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentCyan
                        )
                    }
                }
            }

            // Live Open-Meteo Geocoding Search Field
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cities_search_input"),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_cities_hint),
                            color = palette.mutedText
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search City",
                            tint = palette.accentCyan
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.isSearchingCities) {
                                WeatherOrbitalLoadingIndicator(
                                    sizeDp = 24.dp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Clear search",
                                        tint = palette.secondaryText
                                    )
                                }
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            onExecuteCitySearch()
                        }
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = palette.primaryText,
                        unfocusedTextColor = palette.primaryText,
                        focusedContainerColor = palette.pillBackground,
                        unfocusedContainerColor = palette.pillBackground,
                        focusedBorderColor = palette.accentCyan,
                        unfocusedBorderColor = palette.cardBorderLight
                    )
                )
            }

            // Invalid City Name / Search Error Feedback
            if (uiState.searchErrorMessage != null) {
                item {
                    GlassmorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("city_search_error_card"),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WarningAmber,
                                contentDescription = "Search Notice",
                                tint = palette.accentGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = uiState.searchErrorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.primaryText
                            )
                        }
                    }
                }
            }

            // Live Open-Meteo Geocoding Search Suggestions
            if (uiState.geocodingSuggestions.isNotEmpty()) {
                item {
                    GlassmorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("geocoding_suggestions_card"),
                        isHighlighted = true,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Public,
                                    contentDescription = null,
                                    tint = palette.accentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Open-Meteo Geocoding Matches (${uiState.geocodingSuggestions.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accentCyan
                                )
                            }

                            uiState.geocodingSuggestions.forEachIndexed { idx, suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            focusManager.clearFocus()
                                            onSelectGeocodedLocation(suggestion)
                                        }
                                        .padding(vertical = 10.dp, horizontal = 8.dp)
                                        .testTag("geocoding_suggestion_${suggestion.id}"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            tint = palette.accentCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = suggestion.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.primaryText
                                            )
                                            Text(
                                                text = suggestion.formattedRegionAndCountry,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = palette.secondaryText
                                            )
                                        }
                                    }

                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "%.2f°, %.2f°",
                                            suggestion.latitude,
                                            suggestion.longitude
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.mutedText
                                    )
                                }

                                if (idx < uiState.geocodingSuggestions.lastIndex) {
                                    HorizontalDivider(color = palette.cardBorderSubtle)
                                }
                            }
                        }
                    }
                }
            }

            // Saved Cities Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saved & Favorite Locations (${uiState.cities.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.primaryText
                    )
                    Text(
                        text = "Tap ★ to favorite",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accentGold
                    )
                }
            }

            // Saved Cities List (Favorites sorted first)
            val sortedCities = uiState.cities.sortedByDescending {
                uiState.favoriteCityIds.contains(it.id) || it.isFavorite
            }
            items(items = sortedCities, key = { it.id }) { city ->
                val isSelected = city.id == uiState.selectedCityId
                val isFavorite = uiState.favoriteCityIds.contains(city.id) || city.isFavorite
                val isDefaultCity = uiState.settings.defaultCityId == city.id
                SavedCityGlassCard(
                    city = city,
                    isSelected = isSelected,
                    isFavorite = isFavorite,
                    isDefaultCity = isDefaultCity,
                    temperatureUnit = unit,
                    animationsEnabled = animationsEnabled,
                    canRemove = uiState.cities.size > 1,
                    onSelect = {
                        onSelectCity(city.id)
                        onNavigateHome()
                    },
                    onToggleFavorite = { onToggleFavoriteCity(city.id) },
                    onSetDefault = { onSetDefaultCity(city.id) },
                    onRemove = { onRemoveCity(city.id) }
                )
            }

            // Quick-Add World Cities Section
            if (availableToAdd.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Explore,
                            contentDescription = null,
                            tint = palette.accentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Popular World Cities",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.primaryText
                        )
                    }
                }

                items(items = availableToAdd, key = { it.id }) { discoverCity ->
                    GlassmorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_city_card_${discoverCity.id}"),
                        onClick = {
                            onAddCity(discoverCity)
                            onNavigateHome()
                        },
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                WeatherConditionArtworkIcon(
                                    condition = discoverCity.condition,
                                    iconSize = 40.dp,
                                    animationsEnabled = animationsEnabled
                                )
                                Column {
                                    Text(
                                        text = discoverCity.cityName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primaryText
                                    )
                                    Text(
                                        text = discoverCity.regionAndCountry,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.secondaryText
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(palette.accentCyan.copy(alpha = 0.22f))
                                    .border(1.dp, palette.accentCyan, CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = stringResource(R.string.add_city_button),
                                    tint = palette.accentCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.add_city_button),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.accentCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedCityGlassCard(
    city: CityWeatherData,
    isSelected: Boolean,
    isFavorite: Boolean,
    isDefaultCity: Boolean,
    temperatureUnit: TemperatureUnit,
    animationsEnabled: Boolean,
    canRemove: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetDefault: () -> Unit,
    onRemove: () -> Unit
) {
    val palette = LocalGlassPalette.current

    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_city_card_${city.id}"),
        isHighlighted = isSelected,
        onClick = onSelect,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = city.cityName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                        if (city.isCurrentLocation) {
                            Icon(
                                imageVector = Icons.Filled.MyLocation,
                                contentDescription = "GPS City",
                                tint = palette.accentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Active City",
                                tint = palette.accentEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    val sourceLabel = when {
                        city.isLiveData -> "Live API"
                        city.isCachedOfflineData -> "Offline Cache (${city.cachedTimestampLabel ?: "Saved"})"
                        else -> city.currentTimeDisplay
                    }
                    Text(
                        text = "$sourceLabel • ${city.regionAndCountry}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.secondaryText
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("favorite_city_button_${city.id}")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = if (isFavorite) "Remove ${city.cityName} from favorites" else "Add ${city.cityName} to favorites",
                            tint = if (isFavorite) palette.accentGold else palette.mutedText,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    WeatherConditionArtworkIcon(
                        condition = city.condition,
                        iconSize = 46.dp,
                        animationsEnabled = animationsEnabled
                    )
                    Text(
                        text = formatTemperature(city.currentTempCelsius, temperatureUnit),
                        style = MaterialTheme.typography.displayMedium,
                        color = palette.primaryText
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isDefaultCity) palette.accentCyan.copy(alpha = 0.22f)
                                else palette.pillBackground
                            )
                            .border(
                                width = 1.dp,
                                color = if (isDefaultCity) palette.accentCyan else palette.cardBorderSubtle,
                                shape = CircleShape
                            )
                            .clickable(onClick = onSetDefault)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("default_city_badge_${city.id}")
                    ) {
                        Text(
                            text = if (isDefaultCity) "Default City ✓" else "Set Default",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isDefaultCity) FontWeight.Bold else FontWeight.Medium,
                            color = if (isDefaultCity) palette.accentCyan else palette.secondaryText
                        )
                    }

                    Text(
                        text = city.conditionDescription,
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.accentCyan,
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.high_low_temp,
                            formatTemperature(city.highTempCelsius, temperatureUnit),
                            formatTemperature(city.lowTempCelsius, temperatureUnit)
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.secondaryText
                    )

                    if (canRemove && !isSelected) {
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("remove_city_${city.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Remove ${city.cityName}",
                                tint = palette.mutedText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
