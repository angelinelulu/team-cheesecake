package com.teamcheesecake.doomscrollpet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = ButtonGreen,
    onPrimary = PetText,
    background = YellowMain,
    onBackground = PetText,
    surface = YellowMain,
    onSurface = PetText,
)

@Composable
fun DoomscrollPetTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
