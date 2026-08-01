package com.teamcheesecake.doomscrollpet.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Park
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teamcheesecake.doomscrollpet.model.PetUiState
import com.teamcheesecake.doomscrollpet.ui.theme.YellowBack
import com.teamcheesecake.doomscrollpet.ui.theme.YellowMain
import com.teamcheesecake.doomscrollpet.ui.theme.ParkGreen
import com.teamcheesecake.doomscrollpet.ui.theme.ParkGreenLight
import com.teamcheesecake.doomscrollpet.ui.theme.ParkGreenMedium
import com.teamcheesecake.doomscrollpet.ui.theme.ButtonGreen
import com.teamcheesecake.doomscrollpet.ui.theme.PetText
import java.util.Locale

data class NearbyPark(
    val name: String,
    val distanceKm: Double,
    val description: String,
)

// Hardcoded for the GridAKL / John Lysaght Startup Coworking Space demo — 101 Pakenham
// Street West, Wynyard Quarter, Auckland. Distances are approximate straight-line figures
// from that address, not live-computed.
private val NEARBY_PARKS = listOf(
    NearbyPark("Silo Park", 0.2, "Waterfront park right next to Wynyard Quarter, lots of wonderful harbour views."),
    NearbyPark("Victoria Park", 1.2, "Hangout with friends! Large area with sports fields and playground!"),
    NearbyPark("Western Park", 2.0, "Historic park in Ponsonby with quiet walking trails."),
    NearbyPark("Point Erin Park", 2.8, "Harbour-front park with a public pool, playground, and open lawns."),
)

@Composable
fun NearbyParksScreen(
    state: PetUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ParkGreen),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.CenterStart)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }

            // --- Board Header ---
            Box(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .background(Color(0xFFDDBB88), RoundedCornerShape(12.dp))
                    .padding(horizontal = 32.dp, vertical = 12.dp)
                    .align(Alignment.Center),
            ) {
                Text(
                    "Exercise",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3B5D3B),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Distance Section ---
        Text(
            text = "Distance Covered Today: ${formatKm(state.distanceMetersToday)} km",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = PetText
        )

        if (state.distanceMetersToday < 1000) {
            Text(
                text = "Low Distance Walked",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFF44336),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Text(
            text = "Getting fresh air makes your pet happy!",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = PetText,
            modifier = Modifier.padding(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Suggested Parks Nearby",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PetText,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(NEARBY_PARKS) { index, park ->
                val cardColor = when (index) {
                    0 -> YellowBack
                    1 -> ParkGreenLight
                    2 -> ParkGreenMedium
                    else -> ButtonGreen
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Park,
                            contentDescription = null,
                            tint = PetText,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = park.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PetText
                            )
                            Text(
                                text = "${park.distanceKm} km away",
                                style = MaterialTheme.typography.labelMedium,
                                color = PetText
                            )
                            Text(
                                text = park.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = PetText
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatKm(meters: Double): String = String.format(Locale.US, "%.2f", meters / 1000.0)
