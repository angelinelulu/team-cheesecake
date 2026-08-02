package com.teamcheesecake.doomscrollpet.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.teamcheesecake.doomscrollpet.ui.theme.ButtonGreen
import com.teamcheesecake.doomscrollpet.ui.theme.YellowMain
import com.teamcheesecake.doomscrollpet.model.Animal
import com.teamcheesecake.doomscrollpet.screens.animalDrawableRes

@Composable
fun AnimalScreen(
    selected: Animal,
    onSelect: (Animal) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YellowMain)
            .padding(24.dp),
    ) {
        Text(text = "Choose your pet", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "You'll be looking after this one.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(Animal.entries) { animal ->
                val isSelected = animal == selected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onSelect(animal) }
                        .padding(12.dp),
                ) {
                    val imageSize = if (animal == Animal.CAT) 95.dp else 100.dp
                    Image(
                        // Coil's default (non-GIF) loader — a static frame, not an animation.
                        painter = rememberAsyncImagePainter(model = animalDrawableRes(animal, 100)),
                        contentDescription = animal.displayName,
                        modifier = Modifier.size(imageSize),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = animal.displayName, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(
                containerColor = ButtonGreen,
                contentColor = YellowMain
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Text("Continue")
        }
    }
}
