package com.example.mediactlwithliveupdate.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Primary80,
    onPrimary = Neutral20,
    primaryContainer = Secondary40,
    onPrimaryContainer = Neutral90,
    secondary = Secondary80,
    onSecondary = Neutral20,
    secondaryContainer = Primary40,
    onSecondaryContainer = Neutral90,
    tertiary = Tertiary80,
    onTertiary = Neutral20
)

private val LightColorScheme = lightColorScheme(
    primary = Primary40,
    onPrimary = Neutral90,
    primaryContainer = Primary80,
    onPrimaryContainer = Neutral20,
    secondary = Secondary40,
    onSecondary = Neutral90,
    secondaryContainer = Secondary80,
    onSecondaryContainer = Neutral20,
    tertiary = Tertiary40,
    onTertiary = Neutral90,
    background = Neutral90,
    onBackground = Neutral20,
    surface = Neutral90,
    onSurface = Neutral20

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun MediaCtlWithLiveUpdateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}