package `in`.tom.walkfit.model

import com.squareup.moshi.Json

data class WeatherResponse(
    val daily: DailyWeather
)

data class DailyWeather(
    val time: List<String>,
    @Json(name = "weathercode") val weatherCode: List<Int>,
    @Json(name = "temperature_2m_max") val temperatureMax: List<Double>,
    @Json(name = "temperature_2m_min") val temperatureMin: List<Double>,
    @Json(name = "precipitation_probability_max") val precipitationProbability: List<Int>,
    @Json(name = "windspeed_10m_max") val windSpeed: List<Double>
) 