package `in`.tom.walkfit.model

enum class WeatherCondition(val code: Int, val description: String) {
    CLEAR(0, "Clear sky"),
    PARTLY_CLOUDY(1, "Partly cloudy"),
    CLOUDY(2, "Cloudy"),
    FOG(3, "Fog"),
    DRIZZLE(4, "Drizzle"),
    RAIN(5, "Rain"),
    SNOW(6, "Snow"),
    THUNDERSTORM(7, "Thunderstorm"),
    UNKNOWN(-1, "Unknown")
} 