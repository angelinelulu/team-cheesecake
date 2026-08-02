package com.teamcheesecake.doomscrollpet

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.teamcheesecake.doomscrollpet.data.ProximityNotifier
import com.teamcheesecake.doomscrollpet.model.AVOID_APP_OPTIONS
import com.teamcheesecake.doomscrollpet.model.MORE_APP_OPTIONS
import com.teamcheesecake.doomscrollpet.model.PetViewModel
import com.teamcheesecake.doomscrollpet.screens.FriendsScreen
import com.teamcheesecake.doomscrollpet.screens.NearbyParksScreen
import com.teamcheesecake.doomscrollpet.screens.ParkScreen
import com.teamcheesecake.doomscrollpet.screens.PetHomeScreen
import com.teamcheesecake.doomscrollpet.screens.ProfileScreen
import com.teamcheesecake.doomscrollpet.screens.onboarding.AnimalScreen
import com.teamcheesecake.doomscrollpet.screens.onboarding.AppSelectionScreen
import com.teamcheesecake.doomscrollpet.screens.onboarding.ConnectScreen
import com.teamcheesecake.doomscrollpet.screens.onboarding.NameScreen
import com.teamcheesecake.doomscrollpet.screens.onboarding.SignInScreen
import com.teamcheesecake.doomscrollpet.services.ScreenTimeService
import com.teamcheesecake.doomscrollpet.ui.theme.DoomscrollPetTheme
import com.teamcheesecake.doomscrollpet.ui.theme.YellowBack
import com.teamcheesecake.doomscrollpet.ui.theme.YellowMain
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private object Routes {
    const val SIGN_IN = "onboarding/signin"
    const val NAME = "onboarding/name"
    const val ANIMAL = "onboarding/animal"
    const val AVOID_APPS = "onboarding/avoid"
    const val MORE_APPS = "onboarding/more"
    const val CONNECT = "onboarding/connect"
    const val MAIN = "main"
    const val SETTINGS_APPS = "settings/apps"
    const val PROFILE = "main/profile"
    const val PARK = "main/park"
    const val NEARBY_PARKS = "main/nearby_parks"
}

private const val LOCATION_REFRESH_INTERVAL_MS = 15_000L

class MainActivity : ComponentActivity() {

