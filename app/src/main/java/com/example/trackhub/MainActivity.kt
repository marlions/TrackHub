package com.example.trackhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.trackhub.ui.theme.TrackHubTheme

class MainActivity : ComponentActivity() {
    private val sessionViewModel: SessionViewModel by viewModels()
    private val tracksViewModel: TracksViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            TrackHubTheme {
                TrackHubApp(
                    sessionViewModel = sessionViewModel,
                    tracksViewModel = tracksViewModel,
                    playerViewModel = playerViewModel
                )
            }
        }
    }
}

@Composable
fun TrackHubApp(
    sessionViewModel: SessionViewModel,
    tracksViewModel: TracksViewModel,
    playerViewModel: PlayerViewModel
) {
    val accessToken = sessionViewModel.accessToken

    if (accessToken == null) {
        AuthScreen(
            onAuthSuccess = { token ->
                sessionViewModel.saveAccessToken(token)
            }
        )
    } else {
        CatalogScreen(
            accessToken = accessToken,
            tracksViewModel = tracksViewModel,
            playerViewModel = playerViewModel,
            onLogout = {
                playerViewModel.pause()
                sessionViewModel.clearAccessToken()
            }
        )
    }
}
