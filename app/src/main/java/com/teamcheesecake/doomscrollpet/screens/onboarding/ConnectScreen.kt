package com.teamcheesecake.doomscrollpet.screens.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.teamcheesecake.doomscrollpet.ui.theme.ButtonGreen
import com.teamcheesecake.doomscrollpet.ui.theme.YellowMain

/**
 * Screen time is a special Android permission (opens system settings, we re-check on resume
 * since there's no callback for it). Distance traveled comes from GPS location polling
 * (already running for the friend-proximity feature) rather than a separate health app.
 */
@Composable
fun ConnectScreen(
    screenTimeConnected: Boolean,
    onCheckScreenTimeAccess: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onCheckScreenTimeAccess()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YellowMain)
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
            buttonLabel = "Open settings",
            onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
        )

        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(
                containerColor = ButtonGreen,
                contentColor = YellowMain
            ),
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
    buttonLabel: String,
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
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonGreen,
                    contentColor = YellowMain
                )
            ) {
                Text(if (connected) "Connected ✓" else buttonLabel)
            }
        }
    }
}
