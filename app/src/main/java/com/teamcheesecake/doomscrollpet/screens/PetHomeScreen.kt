package com.teamcheesecake.doomscrollpet.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.teamcheesecake.doomscrollpet.ui.theme.YellowMain
import com.teamcheesecake.doomscrollpet.ui.theme.ButtonGreen
import com.teamcheesecake.doomscrollpet.ui.theme.PetText
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import java.util.Locale
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.teamcheesecake.doomscrollpet.R

@Composable
fun PetActionBottomBar(
    onFood: () -> Unit,
    onWater: () -> Unit,
    onExercise: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(YellowBack)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ActionIcon(label = "Food", iconRes = R.drawable.feed_icon, onClick = onFood)
        ActionIcon(label = "Water", iconRes = R.drawable.water_icon, onClick = onWater)
        ActionIcon(label = "Exercise", iconRes = R.drawable.exercise_icon, onClick = onExercise)
    }
}

@Composable
fun PetHomeScreen(
    state: PetUiState,
    myCode: String,
    onSendFriendRequest: (String) -> Unit,
    onOpenSettings: () -> Unit = {},
    onSignOut: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToPark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var profileMenuExpanded by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {

        // --- Top Bar ---
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
                IconButton(onClick = onOpenSettings) {
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
                        DropdownMenuItem(
                            text = { Text("View Profile") },
                            leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                            onClick = {
                                profileMenuExpanded = false
                                onNavigateToProfile()
                            },
                        )
                    }
                }
            }
            Text(
                text = state.ownerName,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(YellowBack)
                    .padding(bottom = 8.dp),
            )
        }

        // --- Middle Content Area ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .background(YellowMain),
        ) {
            // Pet Park + Swap Pet buttons, side by side and aligned
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = onNavigateToPark),
                ) {
                    Text(text = "\uD83C\uDF33", style = MaterialTheme.typography.headlineSmall)
                    Text(text = "Pet Park", style = MaterialTheme.typography.labelSmall)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { showPopup = true },
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.swap_pets_button),
                        contentDescription = "Swap pet",
                        modifier = Modifier.size(48.dp),
                    )
                    Text(text = "Swap Pet", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Doomscroll Timer: ")
                    }
                    append("${state.doomscrollMinutesToday} / ${state.doomscrollLimitMinutes} min")
                }
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Productivity Timer: ")
                    }
                    append("${state.moreAppMinutesToday} min(s)")
                }
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Distance Covered Today: ")
                    }
                    append("${formatKm(state.distanceMetersToday)} km")
                }
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Streak: ")
                    }
                    append("${state.streakDays} day(s)")
                }
            )
            Text(
                text = "Badges",
                fontWeight = FontWeight.Bold
            )

            if (!state.screenTimeConnected) {
                Text(
                    text = "Not connected: screen time",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hearts row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                val filledHearts = (state.health / 20).coerceIn(0, 5)
                repeat(5) { index ->
                    Text(
                        text = if (index < filledHearts) "\u2764\uFE0F" else "\uD83E\uDD0D",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Health color bar
            LinearProgressIndicator(
                progress = { state.health / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = healthColor(state.health),
                trackColor = Color.LightGray.copy(alpha = 0.5f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.ownerName.isNotBlank()) {
                Text(
                    text = "${state.ownerName}'s ${state.animal.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(YellowMain, RoundedCornerShape(8.dp)),
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
    }

    if (showAddFriendDialog) {
        AddFriendDialog(
            myCode = myCode,
            onDismiss = { showAddFriendDialog = false },
            onSubmit = { code ->
                onSendFriendRequest(code)
                showAddFriendDialog = false
            },
        )
    }

    if (showPopup) {
        AlertDialog(
            onDismissRequest = { showPopup = false },
            title = { Text("Popup Title") },
            text = { Text("Whatever content you want here.") },
            confirmButton = {
                TextButton(onClick = { showPopup = false }) {
                    Text("OK")
                }
            },
        )
    }
}

@Composable
private fun AddFriendDialog(
    myCode: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var codeInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a Friend") },
        text = {
            Column {
                Text(text = "Your code", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = myCode,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Share this with your friend, or enter theirs below. They'll need to accept before you're connected.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
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
                Text("Send Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun formatKm(meters: Double): String = String.format(Locale.US, "%.2f", meters / 1000.0)

@Composable
private fun ActionIcon(label: String, iconRes: Int, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(YellowBack, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                modifier = Modifier.size(48.dp),
            )
        }
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