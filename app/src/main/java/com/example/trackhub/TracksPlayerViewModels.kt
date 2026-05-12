package com.example.trackhub

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TracksViewModel : ViewModel() {
    private val repository = TrackHubRepository

    var tracks by mutableStateOf<List<Track>>(emptyList())
        private set

    var myTracks by mutableStateOf<List<Track>>(emptyList())
        private set

    var likedTracks by mutableStateOf<List<Track>>(emptyList())
        private set

    var searchQuery by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorText by mutableStateOf<String?>(null)
        private set

    var hasMoreTracks by mutableStateOf(true)
        private set

    var hasMoreMyTracks by mutableStateOf(true)
        private set

    var hasMoreLikedTracks by mutableStateOf(true)
        private set

    var isLoadingMoreTracks by mutableStateOf(false)
        private set

    var isLoadingMoreMyTracks by mutableStateOf(false)
        private set

    var isLoadingMoreLikedTracks by mutableStateOf(false)
        private set

    private var activeTrackActionIds by mutableStateOf<Set<Int>>(emptySet())
    private var tracksOffset = 0
    private var myTracksOffset = 0
    private var likedTracksOffset = 0
    private var currentTracksQuery = ""

    fun updateSearchQuery(value: String) {
        searchQuery = value
    }

    fun clearError() {
        errorText = null
    }

    fun loadTracks(
        accessToken: String,
        query: String = searchQuery
    ) {
        if (isLoading) return

        currentTracksQuery = query
        tracksOffset = 0

        viewModelScope.launch {
            isLoading = true
            errorText = null

            try {
                val page = repository.fetchTracks(
                    query = query,
                    accessToken = accessToken,
                    limit = TRACK_PAGE_SIZE,
                    offset = 0
                )
                tracks = page
                tracksOffset = page.size
                hasMoreTracks = page.size == TRACK_PAGE_SIZE
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки треков"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadMoreTracks(accessToken: String) {
        if (isLoading || isLoadingMoreTracks || !hasMoreTracks) return

        viewModelScope.launch {
            isLoadingMoreTracks = true
            errorText = null

            try {
                val page = repository.fetchTracks(
                    query = currentTracksQuery,
                    accessToken = accessToken,
                    limit = TRACK_PAGE_SIZE,
                    offset = tracksOffset
                )
                tracks = appendUniqueTracks(tracks, page)
                tracksOffset += page.size
                hasMoreTracks = page.size == TRACK_PAGE_SIZE
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки следующей страницы"
            } finally {
                isLoadingMoreTracks = false
            }
        }
    }

    fun loadMyTracks(accessToken: String) {
        if (isLoading) return

        myTracksOffset = 0

        viewModelScope.launch {
            isLoading = true
            errorText = null

            try {
                val page = repository.fetchMyTracks(
                    accessToken = accessToken,
                    limit = TRACK_PAGE_SIZE,
                    offset = 0
                )
                myTracks = page
                myTracksOffset = page.size
                hasMoreMyTracks = page.size == TRACK_PAGE_SIZE
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки моих треков"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadMoreMyTracks(accessToken: String) {
        if (isLoading || isLoadingMoreMyTracks || !hasMoreMyTracks) return

        viewModelScope.launch {
            isLoadingMoreMyTracks = true
            errorText = null

            try {
                val page = repository.fetchMyTracks(
                    accessToken = accessToken,
                    limit = TRACK_PAGE_SIZE,
                    offset = myTracksOffset
                )
                myTracks = appendUniqueTracks(myTracks, page)
                myTracksOffset += page.size
                hasMoreMyTracks = page.size == TRACK_PAGE_SIZE
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки моих треков"
            } finally {
                isLoadingMoreMyTracks = false
            }
        }
    }

    fun loadLikedTracks(
        accessToken: String,
        silent: Boolean = true
    ) {
        if (!silent && isLoading) return

        likedTracksOffset = 0

        viewModelScope.launch {
            if (!silent) {
                isLoading = true
                errorText = null
            }

            try {
                val page = repository.fetchLikedTracks(
                    accessToken = accessToken,
                    limit = TRACK_PAGE_SIZE,
                    offset = 0
                )
                likedTracks = page
                likedTracksOffset = page.size
                hasMoreLikedTracks = page.size == TRACK_PAGE_SIZE
            } catch (e: Exception) {
                if (!silent) {
                    errorText = e.message ?: "Ошибка загрузки любимых треков"
                }
            } finally {
                if (!silent) {
                    isLoading = false
                }
            }
        }
    }

    fun loadMoreLikedTracks(accessToken: String) {
        if (isLoading || isLoadingMoreLikedTracks || !hasMoreLikedTracks) return

        viewModelScope.launch {
            isLoadingMoreLikedTracks = true
            errorText = null

            try {
                val page = repository.fetchLikedTracks(
                    accessToken = accessToken,
                    limit = TRACK_PAGE_SIZE,
                    offset = likedTracksOffset
                )
                likedTracks = appendUniqueTracks(likedTracks, page)
                likedTracksOffset += page.size
                hasMoreLikedTracks = page.size == TRACK_PAGE_SIZE
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки любимых треков"
            } finally {
                isLoadingMoreLikedTracks = false
            }
        }
    }

    fun refreshTracksSilently(
        accessToken: String,
        query: String = searchQuery,
        onTrackUpdated: (Track) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                delay(900)
                val refreshLimit = tracks.size.coerceAtLeast(TRACK_PAGE_SIZE)
                val freshTracks = repository.fetchTracks(
                    query = query,
                    accessToken = accessToken,
                    limit = refreshLimit,
                    offset = 0
                )
                tracks = freshTracks
                tracksOffset = freshTracks.size
                hasMoreTracks = freshTracks.size == refreshLimit
                freshTracks.forEach(onTrackUpdated)
            } catch (_: Exception) {
                // Тихое обновление не должно ломать интерфейс, если сервер временно недоступен.
            }
        }
    }

    fun toggleLike(
        track: Track,
        accessToken: String,
        onTrackUpdated: (Track) -> Unit = {}
    ) {
        if (track.id in activeTrackActionIds) return

        activeTrackActionIds = activeTrackActionIds + track.id

        viewModelScope.launch {
            errorText = null

            try {
                val likeResult = repository.likeTrack(track.id, accessToken)
                val updatedTrack = track.copy(
                    likesCount = likeResult.likesCount,
                    isLiked = likeResult.liked
                )

                updateTrackLocally(track.id) { current ->
                    current.copy(
                        likesCount = likeResult.likesCount,
                        isLiked = likeResult.liked
                    )
                }

                if (likeResult.liked) {
                    val fromLoadedLists = (tracks + myTracks + likedTracks + listOf(updatedTrack))
                        .firstOrNull { it.id == track.id }
                        ?.copy(likesCount = likeResult.likesCount, isLiked = true)
                        ?: updatedTrack.copy(isLiked = true)

                    likedTracks = listOf(fromLoadedLists) + likedTracks.filterNot { it.id == track.id }
                    onTrackUpdated(fromLoadedLists)
                } else {
                    likedTracks = likedTracks.filterNot { it.id == track.id }
                    onTrackUpdated(updatedTrack.copy(isLiked = false))
                }
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка лайка"
            } finally {
                activeTrackActionIds = activeTrackActionIds - track.id
            }
        }
    }

    fun deleteTrackAndUpdate(
        track: Track,
        accessToken: String,
        onDeleted: () -> Unit = {}
    ) {
        viewModelScope.launch {
            errorText = null

            try {
                repository.deleteTrack(track.id, accessToken)

                tracks = tracks.filterNot { it.id == track.id }
                myTracks = myTracks.filterNot { it.id == track.id }
                likedTracks = likedTracks.filterNot { it.id == track.id }

                onDeleted()
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка удаления трека"
            } finally {
                activeTrackActionIds = activeTrackActionIds - track.id
            }
        }
    }

    fun addUploadedTrack(track: Track) {
        tracks = listOf(track) + tracks.filterNot { it.id == track.id }
        myTracks = listOf(track) + myTracks.filterNot { it.id == track.id }

        if (track.isLiked) {
            likedTracks = listOf(track) + likedTracks.filterNot { it.id == track.id }
        }
    }

    fun updatePlayCount(
        trackId: Int,
        newPlayCount: Int,
        onTrackUpdated: (Track) -> Unit = {}
    ) {
        updateTrackLocally(trackId) { track ->
            track.copy(playCount = newPlayCount)
        }

        (tracks + myTracks + likedTracks)
            .firstOrNull { it.id == trackId }
            ?.copy(playCount = newPlayCount)
            ?.let(onTrackUpdated)
    }

    private fun appendUniqueTracks(
        current: List<Track>,
        page: List<Track>
    ): List<Track> {
        if (page.isEmpty()) return current

        val existingIds = current.mapTo(mutableSetOf()) { it.id }
        return current + page.filter { it.id !in existingIds }
    }

    fun updateTrackLocally(
        trackId: Int,
        transform: (Track) -> Track
    ) {
        tracks = tracks.map { track ->
            if (track.id == trackId) transform(track) else track
        }

        myTracks = myTracks.map { track ->
            if (track.id == trackId) transform(track) else track
        }

        likedTracks = likedTracks.map { track ->
            if (track.id == trackId) transform(track) else track
        }
    }
}

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TrackHubRepository

    private val player = ExoPlayer.Builder(application).build()
    private var positionTickerJob: Job? = null

    var playbackQueue by mutableStateOf<List<Track>>(emptyList())
        private set

    var currentTrack by mutableStateOf<Track?>(null)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var currentPositionMs by mutableStateOf(0L)
        private set

    var durationMs by mutableStateOf(0L)
        private set

    var playerVolume by mutableStateOf(1f)
        private set

    fun setQueue(tracks: List<Track>) {
        playbackQueue = tracks
    }

    fun playTrack(
        track: Track,
        accessToken: String,
        queue: List<Track> = playbackQueue,
        onPlayCountChanged: (trackId: Int, playCount: Int) -> Unit = { _, _ -> }
    ) {
        if (queue.isNotEmpty()) {
            playbackQueue = queue
        }

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

        startPositionTicker(
            accessToken = accessToken,
            onPlayCountChanged = onPlayCountChanged
        )

        viewModelScope.launch {
            try {
                val newPlayCount = repository.registerTrackPlay(track.id, accessToken)
                updateCurrentTrack(track.id) { item ->
                    item.copy(playCount = newPlayCount)
                }
                onPlayCountChanged(track.id, newPlayCount)
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

    fun seekTo(positionMs: Long) {
        val targetPosition = if (durationMs > 0L) {
            positionMs.coerceIn(0L, durationMs)
        } else {
            positionMs.coerceAtLeast(0L)
        }

        player.seekTo(targetPosition)
        currentPositionMs = targetPosition
    }

    fun changeVolume(newVolume: Float) {
        val safeVolume = newVolume.coerceIn(0f, 1f)

        playerVolume = safeVolume
        player.volume = safeVolume
    }

    fun playAdjacentTrack(
        direction: Int,
        accessToken: String,
        onPlayCountChanged: (trackId: Int, playCount: Int) -> Unit = { _, _ -> }
    ) {
        if (playbackQueue.isEmpty()) return

        val currentId = currentTrack?.id
        val currentIndex = playbackQueue.indexOfFirst { it.id == currentId }
        val safeIndex = if (currentIndex >= 0) currentIndex else 0
        val nextIndex = (safeIndex + direction + playbackQueue.size) % playbackQueue.size

        playTrack(
            track = playbackQueue[nextIndex],
            accessToken = accessToken,
            queue = playbackQueue,
            onPlayCountChanged = onPlayCountChanged
        )
    }

    fun updateCurrentTrack(
        trackId: Int,
        transform: (Track) -> Track
    ) {
        currentTrack = currentTrack?.let { track ->
            if (track.id == trackId) transform(track) else track
        }

        playbackQueue = playbackQueue.map { track ->
            if (track.id == trackId) transform(track) else track
        }
    }

    fun stopIfTrackDeleted(trackId: Int) {
        playbackQueue = playbackQueue.filterNot { it.id == trackId }

        if (currentTrack?.id == trackId) {
            player.stop()
            currentTrack = null
            isPlaying = false
            currentPositionMs = 0L
            durationMs = 0L
            positionTickerJob?.cancel()
            positionTickerJob = null
        }
    }

    fun pause() {
        player.pause()
        isPlaying = false
    }

    private fun startPositionTicker(
        accessToken: String,
        onPlayCountChanged: (trackId: Int, playCount: Int) -> Unit
    ) {
        positionTickerJob?.cancel()
        positionTickerJob = viewModelScope.launch {
            while (currentTrack != null) {
                currentPositionMs = player.currentPosition.coerceAtLeast(0L)

                val playerDuration = player.duration
                durationMs = if (playerDuration > 0L) {
                    playerDuration
                } else {
                    0L
                }

                isPlaying = player.isPlaying

                if (player.playbackState == Player.STATE_ENDED && playbackQueue.size > 1) {
                    playAdjacentTrack(
                        direction = 1,
                        accessToken = accessToken,
                        onPlayCountChanged = onPlayCountChanged
                    )
                    break
                }

                delay(300)
            }
        }
    }

    override fun onCleared() {
        positionTickerJob?.cancel()
        player.release()
        super.onCleared()
    }
}
