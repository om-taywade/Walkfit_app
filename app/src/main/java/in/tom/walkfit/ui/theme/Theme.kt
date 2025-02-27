package `in`.tom.walkfit.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Custom color scheme for Walkfit
private val WalkfitLightColorScheme = lightColorScheme(
    primary = WalkfitGreen,
    onPrimary = Color.White,
    primaryContainer = WalkfitLightGreen,
    onPrimaryContainer = WalkfitDarkGreen,
    secondary = WalkfitBlue,
    onSecondary = Color.White,
    secondaryContainer = WalkfitLightBlue,
    onSecondaryContainer = WalkfitDarkBlue,
    tertiary = WalkfitOrange,
    onTertiary = Color.White,
    tertiaryContainer = WalkfitLightOrange,
    onTertiaryContainer = WalkfitDarkOrange,
    error = WalkfitRed,
    onError = Color.White,
    errorContainer = WalkfitLightRed,
    onErrorContainer = WalkfitDarkRed,
    background = Color.White,
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E)
)

private val WalkfitDarkColorScheme = darkColorScheme(
    primary = WalkfitGreen,
    onPrimary = Color.Black,
    primaryContainer = WalkfitDarkGreen,
    onPrimaryContainer = WalkfitLightGreen,
    secondary = WalkfitBlue,
    onSecondary = Color.Black,
    secondaryContainer = WalkfitDarkBlue,
    onSecondaryContainer = WalkfitLightBlue,
    tertiary = WalkfitOrange,
    onTertiary = Color.Black,
    tertiaryContainer = WalkfitDarkOrange,
    onTertiaryContainer = WalkfitLightOrange,
    error = WalkfitRed,
    onError = Color.Black,
    errorContainer = WalkfitDarkRed,
    onErrorContainer = WalkfitLightRed,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF121212),
    onSurfaceVariant = Color(0xFFE7E0EC),
    outline = Color(0xFF938F99)
)

@Composable
fun WalkfitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> WalkfitDarkColorScheme
        else -> WalkfitLightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}