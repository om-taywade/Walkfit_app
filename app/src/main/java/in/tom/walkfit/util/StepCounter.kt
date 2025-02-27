package `in`.tom.walkfit.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar
import kotlin.math.sqrt

class StepCounter(private val context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    private val _stepCount = MutableStateFlow(0)
    val stepCount: StateFlow<Int> = _stepCount
    
    private var initialSteps: Int? = null
    private var lastDayReset = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    
    // Accelerometer-based step detection variables
    private var lastAcceleration = 0f
    private var currentAcceleration = 0f
    private var acceleration = 0f
    private val accelerationThreshold = 10f
    private var lastStepTime = 0L
    private val minStepInterval = 250L
    
    // Persistence
    private val sharedPrefs = context.getSharedPreferences("WalkFit_prefs", Context.MODE_PRIVATE)
    
    init {
        loadSavedStepData()
    }
    
    private fun loadSavedStepData() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val savedDay = sharedPrefs.getInt("last_step_day", -1)
        val savedSteps = sharedPrefs.getInt("step_count", 0)
        val savedInitialSteps = sharedPrefs.getInt("initial_steps", -1)
        
        if (savedDay == today) {
            _stepCount.value = savedSteps
            if (savedInitialSteps != -1) {
                initialSteps = savedInitialSteps
            }
        } else {
            // New day, reset counters
            _stepCount.value = 0
            initialSteps = null
            saveStepData(0, null)
        }
        lastDayReset = today
    }
    
    private fun saveStepData(steps: Int, initialStepsValue: Int?) {
        sharedPrefs.edit()
            .putInt("step_count", steps)
            .putInt("last_step_day", Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
            .apply()
        
        if (initialStepsValue != null) {
            sharedPrefs.edit().putInt("initial_steps", initialStepsValue).apply()
        }
    }
    
    fun start() {
        if (hasStepCountPermission()) {
            when {
                stepCounterSensor != null -> {
                    sensorManager.registerListener(
                        this,
                        stepCounterSensor,
                        SensorManager.SENSOR_DELAY_NORMAL
                    )
                }
                stepDetectorSensor != null -> {
                    sensorManager.registerListener(
                        this,
                        stepDetectorSensor,
                        SensorManager.SENSOR_DELAY_NORMAL
                    )
                }
                accelerometerSensor != null -> {
                    sensorManager.registerListener(
                        this,
                        accelerometerSensor,
                        SensorManager.SENSOR_DELAY_NORMAL
                    )
                }
            }
        }
    }
    
    fun stop() {
        sensorManager.unregisterListener(this)
        saveStepData(_stepCount.value, initialSteps)
    }
    
    private fun hasStepCountPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        if (currentDay != lastDayReset) {
            // New day started
            initialSteps = null
            _stepCount.value = 0
            lastDayReset = currentDay
            saveStepData(0, null)
        }
        
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> handleStepCounterSensor(event)
            Sensor.TYPE_STEP_DETECTOR -> handleStepDetectorSensor()
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometerSensor(event)
        }
    }
    
    private fun handleStepCounterSensor(event: SensorEvent) {
        val currentSteps = event.values[0].toInt()
        
        if (initialSteps == null) {
            initialSteps = currentSteps
            saveStepData(_stepCount.value, initialSteps)
        }
        
        val stepsToday = currentSteps - (initialSteps ?: currentSteps)
        _stepCount.value = stepsToday
        saveStepData(stepsToday, initialSteps)
    }
    
    private fun handleStepDetectorSensor() {
        _stepCount.value = _stepCount.value + 1
        saveStepData(_stepCount.value, initialSteps)
    }
    
    private fun handleAccelerometerSensor(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        
        lastAcceleration = currentAcceleration
        currentAcceleration = sqrt(x * x + y * y + z * z)
        
        val delta = currentAcceleration - lastAcceleration
        acceleration = acceleration * 0.9f + delta
        
        val currentTime = System.currentTimeMillis()
        
        if (acceleration > accelerationThreshold &&
            currentTime - lastStepTime > minStepInterval) {
            _stepCount.value = _stepCount.value + 1
            lastStepTime = currentTime
            saveStepData(_stepCount.value, initialSteps)
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for step counting
    }
    
    companion object {
        fun isStepCountingAvailable(context: Context): Boolean {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            
            return stepCounter != null || stepDetector != null || accelerometer != null
        }
    }
} 