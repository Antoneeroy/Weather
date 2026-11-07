package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager as SystemLocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

sealed class LocationFetchResult {
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val cityName: String,
        val regionAndCountry: String
    ) : LocationFetchResult()

    data object PermissionDenied : LocationFetchResult()
    data object LocationDisabled : LocationFetchResult()
    data class LocationUnavailable(val message: String) : LocationFetchResult()
}

/**
 * Manages Android runtime location permission checks, FusedLocationProviderClient GPS retrieval,
 * and reverse geocoding to determine the user's current city name.
 */
class LocationManager(private val context: Context) {

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun hasLocationPermission(): Boolean {
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }

    fun isLocationServicesEnabled(): Boolean {
        val sysManager = context.getSystemService(Context.LOCATION_SERVICE) as? SystemLocationManager
            ?: return false
        return runCatching {
            sysManager.isProviderEnabled(SystemLocationManager.GPS_PROVIDER) ||
                sysManager.isProviderEnabled(SystemLocationManager.NETWORK_PROVIDER)
        }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentDeviceLocation(): LocationFetchResult = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            return@withContext LocationFetchResult.PermissionDenied
        }

        if (!isLocationServicesEnabled()) {
            return@withContext LocationFetchResult.LocationDisabled
        }

        try {
            // 1. Try fresh current location with timeout
            val freshLocation: Location? = withTimeoutOrNull(7500L) {
                suspendCancellableCoroutine { cont ->
                    val tokenSource = CancellationTokenSource()
                    cont.invokeOnCancellation { tokenSource.cancel() }
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        tokenSource.token
                    ).addOnSuccessListener { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }.addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            }

            // 2. Fallback to lastLocation or Android SystemLocationManager lastKnownLocation
            val resolvedLocation = freshLocation ?: fetchLastKnownLocationFallback()

            if (resolvedLocation == null) {
                return@withContext LocationFetchResult.LocationUnavailable(
                    "Unable to acquire a GPS fix right now. Try moving outdoors or searching a city manually."
                )
            }

            val (cityName, regionCountry) = reverseGeocodeCoordinates(
                resolvedLocation.latitude,
                resolvedLocation.longitude
            )

            LocationFetchResult.Success(
                latitude = resolvedLocation.latitude,
                longitude = resolvedLocation.longitude,
                cityName = cityName,
                regionAndCountry = regionCountry
            )
        } catch (se: SecurityException) {
            LocationFetchResult.PermissionDenied
        } catch (e: Exception) {
            LocationFetchResult.LocationUnavailable(
                e.localizedMessage ?: "Location service error occurred."
            )
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchLastKnownLocationFallback(): Location? {
        val fusedLast = withTimeoutOrNull(3000L) {
            suspendCancellableCoroutine<Location?> { cont ->
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            }
        }
        if (fusedLast != null) return fusedLast

        val sysManager = context.getSystemService(Context.LOCATION_SERVICE) as? SystemLocationManager
            ?: return null
        return runCatching {
            sysManager.getLastKnownLocation(SystemLocationManager.GPS_PROVIDER)
                ?: sysManager.getLastKnownLocation(SystemLocationManager.NETWORK_PROVIDER)
                ?: sysManager.getLastKnownLocation(SystemLocationManager.PASSIVE_PROVIDER)
        }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocodeCoordinates(lat: Double, lon: Double): Pair<String, String> {
        return try {
            if (!Geocoder.isPresent()) {
                return Pair(
                    "Current Location",
                    String.format(Locale.US, "%.2f°, %.2f°", lat, lon)
                )
            }
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lon, 1)
            } else {
                geocoder.getFromLocation(lat, lon, 1)
            }
            val first = addresses?.firstOrNull()
            if (first != null) {
                val city = first.locality
                    ?: first.subAdminArea
                    ?: first.adminArea
                    ?: "Current Location"
                val region = listOfNotNull(first.adminArea, first.countryName)
                    .distinct()
                    .joinToString(", ")
                    .ifBlank { String.format(Locale.US, "%.2f°, %.2f°", lat, lon) }
                Pair(city, region)
            } else {
                Pair("Current Location", String.format(Locale.US, "%.2f°, %.2f°", lat, lon))
            }
        } catch (_: Exception) {
            Pair("Current Location", String.format(Locale.US, "%.2f°, %.2f°", lat, lon))
        }
    }
}
