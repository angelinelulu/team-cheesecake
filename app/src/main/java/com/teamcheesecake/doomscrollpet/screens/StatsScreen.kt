package com.teamcheesecake.doomscrollpet.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamcheesecake.doomscrollpet.model.PetUiState

@Composable
fun StatsScreen(state: PetUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Stats")
        Text(text = "Current streak: ${state.streakDays} days")
        Text(text = "Badges earned:")
        state.badges.forEach { badge -> Text(text = "• $badge") }
    }
}
