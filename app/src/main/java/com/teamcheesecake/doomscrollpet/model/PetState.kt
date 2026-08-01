package com.teamcheesecake.doomscrollpet.model

enum class PetMood {
    THRIVING, OKAY, SICK, CRITICAL
}

data class Friend(
    val name: String,
    val isNearby: Boolean,
)

data class PetUiState(
    val health: Int = 80, // 0-100, drained by doomscroll time, restored by breaks/proximity/off-phone time
    val streakDays: Int = 0,
    val doomscrollMinutesToday: Int = 0,
    val doomscrollLimitMinutes: Int = 60,
    val friends: List<Friend> = emptyList(),
    val badges: List<String> = emptyList(),
) {
    val mood: PetMood
        get() = when {
            health >= 70 -> PetMood.THRIVING
            health >= 40 -> PetMood.OKAY
            health >= 15 -> PetMood.SICK
            else -> PetMood.CRITICAL
        }

    val isOverLimit: Boolean
        get() = doomscrollMinutesToday >= doomscrollLimitMinutes
}
