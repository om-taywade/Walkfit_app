package `in`.tom.walkfit.model

data class DailyForecast(
    val dates: List<String>,
    val weatherCodes: List<Int>,
    val maxTemperatures: List<Double>,
    val minTemperatures: List<Double>,
    val precipitationProbabilities: List<Int>,
    val windSpeeds: List<Double>
) 