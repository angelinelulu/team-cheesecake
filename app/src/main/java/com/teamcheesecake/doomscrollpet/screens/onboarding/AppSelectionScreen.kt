package com.teamcheesecake.doomscrollpet.screens.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamcheesecake.doomscrollpet.AudioManager
import com.teamcheesecake.doomscrollpet.model.AppOption
import com.teamcheesecake.doomscrollpet.ui.theme.ButtonGreen
import com.teamcheesecake.doomscrollpet.ui.theme.YellowMain

/**
 * Shared by both the "avoid" and "do more of" onboarding steps — same
 * checklist UI, different title/options/selection passed in. `selected`/`onToggle`
 * key off package name, not display name.
 *
 * [showMuteToggle] additionally shows a "Mute music" switch above the list —
 * pass `true` only when this screen is reached from Settings, not onboarding.
 */
@Composable
fun AppSelectionScreen(
    title: String,
    subtitle: String,
    options: List<AppOption>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    showMuteToggle: Boolean = false,
) {
    var musicMuted by remember { mutableStateOf(AudioManager.isMusicMuted()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YellowMain)
            .padding(24.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )

        if (showMuteToggle) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                Text(text = "Mute music", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = musicMuted,
                    onCheckedChange = { isMuted ->
                        AudioManager.playButtonTap()
                        musicMuted = isMuted
                        AudioManager.setMusicMuted(isMuted)
                    },
                )
            }
            HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(options) { app ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Checkbox(
                        checked = selected.contains(app.packageName),
                        onCheckedChange = {
                            AudioManager.playButtonTap()
                            onToggle(app.packageName)
                        },
                    )
                    Text(text = app.displayName)
                }
            }
        }

        Button(
            onClick = {
                AudioManager.playButtonTap()
                onNext()
            },
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