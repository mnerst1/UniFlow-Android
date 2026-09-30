package dev.uniflow.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat

private val Light = lightColorScheme(
    primary = Color(0xFF5558D9), onPrimary = Color.White,
    secondary = Color(0xFF087F88), tertiary = Color(0xFFB25183),
    background = Color(0xFFF2F5FC), surface = Color(0xFFF9FAFF),
    onBackground = Color(0xFF18233A), onSurface = Color(0xFF18233A),
    surfaceVariant = Color(0xFFE8EBF7), onSurfaceVariant = Color(0xFF515C74),
    outline = Color(0xFF737C92)
)
private val Dark = darkColorScheme(
    primary = Color(0xFFB9BBFF), secondary = Color(0xFF7ADCE0), tertiary = Color(0xFFFFAFD2),
    background = Color(0xFF101627), surface = Color(0xFF1A2237),
    onBackground = Color(0xFFE8EDFF), onSurface = Color(0xFFE8EDFF),
    surfaceVariant = Color(0xFF2A3450), onSurfaceVariant = Color(0xFFBDC6DF),
    outline = Color(0xFF8B96B3)
)
@Composable fun UniTheme(mode: String, content: @Composable () -> Unit) {
    val dark = mode == "dark" || (mode == "system" && isSystemInDarkTheme())
    val view = LocalView.current
    SideEffect {
        (view.context as? android.app.Activity)?.let { activity ->
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = Typography()) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground, content = content)
    }
}
