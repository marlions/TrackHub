package com.example.trackhub

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

suspend fun loginUser(
    email: String,
    password: String
): String {
    val body = JSONObject()
        .put("email", email)
        .put("password", password)

    return authRequest("/api/auth/login", body)
}

suspend fun registerUser(
    username: String,
    email: String,
    password: String
): String {
    val body = JSONObject()
        .put("username", username)
        .put("email", email)
        .put("password", password)

    return authRequest("/api/auth/register", body)
}

private val TrackHubJsonMediaType = "application/json; charset=utf-8".toMediaType()
private val TrackHubEmptyRequestBody = ByteArray(0).toRequestBody(null)

private val TrackHubHttpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(5, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .callTimeout(35, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .build()

private fun Request.Builder.acceptJson(): Request.Builder = apply {
    header("Accept", "application/json")
}

private fun Request.Builder.bearerToken(accessToken: String): Request.Builder = apply {
    header("Authorization", "Bearer $accessToken")
}

private suspend fun executeRequest(request: Request): String {
    return withContext(Dispatchers.IO) {
        TrackHubHttpClient.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw RuntimeException("Backend вернул код ${response.code}: $responseText")
            }

            responseText
        }
    }
}

private fun jsonRequestBody(body: JSONObject): RequestBody {
    return body.toString().toRequestBody(TrackHubJsonMediaType)
}

private fun buildUrl(
    path: String,
    queryParams: Map<String, String> = emptyMap()
): String {
    val builder = (BASE_URL + path).toHttpUrl().newBuilder()

    queryParams.forEach { (name, value) ->
        builder.addQueryParameter(name, value)
    }

    return builder.build().toString()
}

private fun parseTrack(item: JSONObject): Track {
    return Track(
        id = item.getInt("id"),
        title = item.getString("title"),
        author = item.getString("author"),
        streamUrl = item.getString("stream_url"),
        likesCount = item.optInt("likes_count", 0),
        commentsCount = item.optInt("comments_count", 0),
        isLiked = item.optBoolean("is_liked", false),
        createdAt = item.optString("created_at", ""),
        fileSizeBytes = item.optLong("file_size_bytes", 0L),
        durationSeconds = item.optInt("duration_seconds", 0),
        playCount = item.optInt("play_count", 0)
    )
}

private fun parseTracks(responseText: String): List<Track> {
    val jsonArray = JSONArray(responseText)
    val result = ArrayList<Track>(jsonArray.length())

    for (i in 0 until jsonArray.length()) {
        result.add(parseTrack(jsonArray.getJSONObject(i)))
    }

    return result
}

private fun parsePlaylist(item: JSONObject): Playlist {
    return Playlist(
        id = item.getInt("id"),
        name = item.getString("name"),
        tracksCount = item.optInt("tracks_count", 0)
    )
}

private fun parsePlaylists(responseText: String): List<Playlist> {
    val jsonArray = JSONArray(responseText)
    val result = ArrayList<Playlist>(jsonArray.length())

    for (i in 0 until jsonArray.length()) {
        result.add(parsePlaylist(jsonArray.getJSONObject(i)))
    }

    return result
}

suspend fun fetchCurrentUser(
    accessToken: String
): UserProfile {
    val request = Request.Builder()
        .url(buildUrl("/api/auth/me"))
        .get()
        .acceptJson()
        .bearerToken(accessToken)
        .build()

    val item = JSONObject(executeRequest(request))

    return UserProfile(
        id = item.getInt("id"),
        username = item.getString("username"),
        email = item.getString("email")
    )
}

suspend fun authRequest(
    path: String,
    body: JSONObject
): String {
    val request = Request.Builder()
        .url(buildUrl(path))
        .post(jsonRequestBody(body))
        .acceptJson()
        .build()

    val json = JSONObject(executeRequest(request))
    return json.getString("access_token")
}

suspend fun fetchTracks(
    query: String,
    accessToken: String? = null
): List<Track> {
    val url = if (query.isBlank()) {
        buildUrl("/api/tracks")
    } else {
        buildUrl(
            path = "/api/tracks/search",
            queryParams = mapOf("query" to query)
        )
    }

    val builder = Request.Builder()
        .url(url)
        .get()
        .acceptJson()

    if (accessToken != null) {
        builder.bearerToken(accessToken)
    }

    return parseTracks(executeRequest(builder.build()))
}

suspend fun fetchMyTracks(
    accessToken: String
): List<Track> {
    val request = Request.Builder()
        .url(buildUrl("/api/tracks/my"))
        .get()
        .acceptJson()
        .bearerToken(accessToken)
        .build()

    return parseTracks(executeRequest(request))
}

