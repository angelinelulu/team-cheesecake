package com.teamcheesecake.doomscrollpet.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import com.teamcheesecake.doomscrollpet.R
import com.teamcheesecake.doomscrollpet.model.Animal
import com.teamcheesecake.doomscrollpet.model.FriendPetStatus
import com.teamcheesecake.doomscrollpet.model.PetMood

@Composable
fun ParkScreen(
    friendPetStatuses: List<FriendPetStatus>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text("Pet Park", style = MaterialTheme.typography.headlineSmall)
        }

        if (friendPetStatuses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No friends yet — add some from your profile!")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(friendPetStatuses) { friend ->
                    FriendPetCard(friend)
                }
            }
        }
    }
}

@Composable
private fun FriendPetCard(friend: FriendPetStatus) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "${friend.ownerName}'s ${friend.animal.displayName}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                moodLabel(friend.mood),
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(modifier = Modifier.height(8.dp))

            StatBar(label = "Health", value = friend.health)
            Spacer(modifier = Modifier.height(4.dp))
            // Placeholder: no separate happiness stat exists yet, reusing health.
            StatBar(label = "Happiness", value = friend.health)

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AnimalVisual(animal = friend.animal)
            }
        }
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
                modifier = Modifier.size(96.dp),
            )
        }
        else -> {
            // Placeholder until real art exists for this animal.
            Text(text = animal.emoji, style = MaterialTheme.typography.displayLarge)
        }
    }
}

@Composable
private fun StatBar(label: String, value: Int) {
    Column {
        Text("$label: $value/100", style = MaterialTheme.typography.labelSmall)
        LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun moodLabel(mood: PetMood): String = when (mood) {
    PetMood.THRIVING -> "Thriving"
    PetMood.OKAY -> "Doing okay"
    PetMood.SICK -> "Getting sick"
    PetMood.CRITICAL -> "Critical!"
}