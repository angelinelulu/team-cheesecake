package com.teamcheesecake.doomscrollpet.model

enum class PetMood {
    THRIVING, OKAY, SICK, CRITICAL
}

enum class Animal(val displayName: String, val emoji: String) {
    CAT("Cat", "🐱"),
    DOG("Dog", "🐶"),
    PANDA("Panda", "🐼"),
    FOX("Fox", "🦊"),
    OWL("Owl", "🦉"),
    BUNNY("Bunny", "🐰"),
}

data class Friend(
    val name: String,
    val isNearby: Boolean,
)

// Curated placeholder lists — a real version would read installed apps off the device.
val AVOID_APP_OPTIONS = listOf("TikTok", "Instagram", "YouTube", "Snapchat", "X / Twitter", "Reddit")
val MORE_APP_OPTIONS = listOf("Notes", "Canvas", "Duolingo", "Books", "Fitness", "Calm")

data class PetUiState(
    // Onboarding
    val ownerName: String = "",
    val animal: Animal = Animal.CAT,
    val avoidApps: Set<String> = emptySet(),
    val moreApps: Set<String> = emptySet(),
    val healthAppConnected: Boolean = false,
    val screenTimeConnected: Boolean = false,
    val onboardingComplete: Boolean = false,

    // Pet / gameplay
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

    val petEmoji: String
        get() = when (mood) {
            PetMood.THRIVING, PetMood.OKAY -> animal.emoji
            PetMood.SICK -> "${animal.emoji}🤒"
            PetMood.CRITICAL -> "${animal.emoji}💀"
        }
}
