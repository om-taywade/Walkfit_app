@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@file:Suppress("DEPRECATION", "MissingPermission", "UnusedImport", "UnresolvedReference")

package `in`.tom.walkfit

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import `in`.tom.walkfit.model.HourlyScore
import `in`.tom.walkfit.model.WeatherCondition
import `in`.tom.walkfit.model.WalkScoreData
import `in`.tom.walkfit.service.StepCounterService
import `in`.tom.walkfit.ui.City
import `in`.tom.walkfit.ui.WeatherState
import `in`.tom.walkfit.ui.WeatherViewModel
import `in`.tom.walkfit.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Request permissions first before starting the service
        requestRequiredPermissions()
        
        // Start the step counter service safely
        try {
            startStepCounterService()
        } catch (e: Exception) {
            e.printStackTrace()
            // Service failed to start, but we can continue with the app
        }

        // Load the saved theme preference
        val sharedPrefs = getSharedPreferences("WalkFit_prefs", Context.MODE_PRIVATE)
        val savedDarkTheme = sharedPrefs.getBoolean("dark_theme", false)
        
        setContent {
            val isDarkTheme = remember { mutableStateOf(savedDarkTheme) }
            
            // Save theme preference when changed
            DisposableEffect(isDarkTheme.value) {
                sharedPrefs.edit()
                    .putBoolean("dark_theme", isDarkTheme.value)
                    .apply()
                onDispose { }
            }
            
            WalkfitTheme(darkTheme = isDarkTheme.value) {
                val viewModel: WeatherViewModel = viewModel()
                val showSettings = remember { mutableStateOf(false) }
                
                if (showSettings.value) {
                    SettingsScreen(
                        onBackPressed = { showSettings.value = false },
                        isDarkTheme = isDarkTheme.value,
                        onThemeChanged = { isDarkTheme.value = it }
                    )
                } else {
                    WeatherScreen(
                        viewModel = viewModel,
                        onSettingsClick = { showSettings.value = true }
                    )
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACTIVITY_RECOGNITION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.VIBRATE,
                    Manifest.permission.FOREGROUND_SERVICE
                ),
                1001
            )
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.VIBRATE
                ),
                1001
            )
        }
    }

    private fun startStepCounterService() {
        val serviceIntent = Intent(this, StepCounterService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    onSettingsClick: () -> Unit
) {
    val weatherState by viewModel.weatherState.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()
    val allCities by viewModel.allCities.collectAsState()
    val stepCount by viewModel.stepCount.collectAsState()
    val context = LocalContext.current
    
    // Pull to refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    
    fun provideHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (e: Exception) {
            // Silently handle any vibration errors
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            Column {
                Spacer(modifier = Modifier.height(32.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    StepCounterBar(
                        stepCount = stepCount,
                        modifier = Modifier.padding(end = 48.dp)
                    )
                    
                    IconButton(
                        onClick = {
                            onSettingsClick()
                            provideHapticFeedback()
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                CitySelector(
                    cities = allCities,
                    selectedCity = selectedCity,
                    onCitySelected = { 
                        viewModel.setCity(it)
                        provideHapticFeedback()
                    },
                    onAddCity = { city ->
                        viewModel.addCity(city)
                        provideHapticFeedback()
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = weatherState) {
                WeatherState.Loading -> {
                    LoadingScreen()
                }
                is WeatherState.Success -> {
                    AnimatedContent(
                        targetState = state.data,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith 
                            fadeOut(animationSpec = tween(300))
                        }
                    ) { data ->
                        WeatherForecastList(
                            walkScores = data,
                            onRefresh = {
                                isRefreshing = true
                                viewModel.setCity(selectedCity)
                                provideHapticFeedback()
                            }
                        )
                    }
                }
                is WeatherState.Error -> {
                    ErrorScreen(
                        message = state.message
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Skeleton loading for title
        item {
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush())
            )
        }
        
        // Skeleton loading for weather cards
        items(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(shimmerBrush())
            )
        }
    }
}

@Composable
fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
    )
    
    val transition = rememberInfiniteTransition()
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
}

@Composable
fun StepCounterBar(
    stepCount: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("WalkFit_prefs", Context.MODE_PRIVATE)
    val stepGoal = sharedPrefs.getString("step_goal", "10000")?.toIntOrNull() ?: 10000

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title and current steps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_footsteps),
                        contentDescription = "Steps",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Today's Progress",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "$stepCount steps",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Progress bar and goal
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Progress bar
                val progress = (stepCount / stepGoal.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (progress >= 1f) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                    )
                }
                
                // Goal text and percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Goal: $stepGoal steps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = if (progress >= 1f) "Completed!" else "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (progress >= 1f) 
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun CitySelector(
    cities: List<City>,
    selectedCity: City,
    onCitySelected: (City) -> Unit,
    onAddCity: (City) -> Unit
) {
    var showAddCityDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val isSearching by remember { mutableStateOf(false) }
    var showSearchTab by remember { mutableStateOf(false) }
    
    if (showAddCityDialog) {
        AlertDialog(
            onDismissRequest = { showAddCityDialog = false },
            title = { Text("Add City") },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("City Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search"
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Warning about adding cities without coordinates
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Note: Adding a custom city will use default coordinates. Weather data may not be accurate.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "For accurate weather data, please select from the predefined cities.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (searchQuery.isNotBlank()) {
                            try {
                                // Create a new city with the search query and default coordinates
                                val newCity = City(
                                    searchQuery, // name
                                    0.0, // latitude
                                    0.0, // longitude
                                    "Asia/Kolkata" // timezone
                                )
                                onAddCity(newCity)
                                showAddCityDialog = false
                                searchQuery = ""
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    enabled = searchQuery.isNotBlank()
                ) {
                    Text("Add Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCityDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current City: ${selectedCity.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Row {
                    // Search button
                    IconButton(onClick = { showSearchTab = !showSearchTab }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Cities",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    // Add city button
                    IconButton(onClick = { showAddCityDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add City",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Search tab
            AnimatedVisibility(
                visible = showSearchTab,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Search field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search for a city...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear"
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    
                    // Helper text
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search for cities from our database with accurate weather data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }
                    
                    // Show search results if query is not empty
                    if (searchQuery.isNotEmpty()) {
                        val filteredCities = cities.filter { 
                            it.name.contains(searchQuery, ignoreCase = true) 
                        }
                        
                        if (filteredCities.isNotEmpty()) {
                            Text(
                                text = "Found ${filteredCities.size} cities:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, start = 4.dp, bottom = 4.dp)
                            )
                            
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredCities) { city ->
                                    CityChip(
                                        city = city,
                                        isSelected = city.name == selectedCity.name,
                                        onClick = { 
                                            onCitySelected(city)
                                            searchQuery = ""
                                            showSearchTab = false
                                        }
                                    )
                                }
                            }
                        } else {
                            // No results found
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No cities found in our database",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Button(
                                    onClick = { showAddCityDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Custom City")
                                }
                            }
                        }
                    }
                }
            }
            
            // Regular city chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cities) { city ->
                    CityChip(
                        city = city,
                        isSelected = city.name == selectedCity.name,
                        onClick = { onCitySelected(city) }
                    )
                }
            }
        }
    }
}

@Composable
fun CityChip(
    city: City,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 0.dp else 2.dp,
        shadowElevation = if (isSelected) 4.dp else 0.dp,
        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) else null,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = city.name,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun WeatherForecastList(
    walkScores: List<WalkScoreData>,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Best Days for Walking",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        items(walkScores) { score ->
            WeatherDayItem(walkScore = score)
        }
        
        // Use onRefresh in pull-to-refresh implementation
        item {
            Button(
                onClick = onRefresh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Refresh Data")
            }
        }
    }
}

