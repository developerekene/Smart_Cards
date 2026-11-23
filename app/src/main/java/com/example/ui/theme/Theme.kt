package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.ThemeMode

@Immutable
data class SmartCardsExtendedColors(
    val easy: Color,
    val easyContainer: Color,
    val difficult: Color,
    val difficultContainer: Color,
    val amber: Color,
    val amberContainer: Color,
    val cardRuledLine: Color,
    val heroScrimStart: Color,
    val heroScrimEnd: Color,
    val isDark: Boolean
)

val LocalSmartCardsColors = staticCompositionLocalOf {
    SmartCardsExtendedColors(
        easy = EasyGreenLight,
        easyContainer = EasyContainerLight,
        difficult = DifficultCoralLight,
        difficultContainer = DifficultContainerLight,
        amber = AmberHighlightLight,
        amberContainer = AmberContainerLight,
        cardRuledLine = Color(0xFFE5E1D5),
        heroScrimStart = Color(0xE6141F3A),
        heroScrimEnd = Color(0x991E3A8A),
        isDark = false
    )
}

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = LightError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = DarkError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer
)

private val SmartCardsShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SmartCardsTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val extendedColors = if (isDark) {
        SmartCardsExtendedColors(
            easy = EasyGreenDark,
            easyContainer = EasyContainerDark,
            difficult = DifficultCoralDark,
            difficultContainer = DifficultContainerDark,
            amber = AmberHighlightDark,
            amberContainer = AmberContainerDark,
            cardRuledLine = Color(0xFF283554),
            heroScrimStart = Color(0xEB0B1222),
            heroScrimEnd = Color(0xB3152242),
            isDark = true
        )
    } else {
        SmartCardsExtendedColors(
            easy = EasyGreenLight,
            easyContainer = EasyContainerLight,
            difficult = DifficultCoralLight,
            difficultContainer = DifficultContainerLight,
            amber = AmberHighlightLight,
            amberContainer = AmberContainerLight,
            cardRuledLine = Color(0xFFE7E2D4),
            heroScrimStart = Color(0xE813203E),
            heroScrimEnd = Color(0xB81B3166),
            isDark = false
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalSmartCardsColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SmartCardsTypography,
            shapes = SmartCardsShapes,
            content = content
        )
    }
}
