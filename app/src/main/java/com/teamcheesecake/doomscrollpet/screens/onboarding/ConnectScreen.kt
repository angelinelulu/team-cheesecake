package com.teamcheesecake.doomscrollpet.screens.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Screen-time access is a real Android special permission (opens system settings).
 * Health Connect needs its own SDK + permission contract, not wired up yet —
 * the button here just flips the "connected" flag so the rest of the app has
 * something to key off of.
 */
@Composable
fun ConnectScreen(
    healthConnected: Boolean,
    screenTimeConnected: Boolean,
    onToggleHealth: () -> Unit,
    onRequestScreenTimeAccess: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(text = "Connect your data", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Your pet's health is driven by how you actually use your phone.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )

        ConnectRow(
            title = "Screen time",
            description = "Lets us see doomscroll minutes vs. time on apps you want to grow.",
            connected = screenTimeConnected,
            onClick = {
                onRequestScreenTimeAccess()
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            },
        )

        ConnectRow(
            title = "Health app",
            description = "Steps and breaks help your pet recover.",
            connected = healthConnected,
            onClick = onToggleHealth,
        )

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
        ) {
            Text("Get started")
        }
    }
}

@Composable
private fun ConnectRow(
    title: String,
    description: String,
    connected: Boolean,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(text = description, style = MaterialTheme.typography.labelSmall)
            }
            Button(onClick = onClick) {
                Text(if (connected) "Connected ✓" else "Connect")
            }
        }
    }
}
