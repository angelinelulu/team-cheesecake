package com.teamcheesecake.doomscrollpet.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Park
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamcheesecake.doomscrollpet.ui.theme.YellowMain

data class NearbyPark(
    val name: String,
    val distanceKm: Double,
    val description: String,
)

// Hardcoded for the GridAKL / John Lysaght Startup Coworking Space demo — 101 Pakenham
// Street West, Wynyard Quarter, Auckland. Distances are approximate straight-line figures
// from that address, not live-computed.
private val NEARBY_PARKS = listOf(
    NearbyPark("Silo Park", 0.2, "Waterfront park right next to Wynyard Quarter — events lawn and harbour views."),
    NearbyPark("Victoria Park", 1.2, "Large green space with sports fields, playground, and walking paths."),
    NearbyPark("Western Park", 2.0, "Historic hillside park in Ponsonby with mature trees and quiet walking trails."),
    NearbyPark("Point Erin Park", 2.8, "Harbour-front park with a public pool, playground, and open lawns."),
)

@Composable
fun NearbyParksScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YellowMain)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text("Parks Near GridAKL", style = MaterialTheme.typography.headlineSmall)
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(NEARBY_PARKS) { park ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Park,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(park.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${park.distanceKm} km away",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                park.description,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}