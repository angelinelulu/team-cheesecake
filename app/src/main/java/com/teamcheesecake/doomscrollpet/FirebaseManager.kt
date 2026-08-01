package com.teamcheesecake.doomscrollpet

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

object FirebaseManager {
    private const val TAG = "FirebaseManager"

    val auth = Firebase.auth
    val db = Firebase.firestore

    fun ensureSignedIn(onReady: (String) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            onReady(currentUser.uid)
        } else {
            auth.signInAnonymously()
                .addOnSuccessListener { result ->
                    onReady(result.user!!.uid)
                }
                .addOnFailureListener { e ->
                    android.util.Log.e(TAG, "Anonymous sign-in failed", e)
                }
        }
    }

    fun getOrCreateUserProfile(userId: String, onReady: () -> Unit) {
        val userDoc = db.collection("users").document(userId)
        userDoc.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // returning user — nothing to create
                    onReady()
                } else {
                    val userData = hashMapOf(
                        "displayName" to "New Trainer",
                        "createdAt" to com.google.firebase.Timestamp.now(),
                        "tokenBalance" to 0
                    )
                    userDoc.set(userData)
                        .addOnSuccessListener {
                            android.util.Log.d(TAG, "User profile created for $userId")
                            onReady()
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e(TAG, "Failed to create user profile", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e(TAG, "Failed to read user profile", e)
            }
    }

    fun getOnboardingStatus(userId: String, onResult: (Boolean) -> Unit) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val complete = document.getBoolean("onboardingComplete") ?: false
                onResult(complete)
            }
            .addOnFailureListener { e ->
                android.util.Log.e(TAG, "Failed to check onboarding status", e)
                onResult(false)
            }
    }

    fun signOut(context: android.content.Context, onComplete: () -> Unit) {
        auth.signOut()
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions
            .Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .build()
        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
            .signOut()
            .addOnCompleteListener { onComplete() }
    }

    fun loadUserProfile(userId: String, onLoaded: (String?) -> Unit) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val name = document.getString("displayName")
                onLoaded(name)
            }
            .addOnFailureListener { e ->
                android.util.Log.e(TAG, "Failed to load user profile", e)
                onLoaded(null)
            }
    }

    fun createPet(userId: String, petName: String, onReady: (() -> Unit)? = null) {
        val petRef = db.collection("pets").document() // auto-generated ID

        val petData = hashMapOf(
            "ownerId" to userId,
            "name" to petName,
            "healthScore" to 100,
            "healthState" to "thriving",
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        petRef.set(petData)
            .addOnSuccessListener {
                // link the pet back to the user doc
                db.collection("users").document(userId)
                    .update("petId", petRef.id)
                    .addOnSuccessListener {
                        android.util.Log.d(TAG, "Pet created and linked: ${petRef.id}")
                        onReady?.invoke()
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e(TAG, "Failed to link pet to user", e)
                    }
            }
            .addOnFailureListener { e ->
                android.util.Log.e(TAG, "Failed to create pet", e)
            }
    }
}