suspend fun fetchLikedTracks(
    accessToken: String
): List<Track> {
    val request = Request.Builder()
        .url(buildUrl("/api/tracks/liked"))
        .get()
        .acceptJson()
        .bearerToken(accessToken)
        .build()

    return parseTracks(executeRequest(request))
}

suspend fun likeTrack(
    trackId: Int,
    accessToken: String
): LikeResult {
    val request = Request.Builder()
        .url(buildUrl("/api/tracks/$trackId/like"))
        .post(TrackHubEmptyRequestBody)
        .acceptJson()
        .bearerToken(accessToken)
        .build()

    val item = JSONObject(executeRequest(request))

    return LikeResult(
        liked = item.optBoolean("liked", false),
        likesCount = item.optInt("likes_count", 0)
    )
}

suspend fun registerTrackPlay(
    trackId: Int,
    accessToken: String
): Int {
    val request = Request.Builder()
        .url(buildUrl("/api/tracks/$trackId/play"))
        .post(TrackHubEmptyRequestBody)
        .acceptJson()
        .bearerToken(accessToken)
        .build()

    val item = JSONObject(executeRequest(request))
    return item.optInt("play_count", 0)
}

suspend fun fetchComments(
    trackId: Int
): List<TrackComment> {
    val request = Request.Builder()
        .url(buildUrl("/api/tracks/$trackId/comments"))
        .get()
        .acceptJson()
        .build()

    val jsonArray = JSONArray(executeRequest(request))
    val result = ArrayList<TrackComment>(jsonArray.length())

    for (i in 0 until jsonArray.length()) {
        val item = jsonArray.getJSONObject(i)

        result.add(
            TrackComment(
                id = item.getInt("id"),
                text = item.getString("text"),
                username = item.optString("username", "Пользователь"),
                createdAt = item.optString("created_at", "")
            )
        )
    }

    return result
}

suspend fun addComment(
    trackId: Int,
    text: String,
    accessToken: String
) {
    val body = JSONObject()
        .put("text", text)

    val request = Request.Builder()
        .url(buildUrl("/api/tracks/$trackId/comments"))
        .post(jsonRequestBody(body))
        .acceptJson()
        .bearerToken(accessToken)
        .build()

    executeRequest(request)
}

suspend fun fetchPlaylists(
    accessToken: String
): List<Playlist> {
    val request = Request.Builder()
        .url(buildUrl("/api/playlists"))
        .get()
        .acceptJson()
        .bearerToken(accessToken)
        .build()

    return parsePlaylists(executeRequest(request))
}

suspend fun createPlaylist(
    name: String,
    accessToken: String
): Playlist {
    val body = JSONObject()
        .put("name", name)

    val request = Request.Builder()
        .url(buildUrl("/api/playlists"))
        .post(jsonRequestBody(body))
        .acceptJson()
        .bearerToken(accessToken)
        .build()

    return parsePlaylist(JSONObject(executeRequest(request)))
}

suspend fun addTrackToPlaylist(
    playlistId: Int,
    trackId: Int,
    accessToken: String
) {
    val request = Request.Builder()
        .url(buildUrl("/api/playlists/$playlistId/tracks/$trackId"))
        .post(TrackHubEmptyRequestBody)
        .acceptJson()
        .bearerToken(accessToken)
        .build()

    executeRequest(request)
}

suspend fun deleteTrack(
    trackId: Int,
    accessToken: String
) {
    val request = Request.Builder()
        .url(buildUrl("/api/tracks/$trackId"))
        .delete()
        .acceptJson()
        .bearerToken(accessToken)
        .build()

    executeRequest(request)
}

suspend fun deletePlaylist(
    playlistId: Int,
    accessToken: String
) {
    val request = Request.Builder()
        .url(buildUrl("/api/playlists/$playlistId"))
        .delete()
        .acceptJson()
        .bearerToken(accessToken)
        .build()

    executeRequest(request)
}

suspend fun removeTrackFromPlaylist(
    playlistId: Int,
    trackId: Int,
    accessToken: String
) {
    val request = Request.Builder()
        .url(buildUrl("/api/playlists/$playlistId/tracks/$trackId"))
        .delete()
        .acceptJson()
        .bearerToken(accessToken)
        .build()

    executeRequest(request)
}

suspend fun fetchPlaylistTracks(
    playlistId: Int,
    accessToken: String
): List<Track> {
    val request = Request.Builder()
        .url(buildUrl("/api/playlists/$playlistId/tracks"))
        .get()
        .acceptJson()
        .bearerToken(accessToken)
        .build()

    return parseTracks(executeRequest(request))
}

