# WalkFit - Smart Step Counter & Weather App

WalkFit is a modern Android application that combines step counting with weather insights to help users make informed decisions about their walking activities. The app features a smart step counter that runs in the background and provides weather-based recommendations for optimal walking times.

## Features

### Step Tracking
- Real-time step counting using device sensors
- Background step tracking service
- Persistent step count data across app restarts
- Daily step goal setting and progress tracking
- Automatic daily reset at midnight

### Weather Integration
- Real-time weather data
- Hourly weather forecasts
- Walk score calculation based on weather conditions
- Optimal walking time recommendations
- Multiple city support with customizable locations

### User Experience
- Modern Material Design 3 UI
- Dark/Light theme support
- Persistent user preferences
- Interactive weather cards
- Progress visualization
- Haptic feedback for interactions

## Technical Details

### Built With
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependencies**:
  - Android Jetpack Components
  - Kotlin Coroutines for asynchronous operations
  - Dagger Hilt for dependency injection
  - Material Design 3 components
  - Android Sensors API
  - Retrofit for network calls

### Key Components
- `StepCounter`: Utilizes device sensors for accurate step counting
- `StepCounterService`: Background service for continuous step tracking
- `WeatherViewModel`: Manages weather data and UI state
- `BootReceiver`: Ensures step counting resumes after device restart

## Installation

1. Clone the repository:
```bash
git clone https://github.com/omtaywade-github/Walkfit_app.git
```

2. Open the project in Android Studio

3. Build and run the app on your device or emulator

## Requirements

- Android 8.0 (API level 26) or higher
- Google Play Services
- Location permissions for weather data
- Activity recognition permissions for step counting

## Permissions Required

- `ACTIVITY_RECOGNITION`: For step counting
- `FOREGROUND_SERVICE`: For background step tracking
- `INTERNET`: For weather data
- `ACCESS_FINE_LOCATION`: For accurate weather information
- `RECEIVE_BOOT_COMPLETED`: For service auto-start

## Features in Detail

### Step Counter
- Uses multiple sensor types for accurate step counting:
  - Step Counter Sensor
  - Step Detector Sensor
  - Accelerometer (as fallback)
- Persists data across app restarts
- Automatically resets at midnight
- Runs as a foreground service with notification

### Weather Integration
- Provides real-time weather updates
- Calculates walk scores based on:
  - Temperature
  - Precipitation probability
  - Wind speed
  - UV index
  - Humidity
- Suggests optimal walking times

### User Interface
- Clean, modern design using Material Design 3
- Responsive layout
- Dark/Light theme support
- Interactive components with haptic feedback
- Progress visualization
- City management interface

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details

## Acknowledgments

- Weather data provided by Open-Meteo API
- Material Design 3 guidelines
- Android Jetpack libraries

## Contact

Om Taywade - [@omtaywade](https://github.com/omtaywade-github)

Project Link: [https://github.com/omtaywade-github/Walkfit_app](https://github.com/omtaywade-github/Walkfit_app) 