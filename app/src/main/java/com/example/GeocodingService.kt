package com.example

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo Geocoding API Service
 * Endpoint: https://geocoding-api.open-meteo.com/v1/search
 */
interface GeocodingService {

    @GET("v1/search")
    suspend fun searchLocations(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): GeocodingSearchResponse

    companion object {
        const val BASE_URL = "https://geocoding-api.open-meteo.com/"

        fun create(client: OkHttpClient = WeatherApiService.createDefaultOkHttpClient()): GeocodingService {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(GeocodingService::class.java)
        }
    }
}

data class GeocodingSearchResponse(
    @Json(name = "results") val results: List<GeocodingLocationDto>? = null
)

data class GeocodingLocationDto(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "name") val name: String = "",
    @Json(name = "latitude") val latitude: Double = 0.0,
    @Json(name = "longitude") val longitude: Double = 0.0,
    @Json(name = "elevation") val elevation: Double? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
    @Json(name = "admin1") val admin1: String? = null,
    @Json(name = "population") val population: Long? = null
) {
    val formattedRegionAndCountry: String
        get() = listOfNotNull(admin1?.takeIf { it.isNotBlank() }, country?.takeIf { it.isNotBlank() })
            .distinct()
            .joinToString(", ")
            .ifBlank { "Global Coordinates (${String.format("%.2f", latitude)}, ${String.format("%.2f", longitude)})" }

    val uniqueCityKey: String
        get() = "geo_${id}_${name.lowercase().replace(" ", "_")}"
}
