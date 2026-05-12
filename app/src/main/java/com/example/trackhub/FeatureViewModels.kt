package com.example.trackhub

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CommentsViewModel : ViewModel() {
    private val repository = TrackHubRepository

    var comments by mutableStateOf<List<TrackComment>>(emptyList())
        private set

    var commentText by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isSending by mutableStateOf(false)
        private set

    var errorText by mutableStateOf<String?>(null)
        private set

    fun updateCommentText(value: String) {
        commentText = value
        errorText = null
    }

    fun clearForTrack() {
        comments = emptyList()
        commentText = ""
        isLoading = false
        isSending = false
        errorText = null
    }

    fun loadComments(trackId: Int) {
        viewModelScope.launch {
            isLoading = true
            errorText = null

            try {
                comments = repository.fetchComments(trackId)
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки комментариев"
            } finally {
                isLoading = false
            }
        }
    }

    fun sendComment(
        trackId: Int,
        accessToken: String
    ) {
        val trimmedText = commentText.trim()

        if (trimmedText.isBlank()) {
            errorText = "Комментарий не должен быть пустым"
            return
        }

        if (trimmedText.length > 500) {
            errorText = "Комментарий должен быть не длиннее 500 символов"
            return
        }

        viewModelScope.launch {
            isSending = true
            errorText = null

            try {
                repository.addComment(trackId, trimmedText, accessToken)
                commentText = ""
                comments = repository.fetchComments(trackId)
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка отправки комментария"
            } finally {
                isSending = false
            }
        }
    }
}

class PlaylistsViewModel : ViewModel() {
    private val repository = TrackHubRepository

    var playlists by mutableStateOf<List<Playlist>>(emptyList())
        private set

    var selectedPlaylist by mutableStateOf<Playlist?>(null)
        private set

    var playlistTracks by mutableStateOf<List<Track>>(emptyList())
        private set

    var playlistName by mutableStateOf("")
        private set

    var selectedPlaylistTrackMenu by mutableStateOf<Track?>(null)
        private set

    var selectedPlaylistForDelete by mutableStateOf<Playlist?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorText by mutableStateOf<String?>(null)
        private set

    var successText by mutableStateOf<String?>(null)
        private set

    fun updatePlaylistName(value: String) {
        playlistName = value
        errorText = null
        successText = null
    }

    fun clearStatus() {
        errorText = null
        successText = null
    }

    fun loadPlaylists(accessToken: String) {
        viewModelScope.launch {
            isLoading = true
            errorText = null

            try {
                playlists = repository.fetchPlaylists(accessToken)
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки плейлистов"
            } finally {
                isLoading = false
            }
        }
    }

    fun createPlaylist(accessToken: String) {
        val trimmedName = playlistName.trim()

        if (trimmedName.isBlank()) {
            errorText = "Название плейлиста не должно быть пустым"
            return
        }

        if (trimmedName.length > 100) {
            errorText = "Название плейлиста должно быть не длиннее 100 символов"
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorText = null
            successText = null

            try {
                val createdPlaylist = repository.createPlaylist(trimmedName, accessToken)
                playlistName = ""
                playlists = listOf(createdPlaylist) + playlists.filterNot { it.id == createdPlaylist.id }
                successText = "Плейлист создан"
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка создания плейлиста"
            } finally {
                isLoading = false
            }
        }
    }

    fun addTrackToPlaylist(
        playlist: Playlist,
        track: Track,
        accessToken: String
    ) {
        viewModelScope.launch {
            isLoading = true
            errorText = null
            successText = null

            try {
                repository.addTrackToPlaylist(
                    playlistId = playlist.id,
                    trackId = track.id,
                    accessToken = accessToken
                )

                playlists = playlists.map { item ->
                    if (item.id == playlist.id) {
                        item.copy(tracksCount = item.tracksCount + 1)
                    } else {
                        item
                    }
                }
                successText = "Трек добавлен в плейлист «${playlist.name}»"
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка добавления в плейлист"
            } finally {
                isLoading = false
            }
        }
    }

    fun openPlaylist(
        playlist: Playlist,
        accessToken: String
    ) {
        viewModelScope.launch {
            isLoading = true
            errorText = null

            try {
                selectedPlaylist = playlist
                playlistTracks = repository.fetchPlaylistTracks(playlist.id, accessToken)
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки треков плейлиста"
            } finally {
                isLoading = false
            }
        }
    }

