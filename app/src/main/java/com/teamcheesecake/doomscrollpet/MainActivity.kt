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
import com.teamcheesecake.doomscrollpet.model.PetViewModel
import com.teamcheesecake.doomscrollpet.screens.FriendsScreen
import com.teamcheesecake.doomscrollpet.screens.PetHomeScreen
import com.teamcheesecake.doomscrollpet.screens.StatsScreen
import com.teamcheesecake.doomscrollpet.ui.theme.DoomscrollPetTheme

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
