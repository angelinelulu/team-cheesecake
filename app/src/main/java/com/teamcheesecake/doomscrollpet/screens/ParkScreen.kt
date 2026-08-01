package com.teamcheesecake.doomscrollpet.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

private val SkyGreen = Color(0xFFD7E8C9)
private val GrassGreen = Color(0xFF8FBF6B)
private val RowHeight = 220.dp
private val HeaderHeight = 160.dp

@Composable
fun ParkScreen(
    friendPetStatuses: List<FriendPetStatus>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = (friendPetStatuses.size + 1) / 2
    val contentHeight = HeaderHeight + RowHeight * rows.coerceAtLeast(1) + 60.dp

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .height(contentHeight)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SkyGreen, SkyGreen, GrassGreen),
                        startY = 0f,
                    ),
                ),
        ) {
            // --- Hanging sign ---
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

            if (friendPetStatuses.isEmpty()) {
                Text(
                    "No friends yet — add some from your profile!",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
            } else {
                friendPetStatuses.forEachIndexed { index, friend ->
                    val isLeftColumn = index % 2 == 0
                    val row = index / 2
                    ScatteredPet(
                        friend = friend,
                        modifier = Modifier
                            .align(if (isLeftColumn) Alignment.TopStart else Alignment.TopEnd)
                            .padding(
                                start = if (isLeftColumn) 32.dp else 0.dp,
                                end = if (!isLeftColumn) 32.dp else 0.dp,
                                top = HeaderHeight + RowHeight * row,
                            ),
                    )
                }
            }
        }

        // Back button stays fixed on top, not part of the scrolling scene.
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }
    }
}

@Composable
private fun ScatteredPet(friend: FriendPetStatus, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
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

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            AnimalVisual(animal = friend.animal)
        }

        Text(
            "${friend.ownerName}'s ${friend.animal.displayName}",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun AnimalVisual(animal: Animal) {
    when (animal) {
        Animal.DOG -> {
            val context = LocalContext.current
            val imageLoader = remember {
                ImageLoader.Builder(context)
                    .components { add(GifDecoder.Factory()) }
                    .build()
            }
            Image(
                painter = rememberAsyncImagePainter(
                    model = R.drawable.dog_character,
                    imageLoader = imageLoader,
                ),
                contentDescription = "Dog",
                modifier = Modifier.size(80.dp),
            )
        }
        else -> {
            Text(text = animal.emoji, style = MaterialTheme.typography.displayLarge)
        }
    }
}