package com.example.trackhub

import android.content.Context
import android.net.Uri

/**
 * Repository layer for TrackHub.
 *
 * UI -> ViewModel -> Repository -> API.
 * The repository currently delegates to TrackHubApi.kt, but keeps ViewModels
 * independent from the concrete network implementation.
 */
object TrackHubRepository {
    suspend fun loginUser(
        email: String,
        password: String
    ): String = com.example.trackhub.loginUser(email, password)

    suspend fun registerUser(
        username: String,
        email: String,
        password: String
    ): String = com.example.trackhub.registerUser(username, email, password)

    suspend fun fetchCurrentUser(accessToken: String): UserProfile =
        com.example.trackhub.fetchCurrentUser(accessToken)

    suspend fun fetchTracks(
        query: String,
        accessToken: String,
        limit: Int = TRACK_PAGE_SIZE,
        offset: Int = 0
    ): List<Track> = com.example.trackhub.fetchTracks(query, accessToken, limit, offset)

    suspend fun fetchMyTracks(
        accessToken: String,
        limit: Int = TRACK_PAGE_SIZE,
        offset: Int = 0
    ): List<Track> = com.example.trackhub.fetchMyTracks(accessToken, limit, offset)

    suspend fun fetchLikedTracks(
        accessToken: String,
        limit: Int = TRACK_PAGE_SIZE,
        offset: Int = 0
    ): List<Track> = com.example.trackhub.fetchLikedTracks(accessToken, limit, offset)

    suspend fun likeTrack(
        trackId: Int,
        accessToken: String
    ): LikeResult = com.example.trackhub.likeTrack(trackId, accessToken)

    suspend fun registerTrackPlay(
        trackId: Int,
        accessToken: String
    ): Int = com.example.trackhub.registerTrackPlay(trackId, accessToken)

    suspend fun deleteTrack(
        trackId: Int,
        accessToken: String
    ) = com.example.trackhub.deleteTrack(trackId, accessToken)

    suspend fun uploadTrack(
        context: Context,
        title: String,
        author: String,
        fileUri: Uri,
        coverImageUri: Uri? = null,
        accessToken: String
    ): Track = com.example.trackhub.uploadTrack(context, title, author, fileUri, coverImageUri, accessToken)

    suspend fun fetchComments(
        trackId: Int,
        limit: Int = COMMENT_PAGE_SIZE,
        offset: Int = 0
    ): List<TrackComment> = com.example.trackhub.fetchComments(trackId, limit, offset)

    suspend fun addComment(
        trackId: Int,
        text: String,
        accessToken: String
    ) = com.example.trackhub.addComment(trackId, text, accessToken)

    suspend fun fetchPlaylists(
        accessToken: String,
        limit: Int = PLAYLIST_PAGE_SIZE,
        offset: Int = 0
    ): List<Playlist> = com.example.trackhub.fetchPlaylists(accessToken, limit, offset)

    suspend fun createPlaylist(
        name: String,
        accessToken: String
    ): Playlist = com.example.trackhub.createPlaylist(name, accessToken)

    suspend fun addTrackToPlaylist(
        playlistId: Int,
        trackId: Int,
        accessToken: String
    ) = com.example.trackhub.addTrackToPlaylist(playlistId, trackId, accessToken)

    suspend fun deletePlaylist(
        playlistId: Int,
        accessToken: String
    ) = com.example.trackhub.deletePlaylist(playlistId, accessToken)

    suspend fun removeTrackFromPlaylist(
        playlistId: Int,
        trackId: Int,
        accessToken: String
    ) = com.example.trackhub.removeTrackFromPlaylist(playlistId, trackId, accessToken)

    suspend fun fetchPlaylistTracks(
        playlistId: Int,
        accessToken: String,
        limit: Int = TRACK_PAGE_SIZE,
        offset: Int = 0
    ): List<Track> = com.example.trackhub.fetchPlaylistTracks(playlistId, accessToken, limit, offset)

    suspend fun searchUsers(
        accessToken: String,
        query: String,
        limit: Int = USER_PAGE_SIZE,
        offset: Int = 0
    ): List<UserPublic> = com.example.trackhub.searchUsers(accessToken, query, limit, offset)

    suspend fun fetchMyFollowing(
        accessToken: String,
        limit: Int = USER_PAGE_SIZE,
        offset: Int = 0
    ): List<FollowUser> = com.example.trackhub.fetchMyFollowing(accessToken, limit, offset)

    suspend fun followUser(
        userId: Int,
        accessToken: String
    ): FollowStatus = com.example.trackhub.followUser(userId, accessToken)

    suspend fun unfollowUser(
        userId: Int,
        accessToken: String
    ): FollowStatus = com.example.trackhub.unfollowUser(userId, accessToken)
}
