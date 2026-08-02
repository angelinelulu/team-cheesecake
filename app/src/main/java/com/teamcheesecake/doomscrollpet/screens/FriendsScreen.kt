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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamcheesecake.doomscrollpet.model.Friend
import com.teamcheesecake.doomscrollpet.ui.theme.ButtonGreen
import com.teamcheesecake.doomscrollpet.ui.theme.PetText
import com.teamcheesecake.doomscrollpet.ui.theme.YellowMain

@Composable
fun FriendsScreen(
    myCode: String,
    friends: List<Friend>,
    incomingRequests: List<Friend>,
    outgoingRequests: List<Friend>,
    onSendFriendRequest: (String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onDeclineRequest: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
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

        if (incomingRequests.isNotEmpty()) {
            Text(text = "Requests", style = MaterialTheme.typography.labelSmall)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(incomingRequests) { request ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = request.name.ifBlank { request.code })
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onDeclineRequest(request.code) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PetText)
                                ) {
                                    Text("Decline")
                                }
                                Button(
                                    onClick = { onAcceptRequest(request.code) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ButtonGreen,
                                        contentColor = YellowMain
                                    )
                                ) {
                                    Text("Accept")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (outgoingRequests.isNotEmpty()) {
            Text(text = "Pending", style = MaterialTheme.typography.labelSmall)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(outgoingRequests) { request ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${request.name.ifBlank { request.code }} — waiting for them to accept",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }

        Text(text = "Friends", style = MaterialTheme.typography.labelSmall)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(friends) { friend ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = friend.name.ifBlank { friend.code },
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}