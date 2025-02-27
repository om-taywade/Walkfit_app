package `in`.tom.walkfit.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.tom.walkfit.data.WeatherService
import `in`.tom.walkfit.data.DailyWeather
import `in`.tom.walkfit.data.HourlyWeather
import `in`.tom.walkfit.model.HourlyScore
import `in`.tom.walkfit.model.WalkScoreData
import `in`.tom.walkfit.util.StepCounter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

sealed class WeatherState {
    data object Loading : WeatherState()
    data class Success(val data: List<WalkScoreData>) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

data class City(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String = "Asia/Kolkata"
)

@HiltViewModel
class WeatherViewModel @Inject constructor(
    application: Application,
    private val weatherService: WeatherService
) : AndroidViewModel(application) {

    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val weatherState: StateFlow<WeatherState> = _weatherState
    
    private val _selectedCity = MutableStateFlow(indianCities[0])
    val selectedCity: StateFlow<City> = _selectedCity
    
    // Step counter
    private val stepCounter = StepCounter(application.applicationContext)
    val stepCount = stepCounter.stepCount
    
    // List of all cities including user-added cities
    private val _allCities = MutableStateFlow<List<City>>(indianCities)
    val allCities: StateFlow<List<City>> = _allCities.asStateFlow()
    
    init {
        // Load saved cities from preferences
        loadSavedCities()
        
        // Load weather for the selected city
        fetchWeatherForSelectedCity()
        
        // Start step counting
        startStepCounting()
    }
    
    private fun startStepCounting() {
        stepCounter.start()
    }
    
    override fun onCleared() {
        super.onCleared()
        stepCounter.stop()
    }
    
    private fun loadSavedCities() {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("WalkFit_prefs", Context.MODE_PRIVATE)
        val savedCitiesJson = sharedPrefs.getString("saved_cities", null)
        
        if (savedCitiesJson != null) {
            try {
                // This is a simplified approach. In a real app, you'd use a proper JSON parser
                // For now, we'll just use the predefined cities
                val combinedList = indianCities.toMutableList()
                _allCities.value = combinedList
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun saveCities() {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("WalkFit_prefs", Context.MODE_PRIVATE)
        
        // In a real app, you'd serialize the cities to JSON
        // For now, we'll just save a placeholder
        sharedPrefs.edit().putString("saved_cities", "saved").apply()
    }
    
    fun addCity(city: City) {
        val currentCities = _allCities.value.toMutableList()
        
        // Check if city already exists
        if (currentCities.none { it.name.equals(city.name, ignoreCase = true) }) {
            // If it's a custom city with default coordinates (0.0, 0.0)
            if (city.latitude == 0.0 && city.longitude == 0.0) {
                // Try to find a similar city in our predefined list
                val similarCity = indianCities.find { 
                    it.name.contains(city.name, ignoreCase = true) || 
                    city.name.contains(it.name, ignoreCase = true) 
                }
                
                if (similarCity != null) {
                    // Use the coordinates of the similar city
                    val updatedCity = city.copy(
                        latitude = similarCity.latitude,
                        longitude = similarCity.longitude
                    )
                    currentCities.add(updatedCity)
                    _allCities.value = currentCities
                    saveCities()
                    
                    // Set as selected city
                    _selectedCity.value = updatedCity
                } else {
                    // Use default coordinates for a location in India if no similar city found
                    val updatedCity = city.copy(
                        latitude = 20.5937, // Central India coordinates
                        longitude = 78.9629
                    )
                    currentCities.add(updatedCity)
                    _allCities.value = currentCities
                    saveCities()
                    
                    // Set as selected city
                    _selectedCity.value = updatedCity
                }
            } else {
                // City already has valid coordinates
                currentCities.add(city)
                _allCities.value = currentCities
                saveCities()
                
                // Set as selected city
                _selectedCity.value = city
            }
        } else {
            // City already exists, just select it
            val existingCity = currentCities.first { it.name.equals(city.name, ignoreCase = true) }
            _selectedCity.value = existingCity
        }
        
        // Fetch weather data for the selected city
        fetchWeatherForSelectedCity()
    }
    
    fun setCity(city: City) {
        _selectedCity.value = city
        fetchWeatherForSelectedCity()
    }
    
    private fun fetchWeatherForSelectedCity() {
        viewModelScope.launch {
            _weatherState.value = WeatherState.Loading
            try {
                val city = _selectedCity.value
                println("Fetching weather for ${city.name} (${city.latitude}, ${city.longitude})")
                
                val response = weatherService.getWeatherForecast(
                    latitude = city.latitude,
                    longitude = city.longitude,
                    timezone = city.timezone
                )
                
                // Log weather data for verification
                println("Weather data for ${city.name}:")
                println("Temperature: ${response.daily.temperatureMax[0]}°/${response.daily.temperatureMin[0]}°")
                println("Humidity: ${response.daily.humidity[0]}%")
                println("UV Index: ${response.daily.uvIndex[0]}")
                println("Wind Speed: ${response.daily.windSpeed[0]} km/h")
                println("Precipitation Probability: ${response.daily.precipitationProbability[0]}%")
                
                // Verify hourly data
                val today = response.hourly.time.take(24)
                val todayHumidity = response.hourly.humidity.take(24)
                val avgHumidity = todayHumidity.average()
                println("Today's hourly humidity values: ${todayHumidity.joinToString()}")
                println("Average humidity for today: $avgHumidity%")
                
                val walkScores = calculateWalkScores(response.daily, response.hourly)
                _weatherState.value = WeatherState.Success(walkScores)
            } catch (e: Exception) {
                println("Error fetching weather: ${e.message}")
                e.printStackTrace()
                _weatherState.value = WeatherState.Error("Failed to fetch weather data: ${e.message}")
            }
        }
    }

    private fun calculateWalkScores(daily: DailyWeather, hourly: HourlyWeather): List<WalkScoreData> {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        
        return daily.time.indices.map { index ->
            val date = daily.time[index]
            val tempMax = daily.temperatureMax[index]
            val tempMin = daily.temperatureMin[index]
            val precipitationProb = daily.precipitationProbability[index]
            val windSpeed = daily.windSpeed[index]
            val weatherCode = daily.weatherCode[index]
            val uvIndex = daily.uvIndex[index]
            val humidity = daily.humidity[index]

            // Validate humidity values
            val validatedHumidity = humidity.coerceIn(0.0, 100.0)
            if (humidity != validatedHumidity) {
                println("Warning: Invalid humidity value corrected from $humidity to $validatedHumidity")
            }

            val reasons = mutableListOf<String>()
            
            // Calculate hourly scores and optimal walking times
            val hourlyScoresWithTimes = calculateHourlyScores(date, hourly)
            
            // Calculate score based on number of optimal walking slots
            val optimalSlots = hourlyScoresWithTimes.count { it.isOptimalTime }
            val totalPossibleSlots = 24 // Total hours in a day
            val score = ((optimalSlots.toDouble() / totalPossibleSlots) * 100).toInt()
            
            // Add reasons based on optimal slots
            when {
                optimalSlots == 0 -> {
                    reasons.add("No suitable walking times today")
                }
                optimalSlots <= 4 -> {
                    reasons.add("Limited walking opportunities today")
                }
                optimalSlots <= 8 -> {
                    reasons.add("Several good walking times available")
                }
                optimalSlots <= 12 -> {
                    reasons.add("Many suitable walking times today")
                }
                else -> {
                    reasons.add("Excellent walking conditions throughout the day")
                }
            }
            
            // Add specific weather condition reasons
            if (tempMax > 35) {
                reasons.add("High temperatures may limit walking times")
            }
            if (precipitationProb > 70) {
                reasons.add("High chance of rain - check hourly forecast")
            }
            if (uvIndex > 8) {
                reasons.add("Very high UV index - protect your skin")
            }
            if (validatedHumidity > 90) {
                reasons.add("Extremely high humidity - consider indoor walking")
            }
            if (windSpeed > 30) {
                reasons.add("Strong winds may affect walking comfort")
            }

            val optimalTimes = findOptimalWalkingTimes(hourlyScoresWithTimes)

            WalkScoreData(
                date = date,
                score = score,
                weatherCode = weatherCode,
                tempMax = tempMax,
                tempMin = tempMin,
                precipitationProb = precipitationProb,
                windSpeed = windSpeed,
                uvIndex = uvIndex,
                humidity = validatedHumidity,
                reasons = reasons,
                hourlyScores = hourlyScoresWithTimes,
                optimalWalkingTimes = optimalTimes
            )
        }
    }
    
    private fun calculateHourlyScores(date: String, hourly: HourlyWeather): List<HourlyScore> {
        val hourlyScores = mutableListOf<HourlyScore>()
        val currentDate = LocalDate.parse(date)
        
        // Find indices for the current day (from 00:00 to 23:00)
        val hourlyIndices = hourly.time.indices.filter { i ->
            val hourlyTime = LocalDateTime.parse(hourly.time[i], DateTimeFormatter.ISO_DATE_TIME)
            hourlyTime.toLocalDate() == currentDate
        }
        
        for (i in hourlyIndices) {
            val hourlyTime = LocalDateTime.parse(hourly.time[i], DateTimeFormatter.ISO_DATE_TIME)
            val hour = hourlyTime.hour
            
            val temp = hourly.temperature[i]
            val precip = hourly.precipitationProbability[i]
            val wind = hourly.windSpeed[i]
            val code = hourly.weatherCode[i]
            val uv = hourly.uvIndex[i]
            val humidity = hourly.humidity[i].coerceIn(0.0, 100.0)
            
            var hourScore = 100
            
            // Adjusted humidity scoring for hourly values
            when {
                humidity > 90 -> hourScore -= 30
                humidity > 80 -> hourScore -= 20
                humidity > 70 -> hourScore -= 10
            }
            
            // Temperature scoring
            when {
                temp > 35 -> hourScore -= 30
                temp > 30 -> hourScore -= 20
                temp < 15 -> hourScore -= 15
            }
            
            // UV Index scoring
            when {
                uv > 8 -> hourScore -= 25
                uv > 6 -> hourScore -= 15
            }
            
            // Precipitation probability scoring
            when {
                precip > 70 -> hourScore -= 40
                precip > 40 -> hourScore -= 25
                precip > 20 -> hourScore -= 10
            }
            
            // Wind speed scoring
            when {
                wind > 30 -> hourScore -= 30
                wind > 20 -> hourScore -= 15
            }
            
            // Weather code scoring
            when (code) {
                0, 1 -> { } // Clear or mainly clear - no penalty
                2 -> hourScore -= 5
                3 -> hourScore -= 10
                in 45..48 -> hourScore -= 15
                in 51..55 -> hourScore -= 20
                in 61..65 -> hourScore -= 35
                in 71..77 -> hourScore -= 50
                in 80..82 -> hourScore -= 40
                in 95..99 -> hourScore -= 60
            }
            
            // Time of day adjustments
            when (hour) {
                in 5..8 -> hourScore += 10  // Early morning bonus
                in 9..11 -> hourScore += 5  // Morning bonus
                in 17..19 -> hourScore += 5 // Evening bonus
                in 0..4 -> hourScore -= 20  // Very early morning penalty
                in 12..15 -> {              // Midday penalty in hot weather
                    if (temp > 28) hourScore -= 15
                }
                in 20..23 -> hourScore -= 10 // Night penalty
            }
            
            // Ensure score doesn't go below 0 or above 100
            hourScore = hourScore.coerceIn(0, 100)
            
            // Determine if this is an optimal time for walking
            val isOptimalTime = hourScore >= 70 && 
                              temp in 15.0..28.0 && 
                              precip <= 20 && 
                              wind <= 20 &&
                              uv <= 6
            
            hourlyScores.add(
                HourlyScore(
                    hour = hour,
                    score = hourScore,
                    temperature = temp,
                    humidity = humidity,
                    uvIndex = uv,
                    isOptimalTime = isOptimalTime
                )
            )
        }
        
        return hourlyScores
    }

    private fun findOptimalWalkingTimes(hourlyScores: List<HourlyScore>): List<String> {
        return hourlyScores
            .filter { it.isOptimalTime }
            .map { score ->
                val hourStr = when {
                    score.hour == 0 -> "12 AM"
                    score.hour < 12 -> "${score.hour} AM"
                    score.hour == 12 -> "12 PM"
                    else -> "${score.hour - 12} PM"
                }
                hourStr
            }
    }
    
    companion object {
        val indianCities = listOf(
            // Major Metropolitan Cities
            City("Mumbai", 19.0760, 72.8777),
            City("Delhi", 28.7041, 77.1025),
            City("Bangalore", 12.9716, 77.5946),
            City("Hyderabad", 17.3850, 78.4867),
            City("Chennai", 13.0827, 80.2707),
            City("Kolkata", 22.5726, 88.3639),
            City("Pune", 18.5204, 73.8567),
            City("Ahmedabad", 23.0225, 72.5714),
            City("Jaipur", 26.9124, 75.7873),
            City("Lucknow", 26.8467, 80.9462),
            
            // Tier 2 Cities
            City("Nagpur", 21.1458, 79.0882),
            City("Kanpur", 26.4499, 80.3319),
            City("Indore", 22.7196, 75.8577),
            City("Thane", 19.2183, 72.9781),
            City("Bhopal", 23.2599, 77.4126),
            City("Visakhapatnam", 17.6868, 83.2185),
            City("Patna", 25.5941, 85.1376),
            City("Vadodara", 22.3072, 73.1812),
            City("Ghaziabad", 28.6692, 77.4538),
            City("Ludhiana", 30.9010, 75.8573),
            City("Agra", 27.1767, 78.0081),
            City("Nashik", 19.9975, 73.7898),
            City("Ranchi", 23.3441, 85.3096),
            City("Faridabad", 28.4089, 77.3178),
            City("Coimbatore", 11.0168, 76.9558),
            City("Srinagar", 34.0837, 74.7973),
            City("Amritsar", 31.6340, 74.8723),
            City("Varanasi", 25.3176, 82.9739),
            City("Kochi", 9.9312, 76.2673),
            City("Guwahati", 26.1445, 91.7362),
            City("Chandigarh", 30.7333, 76.7794),
            City("Thiruvananthapuram", 8.5241, 76.9366),
            City("Dehradun", 30.3165, 78.0322),
            City("Mysore", 12.2958, 76.6394),
            City("Puducherry", 11.9416, 79.8083),
            City("Shimla", 31.1048, 77.1734),
            City("Udaipur", 24.5854, 73.7125),
            City("Darjeeling", 27.0410, 88.2663),
            City("Ooty", 11.4102, 76.6950),
            City("Gangtok", 27.3389, 88.6065),
            
            // Tourist Destinations
            City("Jaisalmer", 26.9157, 70.9083),
            City("Rishikesh", 30.0869, 78.2676),
            City("Haridwar", 29.9457, 78.1642),
            City("Pushkar", 26.4898, 74.5434),
            City("Manali", 32.2432, 77.1892),
            City("Leh", 34.1526, 77.5771),
            City("Munnar", 10.0889, 77.0595),
            City("Goa", 15.2993, 74.1240),
            City("Hampi", 15.3350, 76.4600),
            City("Mahabalipuram", 12.6269, 80.1928),
            City("Khajuraho", 24.8318, 79.9199),
            City("Kovalam", 8.3988, 76.9727),
            City("Alappuzha", 9.4981, 76.3388),
            City("Nainital", 29.3919, 79.4542),
            City("Mussoorie", 30.4598, 78.0644),
            City("Dalhousie", 32.5387, 75.9701),
            City("Andaman Islands", 11.7401, 92.6586),
            
            // Maharashtra District Cities
            City("Mumbai Suburban", 19.0596, 72.8295),
            City("Palghar", 19.6970, 72.7698),
            City("Raigad", 18.5159, 73.1822),
            City("Ratnagiri", 16.9902, 73.3120),
            City("Sindhudurg", 16.0039, 73.4644),
            City("Dhule", 20.9042, 74.7749),
            City("Nandurbar", 21.3704, 74.2425),
            City("Jalgaon", 21.0077, 75.5626),
            City("Ahmednagar", 19.0948, 74.7480),
            City("Satara", 17.6805, 74.0183),
            City("Sangli", 16.8524, 74.5815),
            City("Solapur", 17.6599, 75.9064),
            City("Kolhapur", 16.7050, 74.2433),
            City("Aurangabad", 19.8762, 75.3433),
            City("Jalna", 19.8347, 75.8816),
            City("Beed", 18.9891, 75.7601),
            City("Latur", 18.4088, 76.5604),
            City("Osmanabad", 18.1818, 76.0400),
            City("Nanded", 19.1383, 77.3210),
            City("Parbhani", 19.2608, 76.7748),
            City("Hingoli", 19.7173, 77.1494),
            City("Akola", 20.7002, 77.0082),
            City("Washim", 20.1120, 77.1440),
            City("Amravati", 20.9320, 77.7523),
            City("Yavatmal", 20.3899, 78.1307),
            City("Buldhana", 20.5292, 76.1842),
            City("Wardha", 20.7453, 78.6022),
            City("Chandrapur", 19.9615, 79.2961),
            City("Gadchiroli", 20.1809, 80.0084),
            City("Gondia", 21.4624, 80.1971),
            City("Bhandara", 21.1669, 79.6557),
            
            // North East Cities
            City("Shillong", 25.5788, 91.8933),
            City("Aizawl", 23.7271, 92.7176),
            City("Imphal", 24.8170, 93.9368),
            City("Kohima", 25.6751, 94.1086),
            City("Itanagar", 27.0844, 93.6053),
            City("Agartala", 23.8315, 91.2868),
            City("Dimapur", 25.9091, 93.7266),
            City("Tezpur", 26.6528, 92.8029),
            City("Dibrugarh", 27.4728, 94.9120),
            City("Jorhat", 26.7509, 94.2037),
            
            // Western India
            City("Gandhinagar", 23.2156, 72.6369),
            City("Diu", 20.7144, 70.9874),
            City("Silvassa", 20.2766, 73.0166),
            City("Bhuj", 23.2419, 69.6669),
            City("Somnath", 20.8880, 70.4010),
            City("Dwarka", 22.2442, 68.9685),
            City("Lonavala", 18.7546, 73.4062),
            City("Mahabaleshwar", 17.9216, 73.6557),
            
            // Southern India
            City("Tirupati", 13.6288, 79.4192),
            City("Vellore", 12.9165, 79.1325),
            City("Thanjavur", 10.7870, 79.1378),
            City("Kanyakumari", 8.0883, 77.5385),
            City("Rameshwaram", 9.2876, 79.3129),
            City("Coorg", 12.4244, 75.7382),
            City("Chikmagalur", 13.3161, 75.7720),
            City("Mangalore", 12.9141, 74.8560),
            City("Udupi", 13.3409, 74.7421)
        )
    }
} 