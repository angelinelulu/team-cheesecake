package com.teamcheesecake.doomscrollpet.model

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.teamcheesecake.doomscrollpet.data.DeviceIdentity
import com.teamcheesecake.doomscrollpet.data.LocationRepository
import com.teamcheesecake.doomscrollpet.data.ProximityNotifier
import com.teamcheesecake.doomscrollpet.data.ScreenTimeRepository
import kotlinx.coroutines.launch
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

private const val BASE_HEALTH = 80
private const val AVOID_MINUTE_PENALTY = 2
private const val MORE_MINUTE_BONUS = 1
private const val METERS_PER_HEALTH_POINT = 100

// Ignore GPS deltas smaller than this between fixes — otherwise standing still slowly racks up
// "distance" from ordinary GPS jitter.
private const val MIN_MOVEMENT_METERS = 5.0

class PetViewModel(application: Application) : AndroidViewModel(application) {

    private val locationRepository = LocationRepository(application)
    private val screenTimeRepository = ScreenTimeRepository(application)
    private val friendListeners = mutableMapOf<String, ListenerRegistration>()
    private var myLat: Double? = null
    private var myLng: Double? = null

    var uiState by mutableStateOf(
        PetUiState(
            myCode = DeviceIdentity.getOrCreateCode(application),
            badges = listOf("First Streak"),
        )
    )
        private set

    // Onboarding

    fun loadOrCreateAccountCode(uid: String) {
        viewModelScope.launch {
            val userDocRef = Firebase.firestore.collection("users").document(uid)
            val snapshot = userDocRef.get().await()
            val existingCode = snapshot.getString("myCode")

            val code = existingCode ?: uiState.myCode
            if (existingCode == null) {
                userDocRef.set(mapOf("myCode" to code), SetOptions.merge()).await()
            }
            uiState = uiState.copy(myCode = code)
        }
    }

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

    fun completeOnboarding() {
        uiState = uiState.copy(onboardingComplete = true)
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
    }

    // Location / friends / distance traveled

    fun addFriend(code: String) {
        val normalized = code.trim().uppercase()
        if (normalized.isBlank() || normalized == uiState.myCode) return
        if (uiState.friends.any { it.code == normalized }) return

        uiState = uiState.copy(friends = uiState.friends + Friend(code = normalized))

        val registration = locationRepository.listenToFriend(normalized) { friendLocation ->
            val lat = myLat
            val lng = myLng
            val distance = if (friendLocation != null && lat != null && lng != null) {
                LocationRepository.distanceMeters(lat, lng, friendLocation.lat, friendLocation.lng)
            } else null

            val wasNearby = uiState.friends.firstOrNull { it.code == normalized }?.isNearby ?: false

            uiState = uiState.copy(
                friends = uiState.friends.map {
                    if (it.code == normalized) {
                        it.copy(name = friendLocation?.name ?: it.name, distanceMeters = distance)
                    } else it
                },
            )

            val nowNearby = uiState.friends.firstOrNull { it.code == normalized }?.isNearby ?: false
            if (nowNearby && !wasNearby) {
                val displayName = friendLocation?.name?.ifBlank { normalized } ?: normalized
                ProximityNotifier.notifyNearby(getApplication(), displayName)
                logTimeWithFriend()
            }
        }
        friendListeners[normalized] = registration
    }

    fun removeFriend(code: String) {
        friendListeners.remove(code)?.remove()
        uiState = uiState.copy(friends = uiState.friends.filterNot { it.code == code })
    }

    /**
     * Fetches this device's current location, accumulates distance traveled since the last
     * fix (filtering out small GPS jitter), publishes the new position, and re-checks distance
     * to friends.
     */
    fun refreshMyLocation() {
        viewModelScope.launch {
            val latLng = locationRepository.getCurrentLatLng() ?: return@launch
            val previousLat = myLat
            val previousLng = myLng

            if (previousLat != null && previousLng != null) {
                val delta = LocationRepository.distanceMeters(previousLat, previousLng, latLng.first, latLng.second)
                if (delta >= MIN_MOVEMENT_METERS) {
                    uiState = uiState.copy(distanceMetersToday = uiState.distanceMetersToday + delta)
                    recomputeHealth()
                }
            }

            myLat = latLng.first
            myLng = latLng.second
            locationRepository.publishMyLocation(
                myCode = uiState.myCode,
                myName = uiState.ownerName.ifBlank { uiState.myCode },
                lat = latLng.first,
                lng = latLng.second,
            )
        }
    }

    fun logTimeWithFriend() {
        uiState = uiState.copy(proximityBonus = uiState.proximityBonus + 10)
        recomputeHealth()
    }

    override fun onCleared() {
        friendListeners.values.forEach { it.remove() }
        friendListeners.clear()
    }
}
