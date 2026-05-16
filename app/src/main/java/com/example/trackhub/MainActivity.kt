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
