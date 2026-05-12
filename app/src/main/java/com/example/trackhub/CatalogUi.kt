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
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var myTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var likedTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var libraryInnerScreen by remember { mutableStateOf(LibraryInnerScreen.MAIN) }
    var currentTrack by remember { mutableStateOf<Track?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var playerVolume by remember { mutableStateOf(1f) }
    var showFullPlayerScreen by remember { mutableStateOf(false) }

    var selectedTrackForComments by remember { mutableStateOf<Track?>(null) }
    var selectedTrackForPlaylist by remember { mutableStateOf<Track?>(null) }
    var selectedTrackMenu by remember { mutableStateOf<Track?>(null) }
    var selectedTrackInfo by remember { mutableStateOf<Track?>(null) }
    var trackPendingDelete by remember { mutableStateOf<Track?>(null) }
    var showPlaylistsScreen by remember { mutableStateOf(false) }
    var showProfileScreen by remember { mutableStateOf(false) }
    var showUserSearchScreen by remember { mutableStateOf(false) }
    var showFollowingScreen by remember { mutableStateOf(false) }

    val player = remember {
        ExoPlayer.Builder(context).build()
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    fun loadTracks(query: String = "") {
        scope.launch {
            isLoading = true
            errorText = null

            try {
                tracks = fetchTracks(query, accessToken)
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки треков"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadMyTracks() {
        scope.launch {
            isLoading = true
            errorText = null

            try {
                myTracks = fetchMyTracks(accessToken)
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки моих треков"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadLikedTracks() {
        scope.launch {
            try {
                likedTracks = fetchLikedTracks(accessToken)
            } catch (_: Exception) {
                // Если список лайков временно не загрузился, основной интерфейс продолжает работать.
            }
        }
    }

    fun refreshTracksSilently(query: String = searchQuery) {
        scope.launch {
            try {
                delay(900)
                val freshTracks = fetchTracks(query, accessToken)
                tracks = freshTracks

                currentTrack?.let { playingTrack ->
                    freshTracks.firstOrNull { it.id == playingTrack.id }?.let { updatedTrack ->
                        currentTrack = updatedTrack
                    }
                }
            } catch (_: Exception) {
                // Тихое обновление не должно ломать интерфейс, если сервер временно недоступен.
            }
        }
    }

    fun playTrack(track: Track) {
        val fullStreamUrl = if (track.streamUrl.startsWith("http")) {
            track.streamUrl
        } else {
            BASE_URL + track.streamUrl
        }

        val mediaItem = MediaItem.fromUri(Uri.parse(fullStreamUrl))

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        currentTrack = track
        currentPositionMs = 0L
        durationMs = 0L
        isPlaying = true

        scope.launch {
            try {
                val newPlayCount = registerTrackPlay(track.id, accessToken)

                tracks = tracks.map { item ->
                    if (item.id == track.id) item.copy(playCount = newPlayCount) else item
                }

                myTracks = myTracks.map { item ->
                    if (item.id == track.id) item.copy(playCount = newPlayCount) else item
                }

                currentTrack = currentTrack?.let { item ->
                    if (item.id == track.id) item.copy(playCount = newPlayCount) else item
                }
            } catch (_: Exception) {
                // Воспроизведение не должно останавливаться, если счётчик прослушиваний временно не обновился.
            }
        }
    }

    fun togglePlayPause() {
        if (currentTrack == null) return

        if (player.isPlaying) {
            player.pause()
            isPlaying = false
        } else {
            player.play()
            isPlaying = true
        }
    }

    fun playAdjacentTrack(direction: Int) {
        if (tracks.isEmpty()) return

        val currentId = currentTrack?.id
        val currentIndex = tracks.indexOfFirst { it.id == currentId }
        val safeIndex = if (currentIndex >= 0) currentIndex else 0
        val nextIndex = (safeIndex + direction + tracks.size) % tracks.size

        playTrack(tracks[nextIndex])
    }

    fun updateTrackLocally(trackId: Int, transform: (Track) -> Track) {
        tracks = tracks.map { track ->
            if (track.id == trackId) transform(track) else track
        }

        myTracks = myTracks.map { track ->
            if (track.id == trackId) transform(track) else track
        }

        currentTrack = currentTrack?.let { track ->
            if (track.id == trackId) transform(track) else track
        }
    }

    fun likeAndReload(track: Track) {
        scope.launch {
            errorText = null

            try {
                val likeResult = likeTrack(track.id, accessToken)

                updateTrackLocally(track.id) { current ->
                    current.copy(
                        likesCount = likeResult.likesCount,
                        isLiked = likeResult.liked
                    )
                }

                if (likeResult.liked) {
                    val updatedTrack = (tracks + myTracks + listOf(track))
                        .firstOrNull { it.id == track.id }
                        ?.copy(likesCount = likeResult.likesCount, isLiked = true)
                        ?: track.copy(likesCount = likeResult.likesCount, isLiked = true)

                    likedTracks = listOf(updatedTrack) + likedTracks.filterNot { it.id == track.id }
                } else {
                    likedTracks = likedTracks.filterNot { it.id == track.id }
                }
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка лайка"
            }
        }
    }

    fun deleteTrackAndReload(track: Track) {
        scope.launch {
            errorText = null

            try {
                deleteTrack(track.id, accessToken)

                tracks = tracks.filterNot { it.id == track.id }
                myTracks = myTracks.filterNot { it.id == track.id }
                likedTracks = likedTracks.filterNot { it.id == track.id }

                if (currentTrack?.id == track.id) {
                    player.stop()
                    currentTrack = null
                    isPlaying = false
                    currentPositionMs = 0L
                    durationMs = 0L
                }
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка удаления трека"
            }
        }
    }

    LaunchedEffect(Unit) {
        loadTracks()
        loadLikedTracks()
    }

    LaunchedEffect(currentTrack?.id) {
        if (currentTrack == null) return@LaunchedEffect

        while (true) {
            currentPositionMs = player.currentPosition.coerceAtLeast(0L)

            val playerDuration = player.duration
            durationMs = if (playerDuration > 0L) {
                playerDuration
            } else {
                0L
            }

            isPlaying = player.isPlaying

            if (player.playbackState == Player.STATE_ENDED && tracks.size > 1) {
                playAdjacentTrack(1)
                break
            }

            delay(300)
        }
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
            onBack = closePlaylistAdd
        )
        return
    }

    if (showPlaylistsScreen) {
        val closePlaylists = {
            showPlaylistsScreen = false
            loadTracks(searchQuery)
        }

        BackHandler {
            closePlaylists()
        }

        PlaylistsScreen(
            accessToken = accessToken,
            onBack = closePlaylists
        )
        return
    }


    if (showUserSearchScreen) {
        BackHandler {
            showUserSearchScreen = false
        }

        UserSearchScreen(
            accessToken = accessToken,
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
                player.pause()
                showProfileScreen = false
                onLogout()
            }
        )
        return
    }

    if (showFullPlayerScreen && currentTrack != null) {
        val track = currentTrack!!

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
                val targetPosition = if (durationMs > 0L) {
                    positionMs.coerceIn(0L, durationMs)
                } else {
                    positionMs.coerceAtLeast(0L)
                }

                player.seekTo(targetPosition)
                currentPositionMs = targetPosition
            },
            onVolumeChange = { newVolume ->
                val safeVolume = newVolume.coerceIn(0f, 1f)

                playerVolume = safeVolume
                player.volume = safeVolume
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
                                playTrack(track)
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
                            onSearchQueryChange = { searchQuery = it },
                            tracks = tracks,
                            isLoading = isLoading,
                            errorText = errorText,
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            onSearchClick = {
                                loadTracks(searchQuery)
                            },
                            onAllTracksClick = {
                                searchQuery = ""
                                loadTracks()
                            },
                            onPlayClick = { track ->
                                if (currentTrack?.id == track.id) {
                                    togglePlayPause()
                                } else {
                                    playTrack(track)
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
                                        loadLikedTracks()
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
                                    onBack = {
                                        libraryInnerScreen = LibraryInnerScreen.MAIN
                                    }
                                )
                            }

                            LibraryInnerScreen.LIKED_TRACKS -> {
                                LibraryTracksListScreen(
                                    title = "Любимые треки",
                                    subtitle = "Треки, которые получили лайки",
                                    emptyText = "Пока нет любимых треков",
                                    tracks = likedTracks,
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    onBack = {
                                        libraryInnerScreen = LibraryInnerScreen.MAIN
                                    },
                                    onPlayClick = { track ->
                                        if (currentTrack?.id == track.id) {
                                            togglePlayPause()
                                        } else {
                                            playTrack(track)
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
                                    onBack = {
                                        libraryInnerScreen = LibraryInnerScreen.MAIN
                                    },
                                    onPlayClick = { track ->
                                        if (currentTrack?.id == track.id) {
                                            togglePlayPause()
                                        } else {
                                            playTrack(track)
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
                                    onBack = {
                                        libraryInnerScreen = LibraryInnerScreen.MAIN
                                    },
                                    onPlayClick = { track ->
                                        if (currentTrack?.id == track.id) {
                                            togglePlayPause()
                                        } else {
                                            playTrack(track)
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
                                    }
                                )
                            }
                        }
                    }

                    MainTab.CREATE -> {
                        CreateTabScreen(
                            accessToken = accessToken,
                            onUploadSuccess = { uploadedTrack ->
                                tracks = listOf(uploadedTrack) + tracks
                                myTracks = listOf(uploadedTrack) + myTracks.filterNot { it.id == uploadedTrack.id }
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
                        val targetPosition = if (durationMs > 0L) {
                            positionMs.coerceIn(0L, durationMs)
                        } else {
                            positionMs.coerceAtLeast(0L)
                        }

                        player.seekTo(targetPosition)
                        currentPositionMs = targetPosition
                    },
                    onVolumeChange = { newVolume ->
                        val safeVolume = newVolume.coerceIn(0f, 1f)

                        playerVolume = safeVolume
                        player.volume = safeVolume
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
