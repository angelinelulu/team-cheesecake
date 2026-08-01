package com.teamcheesecake.doomscrollpet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = PetGreen,
    secondary = PetGreenDark,
    error = PetRed,
    background = Cream,
    onBackground = CharcoalText,
)

private val DarkColors = darkColorScheme(
    primary = PetGreen,
    secondary = PetGreenDark,
    error = PetRed,
)

@Composable
fun DoomscrollPetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content,
    )
}
