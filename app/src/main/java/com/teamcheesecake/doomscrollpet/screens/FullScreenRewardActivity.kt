package com.teamcheesecake.doomscrollpet.screens

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.teamcheesecake.doomscrollpet.screens.ProductivityRewardScreen

class FullScreenRewardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure window wakes screen and shows over lockscreen / apps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val minutes = intent.getLongExtra("minutes", 0L)

        setContent {
            MaterialTheme {
                ProductivityRewardScreen(
                    minutes = minutes,
                    onDismiss = {
                        finish() 
                    }
                )
            }
        }
    }
}
