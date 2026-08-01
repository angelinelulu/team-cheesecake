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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val BASE_HEALTH = 80
private const val AVOID_MINUTE_PENALTY = 2
private const val MORE_MINUTE_BONUS = 1
private const val METERS_PER_HEALTH_POINT = 100
private const val MIN_MOVEMENT_METERS = 5.0
private const val TAG = "PetViewModel"

class PetViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val locationRepository = LocationRepository(application)
    private val screenTimeRepository = ScreenTimeRepository(application)
    private val friendRepository = FriendRepository()

    private var friendLinksListener: ListenerRegistration? = null
    private var myLat: Double? = null
    private var myLng: Double? = null
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
        "Snapchat" to "com.snapchat.android"
    )

    private fun Set<String>.toPackageNames(): Set<String> {
        return this.map { appName ->
            appPackageMapping[appName] ?: appName // Use mapped package name if available
        }.toSet()
    }

    // --- Onboarding & Profile Loading ---

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

                val savedAnimal = snapshot.getString("animal")?.let { name ->
                    runCatching { Animal.valueOf(name) }.getOrNull()
                }

                val savedAvoidApps = (snapshot.get("avoidApps") as? List<*>)
                    ?.filterIsInstance<String>()?.toSet() ?: uiState.avoidApps
                val savedMoreApps = (snapshot.get("moreApps") as? List<*>)
                    ?.filterIsInstance<String>()?.toSet() ?: uiState.moreApps

                val savedDoomscroll = (snapshot.getLong("doomscrollMinutesToday") ?: 0L).toInt()
                val savedMoreApp = (snapshot.getLong("moreAppMinutesToday") ?: 0L).toInt()

                uiState = uiState.copy(
                    myCode = code,
                    ownerName = snapshot.getString("ownerName") ?: uiState.ownerName,
                    animal = savedAnimal ?: uiState.animal,
                    avoidApps = savedAvoidApps,
                    moreApps = savedMoreApps,
                    distanceMetersToday = snapshot.getDouble("distanceMetersToday") ?: 0.0,
                    doomscrollMinutesToday = savedDoomscroll,
                    moreAppMinutesToday = savedMoreApp,
                    streakDays = (snapshot.getLong("streakDays") ?: 0L).toInt(),
                    proximityBonus = (snapshot.getLong("proximityBonus") ?: 0L).toInt(),
                    onboardingComplete = snapshot.getBoolean("onboardingComplete") ?: false,
                )

                isAccountLoaded = true
                recomputeHealth()
                refreshScreenTime()
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
        saveSelectedAppsToFirestore()
        refreshScreenTime()
    }

    fun toggleMoreApp(app: String) {
        uiState = uiState.copy(moreApps = uiState.moreApps.toggle(app))
        saveSelectedAppsToFirestore()
        refreshScreenTime()
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
        val computed = BASE_HEALTH -
                uiState.doomscrollMinutesToday * AVOID_MINUTE_PENALTY +
                uiState.moreAppMinutesToday * MORE_MINUTE_BONUS +
                (uiState.distanceMetersToday / METERS_PER_HEALTH_POINT).toInt() +
                uiState.proximityBonus
        uiState = uiState.copy(health = computed.coerceIn(0, 100))
        syncStatsToFirestore()
    }

    private fun syncStatsToFirestore() {
        val userId = uid ?: return
        if (!isAccountLoaded) return

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
            .addOnFailureListener { e -> Log.e(TAG, "Failed to sync stats", e) }
    }

    // --- Friends Flow ---

    fun sendFriendRequest(code: String) {
        val myCode = uiState.myCode
        val myName = uiState.ownerName.ifBlank { myCode }
        viewModelScope.launch { friendRepository.sendRequest(myCode, myName, code) }
    }

    fun acceptFriendRequest(code: String) {
        val myCode = uiState.myCode
        val myName = uiState.ownerName.ifBlank { myCode }
        viewModelScope.launch { friendRepository.acceptRequest(myCode, code, myName) }
    }

    fun declineFriendRequest(code: String) {
        val myCode = uiState.myCode
        viewModelScope.launch { friendRepository.removeLink(myCode, code) }
    }

    fun removeFriend(code: String) {
        val myCode = uiState.myCode
        uiState = uiState.copy(friends = uiState.friends.filterNot { it.code == code })
        viewModelScope.launch { friendRepository.removeLink(myCode, code) }
    }

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
        }
    }

    // --- Location & Distance ---

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

    override fun onCleared() {
        super.onCleared()
        friendLinksListener?.remove()
    }
}