@SuppressLint("SimpleDateFormat")
@Composable
fun WeatherDayItem(walkScore: WalkScoreData) {
    val dayFormatter = SimpleDateFormat("EEEE", Locale.getDefault()) // For day name
    val dateFormatter = SimpleDateFormat("MMM d", Locale.getDefault()) // For date (e.g., "Jan 15")
    val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    var isExpanded by remember { mutableStateOf(false) }
    val date = dateParser.parse(walkScore.date)!!

    // Create gradient colors based on score
    val gradientColors = when (walkScore.score) {
        in 80..100 -> ExcellentScoreGradient
        in 60..79 -> GoodScoreGradient
        in 40..59 -> ModerateScoreGradient
        in 20..39 -> PoorScoreGradient
        else -> BadScoreGradient
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(gradientColors))
            .animateContentSize()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(gradientColors))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dayFormatter.format(date),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Text(
                        text = dateFormatter.format(date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                ScoreIndicator(score = walkScore.score)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            WeatherInfo(walkScore = walkScore)
            
            if (walkScore.reasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                ReasonsList(reasons = walkScore.reasons)
            }
            
            if (isExpanded && walkScore.hourlyScores.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Hourly Walk Suitability",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SimpleHourlyChart(hourlyScores = walkScore.hourlyScores)
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap to collapse",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (!isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap to see hourly forecast",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SimpleHourlyChart(hourlyScores: List<HourlyScore>) {
    Surface(
        color = Color.Black,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Chart area with labels
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Y-axis labels
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 4 downTo 0) {
                        Text(
                            text = "${i * 25}%",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
                
                // Chart canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val barWidth = width / hourlyScores.size - 4
                    
                    // Draw horizontal grid lines
                    val gridColor = Color.White.copy(alpha = 0.2f)
                    for (i in 0..4) {
                        val y = height - (height * i / 4)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }
                    
                    // Draw bars
                    hourlyScores.forEachIndexed { index, score ->
                        val x = index * (width / hourlyScores.size) + (width / hourlyScores.size - barWidth) / 2
                        val barHeight = (score.score / 100f) * height
                        
                        // Bar color based on score
                        val barColor = when (score.score) {
                            in 80..100 -> WalkfitGreen
                            in 60..79 -> WalkfitLightGreen
                            in 40..59 -> WalkfitOrange
                            in 20..39 -> WalkfitLightOrange
                            else -> WalkfitRed
                        }
                        
                        drawRect(
                            color = barColor,
                            topLeft = Offset(x, height - barHeight),
                            size = Size(barWidth, barHeight)
                        )
                    }
                }
            }
            
            // X-axis labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Show only a subset of hours for readability
                val displayHours = listOf(0, 6, 12, 18, 23)
                displayHours.forEach { hour ->
                    Text(
                        text = when {
                            hour == 0 -> "12 AM"
                            hour < 12 -> "$hour AM"
                            hour == 12 -> "12 PM"
                            else -> "${hour - 12} PM"
                        },
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ScoreIndicator(score: Int) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.2f)
    ) {
        Text(
            text = "$score%",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun WeatherInfo(walkScore: WalkScoreData) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Weather condition
        WeatherInfoRow(
            icon = "☀️",
            text = WeatherCondition.entries
                .find { it.code == walkScore.weatherCode }?.description ?: "Unknown"
        )
        
        // Temperature
        WeatherInfoRow(
            icon = "🌡️",
            text = "${walkScore.tempMax.roundToInt()}°/${walkScore.tempMin.roundToInt()}°"
        )
        
        // UV Index
        WeatherInfoRow(
            icon = "☀️",
            text = "UV Index: ${walkScore.uvIndex.roundToInt()}",
            description = getUVDescription(walkScore.uvIndex)
        )
        
        // Rain chance
        WeatherInfoRow(
            icon = "🌧️",
            text = "Rain: ${walkScore.precipitationProb}%"
        )
        
        // Wind speed
        WeatherInfoRow(
            icon = "💨",
            text = "Wind: ${walkScore.windSpeed.roundToInt()} km/h"
        )

        // Optimal walking times
        if (walkScore.optimalWalkingTimes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            OptimalWalkingTimes(times = walkScore.optimalWalkingTimes)
        }
    }
}

@Composable
fun WeatherInfoRow(
    icon: String,
    text: String,
    description: String? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(24.dp)
        ) {
            Text(
                text = icon,
                modifier = Modifier.padding(4.dp),
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = text,
                color = Color.White
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun OptimalWalkingTimes(times: List<String>) {
    Surface(
        color = Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Best Times for Walking",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                times.forEach { time ->
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = time,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

private fun getUVDescription(uvIndex: Double): String {
    return when {
        uvIndex >= 11 -> "Extreme - Extra precautions needed"
        uvIndex >= 8 -> "Very High - Minimize sun exposure"
        uvIndex >= 6 -> "High - Protection required"
        uvIndex >= 3 -> "Moderate - Some protection needed"
        else -> "Low - Minimal protection needed"
    }
}

@Composable
fun ReasonsList(reasons: List<String>) {
    Surface(
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            reasons.forEach { reason ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit,
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    
    // User data state
    var userName by remember { mutableStateOf("") }
    var userAge by remember { mutableStateOf("") }
    var stepGoal by remember { mutableStateOf("10000") }
    
    // Load saved preferences
    LaunchedEffect(Unit) {
        val sharedPrefs = context.getSharedPreferences("WalkFit_prefs", Context.MODE_PRIVATE)
        userName = sharedPrefs.getString("user_name", "") ?: ""
        userAge = sharedPrefs.getString("user_age", "") ?: ""
        stepGoal = sharedPrefs.getString("step_goal", "10000") ?: "10000"
    }
    
    // Save preferences function
    fun savePreferences() {
        val sharedPrefs = context.getSharedPreferences("WalkFit_prefs", Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString("user_name", userName)
            putString("user_age", userAge)
            putString("step_goal", stepGoal)
            apply()
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "Settings",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Profile Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "User Profile",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        OutlinedTextField(
                            value = userName,
                            onValueChange = { userName = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = userAge,
                            onValueChange = { 
                                if (it.isEmpty() || it.toIntOrNull() != null) {
                                    userAge = it 
                                }
                            },
                            label = { Text("Age") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = stepGoal,
                            onValueChange = { 
                                if (it.isEmpty() || it.toIntOrNull() != null) {
                                    stepGoal = it 
                                }
                            },
                            label = { Text("Daily Step Goal") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        
                        Button(
                            onClick = { savePreferences() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
            
            // Add spacing at the bottom
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // About Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "Developed by Om Taywade",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Text(
                            text = "LinkedIn Profile",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            ),
                            modifier = Modifier.clickable {
                                uriHandler.openUri("https://www.linkedin.com/in/omtaywade/")
                            }
                        )
                        
                        Text(
                            text = "WalkFit helps you find the best days and times for walking based on weather conditions. It also tracks your daily steps to help you stay active.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Text(
                            text = "Version 1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            // Add theme toggle in the first Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Appearance",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Dark Theme",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = onThemeChanged,
                                thumbContent = {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    ) {
                                        Text(
                                            text = if (isDarkTheme) "🌙" else "☀️",
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}