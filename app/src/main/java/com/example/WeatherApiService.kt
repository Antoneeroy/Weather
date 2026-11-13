package com.example

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Open-Meteo Forecast API Service (Phase 2 & Phase 3)
 * Endpoint: https://api.open-meteo.com/v1/forecast
 */
interface WeatherApiService {

    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = DEFAULT_CURRENT_FIELDS,
        @Query("hourly") hourly: String = DEFAULT_HOURLY_FIELDS,
        @Query("daily") daily: String = DEFAULT_DAILY_FIELDS,
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 10
    ): OpenMeteoForecastResponse

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"

        const val DEFAULT_CURRENT_FIELDS =
            "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code," +
                "surface_pressure,pressure_msl,wind_speed_10m,wind_direction_10m,wind_gusts_10m,visibility,uv_index"

        const val DEFAULT_HOURLY_FIELDS =
            "temperature_2m,apparent_temperature,relative_humidity_2m,precipitation_probability,precipitation," +
                "weather_code,wind_speed_10m,wind_direction_10m,visibility,uv_index,is_day"

        const val DEFAULT_DAILY_FIELDS =
            "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max," +
                "precipitation_sum,sunrise,sunset,uv_index_max,wind_speed_10m_max"

        fun create(client: OkHttpClient = createDefaultOkHttpClient()): WeatherApiService {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(WeatherApiService::class.java)
        }

        fun createDefaultOkHttpClient(): OkHttpClient {
            val builder = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)

            if (BuildConfig.DEBUG) {
                val logging = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
                builder.addInterceptor(logging)
            }
            return builder.build()
        }
    }
}

data class OpenMeteoForecastResponse(
    @Json(name = "latitude") val latitude: Double = 0.0,
    @Json(name = "longitude") val longitude: Double = 0.0,
    @Json(name = "timezone") val timezone: String = "GMT",
    @Json(name = "timezone_abbreviation") val timezoneAbbreviation: String = "GMT",
    @Json(name = "utc_offset_seconds") val utcOffsetSeconds: Int = 0,
    @Json(name = "current") val current: OpenMeteoCurrentDto? = null,
    @Json(name = "hourly") val hourly: OpenMeteoHourlyDto? = null,
    @Json(name = "daily") val daily: OpenMeteoDailyDto? = null
)

data class OpenMeteoCurrentDto(
    @Json(name = "time") val time: String = "",
    @Json(name = "temperature_2m") val temperature2m: Double = 0.0,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int = 50,
    @Json(name = "apparent_temperature") val apparentTemperature: Double = 0.0,
    @Json(name = "is_day") val isDay: Int = 1,
    @Json(name = "weather_code") val weatherCode: Int = 0,
    @Json(name = "surface_pressure") val surfacePressure: Double? = null,
    @Json(name = "pressure_msl") val pressureMsl: Double? = null,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double = 0.0,
    @Json(name = "wind_direction_10m") val windDirection10m: Int = 0,
    @Json(name = "wind_gusts_10m") val windGusts10m: Double? = null,
    @Json(name = "visibility") val visibilityMeters: Double? = null,
    @Json(name = "uv_index") val uvIndex: Double? = null
)

data class OpenMeteoHourlyDto(
    @Json(name = "time") val time: List<String> = emptyList(),
    @Json(name = "temperature_2m") val temperature2m: List<Double?> = emptyList(),
    @Json(name = "apparent_temperature") val apparentTemperature: List<Double?> = emptyList(),
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: List<Int?> = emptyList(),
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int?> = emptyList(),
    @Json(name = "precipitation") val precipitationMm: List<Double?> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int?> = emptyList(),
    @Json(name = "wind_speed_10m") val windSpeed10m: List<Double?> = emptyList(),
    @Json(name = "wind_direction_10m") val windDirection10m: List<Int?> = emptyList(),
    @Json(name = "visibility") val visibilityMeters: List<Double?> = emptyList(),
    @Json(name = "uv_index") val uvIndex: List<Double?> = emptyList(),
    @Json(name = "is_day") val isDay: List<Int?> = emptyList()
)

data class OpenMeteoDailyDto(
    @Json(name = "time") val time: List<String> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int?> = emptyList(),
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double?> = emptyList(),
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double?> = emptyList(),
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int?> = emptyList(),
    @Json(name = "precipitation_sum") val precipitationSumMm: List<Double?> = emptyList(),
    @Json(name = "sunrise") val sunrise: List<String?> = emptyList(),
    @Json(name = "sunset") val sunset: List<String?> = emptyList(),
    @Json(name = "uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
    @Json(name = "wind_speed_10m_max") val windSpeed10mMax: List<Double?> = emptyList()
)
