package com.teamcheesecake.doomscrollpet.screens.onboarding

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import com.teamcheesecake.doomscrollpet.R

@Composable
fun SignInScreen(onSignInSuccess: (uid: String) -> Unit)  {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient: GoogleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isLoading = true
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                Firebase.auth.signInWithCredential(credential)
                    .addOnCompleteListener { authResult ->
                        isLoading = false
                        if (authResult.isSuccessful) {
                            val user = Firebase.auth.currentUser
                            user?.let {
                                createOrUpdateUserDoc(it.uid, it.email, it.displayName)
                                onSignInSuccess(it.uid)
                            }
                        } else {
                            errorMessage = "Sign-in failed: ${authResult.exception?.message}"
                        }
                    }
            } catch (e: ApiException) {
                isLoading = false
                errorMessage = "Google sign-in error: ${e.statusCode}"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Meet your pet", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(onClick = { launcher.launch(googleSignInClient.signInIntent) }) {
                Text("Sign in with Google")
            }
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun createOrUpdateUserDoc(uid: String, email: String?, displayName: String?) {
    val userDoc = Firebase.firestore.collection("users").document(uid)
    val data = mapOf(
        "email" to email,
        "displayName" to displayName,
        "lastSignIn" to com.google.firebase.Timestamp.now()
    )
    userDoc.set(data, com.google.firebase.firestore.SetOptions.merge())
}