// Plik: ui/theme/Theme.kt
package com.itsorderkds.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

/* ---------- 1) EXTRA kolory sukcesu ---------- */

data class SuccessColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
)

private val LocalSuccessColors = staticCompositionLocalOf {
    // wartości domyślne (fallback – nieużywane w praktyce)
    SuccessColors(
        success = Color(0xFF2E7D32),
        onSuccess = Color.White,
        successContainer = Color(0xFFE8F5E9),
        onSuccessContainer = Color(0xFF0B3D0B),
    )
}

/* WYGODNE rozszerzenia, by używać jak MaterialTheme.colorScheme.success */
val androidx.compose.material3.ColorScheme.success: Color
    @Composable @ReadOnlyComposable get() = LocalSuccessColors.current.success

val androidx.compose.material3.ColorScheme.onSuccess: Color
    @Composable @ReadOnlyComposable get() = LocalSuccessColors.current.onSuccess

val androidx.compose.material3.ColorScheme.successContainer: Color
    @Composable @ReadOnlyComposable get() = LocalSuccessColors.current.successContainer

val androidx.compose.material3.ColorScheme.onSuccessContainer: Color
    @Composable @ReadOnlyComposable get() = LocalSuccessColors.current.onSuccessContainer

/* ---------- 2) Standardowe ColorScheme (BEZ success) ---------- */

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    error = ErrorDark,
    onError = OnErrorDark,
    // <--- bez success
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    error = ErrorLight,
    onError = OnErrorLight,
    secondaryContainer = SecondaryContainerLight,
    // <--- bez success
)

/* ---------- 3) Theme z dostarczeniem SuccessColors przez CompositionLocal ---------- */

@Composable
fun ItsOrderChatTheme(
    darkTheme: Boolean = true,          // KDS zawsze ciemny — wymuszony
    dynamicColor: Boolean = false,      // wyłączone — chcemy własną paletę
    content: @Composable () -> Unit
) {
    // KDS zawsze używa ciemnego schematu — Dynamic Color wyłączony
    val colorScheme = DarkColorScheme

    // Zestaw success zawsze ciemny
    val successColors = SuccessColors(
        success            = SuccessDark,
        onSuccess          = OnSuccessDark,
        successContainer   = SuccessContainerDark,
        onSuccessContainer = OnSuccessContainerDark
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.setDecorFitsSystemWindows(window, false)
            // Ciemny status bar — białe ikony na ciemnym tle
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    CompositionLocalProvider(LocalSuccessColors provides successColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            content     = content
        )
    }
}
