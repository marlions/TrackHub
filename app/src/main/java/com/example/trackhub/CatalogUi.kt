package com.example.trackhub


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandHorizontally
import kotlinx.coroutines.delay
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.lifecycle.AndroidViewModel
import com.example.trackhub.ui.theme.TrackHubTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    accessToken: String,
    tracksViewModel: TracksViewModel,
    playerViewModel: PlayerViewModel,
    commentsViewModel: CommentsViewModel,
    playlistsViewModel: PlaylistsViewModel,
    usersViewModel: UsersViewModel,
    onLogout: () -> Unit
) {
    val tracks = tracksViewModel.tracks
    val myTracks = tracksViewModel.myTracks
    val likedTracks = tracksViewModel.likedTracks
    val searchQuery = tracksViewModel.searchQuery
    val isLoading = tracksViewModel.isLoading
    val errorText = tracksViewModel.errorText
    val hasMoreTracks = tracksViewModel.hasMoreTracks
    val hasMoreMyTracks = tracksViewModel.hasMoreMyTracks
    val hasMoreLikedTracks = tracksViewModel.hasMoreLikedTracks
    val isLoadingMoreTracks = tracksViewModel.isLoadingMoreTracks
    val isLoadingMoreMyTracks = tracksViewModel.isLoadingMoreMyTracks
    val isLoadingMoreLikedTracks = tracksViewModel.isLoadingMoreLikedTracks

    val currentTrack = playerViewModel.currentTrack
    val isPlaying = playerViewModel.isPlaying
    val currentPositionMs = playerViewModel.currentPositionMs
    val durationMs = playerViewModel.durationMs
    val playerVolume = playerViewModel.playerVolume

    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var libraryInnerScreen by remember { mutableStateOf(LibraryInnerScreen.MAIN) }
    var showFullPlayerScreen by remember { mutableStateOf(false) }

    var selectedTrackForComments by remember { mutableStateOf<Track?>(null) }
    var selectedTrackForPlaylist by remember { mutableStateOf<Track?>(null) }
    var selectedTrackMenu by remember { mutableStateOf<Track?>(null) }
    var selectedTrackInfo by remember { mutableStateOf<Track?>(null) }
    var trackPendingDelete by remember { mutableStateOf<Track?>(null) }
    var showUserSearchScreen by remember { mutableStateOf(false) }
    var showFollowingScreen by remember { mutableStateOf(false) }
    var showProfileScreen by remember { mutableStateOf(false) }

    fun onPlayCountChanged(trackId: Int, playCount: Int) {
        tracksViewModel.updatePlayCount(trackId, playCount) { updatedTrack ->
            playerViewModel.updateCurrentTrack(trackId) { updatedTrack }
        }
    }

    fun loadTracks(query: String = searchQuery) {
        tracksViewModel.loadTracks(accessToken, query)
    }

    fun loadMyTracks() {
        tracksViewModel.loadMyTracks(accessToken)
    }

    fun loadLikedTracks(silent: Boolean = true) {
        tracksViewModel.loadLikedTracks(
            accessToken = accessToken,
            silent = silent
        )
    }

    fun loadMoreTracks() {
        tracksViewModel.loadMoreTracks(accessToken)
    }

    fun loadMoreMyTracks() {
        tracksViewModel.loadMoreMyTracks(accessToken)
    }

    fun loadMoreLikedTracks() {
        tracksViewModel.loadMoreLikedTracks(accessToken)
    }

    fun playTrack(
        track: Track,
        queue: List<Track> = tracks
    ) {
        playerViewModel.playTrack(
            track = track,
            accessToken = accessToken,
            queue = queue,
            onPlayCountChanged = ::onPlayCountChanged
        )
    }

    fun togglePlayPause() {
        playerViewModel.togglePlayPause()
    }

    fun playAdjacentTrack(direction: Int) {
        playerViewModel.playAdjacentTrack(
            direction = direction,
            accessToken = accessToken,
            onPlayCountChanged = ::onPlayCountChanged
        )
    }

    fun likeAndReload(track: Track) {
        tracksViewModel.toggleLike(
            track = track,
            accessToken = accessToken,
            onTrackUpdated = { updatedTrack ->
                playerViewModel.updateCurrentTrack(updatedTrack.id) { updatedTrack }
            }
        )
    }

    fun deleteTrackAndReload(track: Track) {
        tracksViewModel.deleteTrackAndUpdate(
            track = track,
            accessToken = accessToken,
            onDeleted = {
                playerViewModel.stopIfTrackDeleted(track.id)
            }
        )
    }

    LaunchedEffect(Unit) {
        loadTracks()
        loadLikedTracks()
    }

    val selectedCommentsTrack = selectedTrackForComments
    val selectedPlaylistTrack = selectedTrackForPlaylist

    if (selectedCommentsTrack != null) {
        val closeComments = {
            selectedTrackForComments = null
            loadTracks(searchQuery)
        }

        BackHandler {
            closeComments()
        }

        CommentsScreen(
            track = selectedCommentsTrack,
            accessToken = accessToken,
            commentsViewModel = commentsViewModel,
            onBack = closeComments
        )
        return
    }

    if (selectedPlaylistTrack != null) {
        val closePlaylistAdd = {
            selectedTrackForPlaylist = null
            loadTracks(searchQuery)
        }

        BackHandler {
            closePlaylistAdd()
        }

        AddToPlaylistScreen(
            track = selectedPlaylistTrack,
            accessToken = accessToken,
            playlistsViewModel = playlistsViewModel,
            onBack = closePlaylistAdd
        )
        return
    }

    if (showUserSearchScreen) {
        BackHandler {
            showUserSearchScreen = false
        }

        UserSearchScreen(
            accessToken = accessToken,
            usersViewModel = usersViewModel,
            onBack = {
                showUserSearchScreen = false
            }
        )
        return
    }

    if (showFollowingScreen) {
        BackHandler {
            showFollowingScreen = false
        }

        FollowingScreen(
            accessToken = accessToken,
            usersViewModel = usersViewModel,
            onBack = {
                showFollowingScreen = false
            }
        )
        return
    }

    if (showProfileScreen) {
        BackHandler {
            showProfileScreen = false
        }

        ProfileScreen(
            accessToken = accessToken,
            tracksCount = tracks.size,
            likedTracksCount = likedTracks.size,
            onFindUsersClick = {
                showUserSearchScreen = true
            },
            onFollowingClick = {
                showFollowingScreen = true
            },
            onBack = {
                showProfileScreen = false
            },
            onLogout = {
                playerViewModel.pause()
                showProfileScreen = false
                onLogout()
            }
        )
        return
    }

    if (showFullPlayerScreen && currentTrack != null) {
        val track = currentTrack

        TrackHubFullPlayerScreen(
            track = track,
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            volume = playerVolume,
            isLiked = track.isLiked,
            onBack = {
                showFullPlayerScreen = false
            },
            onLikeClick = {
                likeAndReload(track)
            },
            onSeekTo = { positionMs ->
                playerViewModel.seekTo(positionMs)
            },
            onVolumeChange = { newVolume ->
                playerViewModel.changeVolume(newVolume)
            },
            onPlayPauseClick = {
                togglePlayPause()
            },
            onPreviousClick = {
                playAdjacentTrack(-1)
            },
            onNextClick = {
                playAdjacentTrack(1)
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        GoldBackgroundDecorations()

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            BackHandler(
                enabled = currentTab == MainTab.LIBRARY && libraryInnerScreen != LibraryInnerScreen.MAIN
            ) {
                libraryInnerScreen = LibraryInnerScreen.MAIN
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentTab) {
                    MainTab.HOME -> {
                        HomeTabScreen(
                            tracks = tracks,
                            likedTracks = likedTracks,
                            isLoading = isLoading,
                            errorText = errorText,
                            onRetry = {
                                loadTracks(searchQuery)
                            },
                            onShowAllClick = {
                                currentTab = MainTab.LIBRARY
                                libraryInnerScreen = LibraryInnerScreen.ALL_TRACKS
                            },
                            onLogout = {
                                showProfileScreen = true
                            },
                            onPlayClick = { track ->
                                playTrack(track, tracks)
                            },
                            onLikeClick = { track ->
                                likeAndReload(track)
                            },
                            onCommentsClick = { track ->
                                selectedTrackForComments = track
                            },
                            onAddToPlaylistClick = { track ->
                                selectedTrackForPlaylist = track
                            },
                            onMoreClick = { track ->
                                selectedTrackMenu = track
                            }
                        )
                    }

                    MainTab.SEARCH -> {
                        SearchTabScreen(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { value ->
                                tracksViewModel.updateSearchQuery(value)
                            },
                            tracks = tracks,
                            isLoading = isLoading,
                            errorText = errorText,
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            hasMoreTracks = hasMoreTracks,
                            isLoadingMoreTracks = isLoadingMoreTracks,
                            onSearchClick = {
                                loadTracks(searchQuery)
                            },
                            onAllTracksClick = {
                                tracksViewModel.updateSearchQuery("")
                                loadTracks("")
                            },
                            onPlayClick = { track ->
                                if (currentTrack?.id == track.id) {
                                    togglePlayPause()
                                } else {
                                    playTrack(track, tracks)
                                }
                            },
                            onLikeClick = { track ->
                                likeAndReload(track)
                            },
                            onCommentsClick = { track ->
                                selectedTrackForComments = track
                            },
                            onAddToPlaylistClick = { track ->
                                selectedTrackForPlaylist = track
                            },
                            onMoreClick = { track ->
                                selectedTrackMenu = track
                            },
                            onLoadMoreClick = {
                                loadMoreTracks()
                            }
                        )
                    }

                    MainTab.LIBRARY -> {
                        when (libraryInnerScreen) {
                            LibraryInnerScreen.MAIN -> {
                                LibraryTabScreen(
                                    tracks = tracks,
                                    likedTracksCount = likedTracks.size,
                                    onPlaylistsClick = {
                                        libraryInnerScreen = LibraryInnerScreen.PLAYLISTS
                                    },
                                    onLikedTracksClick = {
                                        libraryInnerScreen = LibraryInnerScreen.LIKED_TRACKS
                                        loadLikedTracks(silent = false)
                                    },
                                    onMyTracksClick = {
                                        libraryInnerScreen = LibraryInnerScreen.MY_TRACKS
                                        loadMyTracks()
                                    },
                                    onAllTracksClick = {
                                        libraryInnerScreen = LibraryInnerScreen.ALL_TRACKS
                                    }
                                )
                            }

                            LibraryInnerScreen.PLAYLISTS -> {
                                PlaylistsScreen(
                                    accessToken = accessToken,
                                    playlistsViewModel = playlistsViewModel,
                                    onBack = {
                                        libraryInnerScreen = LibraryInnerScreen.MAIN
                                    }
                                )
                            }

                            LibraryInnerScreen.LIKED_TRACKS -> {
                                LibraryTracksListScreen(
                                    title = "Любимые треки",
                                    subtitle = "Треки, которые понравились именно вам",
                                    emptyText = "Пока нет любимых треков",
                                    tracks = likedTracks,
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    hasMoreTracks = hasMoreLikedTracks,
                                    isLoadingMoreTracks = isLoadingMoreLikedTracks,
                                    onBack = {
                                        libraryInnerScreen = LibraryInnerScreen.MAIN
                                    },
                                    onPlayClick = { track ->
                                        if (currentTrack?.id == track.id) {
                                            togglePlayPause()
                                        } else {
                                            playTrack(track, likedTracks)
                                        }
                                    },
                                    onLikeClick = { track ->
                                        likeAndReload(track)
                                    },
                                    onCommentsClick = { track ->
                                        selectedTrackForComments = track
                                    },
                                    onAddToPlaylistClick = { track ->
                                        selectedTrackForPlaylist = track
                                    },
                                    onMoreClick = { track ->
                                        selectedTrackMenu = track
                                    },
                                    onLoadMoreClick = {
                                        loadMoreLikedTracks()
                                    }
                                )
                            }

                            LibraryInnerScreen.MY_TRACKS -> {
                                LibraryTracksListScreen(
                                    title = "Мои треки",
                                    subtitle = "Треки, которые загрузили именно вы",
                                    emptyText = "Вы пока не загрузили ни одного трека",
                                    tracks = myTracks,
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    hasMoreTracks = hasMoreMyTracks,
                                    isLoadingMoreTracks = isLoadingMoreMyTracks,
                                    onBack = {
                                        libraryInnerScreen = LibraryInnerScreen.MAIN
                                    },
                                    onPlayClick = { track ->
                                        if (currentTrack?.id == track.id) {
                                            togglePlayPause()
                                        } else {
                                            playTrack(track, myTracks)
                                        }
                                    },
                                    onLikeClick = { track ->
                                        likeAndReload(track)
                                    },
                                    onCommentsClick = { track ->
                                        selectedTrackForComments = track
                                    },
                                    onAddToPlaylistClick = { track ->
                                        selectedTrackForPlaylist = track
                                    },
                                    onMoreClick = { track ->
                                        selectedTrackMenu = track
                                    },
                                    onLoadMoreClick = {
                                        loadMoreMyTracks()
                                    }
                                )
                            }

                            LibraryInnerScreen.ALL_TRACKS -> {
                                LibraryTracksListScreen(
                                    title = "Все треки",
                                    subtitle = "Полный список доступных треков",
                                    emptyText = "Пока нет загруженных треков",
                                    tracks = tracks,
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    hasMoreTracks = hasMoreTracks,
                                    isLoadingMoreTracks = isLoadingMoreTracks,
                                    onBack = {
                                        libraryInnerScreen = LibraryInnerScreen.MAIN
                                    },
                                    onPlayClick = { track ->
                                        if (currentTrack?.id == track.id) {
                                            togglePlayPause()
                                        } else {
                                            playTrack(track, tracks)
                                        }
                                    },
                                    onLikeClick = { track ->
                                        likeAndReload(track)
                                    },
                                    onCommentsClick = { track ->
                                        selectedTrackForComments = track
                                    },
                                    onAddToPlaylistClick = { track ->
                                        selectedTrackForPlaylist = track
                                    },
                                    onMoreClick = { track ->
                                        selectedTrackMenu = track
                                    },
                                    onLoadMoreClick = {
                                        loadMoreTracks()
                                    }
                                )
                            }
                        }
                    }

                    MainTab.CREATE -> {
                        CreateTabScreen(
                            accessToken = accessToken,
                            onUploadSuccess = { uploadedTrack ->
                                tracksViewModel.addUploadedTrack(uploadedTrack)
                            }
                        )
                    }
                }
            }

            currentTrack?.let { track ->
                TrackHubMiniPlayer(
                    track = track,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    volume = playerVolume,
                    onSeekTo = { positionMs ->
                        playerViewModel.seekTo(positionMs)
                    },
                    onVolumeChange = { newVolume ->
                        playerViewModel.changeVolume(newVolume)
                    },
                    onOpenPlayerClick = {
                        showFullPlayerScreen = true
                    },
                    onPlayPauseClick = {
                        togglePlayPause()
                    }
                )
            }

            TrackHubBottomNavigation(
                currentTab = currentTab,
                onTabClick = { tab ->
                    currentTab = tab

                    if (tab != MainTab.LIBRARY) {
                        libraryInnerScreen = LibraryInnerScreen.MAIN
                    }
                }
            )
        }

        selectedTrackMenu?.let { track ->
            TrackOptionsDialog(
                track = track,
                onDismiss = {
                    selectedTrackMenu = null
                },
                onInfoClick = {
                    selectedTrackMenu = null
                    selectedTrackInfo = track
                },
                onCommentsClick = {
                    selectedTrackMenu = null
                    selectedTrackForComments = track
                },
                onAddToPlaylistClick = {
                    selectedTrackMenu = null
                    selectedTrackForPlaylist = track
                },
                onDeleteTrackClick = {
                    selectedTrackMenu = null
                    trackPendingDelete = track
                }
            )
        }

        selectedTrackInfo?.let { track ->
            TrackInfoDialog(
                track = track,
                onDismiss = {
                    selectedTrackInfo = null
                }
            )
        }

        trackPendingDelete?.let { track ->
            DeleteTrackConfirmDialog(
                track = track,
                onDismiss = {
                    trackPendingDelete = null
                },
                onConfirm = {
                    trackPendingDelete = null
                    deleteTrackAndReload(track)
                }
            )
        }
    }
}
