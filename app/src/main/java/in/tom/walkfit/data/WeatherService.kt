package `in`.tom.walkfit.data

import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("v1/forecast")
    suspend fun getWeatherForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("timezone") timezone: String,
        @Query("daily") daily: String = "weathercode,temperature_2m_max,temperature_2m_min,precipitation_probability_max,windspeed_10m_max,uv_index_max,relative_humidity_2m_max",
        @Query("hourly") hourly: String = "temperature_2m,precipitation_probability,windspeed_10m,weathercode,uv_index,relative_humidity_2m"
    ): WeatherResponse
}

data class WeatherResponse(
    val daily: DailyWeather,
    val hourly: HourlyWeather
)

data class DailyWeather(
    val time: List<String>,
    @Json(name = "weathercode") val weatherCode: List<Int>,
    @Json(name = "temperature_2m_max") val temperatureMax: List<Double>,
    @Json(name = "temperature_2m_min") val temperatureMin: List<Double>,
    @Json(name = "precipitation_probability_max") val precipitationProbability: List<Int>,
    @Json(name = "windspeed_10m_max") val windSpeed: List<Double>,
    @Json(name = "uv_index_max") val uvIndex: List<Double>,
    @Json(name = "relative_humidity_2m_max") val humidity: List<Double>
)

data class HourlyWeather(
    val time: List<String>,
    @Json(name = "temperature_2m") val temperature: List<Double>,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int>,
    @Json(name = "windspeed_10m") val windSpeed: List<Double>,
    @Json(name = "weathercode") val weatherCode: List<Int>,
    @Json(name = "uv_index") val uvIndex: List<Double>,
    @Json(name = "relative_humidity_2m") val humidity: List<Double>
)