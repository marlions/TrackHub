package com.example.trackhub

import androidx.compose.ui.graphics.Color

const val BASE_URL = "http://10.0.2.2:8000"
const val SESSION_PREFS_NAME = "trackhub_session"
const val SESSION_ACCESS_TOKEN_KEY = "access_token"

const val MIN_PASSWORD_LENGTH = 8
const val MAX_TRACK_TITLE_LENGTH = 100
const val MAX_TRACK_AUTHOR_LENGTH = 50
const val MAX_PLAYLIST_NAME_LENGTH = 100
const val MAX_COMMENT_LENGTH = 500
const val MAX_AUDIO_FILE_SIZE_BYTES = 50L * 1024L * 1024L

const val TRACK_PAGE_SIZE = 20
const val USER_PAGE_SIZE = 20
const val PLAYLIST_PAGE_SIZE = 20
const val COMMENT_PAGE_SIZE = 50

val TrackHubBackground = Color(0xFF030303)
val TrackHubSurface = Color(0xE6080809)
val TrackHubSurfaceSoft = Color(0xFF111112)
val TrackHubGold = Color(0xFFD09400)
val TrackHubGoldDark = Color(0xFF9C6A00)
val TrackHubGoldLight = Color(0xFFFFC84D)
val TrackHubText = Color(0xFFF6F6F6)
val TrackHubMutedText = Color(0xFF8A878E)
val TrackHubBorder = Color(0x4AA77D18)
val TrackHubFieldBorder = Color(0xFF473A1A)
