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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val BASE_HEALTH = 80
private const val AVOID_MINUTE_PENALTY = 2
private const val MORE_MINUTE_BONUS = 1
private const val METERS_PER_HEALTH_POINT = 100

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

    // Onboarding

    fun loadOrCreateAccountCode(userId: String) {
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
                val savedAnimal = snapshot.getString("animal")?.let { name ->
                    runCatching { Animal.valueOf(name) }.getOrNull()
                }
                uiState = uiState.copy(
                    myCode = code,
                    ownerName = snapshot.getString("ownerName") ?: uiState.ownerName,
                    animal = savedAnimal ?: uiState.animal,
                    distanceMetersToday = snapshot.getDouble("distanceMetersToday") ?: 0.0,
                    doomscrollMinutesToday = (snapshot.getLong("doomscrollMinutesToday") ?: 0L).toInt(),
                    moreAppMinutesToday = (snapshot.getLong("moreAppMinutesToday") ?: 0L).toInt(),
                    streakDays = (snapshot.getLong("streakDays") ?: 0L).toInt(),
                    proximityBonus = (snapshot.getLong("proximityBonus") ?: 0L).toInt(),
                    onboardingComplete = snapshot.getBoolean("onboardingComplete") ?: false,
                )
                recomputeHealth()
                startListeningToFriendLinks()
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

    fun selectAnimal(animal: Animal) {
        uiState = uiState.copy(animal = animal)
        val userId = uid ?: return
        db.collection("users").document(userId)
            .set(mapOf("animal" to animal.name), SetOptions.merge())
            .addOnFailureListener { e -> Log.e(TAG, "Failed to save animal", e) }
    }

    fun toggleAvoidApp(app: String) {
        uiState = uiState.copy(avoidApps = uiState.avoidApps.toggle(app))
        refreshScreenTime()
    }

    fun toggleMoreApp(app: String) {
        uiState = uiState.copy(moreApps = uiState.moreApps.toggle(app))
        refreshScreenTime()
    }

    fun completeOnboarding() {
        val userId = uid
        if (userId == null) {
            Log.w(TAG, "completeOnboarding called before uid was set — not saved")
            return
        }
        uiState = uiState.copy(onboardingComplete = true)
        db.collection("users").document(userId)
            .set(mapOf("onboardingComplete" to true), SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save onboarding completion", e)
            }
    }

    private fun Set<String>.toggle(item: String): Set<String> =
        if (contains(item)) this - item else this + item

    // Screen time (real, via UsageStatsManager)

    /** Call after returning from the usage-access settings screen, and periodically after. */
    fun refreshScreenTime() {
        if (!screenTimeRepository.hasUsageAccess()) {
            uiState = uiState.copy(screenTimeConnected = false)
            return
        }
        val avoidMinutes = screenTimeRepository.getTodayUsageMinutes(uiState.avoidApps).values.sum()
        val moreMinutes = screenTimeRepository.getTodayUsageMinutes(uiState.moreApps).values.sum()
        uiState = uiState.copy(
            doomscrollMinutesToday = avoidMinutes.toInt(),
            moreAppMinutesToday = moreMinutes.toInt(),
            screenTimeConnected = true,
        )
        recomputeHealth()
    }

    private fun recomputeHealth() {
        val computed = BASE_HEALTH -
                uiState.doomscrollMinutesToday * AVOID_MINUTE_PENALTY +
                uiState.moreAppMinutesToday * MORE_MINUTE_BONUS +
                (uiState.distanceMetersToday / METERS_PER_HEALTH_POINT).toInt() +
                uiState.proximityBonus
        uiState = uiState.copy(health = computed.coerceIn(0, 100))
        syncStatsToFirestore()
    }

    /**
     * Pushes the current stats snapshot to this user's Firestore document. Called after every
     * stat-affecting change (see recomputeHealth) so the saved copy never drifts far from what's
     * on screen. Uses merge so it never touches unrelated fields like myCode or onboardingComplete.
     */
    private fun syncStatsToFirestore() {
        val userId = uid ?: return
        val data = mapOf(
            "doomscrollMinutesToday" to uiState.doomscrollMinutesToday,
            "moreAppMinutesToday" to uiState.moreAppMinutesToday,
            "distanceMetersToday" to uiState.distanceMetersToday,
            "streakDays" to uiState.streakDays,
            "health" to uiState.health,
            "proximityBonus" to uiState.proximityBonus,
            "lastStatsUpdate" to com.google.firebase.Timestamp.now(),
        )
        db.collection("users").document(userId)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to sync stats", e)
            }
    }

    // Friends — request / accept flow (no location involved)

    /** Sends a friend request to [code]. Does nothing visible until the other side accepts. */
    fun sendFriendRequest(code: String) {
        val myCode = uiState.myCode
        val myName = uiState.ownerName.ifBlank { myCode }
        viewModelScope.launch {
            friendRepository.sendRequest(myCode, myName, code)
        }
    }

    /** Accepts an incoming request from [code], turning it into an active friendship. */
    fun acceptFriendRequest(code: String) {
        val myCode = uiState.myCode
        val myName = uiState.ownerName.ifBlank { myCode }
        viewModelScope.launch {
            friendRepository.acceptRequest(myCode, code, myName)
        }
    }

    /** Declines an incoming request from [code] (or cancels one you sent). */
    fun declineFriendRequest(code: String) {
        val myCode = uiState.myCode
        viewModelScope.launch {
            friendRepository.removeLink(myCode, code)
        }
    }

    /** Removes an existing friend entirely. */
    fun removeFriend(code: String) {
        val myCode = uiState.myCode
        uiState = uiState.copy(friends = uiState.friends.filterNot { it.code == code })
        viewModelScope.launch {
            friendRepository.removeLink(myCode, code)
        }
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
                friends = accepted.map {
                    Friend(code = it.otherCode(myCode), name = it.otherName(myCode))
                },
                incomingRequests = incoming.map {
                    Friend(code = it.otherCode(myCode), name = it.otherName(myCode))
                },
                outgoingRequests = outgoing.map {
                    Friend(code = it.otherCode(myCode), name = it.otherName(myCode))
                },
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
                    val animal = doc.getString("animal")
                        ?.let { runCatching { Animal.valueOf(it) }.getOrNull() }
                        ?: Animal.CAT
                    val health = (doc.getLong("health") ?: 80L).toInt()
                    FriendPetStatus(code = code, ownerName = ownerName, animal = animal, health = health)
                }
                uiState = uiState.copy(friendPetStatuses = statuses)
            }
    }

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
                    uiState = uiState.copy(
                        distanceMetersToday = uiState.distanceMetersToday + delta
                    )
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
        uiState = uiState.copy(health = (uiState.health + 5).coerceAtMost(100))
    }

    fun waterPet() {
        uiState = uiState.copy(health = (uiState.health + 2).coerceAtMost(100))
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