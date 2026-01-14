package com.example.myapplication.ui.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun TimerScreen(modifier: Modifier = Modifier) {
    var totalTime by remember { mutableStateOf(25 * 60L) }
    var remainingTime by remember { mutableStateOf(totalTime) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = remainingTime, key2 = isRunning) {
        if (isRunning && remainingTime > 0) {
            delay(1000L)
            remainingTime--
        } else if (remainingTime == 0L) {
            isRunning = false
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = remainingTime.toFormattedTimeString(),
            fontSize = 72.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Slider(
            value = (totalTime / 60).toFloat(),
            onValueChange = {
                if (!isRunning) {
                    totalTime = (it.toLong() * 60)
                    remainingTime = totalTime
                }
            },
            valueRange = 1f..60f,
            steps = 59
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row {
            Button(onClick = { isRunning = true }, enabled = !isRunning && remainingTime > 0) {
                Text(text = "开始")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { isRunning = false }, enabled = isRunning) {
                Text(text = "暂停")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = {
                isRunning = false
                remainingTime = totalTime
            }) {
                Text(text = "停止")
            }
        }
    }
}

private fun Long.toFormattedTimeString(): String {
    val minutes = TimeUnit.SECONDS.toMinutes(this)
    val seconds = this % 60
    return String.format("%02d:%02d", minutes, seconds)
}
