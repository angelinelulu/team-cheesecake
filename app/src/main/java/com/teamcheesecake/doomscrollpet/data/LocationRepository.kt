package com.teamcheesecake.doomscrollpet.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

data class FriendLocation(
    val name: String,
    val lat: Double,
    val lng: Double,
    val updatedAtMillis: Long,
)

/**
 * Publishes this device's location under its friend code and listens for
 * friends' locations. Firestore doc per code: locations/{code} = {name, lat, lng, updatedAt}.
 * No auth — Firestore rules should be locked down before this ships beyond a demo.
 */
class LocationRepository(context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationsCollection = FirebaseFirestore.getInstance().collection("locations")

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLatLng(): Pair<Double, Double>? {
        val location = fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .await() ?: return null
        return location.latitude to location.longitude
    }

    suspend fun publishMyLocation(myCode: String, myName: String, lat: Double, lng: Double) {
        locationsCollection.document(myCode).set(
            mapOf(
                "name" to myName,
                "lat" to lat,
                "lng" to lng,
                "updatedAt" to System.currentTimeMillis(),
            )
        ).await()
    }

    fun listenToFriend(code: String, onUpdate: (FriendLocation?) -> Unit): ListenerRegistration {
        return locationsCollection.document(code).addSnapshotListener { snapshot, _ ->
            val lat = snapshot?.getDouble("lat")
            val lng = snapshot?.getDouble("lng")
            if (snapshot == null || !snapshot.exists() || lat == null || lng == null) {
                onUpdate(null)
                return@addSnapshotListener
            }
            onUpdate(
                FriendLocation(
                    name = snapshot.getString("name") ?: "",
                    lat = lat,
                    lng = lng,
                    updatedAtMillis = snapshot.getLong("updatedAt") ?: 0L,
                )
            )
        }
    }

    companion object {
        fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val results = FloatArray(1)
            Location.distanceBetween(lat1, lng1, lat2, lng2, results)
            return results[0].toDouble()
        }
    }
}
