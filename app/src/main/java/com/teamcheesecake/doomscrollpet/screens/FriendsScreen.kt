package com.teamcheesecake.doomscrollpet.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamcheesecake.doomscrollpet.model.Friend

@Composable
fun FriendsScreen(
    myCode: String,
    friends: List<Friend>,
    onAddFriend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var codeInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Your code", style = MaterialTheme.typography.labelSmall)
        Text(text = myCode, style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Share this with friends so they can add you.",
            style = MaterialTheme.typography.labelSmall,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = codeInput,
                onValueChange = { codeInput = it },
                label = { Text("Friend's code") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    onAddFriend(codeInput)
                    codeInput = ""
                },
                enabled = codeInput.isNotBlank(),
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text("Add")
            }
        }

        Text(text = "Friends", style = MaterialTheme.typography.labelSmall)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(friends) { friend ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${friend.name.ifBlank { friend.code }} — ${friendStatus(friend)}",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

private fun friendStatus(friend: Friend): String {
    val distance = friend.distanceMeters ?: return "waiting for location…"
    return if (friend.isNearby) "nearby 📍 (${distance.toInt()}m)" else "${(distance / 1000).toInt()}km away"
}