    private val petViewModel: PetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AudioManager.init(this)
        ProximityNotifier.ensureChannel(this)
        setContent {
            DoomscrollPetTheme {
                DoomscrollPetApp(petViewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        AudioManager.play()
    }

    override fun onStop() {
        super.onStop()
        AudioManager.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            AudioManager.release()
        }
    }
}

@Composable
private fun DoomscrollPetApp(petViewModel: PetViewModel) {
    val navController = rememberNavController()
    val state = petViewModel.uiState

    val startDestination = remember {
        if (FirebaseAuth.getInstance().currentUser == null) Routes.SIGN_IN else Routes.SIGN_IN
    }

    val signOut: () -> Unit = {
        val context = navController.context
        petViewModel.clearState()
        FirebaseManager.signOut(context) {
            navController.navigate(Routes.SIGN_IN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Surface(color = YellowBack) {
        NavHost(navController = navController, startDestination = startDestination) {
            composable(Routes.SIGN_IN) {
                SignInScreen(
                    onSignInSuccess = { uid ->
                        AudioManager.play()
                        FirebaseManager.getOrCreateUserProfile(uid) {
                            FirebaseManager.getOnboardingStatus(uid) { onboardingComplete ->
                                petViewModel.loadOrCreateAccountCode(uid)
                                val destination = if (onboardingComplete) Routes.MAIN else Routes.NAME
                                navController.navigate(destination) {
                                    popUpTo(Routes.SIGN_IN) { inclusive = true }
                                }
                            }
                        }
                    },
                )
            }
            composable(Routes.NAME) {
                NameScreen(
                    name = state.ownerName,
                    onNameChange = petViewModel::setName,
                    onNext = { navController.navigate(Routes.ANIMAL) },
                )
            }
            composable(Routes.ANIMAL) {
                AnimalScreen(
                    selected = state.animal,
                    onSelect = petViewModel::selectAnimal,
                    onNext = { navController.navigate(Routes.AVOID_APPS) },
                )
            }
            composable(Routes.AVOID_APPS) {
                AppSelectionScreen(
                    title = "Apps to avoid",
                    subtitle = "Time here will make your pet sick.",
                    options = AVOID_APP_OPTIONS,
                    selected = state.avoidApps,
                    onToggle = petViewModel::toggleAvoidApp,
                    onNext = { navController.navigate(Routes.MORE_APPS) },
                )
            }
            composable(Routes.MORE_APPS) {
                AppSelectionScreen(
                    title = "Apps to do more of",
                    subtitle = "Time here will keep your pet happy and healthy.",
                    options = MORE_APP_OPTIONS,
                    selected = state.moreApps,
                    onToggle = petViewModel::toggleMoreApp,
                    onNext = { navController.navigate(Routes.CONNECT) },
                )
            }
            composable(Routes.CONNECT) {
                ConnectScreen(
                    screenTimeConnected = state.screenTimeConnected,
                    onCheckScreenTimeAccess = { petViewModel.refreshScreenTime() },
                    onFinish = {
                        petViewModel.completeOnboarding()
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.AVOID_APPS) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.MAIN) {
                MainAppScreen(
                    petViewModel = petViewModel,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS_APPS) },
                    onSignOut = signOut,
                    onNavigateToProfile = { navController.navigate(Routes.PROFILE) },
                    onNavigateToPark = { navController.navigate(Routes.PARK) },
                    onNavigateToNearbyParks = { navController.navigate(Routes.NEARBY_PARKS) },
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    state = state,
                    onSendFriendRequest = petViewModel::sendFriendRequest,
                    onAcceptRequest = petViewModel::acceptFriendRequest,
                    onDeclineRequest = petViewModel::declineFriendRequest,
                    onSignOut = signOut,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.PARK) {
                ParkScreen(
                    friendPetStatuses = state.friendPetStatuses,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.NEARBY_PARKS) {
                NearbyParksScreen(
                    state = state,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SETTINGS_APPS) {
                AppSelectionScreen(
                    title = "Apps to avoid",
                    subtitle = "Time here will reduce your pet's health.",
                    options = AVOID_APP_OPTIONS,
                    selected = state.avoidApps,
                    onToggle = petViewModel::toggleAvoidApp,
                    onNext = { navController.popBackStack() },
                    showMuteToggle = true,
                )
            }
        }
    }
}

@Composable
private fun MainAppScreen(
    petViewModel: PetViewModel,
    onOpenSettings: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToPark: () -> Unit,
    onNavigateToNearbyParks: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val state = petViewModel.uiState
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var locationPermissionGranted by remember { mutableStateOf(hasLocationPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        locationPermissionGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission(context)) {
            permissionLauncher.launch(locationPermissions())
        }
    }

    LaunchedEffect(locationPermissionGranted) {
        while (locationPermissionGranted) {
            petViewModel.refreshMyLocation()
            delay(LOCATION_REFRESH_INTERVAL_MS)
        }
    }

    // Single setup for Overlay Permissions, Screen Time Refresh, and Background Service
    LaunchedEffect(Unit) {
        checkAndRequestOverlayPermission(context)
        petViewModel.refreshScreenTime()

        val serviceIntent = Intent(context, ScreenTimeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> AppVisibilityTracker.isAppInForeground = true
                Lifecycle.Event.ON_STOP -> AppVisibilityTracker.isAppInForeground = false
                Lifecycle.Event.ON_RESUME -> petViewModel.refreshScreenTime()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            AppVisibilityTracker.isAppInForeground = false
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Note: PetHomeScreen now draws its own top bar (title + profile dropdown), so
    // Scaffold no longer supplies a separate topBar here — that avoids stacking two
    // top bars on screen. The friends tab/toggle was removed since friend requests
    // are already handled from the profile menu / ProfileScreen.
    Scaffold(
        containerColor = YellowMain,
        bottomBar = {
            com.teamcheesecake.doomscrollpet.screens.PetActionBottomBar(
                state = state,
                onFood = petViewModel::feedPet,
                onWater = petViewModel::waterPet,
                onExercise = onNavigateToNearbyParks,
            )
        },
    ) { innerPadding ->
        when (selectedTab) {
            0 -> PetHomeScreen(
                state = state,
                myCode = state.myCode,
                onSendFriendRequest = petViewModel::sendFriendRequest,
                onOpenSettings = onOpenSettings,
                onSignOut = onSignOut,
                onNavigateToProfile = onNavigateToProfile,
                onNavigateToPark = onNavigateToPark,
                modifier = Modifier.padding(innerPadding),
            )
            1 -> FriendsScreen(
                myCode = state.myCode,
                friends = state.friends,
                incomingRequests = state.incomingRequests,
                outgoingRequests = state.outgoingRequests,
                onSendFriendRequest = petViewModel::sendFriendRequest,
                onAcceptRequest = petViewModel::acceptFriendRequest,
                onDeclineRequest = petViewModel::declineFriendRequest,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

/** Requests 'Display over other apps' permission ONLY if not already granted. */
private fun checkAndRequestOverlayPermission(context: Context) {
    if (!Settings.canDrawOverlays(context)) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

private fun locationPermissions(): Array<String> {
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions += Manifest.permission.POST_NOTIFICATIONS
    }
    return permissions.toTypedArray()
}