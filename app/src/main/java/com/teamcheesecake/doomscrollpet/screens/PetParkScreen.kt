package com.teamcheesecake.doomscrollpet.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teamcheesecake.doomscrollpet.model.PetMood

/**
 * Represents a single friend's pet as shown in the Pet Park.
 * Replace/merge this with your real data model if you already
 * have something similar (e.g. sourced from a FriendsRepository).
 */
data class FriendPetUiState(
    val friendName: String,
    val petEmoji: String,
    val animalDisplayName: String,
    val health: Int, // 0-100, drives the heart badge percentage
    val mood: PetMood,
)

@Composable
fun PetParkScreen(
    friends: List<FriendPetUiState>,
    onBack: () -> Unit,
    onFriendClick: (FriendPetUiState) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {

        // Scenic sky/hills/grass background
        ParkBackground(modifier = Modifier.fillMaxSize())

        // Back button, floating over the scene
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(50)),
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }

        // Hanging wooden sign
        ParkSign(
            text = "Pet Park",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
        )
    }
}

/** Fractional (x, y) positions within the screen where pets are placed, park-bench style. */
private fun parkSlots(): List<Pair<Float, Float>> = listOf(
    0.08f to 0.50f,
    0.55f to 0.44f,
    0.50f to 0.72f,
    0.15f to 0.78f,
    0.68f to 0.60f,
    0.05f to 0.66f,
)

@Composable
private fun ParkSign(text: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        // hanging strings
        Row(horizontalArrangement = Arrangement.spacedBy(64.dp)) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(18.dp)
                        .background(Color(0xFF6B4A2F)),
                )
            }
        }
        Box(
            modifier = Modifier
                .background(Color(0xFFC08A52), RoundedCornerShape(6.dp))
                .border(3.dp, Color(0xFF6B4A2F), RoundedCornerShape(6.dp))
                .padding(horizontal = 28.dp, vertical = 14.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF3B6B3B),
            )
        }
    }
}

@Composable
private fun PetMarker(
    friend: FriendPetUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .background(Color.White, RoundedCornerShape(4.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            // Swap this for a hand-drawn line-art icon (per-animal drawable) to match
            // the sketch style in the mockup; emoji is a placeholder.
            Text(text = friend.petEmoji, style = MaterialTheme.typography.displaySmall)
        }
        HeartBadge(
            percent = friend.health,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-12).dp, y = (-12).dp),
        )
    }
}

@Composable
private fun HeartBadge(percent: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val heart = Path().apply {
                moveTo(w / 2f, h * 0.92f)
                cubicTo(-w * 0.1f, h * 0.55f, w * 0.15f, -h * 0.05f, w / 2f, h * 0.30f)
                cubicTo(w * 0.85f, -h * 0.05f, w * 1.1f, h * 0.55f, w / 2f, h * 0.92f)
                close()
            }
            drawPath(heart, color = Color(0xFFE53935))
            drawPath(heart, color = Color.White, style = Stroke(width = 3f))
        }
        Text(
            text = "$percent%",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun ParkBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Sky-to-grass gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFC9E4D6), Color(0xFFD7E9C9), Color(0xFFA9CE8E)),
                startY = 0f,
                endY = h,
            ),
        )

        // Clouds
        fun cloud(cx: Float, cy: Float, scale: Float) {
            val c = Color.White.copy(alpha = 0.9f)
            drawOval(c, topLeft = Offset(cx - 40 * scale, cy - 18 * scale), size = Size(90 * scale, 40 * scale))
            drawOval(c, topLeft = Offset(cx - 10 * scale, cy - 30 * scale), size = Size(70 * scale, 50 * scale))
            drawOval(c, topLeft = Offset(cx - 60 * scale, cy - 10 * scale), size = Size(70 * scale, 34 * scale))
        }
        cloud(w * 0.28f, h * 0.16f, 1.1f)
        cloud(w * 0.72f, h * 0.26f, 0.9f)
        cloud(w * 0.5f, h * 0.10f, 0.7f)

        // Distant hills
        val hills = Path().apply {
            moveTo(0f, h * 0.55f)
            cubicTo(w * 0.25f, h * 0.48f, w * 0.4f, h * 0.58f, w * 0.65f, h * 0.5f)
            cubicTo(w * 0.8f, h * 0.46f, w * 0.9f, h * 0.52f, w, h * 0.5f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(hills, color = Color(0xFF9FC98A).copy(alpha = 0.6f))

        // Foreground grass
        val grass = Path().apply {
            moveTo(0f, h * 0.65f)
            cubicTo(w * 0.3f, h * 0.6f, w * 0.6f, h * 0.68f, w, h * 0.62f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(grass, color = Color(0xFF8BBF64))

        // Little flowers dotted in the grass
        val flowerSpots = listOf(
            0.10f to 0.90f, 0.25f to 0.95f, 0.40f to 0.88f,
            0.60f to 0.93f, 0.75f to 0.87f, 0.90f to 0.94f,
        )
        flowerSpots.forEach { (fx, fy) ->
            drawCircle(Color.White, radius = 4f, center = Offset(w * fx, h * fy))
            drawCircle(Color(0xFFFFD54F), radius = 1.5f, center = Offset(w * fx, h * fy))
        }
    }
}