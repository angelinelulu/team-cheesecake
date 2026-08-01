package com.teamcheesecake.doomscrollpet

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.teamcheesecake.doomscrollpet.data.ProximityNotifier
import com.teamcheesecake.doomscrollpet.model.AVOID_APP_OPTIONS
import com.teamcheesecake.doomscrollpet.model.MORE_APP_OPTIONS
import com.teamcheesecake.doomscrollpet.model.PetViewModel
import com.teamcheesecake.doomscrollpet.screens.FriendsScreen
import com.teamcheesecake.doomscrollpet.screens.PetHomeScreen
import com.teamcheesecake.doomscrollpet.screens.StatsScreen
import com.teamcheesecake.doomscrollpet.screens.onboarding.AnimalScreen
import com.teamcheesecake.doomscrollpet.screens.onboarding.AppSelectionScreen
import com.teamcheesecake.doomscrollpet.screens.onboarding.ConnectScreen
import com.teamcheesecake.doomscrollpet.screens.onboarding.NameScreen
import com.teamcheesecake.doomscrollpet.ui.theme.DoomscrollPetTheme
import kotlinx.coroutines.delay

private object Routes {
    const val NAME = "onboarding/name"
    const val ANIMAL = "onboarding/animal"
    const val AVOID_APPS = "onboarding/avoid"
    const val MORE_APPS = "onboarding/more"
    const val CONNECT = "onboarding/connect"
    const val MAIN = "main"
}

private const val LOCATION_REFRESH_INTERVAL_MS = 15_000L

class MainActivity : ComponentActivity() {

    private val petViewModel: PetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProximityNotifier.ensureChannel(this)
        setContent {
            DoomscrollPetTheme {
                DoomscrollPetApp(petViewModel)
            }
        }
    }
}

@Composable
private fun DoomscrollPetApp(petViewModel: PetViewModel) {
    val navController = rememberNavController()
    val state = petViewModel.uiState

    NavHost(navController = navController, startDestination = Routes.NAME) {
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
                healthConnected = state.healthAppConnected,
                screenTimeConnected = state.screenTimeConnected,
                healthConnectAvailable = petViewModel.isHealthConnectAvailable(),
                healthPermissions = petViewModel.healthConnectPermissions,
                onCheckScreenTimeAccess = { petViewModel.refreshScreenTime() },
                onHealthPermissionsResult = { petViewModel.refreshHealthConnect() },
                onFinish = {
                    petViewModel.completeOnboarding()
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.NAME) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.MAIN) {
            MainAppScreen(petViewModel)
        }
    }
}

@Composable
private fun MainAppScreen(petViewModel: PetViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val state = petViewModel.uiState
    val context = LocalContext.current

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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                petViewModel.refreshScreenTime()
                petViewModel.refreshHealthConnect()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        petViewModel.refreshScreenTime()
        petViewModel.refreshHealthConnect()
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Face, contentDescription = "Pet") },
                    label = { Text("Pet") },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Friends") },
                    label = { Text("Friends") },
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Stats") },
                    label = { Text("Stats") },
                )
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            0 -> PetHomeScreen(state = state, modifier = Modifier.padding(innerPadding))
            1 -> FriendsScreen(
                myCode = state.myCode,
                friends = state.friends,
                onAddFriend = petViewModel::addFriend,
                modifier = Modifier.padding(innerPadding),
            )
            2 -> StatsScreen(state = state, modifier = Modifier.padding(innerPadding))
        }
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
