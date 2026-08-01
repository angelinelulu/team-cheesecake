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

    // Onboarding

    fun setName(name: String) {
        uiState = uiState.copy(ownerName = name)
    }

    fun selectAnimal(animal: Animal) {
        uiState = uiState.copy(animal = animal)
    }

    fun toggleAvoidApp(app: String) {
        uiState = uiState.copy(avoidApps = uiState.avoidApps.toggle(app))
    }

    fun toggleMoreApp(app: String) {
        uiState = uiState.copy(moreApps = uiState.moreApps.toggle(app))
    }

    fun setHealthAppConnected(connected: Boolean) {
        uiState = uiState.copy(healthAppConnected = connected)
    }

    fun setScreenTimeConnected(connected: Boolean) {
        uiState = uiState.copy(screenTimeConnected = connected)
    }

    fun completeOnboarding() {
        uiState = uiState.copy(onboardingComplete = true)
    }

    private fun Set<String>.toggle(item: String): Set<String> =
        if (contains(item)) this - item else this + item

    // Gameplay

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