    fun closeCurrentPlaylist() {
        selectedPlaylist = null
        playlistTracks = emptyList()
        selectedPlaylistTrackMenu = null
    }

    fun selectPlaylistTrackMenu(track: Track) {
        selectedPlaylistTrackMenu = track
    }

    fun dismissPlaylistTrackMenu() {
        selectedPlaylistTrackMenu = null
    }

    fun removeTrackFromCurrentPlaylist(
        track: Track,
        accessToken: String
    ) {
        val playlist = selectedPlaylist ?: return

        viewModelScope.launch {
            isLoading = true
            errorText = null

            try {
                repository.removeTrackFromPlaylist(
                    playlistId = playlist.id,
                    trackId = track.id,
                    accessToken = accessToken
                )

                playlistTracks = playlistTracks.filterNot { it.id == track.id }
                playlists = playlists.map { item ->
                    if (item.id == playlist.id) {
                        item.copy(tracksCount = (item.tracksCount - 1).coerceAtLeast(0))
                    } else {
                        item
                    }
                }
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка удаления трека из плейлиста"
            } finally {
                isLoading = false
            }
        }
    }

    fun requestDeletePlaylist(playlist: Playlist) {
        selectedPlaylistForDelete = playlist
    }

    fun dismissDeletePlaylist() {
        selectedPlaylistForDelete = null
    }

    fun deleteSelectedPlaylist(accessToken: String) {
        val playlist = selectedPlaylistForDelete ?: return

        viewModelScope.launch {
            isLoading = true
            errorText = null

            try {
                repository.deletePlaylist(playlist.id, accessToken)

                if (selectedPlaylist?.id == playlist.id) {
                    selectedPlaylist = null
                    playlistTracks = emptyList()
                }

                playlists = playlists.filterNot { it.id == playlist.id }
                selectedPlaylistForDelete = null
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка удаления плейлиста"
            } finally {
                isLoading = false
            }
        }
    }
}

class UsersViewModel : ViewModel() {
    private val repository = TrackHubRepository

    var query by mutableStateOf("")
        private set

    var users by mutableStateOf<List<UserPublic>>(emptyList())
        private set

    var following by mutableStateOf<List<FollowUser>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorText by mutableStateOf<String?>(null)
        private set

    private var searchJob: Job? = null

    fun updateQuery(
        value: String,
        accessToken: String
    ) {
        query = value
        errorText = null

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            loadUsers(accessToken, query)
        }
    }

    fun loadUsers(
        accessToken: String,
        searchText: String = query
    ) {
        viewModelScope.launch {
            isLoading = true
            errorText = null

            try {
                users = repository.searchUsers(
                    accessToken = accessToken,
                    query = searchText
                )
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка поиска пользователей"
            } finally {
                isLoading = false
            }
        }
    }

    fun toggleFollow(
        user: UserPublic,
        accessToken: String
    ) {
        viewModelScope.launch {
            errorText = null

            try {
                val updatedUser = if (user.isFollowing) {
                    repository.unfollowUser(user.id, accessToken)
                    user.copy(
                        isFollowing = false,
                        followersCount = (user.followersCount - 1).coerceAtLeast(0)
                    )
                } else {
                    repository.followUser(user.id, accessToken)
                    user.copy(
                        isFollowing = true,
                        followersCount = user.followersCount + 1
                    )
                }

                users = users.map { item ->
                    if (item.id == user.id) updatedUser else item
                }
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка изменения подписки"
            }
        }
    }

    fun loadFollowing(accessToken: String) {
        viewModelScope.launch {
            isLoading = true
            errorText = null

            try {
                following = repository.fetchMyFollowing(accessToken)
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки подписок"
            } finally {
                isLoading = false
            }
        }
    }

    fun unfollowAndRemove(
        user: FollowUser,
        accessToken: String
    ) {
        viewModelScope.launch {
            errorText = null

            try {
                repository.unfollowUser(user.id, accessToken)
                following = following.filterNot { it.id == user.id }
                users = users.map { item ->
                    if (item.id == user.id) {
                        item.copy(
                            isFollowing = false,
                            followersCount = (item.followersCount - 1).coerceAtLeast(0)
                        )
                    } else {
                        item
                    }
                }
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка отписки"
            }
        }
    }
}
