package com.teamcheesecake.doomscrollpet.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamcheesecake.doomscrollpet.model.Friend

@Composable
fun FriendsScreen(friends: List<Friend>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Friends")
        friends.forEach { friend ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${friend.name} — ${if (friend.isNearby) "nearby 📍" else "not nearby"}",
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
