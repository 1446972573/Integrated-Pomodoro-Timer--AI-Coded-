package com.example.myapplication.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import java.util.concurrent.TimeUnit

@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = viewModel()
) {
    val uiState by playerViewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Vinyl/CD animation placeholder
        Box(
            modifier = Modifier
                .size(250.dp)
                .clip(CircleShape)
                .background(Color.DarkGray)
        ) { /* Animation here */ }

        Spacer(modifier = Modifier.height(32.dp))

        // Progress Bar and Time
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = if (uiState.duration > 0) uiState.currentPosition.toFloat() / uiState.duration.toFloat() else 0f,
                onValueChange = {},
                enabled = false
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = uiState.currentPosition.toFormattedTimeString())
                Text(text = uiState.duration.toFormattedTimeString())
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Control Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { playerViewModel.skipPrevious() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
            }
            IconButton(onClick = { playerViewModel.playPause() }, modifier = Modifier.size(72.dp)) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    modifier = Modifier.fillMaxSize()
                )
            }
            IconButton(onClick = { playerViewModel.skipNext() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { playerViewModel.toggleShuffleMode() }) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (uiState.shuffleModeEnabled) MaterialTheme.colorScheme.primary else Color.Unspecified
                )
            }
            IconButton(onClick = { playerViewModel.cycleRepeatMode() }) {
                val (icon, tint) = when (uiState.repeatMode) {
                    Player.REPEAT_MODE_ONE -> Pair(Icons.Default.RepeatOne, MaterialTheme.colorScheme.primary)
                    Player.REPEAT_MODE_ALL -> Pair(Icons.Default.Repeat, MaterialTheme.colorScheme.primary)
                    else -> Pair(Icons.Default.Repeat, Color.Unspecified)
                }
                Icon(icon, contentDescription = "Repeat", tint = tint)
            }
        }
    }
}

private fun Long.toFormattedTimeString(): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
