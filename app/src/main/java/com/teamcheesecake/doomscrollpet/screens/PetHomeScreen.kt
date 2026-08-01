package com.teamcheesecake.doomscrollpet.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teamcheesecake.doomscrollpet.model.PetMood
import com.teamcheesecake.doomscrollpet.model.PetUiState
import com.teamcheesecake.doomscrollpet.ui.theme.YellowBack
import java.util.Locale

@Composable
fun PetHomeScreen(
    state: PetUiState,
    onAddFriend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var profileMenuExpanded by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {

        // --- Top bar ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(YellowBack)
                .padding(bottom = 12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { /* TODO settings */ }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
                Text(
                    text = "SNOOT",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )

                Box {
                    IconButton(onClick = { profileMenuExpanded = true }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                    DropdownMenu(
                        expanded = profileMenuExpanded,
                        onDismissRequest = { profileMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add Friend") },
                            leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                            onClick = {
                                profileMenuExpanded = false
                                showAddFriendDialog = true
                            },
                        )
                    }
                }
            }
            Text(
                text = state.ownerName,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // --- Middle content area ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
        ) {
            // Pet Park icon row (swap in your own icon assets here)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🌳", style = MaterialTheme.typography.headlineSmall)
                    Text(text = "Pet Park", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Doomscroll today: ${state.doomscrollMinutesToday} / ${state.doomscrollLimitMinutes} min")
            Text(text = "Good-app time today: ${state.moreAppMinutesToday} min")
            Text(text = "Distance today: ${formatKm(state.distanceMetersToday)} km")
            Text(text = "Streak: ${state.streakDays} days")
            if (!state.screenTimeConnected) {
                Text(
                    text = "Not connected: screen time",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hearts row (based on health)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                val filledHearts = (state.health / 20).coerceIn(0, 5)
                repeat(5) { index ->
                    Text(
                        text = if (index < filledHearts) "❤️" else "🤍",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Health color bar (red -> green gradient feel via progress color)
            LinearProgressIndicator(
                progress = { state.health / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = healthColor(state.health),
                trackColor = Color.LightGray,
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.ownerName.isNotBlank()) {
                Text(
                    text = "${state.ownerName}'s ${state.animal.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            // Big pet display box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.LightGray, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.petEmoji, style = MaterialTheme.typography.headlineLarge)
                    Text(
                        text = moodMessage(state),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // --- Bottom action bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(YellowBack)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ActionIcon(label = "Food")
            ActionIcon(label = "Water")
            ActionIcon(label = "Exercise")
        }
    }

    if (showAddFriendDialog) {
        AddFriendDialog(
            onDismiss = { showAddFriendDialog = false },
            onSubmit = { code ->
                onAddFriend(code)
                showAddFriendDialog = false
            },
        )
    }
}

@Composable
private fun AddFriendDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var codeInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a Friend") },
        text = {
            Column {
                Text(
                    text = "Enter your friend's code to connect your pets.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it },
                    label = { Text("Friend code") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(codeInput.trim()) },
                enabled = codeInput.isNotBlank(),
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun formatKm(meters: Double): String = String.format(Locale.US, "%.2f", meters / 1000.0)

@Composable
private fun ActionIcon(label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color.White, RoundedCornerShape(8.dp)),
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun healthColor(health: Int): Color = when {
    health >= 80 -> Color(0xFF4CAF50)
    health >= 60 -> Color(0xFF8BC34A)
    health >= 40 -> Color(0xFFFFC107)
    health >= 20 -> Color(0xFFFF9800)
    else -> Color(0xFFF44336)
}

private fun moodMessage(state: PetUiState): String = when (state.mood) {
    PetMood.THRIVING -> "Your pet is thriving! Keep it up."
    PetMood.OKAY -> "Your pet is doing okay. Take a break soon."
    PetMood.SICK -> "Your pet is getting sick from all the scrolling..."
    PetMood.CRITICAL -> "Your pet is really sick! Put the phone down."
}