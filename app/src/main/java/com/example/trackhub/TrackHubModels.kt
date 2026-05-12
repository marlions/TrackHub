package com.example.trackhub

data class Track(
    val id: Int,
    val title: String,
    val author: String,
    val streamUrl: String,
    val likesCount: Int,
    val commentsCount: Int,
    val isLiked: Boolean,
    val createdAt: String,
    val fileSizeBytes: Long,
    val durationSeconds: Int,
    val playCount: Int
)

data class TrackComment(
    val id: Int,
    val text: String,
    val username: String,
    val createdAt: String
)

data class Playlist(
    val id: Int,
    val name: String,
    val tracksCount: Int
)

data class UserProfile(
    val id: Int,
    val username: String,
    val email: String
)

data class UserPublic(
    val id: Int,
    val username: String,
    val isFollowing: Boolean,
    val followersCount: Int,
    val followingCount: Int
)

data class FollowUser(
    val id: Int,
    val username: String,
    val followedAt: String
)

data class LikeResult(
    val liked: Boolean,
    val likesCount: Int
)

data class FollowStatus(
    val userId: Int,
    val isFollowing: Boolean,
    val followersCount: Int
)

enum class MainTab {
    HOME,
    SEARCH,
    LIBRARY,
    CREATE
}

enum class LibraryInnerScreen {
    MAIN,
    PLAYLISTS,
    LIKED_TRACKS,
    MY_TRACKS,
    ALL_TRACKS
}
