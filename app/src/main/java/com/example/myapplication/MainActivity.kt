package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.services.MusicService
import com.example.myapplication.ui.allsongs.AllSongsScreen
import com.example.myapplication.ui.player.PlayerScreen
import com.example.myapplication.ui.playlist.PlaylistScreen
import com.example.myapplication.ui.settings.SettingsScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.timer.TimerScreen

enum class Screen {
    Main,
    AllSongs,
    Settings
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentScreen by rememberSaveable { mutableStateOf(Screen.Main) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (currentScreen) {
                        Screen.Main -> MainScreen(
                            modifier = Modifier.padding(innerPadding),
                            onNavigateToAllSongs = { currentScreen = Screen.AllSongs },
                            onNavigateToSettings = { currentScreen = Screen.Settings }
                        )
                        Screen.AllSongs -> AllSongsScreen(
                            modifier = Modifier.padding(innerPadding),
                            onNavigateBack = { currentScreen = Screen.Main }
                        )
                        Screen.Settings -> SettingsScreen(
                            modifier = Modifier.padding(innerPadding),
                            onNavigateBack = { currentScreen = Screen.Main }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onNavigateToAllSongs: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 3 })

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val intent = Intent(context, MusicService::class.java)
            context.startService(intent)
        }
    }

    LaunchedEffect(Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        permissionLauncher.launch(permission)
    }

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> TimerScreen()
                1 -> PlayerScreen()
                2 -> PlaylistScreen(
                    onNavigateToAllSongs = onNavigateToAllSongs,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        MainScreen(onNavigateToAllSongs = {}, onNavigateToSettings = {})
    }
}
