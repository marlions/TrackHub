package com.example.trackhub

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.trackhub.ui.theme.TrackHubTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val sessionViewModel: SessionViewModel by viewModels()
    private val tracksViewModel: TracksViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()
    private val commentsViewModel: CommentsViewModel by viewModels()
    private val playlistsViewModel: PlaylistsViewModel by viewModels()
    private val usersViewModel: UsersViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.BLACK)
        )

        setContent {
            TrackHubTheme {
                var showStartupSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(1100)
                    showStartupSplash = false
                }

                if (showStartupSplash) {
                    TrackHubStartupSplash()
                } else {
                    TrackHubApp(
                        sessionViewModel = sessionViewModel,
                        tracksViewModel = tracksViewModel,
                        playerViewModel = playerViewModel,
                        commentsViewModel = commentsViewModel,
                        playlistsViewModel = playlistsViewModel,
                        usersViewModel = usersViewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackHubStartupSplash() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.Black),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.trackhub_splash_icon),
            contentDescription = null,
            modifier = Modifier.size(230.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun TrackHubApp(
    sessionViewModel: SessionViewModel,
    tracksViewModel: TracksViewModel,
    playerViewModel: PlayerViewModel,
    commentsViewModel: CommentsViewModel,
    playlistsViewModel: PlaylistsViewModel,
    usersViewModel: UsersViewModel
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
            commentsViewModel = commentsViewModel,
            playlistsViewModel = playlistsViewModel,
            usersViewModel = usersViewModel,
            onLogout = {
                playerViewModel.pause()
                sessionViewModel.clearAccessToken()
            }
        )
    }
}
