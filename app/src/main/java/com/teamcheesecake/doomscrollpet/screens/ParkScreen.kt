package com.teamcheesecake.doomscrollpet.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun ParkScreen(
    friendPetStatuses: List<FriendPetStatus>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    ScatteredPet(friend)
                }
            }
        }
    }
}

@Composable
private fun ScatteredPet(friend: FriendPetStatus) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            AnimalVisual(animal = friend.animal, health = friend.health, size = 120.dp)
        }

        Text(
            "${friend.ownerName}'s ${friend.animal.displayName}",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
internal fun AnimalVisual(animal: Animal, health: Int, size: androidx.compose.ui.unit.Dp = 240.dp) {
    val isDogSick = health < 60
    val isCatSick = health < 20

    val gifRes: Int? = when (animal) {
        Animal.DOG -> if (isDogSick) R.drawable.dog_character_sick else R.drawable.dog_character
        Animal.CAT -> if (isCatSick) R.drawable.cat_character_sick else R.drawable.cat_character
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
            modifier = Modifier.size(size),
        )
    } else {
        Text(text = animal.emoji, style = MaterialTheme.typography.displayLarge)
    }
}