package com.teamcheesecake.doomscrollpet.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.teamcheesecake.doomscrollpet.model.Friend
import com.teamcheesecake.doomscrollpet.model.PetUiState
import com.teamcheesecake.doomscrollpet.ui.theme.ButtonGreen
import com.teamcheesecake.doomscrollpet.ui.theme.YellowMain
import java.util.Locale

@Composable
fun ProfileScreen(
    state: PetUiState,
    onSendFriendRequest: (String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onDeclineRequest: (String) -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var codeInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(YellowMain),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Profile Details",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Doomscroll Timer: ${state.doomscrollMinutesToday}/${state.doomscrollLimitMinutes} minutes",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "Productivity Timer: ${state.moreAppMinutesToday} minutes",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "Distance Covered Today: ${formatKm(state.distanceMetersToday)} km",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "Streak: ${state.streakDays} days",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        item {
            Column {
                Text("Badges", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.badges.forEach { badge ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = badge,
                                tint = Color(0xFFDCE775),
                                modifier = Modifier.size(40.dp),
                            )
                            Text(badge, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        item { HorizontalDivider() }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Manage Friends", style = MaterialTheme.typography.titleMedium)
                Text("Share your code", style = MaterialTheme.typography.labelSmall)
                Text(
                    state.myCode,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it },
                    label = { Text("Add friend by code") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        onSendFriendRequest(codeInput.trim())
                        codeInput = ""
                    },
                    enabled = codeInput.isNotBlank(),
                    modifier = Modifier.padding(start = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonGreen,
                        contentColor = YellowMain
                    )
                ) {
                    Text("Send")
                }
            }
        }

        if (state.incomingRequests.isNotEmpty()) {
            item { Text("Requests", style = MaterialTheme.typography.labelLarge) }
            items(state.incomingRequests) { request ->
                FriendRow(
                    friend = request,
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onDeclineRequest(request.code) }) {
                                Text("Decline")
                            }
                            Button(
                                onClick = { onAcceptRequest(request.code) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ButtonGreen,
                                    contentColor = YellowMain
                                )
                            ) {
                                Text("Accept")
                            }
                        }
                    },
                )
            }
        }

        if (state.outgoingRequests.isNotEmpty()) {
            item { Text("Pending", style = MaterialTheme.typography.labelLarge) }
            items(state.outgoingRequests) { request ->
                Text(
                    "${request.name.ifBlank { request.code }} — waiting for them to accept",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        item { Text("Your friends", style = MaterialTheme.typography.labelLarge) }
        items(state.friends) { friend ->
            FriendRow(
                friend = friend,
                trailing = {
                    Button(
                        onClick = { /* nudge not implemented yet */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonGreen,
                            contentColor = YellowMain
                        )
                    ) {
                        Text("Nudge!")
                    }
                },
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonGreen,
                    contentColor = YellowMain
                )
            ) {
                Text("Sign Out")
            }
        }
    }
}

@Composable
private fun FriendRow(friend: Friend, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(friend.name.ifBlank { friend.code })
        trailing()
    }
}

private fun formatKm(meters: Double): String = String.format(Locale.US, "%.2f", meters / 1000.0)