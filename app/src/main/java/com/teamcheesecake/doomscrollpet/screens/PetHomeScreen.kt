package com.teamcheesecake.doomscrollpet.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teamcheesecake.doomscrollpet.model.PetMood
import com.teamcheesecake.doomscrollpet.model.PetUiState
import java.util.Locale

@Composable
fun PetHomeScreen(state: PetUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (state.ownerName.isNotBlank()) {
            Text(
                text = "${state.ownerName}'s ${state.animal.displayName}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Text(text = state.petEmoji, style = MaterialTheme.typography.titleLarge)
        Text(
            text = moodMessage(state),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
        )

        Text(text = "Health: ${state.health}/100")
        LinearProgressIndicator(
            progress = { state.health / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 24.dp),
        )

        Text(text = "Doomscroll today: ${state.doomscrollMinutesToday} / ${state.doomscrollLimitMinutes} min")
        Text(text = "Good-app time today: ${state.moreAppMinutesToday} min")
        Text(text = "Distance today: ${formatKm(state.distanceMetersToday)} km")
        Text(text = "Streak: ${state.streakDays} days")

        if (!state.screenTimeConnected) {
            Text(
                text = "Not connected: screen time",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun formatKm(meters: Double): String = String.format(Locale.US, "%.2f", meters / 1000.0)

private fun moodMessage(state: PetUiState): String = when (state.mood) {
    PetMood.THRIVING -> "Your pet is thriving! Keep it up."
    PetMood.OKAY -> "Your pet is doing okay. Take a break soon."
    PetMood.SICK -> "Your pet is getting sick from all the scrolling..."
    PetMood.CRITICAL -> "Your pet is really sick! Put the phone down."
}
