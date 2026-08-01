package com.teamcheesecake.doomscrollpet.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * Placeholder state holder. Doomscroll time should eventually come from
 * UsageStatsManager (screen time) and health app step/break data; proximity
 * from location, all wired in later.
 */
class PetViewModel : ViewModel() {

    var uiState by mutableStateOf(
        PetUiState(
            friends = listOf(
                Friend(name = "Sam", isNearby = true),
                Friend(name = "Priya", isNearby = false),
            ),
            badges = listOf("First Streak"),
        )
    )
        private set

    fun logDoomscrollMinutes(minutes: Int) {
        val newMinutes = uiState.doomscrollMinutesToday + minutes
        val penalty = minutes * 2
        uiState = uiState.copy(
            doomscrollMinutesToday = newMinutes,
            health = (uiState.health - penalty).coerceIn(0, 100),
        )
    }

    fun logBreakMinutes(minutes: Int) {
        val recovery = minutes * 3
        uiState = uiState.copy(
            health = (uiState.health + recovery).coerceIn(0, 100),
        )
    }

    fun logTimeWithFriend() {
        val recovery = 10
        uiState = uiState.copy(
            health = (uiState.health + recovery).coerceIn(0, 100),
        )
    }
}
