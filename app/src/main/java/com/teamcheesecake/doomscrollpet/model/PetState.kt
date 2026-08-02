package com.teamcheesecake.doomscrollpet.model

enum class PetMood {
    THRIVING, OKAY, SICK, CRITICAL
}

enum class Animal(val displayName: String, val emoji: String) {
    CAT("Cat", "🐱"),
    DOG("Dog", "🐶"),
    BEAR("Bear", "🐻"),
    BUNNY("Bunny", "🐰"),
    COW("Cow", "🐮"),
}

// Distance under which two friends are considered "nearby" for alerting/health-recovery purposes.
const val NEARBY_THRESHOLD_METERS = 100.0

data class Friend(
    val code: String,
    val name: String = "",
    val distanceMeters: Double? = null, // null = no location yet / friend hasn't shared
) {
    val isNearby: Boolean
        get() = distanceMeters != null && distanceMeters <= NEARBY_THRESHOLD_METERS
}

/** Snapshot of a friend's pet, read live from their Firestore user document. */
data class FriendPetStatus(
    val code: String,
    val ownerName: String,
    val animal: Animal,
    val health: Int,
) {
    val mood: PetMood
        get() = when {
            health >= 70 -> PetMood.THRIVING
            health >= 40 -> PetMood.OKAY
            health >= 15 -> PetMood.SICK
            else -> PetMood.CRITICAL
        }
}

data class AppOption(val displayName: String, val packageName: String)

// Best-effort package names for the common/global build of each app — a real version would
// let the user pick from their actually-installed apps instead of a fixed list.
val AVOID_APP_OPTIONS = listOf(
    AppOption("TikTok", "com.zhiliaoapp.musically"),
    AppOption("Instagram", "com.instagram.android"),
    AppOption("YouTube", "com.google.android.youtube"),
    AppOption("Snapchat", "com.snapchat.android"),
    AppOption("X / Twitter", "com.twitter.android"),
    AppOption("Reddit", "com.reddit.frontpage"),
)
val MORE_APP_OPTIONS = listOf(
    AppOption("Notes", "com.google.android.keep"),
    AppOption("Canvas", "com.instructure.candroid"),
    AppOption("Duolingo", "com.duolingo"),
    AppOption("Books", "com.google.android.apps.books"),
    AppOption("Fitness", "com.google.android.apps.fitness"),
    AppOption("Calm", "com.calm.android"),
)

data class PetUiState(
    // Onboarding
    val ownerName: String = "",
    val animal: Animal = Animal.CAT,
    val avoidApps: Set<String> = emptySet(), // package names
    val moreApps: Set<String> = emptySet(), // package names
    val screenTimeConnected: Boolean = false,
    val onboardingComplete: Boolean = false,

    // Pet / gameplay — doomscrollMinutesToday and moreAppMinutesToday come from
    // UsageStatsManager, distanceMetersToday accumulated from GPS location updates.
    val health: Int = 80, // 0-100
    val streakDays: Int = 0,
    val doomscrollMinutesToday: Int = 0,
    val doomscrollLimitMinutes: Int = 60,
    val moreAppMinutesToday: Int = 0,
    val distanceMetersToday: Double = 0.0,
    val proximityBonus: Int = 0, // accumulates from time spent near friends today
    val careBonus: Int = 0, // accumulates from feeding/watering today
    val lastFedTimestamp: Long = 0, // epoch milliseconds
    val lastWaterTimestamp: Long = 0, // epoch milliseconds
    val lastStatsUpdateMillis: Long = 0, // epoch milliseconds
    val careReactionTrigger: Long = 0, // bumped on feedPet/waterPet to replay the reaction animation; not synced
    val myCode: String = "",
    val friends: List<Friend> = emptyList(),
    val badges: List<String> = emptyList(),
    val incomingRequests: List<Friend>,
    val outgoingRequests: List<Friend>,
    val friendPetStatuses: List<FriendPetStatus> = emptyList(),
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

    val canFeedTreat: Boolean
        get() = System.currentTimeMillis() - lastFedTimestamp >= 60 * 60 * 1000 * 24

    val canGiveWater: Boolean
        get() = System.currentTimeMillis() - lastWaterTimestamp >= 60 * 60 * 1000 * 24
}