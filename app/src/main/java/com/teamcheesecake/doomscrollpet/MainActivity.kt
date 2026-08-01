package com.teamcheesecake.doomscrollpet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

private object Routes {
    const val NAME = "onboarding/name"
    const val ANIMAL = "onboarding/animal"
    const val AVOID_APPS = "onboarding/avoid"
    const val MORE_APPS = "onboarding/more"
    const val CONNECT = "onboarding/connect"
    const val MAIN = "main"
}

class MainActivity : ComponentActivity() {

    private val petViewModel: PetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                onToggleHealth = { petViewModel.setHealthAppConnected(!state.healthAppConnected) },
                onRequestScreenTimeAccess = { petViewModel.setScreenTimeConnected(true) },
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
            1 -> FriendsScreen(friends = state.friends, modifier = Modifier.padding(innerPadding))
            2 -> StatsScreen(state = state, modifier = Modifier.padding(innerPadding))
        }
    }
}
