package com.teamcheesecake.doomscrollpet.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.teamcheesecake.doomscrollpet.model.Animal
import com.teamcheesecake.doomscrollpet.R
import com.teamcheesecake.doomscrollpet.model.PetMood
import com.teamcheesecake.doomscrollpet.model.PetUiState
import com.teamcheesecake.doomscrollpet.ui.theme.ButtonGreen
import com.teamcheesecake.doomscrollpet.ui.theme.PetText
import com.teamcheesecake.doomscrollpet.AudioManager
import com.teamcheesecake.doomscrollpet.ui.theme.YellowBack
import com.teamcheesecake.doomscrollpet.ui.theme.YellowMain
import kotlinx.coroutines.delay
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
fun PetActionBottomBar(
    state: PetUiState,
    onFood: () -> Unit,
    onWater: () -> Unit,
    onExercise: () -> Unit
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(YellowBack)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ActionIcon(
            label = "Food",
            iconRes = R.drawable.feed_icon,
            enabled = state.canFeedTreat,
            cooldownMillis = getCooldownMillis(state.lastFedTimestamp, currentTime),
            onClick = onFood
        )
        ActionIcon(
            label = "Water",
            iconRes = R.drawable.water_icon,
            enabled = state.canGiveWater,
            cooldownMillis = getCooldownMillis(state.lastWaterTimestamp, currentTime),
            onClick = onWater
        )
        ActionIcon(label = "Exercise", iconRes = R.drawable.exercise_icon, onClick = onExercise)
    }
}

private fun getCooldownMillis(lastTimestamp: Long, currentTime: Long): Long {
    val cooldownPeriod = 24 * 60 * 60 * 1000L
    val elapsed = currentTime - lastTimestamp
    return if (elapsed < cooldownPeriod) cooldownPeriod - elapsed else 0L
}

private fun formatCooldown(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return String.format(Locale.US, "%dh %dm", hours, minutes)
}

@Composable
fun PetHomeScreen(
    state: PetUiState,
    myCode: String,
    onSendFriendRequest: (String) -> Unit,
    onOpenSettings: () -> Unit = {},
    onSignOut: () -> Unit,
    onSelectAnimal: (Animal) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToPark: () -> Unit,
    onMarkRewardSeen: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var profileMenuExpanded by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }

    // Logic to show productivity reward if we have new minutes since last seen
    if (state.moreAppMinutesToday > state.lastSeenRewardMinutes) {
        ProductivityRewardDialog(
            minutes = state.moreAppMinutesToday.toLong(),
            onDismiss = { onMarkRewardSeen(state.moreAppMinutesToday) }
        )
    }

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
                IconButton(onClick = {
                    AudioManager.playButtonTap()
                    onOpenSettings()
                }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
                Text(
                    text = "SNOOT",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )

                Box {
                    IconButton(onClick = {
                        AudioManager.playButtonTap()
                        profileMenuExpanded = true
                    }) {
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
                                AudioManager.playButtonTap()
                                profileMenuExpanded = false
                                showAddFriendDialog = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("View Profile") },
                            leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                            onClick = {
                                AudioManager.playButtonTap()
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
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = {
                        AudioManager.playButtonTap()
                        onNavigateToPark()
                    }),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.park_icon),
                        contentDescription = "Pet Park",
                        modifier = Modifier.size(32.dp),
                    )
                    Text(text = "Pet Park", style = MaterialTheme.typography.labelSmall)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        AudioManager.playButtonTap()
                        showPopup = true
                    },
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
                    var showCareReaction by remember { mutableStateOf(false) }
                    LaunchedEffect(state.careReactionTrigger) {
                        if (state.careReactionTrigger > 0 && state.animal == Animal.DOG) {
                            showCareReaction = true
                            delay(1500)
                            showCareReaction = false
                        }
                    }

                    if (showCareReaction) {
                        val context = LocalContext.current
                        val imageLoader = remember {
                            ImageLoader.Builder(context)
                                .components { add(GifDecoder.Factory()) }
                                .build()
                        }
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = R.drawable.dog_eating,
                                imageLoader = imageLoader,
                            ),
                            contentDescription = "${state.animal.displayName} reacting",
                            modifier = Modifier.size(240.dp),
                        )
                    } else {
                        AnimalVisual(animal = state.animal, health = state.health)
                    }

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
            title = {
                Text(
                    text = "Swap Pet",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.cat),
                        contentDescription = "Cat Pet",
                        modifier = Modifier
                            .size(120.dp)
                            .padding(bottom = 12.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = "Would you like to swap to the Cat?",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSelectAnimal(Animal.CAT)
                        AudioManager.playButtonTap()
                        showPopup = false
                    }
                ) {
                    Text("Select Cat")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    AudioManager.playButtonTap()
                    showPopup = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProductivityRewardDialog(
    minutes: Long,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "⭐", style = MaterialTheme.typography.displayMedium)
                Text(text = "Great job!", style = MaterialTheme.typography.headlineMedium)
            }
        },
        text = {
            Text(
                text = "You've spent $minutes productive minute(s) today! Your pet is feeling stronger and happier.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
            ) {
                Text("Keep it up!")
            }
        }
    )
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
                onClick = {
                    AudioManager.playButtonTap()
                    onSubmit(codeInput.trim())
                },
                enabled = codeInput.isNotBlank(),
            ) {
                Text("Send Request")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                AudioManager.playButtonTap()
                onDismiss()
            }) { Text("Cancel") }
        },
    )
}

private fun formatKm(meters: Double): String = String.format(Locale.US, "%.2f", meters / 1000.0)

@Composable
private fun ActionIcon(
    label: String,
    iconRes: Int,
    enabled: Boolean = true,
    cooldownMillis: Long = 0,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = enabled) {
            AudioManager.playButtonTap()
            onClick()
        }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    if (enabled) YellowBack else Color.Gray.copy(alpha = 0.3f),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                modifier = Modifier.size(48.dp),
                alpha = if (enabled) 1f else 0.5f
            )
            if (!enabled && cooldownMillis > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatCooldown(cooldownMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) Color.Unspecified else Color.Gray
        )
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