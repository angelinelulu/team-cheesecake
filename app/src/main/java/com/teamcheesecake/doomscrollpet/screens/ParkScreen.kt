package com.teamcheesecake.doomscrollpet.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import com.teamcheesecake.doomscrollpet.R
import com.teamcheesecake.doomscrollpet.model.Animal
import com.teamcheesecake.doomscrollpet.model.FriendPetStatus
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.offset

@Composable
fun ParkScreen(
    friendPetStatuses: List<FriendPetStatus>,
    onGiveTreat: (String) -> Unit,
    onNudge: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedFriendCode by remember { mutableStateOf<String?>(null) }
    val selectedFriend = friendPetStatuses.firstOrNull { it.code == selectedFriendCode }

    Box(modifier = modifier.fillMaxSize()) {
        // --- Full-screen background scene ---
        Image(
            painter = painterResource(id = R.drawable.park_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // --- Back button, fixed top-left ---
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }

        // --- Sign + banner, fixed near the top ---
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFDDBB88), RoundedCornerShape(12.dp))
                    .padding(horizontal = 32.dp, vertical = 12.dp),
            ) {
                Text(
                    "Pet Park",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3B5D3B),
                )
            }
        }

        // --- Pets, pinned to the bottom ---
        if (friendPetStatuses.isEmpty()) {
            Text(
                "No friends yet — add some from your profile!",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .padding(horizontal = 24.dp),
            )
        } else {
            LazyRow(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(friendPetStatuses) { friend ->
                    ScatteredPet(friend, onClick = { selectedFriendCode = friend.code })
                }
            }
        }

        selectedFriend?.let { friend ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(onClick = { selectedFriendCode = null }),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    modifier = Modifier
                        .padding(32.dp)
                        .clickable(enabled = false) {}, // absorb clicks so tapping the card doesn't dismiss it
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(48.dp),
                            )
                            Text(
                                "${friend.health}%",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${friend.ownerName}'s ${friend.animal.displayName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Row {
                            val filledHearts = (friend.health / 20).coerceIn(0, 5)
                            repeat(5) { index ->
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = if (index < filledHearts) Color(0xFFE53935) else Color.LightGray,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .padding(horizontal = 2.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        HealthHappinessBar(health = friend.health, happiness = friend.happiness)

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Productivity Percentage: ${productivityPercent(friend.doomscrollMinutesToday)}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { onGiveTreat(friend.code) }) {
                                Text("Give Treat!")
                            }
                            Button(onClick = { onNudge(friend.code) }) {
                                Text("Nudge!")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScatteredPet(friend: FriendPetStatus, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFE53935),
                modifier = Modifier.size(48.dp),
            )
            Text(
                "${friend.health}%",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
            )
        }

        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimalVisual(animal = friend.animal, health = friend.health)
        }

        Text(
            "${friend.ownerName}'s ${friend.animal.displayName}",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun AnimalVisual(animal: Animal, health: Int) {
    val isSick = health < 20 // adjust threshold to match whatever "sick" means elsewhere in your app

    val gifRes: Int? = when (animal) {
        Animal.DOG -> if (isSick) R.drawable.dog_character_sick else R.drawable.dog_character
        Animal.CAT -> R.drawable.cat_character // no sick variant yet — always healthy gif
        else -> null
    }

    if (gifRes != null) {
        val context = LocalContext.current
        val imageLoader = remember {
            ImageLoader.Builder(context)
                .components { add(GifDecoder.Factory()) }
                .build()
        }
        Image(
            painter = rememberAsyncImagePainter(model = gifRes, imageLoader = imageLoader),
            contentDescription = animal.displayName,
            modifier = Modifier.size(120.dp),
        )
    } else {
        Text(text = animal.emoji, style = MaterialTheme.typography.displayLarge)
    }
}

@Composable
private fun HealthHappinessBar(health: Int, happiness: Int) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp), // extra vertical room for the markers above/below
    ) {
        val barWidth = maxWidth

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFD32F2F),
                            Color(0xFFFF9800),
                            Color(0xFFFFEB3B),
                            Color(0xFF8BC34A),
                            Color(0xFF4CAF50),
                        ),
                    ),
                ),
        )

        // Health marker, above the bar.
        Icon(
            Icons.Filled.ArrowDropDown,
            contentDescription = "Health",
            tint = Color.Black,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = barWidth * (health / 100f) - 12.dp),
        )

        // Happiness marker, below the bar.
        Icon(
            Icons.Filled.ArrowDropUp,
            contentDescription = "Happiness",
            tint = Color(0xFF3949AB),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = barWidth * (happiness / 100f) - 12.dp),
        )
    }
}

private fun productivityPercent(doomscrollMinutes: Int): Int =
    (100 - (doomscrollMinutes * 100 / DOOMSCROLL_LIMIT_MINUTES)).coerceIn(0, 100)

// Matches the app's default doomscroll limit (see PetUiState.doomscrollLimitMinutes).
private const val DOOMSCROLL_LIMIT_MINUTES = 60