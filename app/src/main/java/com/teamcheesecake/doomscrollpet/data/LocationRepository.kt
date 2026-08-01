package com.teamcheesecake.doomscrollpet.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

/**
 * Reads this device's own GPS location — used only to track distance traveled locally
 * (for the pet's "steps"/movement stat). Nothing here is published anywhere; there is no
 * location sharing with friends.
 */
class LocationRepository(context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLatLng(): Pair<Double, Double>? {
        val location = fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .await() ?: return null
        return location.latitude to location.longitude
    }

    companion object {
        fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val results = FloatArray(1)
            Location.distanceBetween(lat1, lng1, lat2, lng2, results)
            return results[0].toDouble()
        }
    }
}