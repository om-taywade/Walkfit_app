package `in`.tom.walkfit.model

data class WalkScoreData(
    val date: String,
    val score: Int,
    val weatherCode: Int,
    val tempMax: Double,
    val tempMin: Double,
    val precipitationProb: Int,
    val windSpeed: Double,
    val uvIndex: Double,
    val humidity: Double,
    val reasons: List<String>,
    val hourlyScores: List<HourlyScore> = emptyList(),
    val optimalWalkingTimes: List<String> = emptyList()
)

data class HourlyScore(
    val hour: Int,
    val score: Int,
    val temperature: Double,
    val humidity: Double,
    val uvIndex: Double,
    val isOptimalTime: Boolean = false
) 