suspend fun searchUsers(
    accessToken: String,
    query: String
): List<UserPublic> {
    val request = Request.Builder()
        .url(
            buildUrl(
                path = "/api/users/search",
                queryParams = mapOf("query" to query)
            )
        )
        .get()
        .acceptJson()
        .bearerToken(accessToken)
        .build()

    val jsonArray = JSONArray(executeRequest(request))
    val result = ArrayList<UserPublic>(jsonArray.length())

    for (i in 0 until jsonArray.length()) {
        val item = jsonArray.getJSONObject(i)

        result.add(
            UserPublic(
                id = item.getInt("id"),
                username = item.getString("username"),
                isFollowing = item.optBoolean("is_following", false),
                followersCount = item.optInt("followers_count", 0),
                followingCount = item.optInt("following_count", 0)
            )
        )
    }

    return result
}

suspend fun fetchMyFollowing(
    accessToken: String
): List<FollowUser> {
    val request = Request.Builder()
        .url(buildUrl("/api/users/me/following"))
        .get()
        .acceptJson()
        .bearerToken(accessToken)
        .build()

    val jsonArray = JSONArray(executeRequest(request))
    val result = ArrayList<FollowUser>(jsonArray.length())

    for (i in 0 until jsonArray.length()) {
        val item = jsonArray.getJSONObject(i)

        result.add(
            FollowUser(
                id = item.getInt("id"),
                username = item.getString("username"),
                followedAt = item.optString("followed_at", "")
            )
        )
    }

    return result
}

suspend fun followUser(
    userId: Int,
    accessToken: String
): FollowStatus {
    return followRequest(
        userId = userId,
        accessToken = accessToken,
        method = "POST"
    )
}

suspend fun unfollowUser(
    userId: Int,
    accessToken: String
): FollowStatus {
    return followRequest(
        userId = userId,
        accessToken = accessToken,
        method = "DELETE"
    )
}

suspend fun followRequest(
    userId: Int,
    accessToken: String,
    method: String
): FollowStatus {
    val builder = Request.Builder()
        .url(buildUrl("/api/users/$userId/follow"))
        .acceptJson()
        .bearerToken(accessToken)

    val request = when (method) {
        "POST" -> builder.post(TrackHubEmptyRequestBody).build()
        "DELETE" -> builder.delete().build()
        else -> throw IllegalArgumentException("Unsupported follow method: $method")
    }

    val item = JSONObject(executeRequest(request))

    return FollowStatus(
        userId = item.optInt("user_id", userId),
        isFollowing = item.optBoolean("is_following", method == "POST"),
        followersCount = item.optInt("followers_count", 0)
    )
}

suspend fun uploadTrack(
    context: Context,
    title: String,
    author: String,
    fileUri: Uri,
    accessToken: String
): Track {
    val fileName = getFileName(context, fileUri).replace("\"", "")
    val contentType = context.contentResolver.getType(fileUri) ?: "audio/mpeg"

    val fileRequestBody = UriRequestBody(
        context = context,
        uri = fileUri,
        mediaType = contentType
    )

    val multipartBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("title", title)
        .addFormDataPart("author", author)
        .addFormDataPart("file", fileName, fileRequestBody)
        .build()

    val request = Request.Builder()
        .url(buildUrl("/api/tracks/upload"))
        .post(multipartBody)
        .acceptJson()
        .bearerToken(accessToken)
        .build()

    return parseTrack(JSONObject(executeRequest(request)))
}

private class UriRequestBody(
    private val context: Context,
    private val uri: Uri,
    private val mediaType: String
) : RequestBody() {
    override fun contentType(): MediaType? {
        return mediaType.toMediaTypeOrNull()
    }

    override fun contentLength(): Long {
        return getContentLength(context, uri)
    }

    override fun writeTo(sink: BufferedSink) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(64 * 1024)

            while (true) {
                val bytesRead = input.read(buffer)

                if (bytesRead == -1) {
                    break
                }

                sink.write(buffer, 0, bytesRead)
            }
        } ?: throw RuntimeException("Не удалось открыть выбранный файл")
    }
}

private fun getContentLength(
    context: Context,
    uri: Uri
): Long {
    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
        if (descriptor.length >= 0L) {
            return descriptor.length
        }
    }

    return -1L
}

fun getFileName(
    context: Context,
    uri: Uri
): String {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

        if (nameIndex >= 0 && cursor.moveToFirst()) {
            return cursor.getString(nameIndex)
        }
    }

    return "audio_${System.currentTimeMillis()}.mp3"
}

fun formatTrackTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L

    return "%d:%02d".format(minutes, seconds)
}
