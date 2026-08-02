package com.teamcheesecake.doomscrollpet.model

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.teamcheesecake.doomscrollpet.data.DeviceIdentity
import com.teamcheesecake.doomscrollpet.data.FriendRepository
import com.teamcheesecake.doomscrollpet.data.LocationRepository
import com.teamcheesecake.doomscrollpet.data.ScreenTimeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import kotlinx.coroutines.withContext

private fun parseAnimal(name: String): Animal? = runCatching { Animal.valueOf(name) }.getOrNull()

private const val BASE_HEALTH = 80
private const val AVOID_MINUTE_PENALTY = 2
private const val MORE_MINUTE_BONUS = 1
private const val METERS_PER_HEALTH_POINT = 100
private const val FOOD_HEALTH_BONUS = 50
private const val WATER_HEALTH_BONUS = 25
private const val HAPPINESS_TICK_INTERVAL_MS = 30_000L
private const val HAPPINESS_RECOVERY_PER_TICK = 1

// Ignore GPS deltas smaller than this between fixes — otherwise standing still slowly racks up
// "distance" from ordinary GPS jitter.
private const val MIN_MOVEMENT_METERS = 5.0
private const val TAG = "PetViewModel"

class PetViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()

    // Used only to read this device's own GPS for the movement/distance stat — no location
    // is published or shared with friends.
    private val locationRepository = LocationRepository(application)
    private val screenTimeRepository = ScreenTimeRepository(application)
    private val friendRepository = FriendRepository()

    private var friendLinksListener: ListenerRegistration? = null
    private var friendStatsListener: ListenerRegistration? = null

    private var myLat: Double? = null
    private var myLng: Double? = null

    // The signed-in user's Firebase Auth uid — set once in loadOrCreateAccountCode.
    private var uid: String? = null
    private var isAccountLoaded = false

    var uiState by mutableStateOf(
        PetUiState(
            myCode = DeviceIdentity.getOrCreateCode(application),
            badges = listOf("First Streak"),
            friends = emptyList(),
            incomingRequests = emptyList(),
            outgoingRequests = emptyList()
        )
    )
        private set

    // Package name mapping for common apps
    private val appPackageMapping = mapOf(
        "YouTube" to "com.google.android.youtube",
        "Instagram" to "com.instagram.android",
        "TikTok" to "com.zhiliaoapp.musically",
        "Twitter" to "com.twitter.android",
        "X" to "com.twitter.android",
        "Facebook" to "com.facebook.katana",
        "Reddit" to "com.reddit.frontpage",
        "Snapchat" to "com.snapchat.android",
        // Good app mappings
        "Notes" to "com.google.android.keep",
        "Canvas" to "com.instructure.candroid",
        "Duolingo" to "com.duolingo",
        "Books" to "com.google.android.apps.books",
        "Fitness" to "com.google.android.apps.fitness",
        "Google" to "com.google.android.googlequicksearchbox"
    )

    private fun Set<String>.toPackageNames(): Set<String> {
        return this.map { appName ->
            appPackageMapping[appName] ?: appName // Use mapped package name if available
        }.toSet()
    }

    // --- Onboarding & Profile Loading ---

    /**
     * Resets the UI state and clears user-specific data. Called when a user signs out
     * or before loading a new account to prevent cross-user state leakage.
     */
    fun clearState() {
        uid = null
        isAccountLoaded = false
        friendLinksListener?.remove()
        friendLinksListener = null
        friendStatsListener?.remove()
        friendStatsListener = null
        myLat = null
        myLng = null

        uiState = PetUiState(
            myCode = DeviceIdentity.getOrCreateCode(getApplication()),
            badges = listOf("First Streak"),
            friends = emptyList(),
            incomingRequests = emptyList(),
            outgoingRequests = emptyList()
        )
    }

    fun loadOrCreateAccountCode(userId: String) {
        clearState()
        uid = userId
        viewModelScope.launch {
            try {
                val userDocRef = db.collection("users").document(userId)
                val snapshot = userDocRef.get().await()
                val existingCode = snapshot.getString("myCode")

                val code = existingCode ?: uiState.myCode
                if (existingCode == null) {
                    userDocRef.set(mapOf("myCode" to code), SetOptions.merge()).await()
                }

                // Load back in whatever was previously saved, so a restart doesn't wipe progress.
                val savedAnimal = snapshot.getString("animal")?.let(::parseAnimal)

                val savedAvoidApps = (snapshot.get("avoidApps") as? List<*>)
                    ?.filterIsInstance<String>()?.toSet() ?: uiState.avoidApps
                val savedMoreApps = (snapshot.get("moreApps") as? List<*>)
                    ?.filterIsInstance<String>()?.toSet() ?: uiState.moreApps

                val savedDoomscroll = (snapshot.getLong("doomscrollMinutesToday") ?: 0L).toInt()
                val savedMoreApp = (snapshot.getLong("moreAppMinutesToday") ?: 0L).toInt()

                uiState = uiState.copy(
                    myCode = code,
                    ownerName = snapshot.getString("ownerName") ?: uiState.ownerName,
                    petName = snapshot.getString("petName") ?: uiState.petName,
                    animal = savedAnimal ?: uiState.animal,
                    avoidApps = savedAvoidApps,
                    moreApps = savedMoreApps,
                    distanceMetersToday = snapshot.getDouble("distanceMetersToday") ?: 0.0,
                    doomscrollMinutesToday = savedDoomscroll,
                    moreAppMinutesToday = savedMoreApp,
                    streakDays = (snapshot.getLong("streakDays") ?: 0L).toInt(),
                    proximityBonus = (snapshot.getLong("proximityBonus") ?: 0L).toInt(),
                    careBonus = (snapshot.getLong("careBonus") ?: 0L).toInt(),
                    lastFedTimestamp = snapshot.getLong("lastFedTimestamp") ?: 0L,
                    lastWaterTimestamp = snapshot.getLong("lastWaterTimestamp") ?: 0L,
                    lastStatsUpdateMillis = snapshot.getTimestamp("lastStatsUpdate")?.toDate()?.time ?: 0L,
                    lastSeenRewardMinutes = (snapshot.getLong("lastSeenRewardMinutes") ?: 0L).toInt(),
                    onboardingComplete = snapshot.getBoolean("onboardingComplete") ?: false,
                    happiness = (snapshot.getLong("happiness") ?: 100L).toInt(),
                )

                isAccountLoaded = true
                recomputeHealth()
                refreshScreenTime()
                startListeningToFriendLinks()
                startHappinessTicker()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load or create account code", e)
            }
        }
    }

    fun setName(name: String) {
        uiState = uiState.copy(ownerName = name)
        val userId = uid ?: return
        db.collection("users").document(userId)
            .set(mapOf("ownerName" to name), SetOptions.merge())
            .addOnFailureListener { e -> Log.e(TAG, "Failed to save name", e) }
    }

    fun setPetName(name: String) {
        uiState = uiState.copy(petName = name)
        val userId = uid ?: return
        db.collection("users").document(userId)
            .set(mapOf("petName" to name), SetOptions.merge())
            .addOnFailureListener { e -> Log.e(TAG, "Failed to save pet name", e) }
    }

    fun selectAnimal(animal: Animal) {
        uiState = uiState.copy(animal = animal)
        val userId = uid ?: return
        db.collection("users").document(userId)
            .set(mapOf("animal" to animal.name), SetOptions.merge())
            .addOnFailureListener { e -> Log.e(TAG, "Failed to save animal", e) }
    }

    fun toggleAvoidApp(app: String) {
        uiState = uiState.copy(avoidApps = uiState.avoidApps.toggle(app))
        saveSelectedAppsToFirestore()
        refreshScreenTime()
    }

    fun toggleMoreApp(app: String) {
        uiState = uiState.copy(moreApps = uiState.moreApps.toggle(app))
        saveSelectedAppsToFirestore()
        refreshScreenTime()
    }

    fun markRewardAsSeen(minutes: Int) {
        uiState = uiState.copy(lastSeenRewardMinutes = minutes)
        val userId = uid ?: return
        db.collection("users").document(userId)
            .set(mapOf("lastSeenRewardMinutes" to minutes), SetOptions.merge())
            .addOnFailureListener { e -> Log.e(TAG, "Failed to save seen reward minutes", e) }
    }

    private fun saveSelectedAppsToFirestore() {
        val userId = uid ?: return
        val data = mapOf(
            "avoidApps" to uiState.avoidApps.toList(),
            "moreApps" to uiState.moreApps.toList()
        )
        db.collection("users").document(userId)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e -> Log.e(TAG, "Failed to save app selections", e) }
    }

    fun completeOnboarding() {
        val userId = uid ?: return
        uiState = uiState.copy(onboardingComplete = true)
        db.collection("users").document(userId)
            .set(mapOf("onboardingComplete" to true), SetOptions.merge())
            .addOnFailureListener { e -> Log.e(TAG, "Failed to save onboarding completion", e) }
    }

    private fun Set<String>.toggle(item: String): Set<String> =
        if (contains(item)) this - item else this + item

    // --- Screen Time & Health ---

    /** Call after returning from the usage-access settings screen, and periodically after. */
    fun refreshScreenTime() {
        if (!isAccountLoaded || !screenTimeRepository.hasUsageAccess()) {
            uiState = uiState.copy(screenTimeConnected = screenTimeRepository.hasUsageAccess())
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val avoidPackages = uiState.avoidApps.toPackageNames()
            val morePackages = uiState.moreApps.toPackageNames()

            val avoidMinutes = screenTimeRepository.getTodayUsageMinutes(avoidPackages).values.sum().toInt()
            val moreMinutes = screenTimeRepository.getTodayUsageMinutes(morePackages).values.sum().toInt()

            withContext(Dispatchers.Main) {
                uiState = uiState.copy(
                    doomscrollMinutesToday = avoidMinutes,
                    moreAppMinutesToday = moreMinutes,
                    screenTimeConnected = true,
                )
                recomputeHealth()
            }
        }
    }

    private fun recomputeHealth() {
        val previousHealth = uiState.health
        val computed = BASE_HEALTH -
                uiState.doomscrollMinutesToday * AVOID_MINUTE_PENALTY +
                uiState.moreAppMinutesToday * MORE_MINUTE_BONUS +
                (uiState.distanceMetersToday / METERS_PER_HEALTH_POINT).toInt() +
                uiState.proximityBonus
        val newHealth = computed.coerceIn(0, 100)

        // Happiness depletes by the same amount health drops. It does NOT rise here —
        // recovery when health is full happens on its own timer, see startHappinessTicker().
        val newHappiness = if (newHealth < previousHealth) {
            (uiState.happiness - (previousHealth - newHealth)).coerceIn(0, 100)
        } else {
            uiState.happiness
        }

        uiState = uiState.copy(health = newHealth, happiness = newHappiness)
        syncStatsToFirestore()
    }

    /** While health is full, happiness slowly climbs back to 100 over time. */
    private fun startHappinessTicker() {
        viewModelScope.launch {
            while (true) {
                delay(HAPPINESS_TICK_INTERVAL_MS)
                if (uiState.health >= 100 && uiState.happiness < 100) {
                    uiState = uiState.copy(
                        happiness = (uiState.happiness + HAPPINESS_RECOVERY_PER_TICK).coerceAtMost(100),
                    )
                    syncStatsToFirestore()
                }
            }
        }
    }

    /**
     * Pushes the current stats snapshot to this user's Firestore document. Called after every
     * stat-affecting change (see recomputeHealth) so the saved copy never drifts far from what's
     * on screen. Uses merge so it never touches unrelated fields like myCode or onboardingComplete.
     */
    private fun syncStatsToFirestore() {
        val userId = uid ?: return
        if (!isAccountLoaded) return

        val now = com.google.firebase.Timestamp.now()

        // Update local state first so checkDailyReset has the latest baseline
        uiState = uiState.copy(lastStatsUpdateMillis = now.toDate().time)

        val data = mapOf(
            "doomscrollMinutesToday" to uiState.doomscrollMinutesToday,
            "moreAppMinutesToday" to uiState.moreAppMinutesToday,
            "distanceMetersToday" to uiState.distanceMetersToday,
            "streakDays" to uiState.streakDays,
            "health" to uiState.health,
            "happiness" to uiState.happiness,
            "proximityBonus" to uiState.proximityBonus,
            "careBonus" to uiState.careBonus,
            "lastFedTimestamp" to uiState.lastFedTimestamp,
            "lastWaterTimestamp" to uiState.lastWaterTimestamp,
            "lastStatsUpdate" to now,
        )
        db.collection("users").document(userId)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e -> Log.e(TAG, "Failed to sync stats", e) }
    }

    private fun checkDailyReset() {
        val lastUpdate = uiState.lastStatsUpdateMillis ?: return
        val now = System.currentTimeMillis()

        val calLast = Calendar.getInstance().apply { timeInMillis = lastUpdate }
        val calNow = Calendar.getInstance().apply { timeInMillis = now }

        if (calLast.get(Calendar.DAY_OF_YEAR) != calNow.get(Calendar.DAY_OF_YEAR) ||
            calLast.get(Calendar.YEAR) != calNow.get(Calendar.YEAR)) {
            // New day: reset "Today" accumulators
            uiState = uiState.copy(
                doomscrollMinutesToday = 0,
                moreAppMinutesToday = 0,
                distanceMetersToday = 0.0,
                proximityBonus = 0,
                careBonus = 0
            )
        }
    }

    // Friends — request / accept flow (no location involved)

    /** Sends a friend request to [code]. Does nothing visible until the other side accepts. */
    fun sendFriendRequest(code: String) {
        val myCode = uiState.myCode
        val myName = uiState.ownerName.ifBlank { myCode }
        viewModelScope.launch { friendRepository.sendRequest(myCode, myName, code) }
    }

    /** Accepts an incoming request from [code], turning it into an active friendship. */
    fun acceptFriendRequest(code: String) {
        val myCode = uiState.myCode
        val myName = uiState.ownerName.ifBlank { myCode }
        viewModelScope.launch { friendRepository.acceptRequest(myCode, code, myName) }
    }

    /** Declines an incoming request from [code] (or cancels one you sent). */
    fun declineFriendRequest(code: String) {
        val myCode = uiState.myCode
        viewModelScope.launch { friendRepository.removeLink(myCode, code) }
    }

    /** Removes an existing friend entirely. */
    fun removeFriend(code: String) {
        val myCode = uiState.myCode
        uiState = uiState.copy(friends = uiState.friends.filterNot { it.code == code })
        viewModelScope.launch { friendRepository.removeLink(myCode, code) }
    }

    /**
     * Starts listening to every friend_link involving this user. Splits the results into
     * accepted friends vs incoming/outgoing pending requests (for the UI to show an
     * accept/decline prompt). Friends here carry no location data — just code + name.
     */
    private fun startListeningToFriendLinks() {
        friendLinksListener?.remove()
        val myCode = uiState.myCode
        friendLinksListener = friendRepository.listenToLinks(myCode) { links ->
            val accepted = links.filter { it.isAccepted() }
            val incoming = links.filter { it.isIncomingFor(myCode) }
            val outgoing = links.filter { it.isOutgoingFor(myCode) }

            uiState = uiState.copy(
                friends = accepted.map { Friend(code = it.otherCode(myCode), name = it.otherName(myCode)) },
                incomingRequests = incoming.map { Friend(code = it.otherCode(myCode), name = it.otherName(myCode)) },
                outgoingRequests = outgoing.map { Friend(code = it.otherCode(myCode), name = it.otherName(myCode)) },
            )
            refreshFriendStatsListener()
        }
    }

    /**
     * Listens to the Firestore user docs of everyone in uiState.friends, pulling their animal type
     * and live health. Restarted every time the friends list changes. Firestore's whereIn caps at
     * 30 values — fine for now, but if you expect more than 30 friends this needs batching.
     */
    private fun refreshFriendStatsListener() {
        friendStatsListener?.remove()
        val codes = uiState.friends.map { it.code }
        if (codes.isEmpty()) {
            uiState = uiState.copy(friendPetStatuses = emptyList())
            return
        }
        friendStatsListener = db.collection("users")
            .whereIn("myCode", codes.take(30))
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    Log.e(TAG, "Failed to listen to friend stats", error)
                    return@addSnapshotListener
                }
                val statuses = snapshot.documents.mapNotNull { doc ->
                    val code = doc.getString("myCode") ?: return@mapNotNull null
                    val ownerName = doc.getString("ownerName") ?: code
                    val animal = doc.getString("animal")?.let(::parseAnimal) ?: Animal.CAT
                    val health = (doc.getLong("health") ?: 80L).toInt()
                    val happiness = (doc.getLong("happiness") ?: 100L).toInt()
                    val doomscrollMinutes = (doc.getLong("doomscrollMinutesToday") ?: 0L).toInt()
                    FriendPetStatus(
                        code = code,
                        ownerName = ownerName,
                        animal = animal,
                        health = health,
                        happiness = happiness,
                        doomscrollMinutesToday = doomscrollMinutes,
                    )
                }
                uiState = uiState.copy(friendPetStatuses = statuses)
            }
    }

    /**
     * Adjusts a friend's health by [delta] (e.g. from "Give Treat!" or "Nudge!"), clamped 0-100.
     * Looks up their Firestore doc by myCode since that's the only identifier we have for them.
     */
    fun adjustFriendHealth(code: String, delta: Int) {
        viewModelScope.launch {
            try {
                val query = db.collection("users")
                    .whereEqualTo("myCode", code)
                    .limit(1)
                    .get()
                    .await()
                val doc = query.documents.firstOrNull() ?: return@launch
                val currentHealth = (doc.getLong("health") ?: 80L).toInt()
                val newHealth = (currentHealth + delta).coerceIn(0, 100)
                doc.reference.set(mapOf("health" to newHealth), SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to adjust friend health for code $code", e)
            }
        }
    }

    fun giveTreatToFriend(code: String) = adjustFriendHealth(code, 10)
    fun nudgeFriend(code: String) = adjustFriendHealth(code, 5)

    /** Exposes a one-off location fix for UI actions (e.g. finding nearby parks). Returns null if
     *  location is unavailable or permission hasn't been granted. */
    suspend fun getCurrentLocation(): Pair<Double, Double>? = locationRepository.getCurrentLatLng()

    /**
     * Fetches this device's current location and accumulates distance traveled since the last
     * fix (filtering out small GPS jitter). Purely local — nothing here is published or shared.
     */
    fun refreshMyLocation() {
        viewModelScope.launch {
            val latLng = locationRepository.getCurrentLatLng() ?: return@launch
            val previousLat = myLat
            val previousLng = myLng

            if (previousLat == null || previousLng == null) {
                myLat = latLng.first
                myLng = latLng.second
            } else {
                val delta = LocationRepository.distanceMeters(
                    previousLat, previousLng, latLng.first, latLng.second
                )
                if (delta >= MIN_MOVEMENT_METERS) {
                    uiState = uiState.copy(distanceMetersToday = uiState.distanceMetersToday + delta)
                    myLat = latLng.first
                    myLng = latLng.second
                    recomputeHealth()
                }
            }
        }
    }

    fun logTimeWithFriend() {
        uiState = uiState.copy(proximityBonus = uiState.proximityBonus + 10)
        recomputeHealth()
    }

    fun feedPet() {
        if (uiState.canFeedTreat) {
            uiState = uiState.copy(
                lastFedTimestamp = System.currentTimeMillis(),
                careBonus = uiState.careBonus + FOOD_HEALTH_BONUS,
                careReactionTrigger = uiState.careReactionTrigger + 1,
            )
            recomputeHealth()
        }
    }

    fun waterPet() {
        if (uiState.canGiveWater) {
            uiState = uiState.copy(
                lastWaterTimestamp = System.currentTimeMillis(),
                careBonus = uiState.careBonus + WATER_HEALTH_BONUS,
                careReactionTrigger = uiState.careReactionTrigger + 1,
            )
            recomputeHealth()
        }
    }

    fun exercisePet() {
        uiState = uiState.copy(health = (uiState.health + 8).coerceAtMost(100))
    }

    override fun onCleared() {
        super.onCleared()
        friendLinksListener?.remove()
        friendStatsListener?.remove()
    }
}