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

    var hasMoreComments by mutableStateOf(true)
        private set

    var isLoadingMoreComments by mutableStateOf(false)
        private set

    private var commentsOffset = 0

    fun updateCommentText(value: String) {
        commentText = value.take(MAX_COMMENT_LENGTH)
        errorText = null
    }

    fun clearForTrack() {
        comments = emptyList()
        commentText = ""
        isLoading = false
        isSending = false
        errorText = null
        hasMoreComments = true
        isLoadingMoreComments = false
        commentsOffset = 0
    }

    fun loadComments(trackId: Int) {
        viewModelScope.launch {
            isLoading = true
            errorText = null

            try {
                val page = repository.fetchComments(trackId, COMMENT_PAGE_SIZE, 0)
                comments = page
                commentsOffset = page.size
                hasMoreComments = page.size == COMMENT_PAGE_SIZE
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки комментариев"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadMoreComments(trackId: Int) {
        if (isLoading || isLoadingMoreComments || !hasMoreComments) return

        viewModelScope.launch {
            isLoadingMoreComments = true
            errorText = null

            try {
                val page = repository.fetchComments(trackId, COMMENT_PAGE_SIZE, commentsOffset)
                comments = appendUniqueComments(comments, page)
                commentsOffset += page.size
                hasMoreComments = page.size == COMMENT_PAGE_SIZE
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки комментариев"
            } finally {
                isLoadingMoreComments = false
            }
        }
    }

    private fun appendUniqueComments(current: List<TrackComment>, page: List<TrackComment>): List<TrackComment> {
        if (page.isEmpty()) return current
        val existingIds = current.mapTo(mutableSetOf()) { it.id }
        return current + page.filter { it.id !in existingIds }
    }

    fun sendComment(
        trackId: Int,
        accessToken: String
    ) {
        if (isSending || isLoading) return

        val trimmedText = commentText.trim()

        if (trimmedText.isBlank()) {
            errorText = "Комментарий не должен быть пустым"
            return
        }

        if (trimmedText.length > MAX_COMMENT_LENGTH) {
            errorText = "Комментарий должен быть не длиннее 500 символов"
            return
        }

        viewModelScope.launch {
            isSending = true
            errorText = null

            try {
                repository.addComment(trackId, trimmedText, accessToken)
                commentText = ""
                val page = repository.fetchComments(trackId, COMMENT_PAGE_SIZE, 0)
                comments = page
                commentsOffset = page.size
                hasMoreComments = page.size == COMMENT_PAGE_SIZE
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

    var hasMorePlaylists by mutableStateOf(true)
        private set

    var hasMorePlaylistTracks by mutableStateOf(true)
        private set

    var isLoadingMorePlaylists by mutableStateOf(false)
        private set

    var isLoadingMorePlaylistTracks by mutableStateOf(false)
        private set

    private var playlistsOffset = 0
    private var playlistTracksOffset = 0

    fun updatePlaylistName(value: String) {
        playlistName = value.take(MAX_PLAYLIST_NAME_LENGTH)
        errorText = null
        successText = null
    }

    fun clearStatus() {
        errorText = null
        successText = null
    }

    fun loadPlaylists(accessToken: String) {
        if (isLoading) return
        playlistsOffset = 0

        viewModelScope.launch {
            isLoading = true
            errorText = null

            try {
                val page = repository.fetchPlaylists(accessToken, PLAYLIST_PAGE_SIZE, 0)
                playlists = page
                playlistsOffset = page.size
                hasMorePlaylists = page.size == PLAYLIST_PAGE_SIZE
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки плейлистов"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadMorePlaylists(accessToken: String) {
        if (isLoading || isLoadingMorePlaylists || !hasMorePlaylists) return

        viewModelScope.launch {
            isLoadingMorePlaylists = true
            errorText = null

            try {
                val page = repository.fetchPlaylists(accessToken, PLAYLIST_PAGE_SIZE, playlistsOffset)
                playlists = appendUniquePlaylists(playlists, page)
                playlistsOffset += page.size
                hasMorePlaylists = page.size == PLAYLIST_PAGE_SIZE
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки плейлистов"
            } finally {
                isLoadingMorePlaylists = false
            }
        }
    }

    fun createPlaylist(accessToken: String) {
        if (isLoading) return
        val trimmedName = playlistName.trim()

        if (trimmedName.isBlank()) {
            errorText = "Название плейлиста не должно быть пустым"
            return
        }

        if (trimmedName.length > MAX_PLAYLIST_NAME_LENGTH) {
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
        if (isLoading) return
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
        if (isLoading) return
        viewModelScope.launch {
            isLoading = true
            errorText = null

            try {
                selectedPlaylist = playlist
                playlistTracksOffset = 0
                val page = repository.fetchPlaylistTracks(playlist.id, accessToken, TRACK_PAGE_SIZE, 0)
                playlistTracks = page
                playlistTracksOffset = page.size
                hasMorePlaylistTracks = page.size == TRACK_PAGE_SIZE
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки треков плейлиста"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadMorePlaylistTracks(accessToken: String) {
        val playlist = selectedPlaylist ?: return
        if (isLoading || isLoadingMorePlaylistTracks || !hasMorePlaylistTracks) return

        viewModelScope.launch {
            isLoadingMorePlaylistTracks = true
            errorText = null

            try {
                val page = repository.fetchPlaylistTracks(
                    playlistId = playlist.id,
                    accessToken = accessToken,
                    limit = TRACK_PAGE_SIZE,
                    offset = playlistTracksOffset
                )
                playlistTracks = appendUniqueTracks(playlistTracks, page)
                playlistTracksOffset += page.size
                hasMorePlaylistTracks = page.size == TRACK_PAGE_SIZE
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки треков плейлиста"
            } finally {
                isLoadingMorePlaylistTracks = false
            }
        }
    }

    fun closeCurrentPlaylist() {
        selectedPlaylist = null
        playlistTracks = emptyList()
        playlistTracksOffset = 0
        hasMorePlaylistTracks = true
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
        if (isLoading) return
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
        if (isLoading) return
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


    private fun appendUniquePlaylists(current: List<Playlist>, page: List<Playlist>): List<Playlist> {
        if (page.isEmpty()) return current
        val existingIds = current.mapTo(mutableSetOf()) { it.id }
        return current + page.filter { it.id !in existingIds }
    }

    private fun appendUniqueTracks(current: List<Track>, page: List<Track>): List<Track> {
        if (page.isEmpty()) return current
        val existingIds = current.mapTo(mutableSetOf()) { it.id }
        return current + page.filter { it.id !in existingIds }
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

    var hasMoreUsers by mutableStateOf(true)
        private set

    var hasMoreFollowing by mutableStateOf(true)
        private set

    var isLoadingMoreUsers by mutableStateOf(false)
        private set

    var isLoadingMoreFollowing by mutableStateOf(false)
        private set

    private var searchJob: Job? = null
    private var usersOffset = 0
    private var followingOffset = 0
    private var currentUsersQuery = ""

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
        if (isLoading) return
        currentUsersQuery = searchText
        usersOffset = 0

        viewModelScope.launch {
            isLoading = true
            errorText = null

            try {
                val page = repository.searchUsers(
                    accessToken = accessToken,
                    query = searchText,
                    limit = USER_PAGE_SIZE,
                    offset = 0
                )
                users = page
                usersOffset = page.size
                hasMoreUsers = page.size == USER_PAGE_SIZE
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка поиска пользователей"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadMoreUsers(accessToken: String) {
        if (isLoading || isLoadingMoreUsers || !hasMoreUsers) return

        viewModelScope.launch {
            isLoadingMoreUsers = true
            errorText = null

            try {
                val page = repository.searchUsers(
                    accessToken = accessToken,
                    query = currentUsersQuery,
                    limit = USER_PAGE_SIZE,
                    offset = usersOffset
                )
                users = appendUniqueUsers(users, page)
                usersOffset += page.size
                hasMoreUsers = page.size == USER_PAGE_SIZE
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка поиска пользователей"
            } finally {
                isLoadingMoreUsers = false
            }
        }
    }

    fun toggleFollow(
        user: UserPublic,
        accessToken: String
    ) {
        if (isLoading) return

        viewModelScope.launch {
            isLoading = true
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
            } finally {
                isLoading = false
            }
        }
    }

    fun loadFollowing(accessToken: String) {
        if (isLoading) return
        followingOffset = 0

        viewModelScope.launch {
            isLoading = true
            errorText = null

            try {
                val page = repository.fetchMyFollowing(accessToken, USER_PAGE_SIZE, 0)
                following = page
                followingOffset = page.size
                hasMoreFollowing = page.size == USER_PAGE_SIZE
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки подписок"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadMoreFollowing(accessToken: String) {
        if (isLoading || isLoadingMoreFollowing || !hasMoreFollowing) return

        viewModelScope.launch {
            isLoadingMoreFollowing = true
            errorText = null

            try {
                val page = repository.fetchMyFollowing(accessToken, USER_PAGE_SIZE, followingOffset)
                following = appendUniqueFollowing(following, page)
                followingOffset += page.size
                hasMoreFollowing = page.size == USER_PAGE_SIZE
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки подписок"
            } finally {
                isLoadingMoreFollowing = false
            }
        }
    }

    fun unfollowAndRemove(
        user: FollowUser,
        accessToken: String
    ) {
        if (isLoading) return

        viewModelScope.launch {
            isLoading = true
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
            } finally {
                isLoading = false
            }
        }
    }

    private fun appendUniqueUsers(current: List<UserPublic>, page: List<UserPublic>): List<UserPublic> {
        if (page.isEmpty()) return current
        val existingIds = current.mapTo(mutableSetOf()) { it.id }
        return current + page.filter { it.id !in existingIds }
    }

    private fun appendUniqueFollowing(current: List<FollowUser>, page: List<FollowUser>): List<FollowUser> {
        if (page.isEmpty()) return current
        val existingIds = current.mapTo(mutableSetOf()) { it.id }
        return current + page.filter { it.id !in existingIds }
    }

}
