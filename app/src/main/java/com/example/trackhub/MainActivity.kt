package com.example.trackhub

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.trackhub.ui.theme.TrackHubTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val BASE_URL = "http://10.0.2.2:8000"

private val TrackHubBackground = Color(0xFF050505)
private val TrackHubSurface = Color(0xFF0B0B0D)
private val TrackHubSurfaceLight = Color(0xFF111114)
private val TrackHubGold = Color(0xFFD49A00)
private val TrackHubGoldLight = Color(0xFFFFC84D)
private val TrackHubText = Color(0xFFF4F4F4)
private val TrackHubMutedText = Color(0xFF9E9E9E)
private val TrackHubBorder = Color(0xFF2A2418)

data class Track(
    val id: Int,
    val title: String,
    val author: String,
    val streamUrl: String,
    val likesCount: Int,
    val commentsCount: Int
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

class MainActivity : ComponentActivity() {
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TrackHubTheme {
                TrackHubApp()
            }
        }
    }
}

@Composable
fun TrackHubApp() {
    var accessToken by remember { mutableStateOf<String?>(null) }

    if (accessToken == null) {
        AuthScreen(
            onAuthSuccess = { token ->
                accessToken = token
            }
        )
    } else {
        CatalogScreen(
            accessToken = accessToken!!,
            onLogout = {
                accessToken = null
            }
        )
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    var isRegisterMode by remember { mutableStateOf(false) }

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("test@example.com") }
    var password by remember { mutableStateOf("12345678") }

    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF020202),
                        Color(0xFF070707),
                        Color(0xFF000000)
                    )
                )
            )
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TrackHubLogo()

            Spacer(modifier = Modifier.height(28.dp))

            AuthModeSwitcher(
                isRegisterMode = isRegisterMode,
                onLoginClick = {
                    errorText = null
                    isRegisterMode = false
                },
                onRegisterClick = {
                    errorText = null
                    isRegisterMode = true
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = Color.Black,
                        spotColor = TrackHubGold.copy(alpha = 0.18f)
                    )
                    .background(
                        color = TrackHubSurface.copy(alpha = 0.94f),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = TrackHubBorder,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(22.dp)
            ) {
                if (isRegisterMode) {
                    Text(
                        text = "Имя пользователя",
                        color = TrackHubText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TrackHubTextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = "Введите имя пользователя",
                        leadingIcon = "👤"
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = "Email",
                    color = TrackHubText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                TrackHubTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Введите ваш email",
                    leadingIcon = "✉"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Пароль",
                    color = TrackHubText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                TrackHubTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Введите ваш пароль",
                    leadingIcon = "🔒",
                    trailingIcon = "◉"
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Забыли пароль?",
                    color = TrackHubGoldLight,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.End)
                )

                Spacer(modifier = Modifier.height(22.dp))

                GoldPrimaryButton(
                    text = if (isRegisterMode) "Зарегистрироваться" else "Войти",
                    enabled = !isLoading,
                    onClick = validateAndSubmit@{
                        val trimmedUsername = username.trim()
                        val trimmedEmail = email.trim()
                        val trimmedPassword = password.trim()

                        errorText = null

                        if (isRegisterMode && trimmedUsername.length < 3) {
                            errorText = "Имя пользователя должно содержать минимум 3 символа"
                            return@validateAndSubmit
                        }

                        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
                            errorText = "Введите корректный email"
                            return@validateAndSubmit
                        }

                        if (trimmedPassword.length < 6) {
                            errorText = "Пароль должен содержать минимум 6 символов"
                            return@validateAndSubmit
                        }

                        scope.launch {
                            isLoading = true
                            errorText = null

                            try {
                                val token = if (isRegisterMode) {
                                    registerUser(trimmedUsername, trimmedEmail, trimmedPassword)
                                } else {
                                    loginUser(trimmedEmail, trimmedPassword)
                                }

                                onAuthSuccess(token)
                            } catch (e: Exception) {
                                errorText = "Не удалось выполнить запрос. Проверьте данные или подключение к серверу."
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                )

                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))

                    CircularProgressIndicator(
                        color = TrackHubGoldLight
                    )
                }

                errorText?.let {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = it,
                        color = Color(0xFFFF6B6B),
                        fontSize = 14.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TrackHubLogo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "◖◗",
            color = TrackHubGoldLight,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "TrackHub",
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = TrackHubText
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Музыка всегда рядом ♫",
            color = TrackHubMutedText,
            fontSize = 18.sp
        )
    }
}

@Composable
fun AuthModeSwitcher(
    isRegisterMode: Boolean,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(
                color = TrackHubSurfaceLight,
                shape = RoundedCornerShape(32.dp)
            )
            .border(
                width = 1.dp,
                color = TrackHubBorder,
                shape = RoundedCornerShape(32.dp)
            )
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AuthTabButton(
            text = "↪  Вход",
            isSelected = !isRegisterMode,
            onClick = onLoginClick,
            modifier = Modifier.weight(1f)
        )

        AuthTabButton(
            text = "♙  Регистрация",
            isSelected = isRegisterMode,
            onClick = onRegisterClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun AuthTabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = if (isSelected) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            TrackHubGold,
                            TrackHubGoldLight,
                            TrackHubGold
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent
                        )
                    )
                },
                shape = RoundedCornerShape(28.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else TrackHubMutedText,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun TrackHubTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: String,
    trailingIcon: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = {
            Text(
                text = placeholder,
                color = TrackHubMutedText
            )
        },
        leadingIcon = {
            Text(
                text = leadingIcon,
                color = TrackHubGoldLight,
                fontSize = 20.sp
            )
        },
        trailingIcon = trailingIcon?.let {
            {
                Text(
                    text = it,
                    color = TrackHubGoldLight,
                    fontSize = 18.sp
                )
            }
        },
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
fun GoldPrimaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = TrackHubGold.copy(alpha = 0.35f),
                spotColor = TrackHubGold.copy(alpha = 0.35f)
            )
            .background(
                brush = if (enabled) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFB47A00),
                            Color(0xFFFFC04D),
                            Color(0xFFB47A00)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF4A4A4A),
                            Color(0xFF5A5A5A)
                        )
                    )
                },
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$text   ›",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    accessToken: String,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var currentTrackTitle by remember { mutableStateOf<String?>(null) }

    var selectedTrackForComments by remember { mutableStateOf<Track?>(null) }
    var selectedTrackForPlaylist by remember { mutableStateOf<Track?>(null) }
    var showPlaylistsScreen by remember { mutableStateOf(false) }
    var showUploadTrackScreen by remember { mutableStateOf(false) }

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
                tracks = fetchTracks(query)
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки треков"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadTracks()
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
    } else if (selectedPlaylistTrack != null) {
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
    } else if (showPlaylistsScreen) {
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
    } else if (showUploadTrackScreen) {
        val closeUploadScreen = {
            showUploadTrackScreen = false
            loadTracks(searchQuery)
        }

        BackHandler {
            closeUploadScreen()
        }

        UploadTrackScreen(
            accessToken = accessToken,
            onBack = closeUploadScreen,
            onUploadSuccess = {
                closeUploadScreen()
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("TrackHub")
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                player.pause()
                                onLogout()
                            }
                        ) {
                            Text("Выйти")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Музыкальный каталог",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Поиск трека") },
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            loadTracks(searchQuery)
                        }
                    ) {
                        Text("Найти")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            searchQuery = ""
                            loadTracks()
                        }
                    ) {
                        Text("Все треки")
                    }

                    Button(
                        onClick = {
                            showPlaylistsScreen = true
                        }
                    ) {
                        Text("Мои плейлисты")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        showUploadTrackScreen = true
                    }
                ) {
                    Text("Загрузить трек")
                }

                Spacer(modifier = Modifier.height(12.dp))

                currentTrackTitle?.let {
                    Text(
                        text = "Сейчас играет: $it",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            player.pause()
                        }
                    ) {
                        Text("Пауза")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (isLoading) {
                    CircularProgressIndicator()
                }

                errorText?.let {
                    Text(
                        text = "Ошибка: $it",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tracks) { track ->
                        TrackCard(
                            track = track,
                            onPlayClick = {
                                val fullStreamUrl = if (track.streamUrl.startsWith("http")) {
                                    track.streamUrl
                                } else {
                                    BASE_URL + track.streamUrl
                                }

                                val mediaItem = MediaItem.fromUri(Uri.parse(fullStreamUrl))

                                player.setMediaItem(mediaItem)
                                player.prepare()
                                player.play()

                                currentTrackTitle = "${track.title} — ${track.author}"
                            },
                            onLikeClick = {
                                scope.launch {
                                    errorText = null

                                    try {
                                        likeTrack(track.id, accessToken)
                                        loadTracks(searchQuery)
                                    } catch (e: Exception) {
                                        errorText = e.message ?: "Ошибка лайка"
                                    }
                                }
                            },
                            onCommentsClick = {
                                selectedTrackForComments = track
                            },
                            onAddToPlaylistClick = {
                                selectedTrackForPlaylist = track
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrackCard(
    track: Track,
    onPlayClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = track.author,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Лайков: ${track.likesCount} · Комментариев: ${track.commentsCount}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPlayClick
                    ) {
                        Text("Play")
                    }

                    Button(
                        onClick = onLikeClick
                    ) {
                        Text("Лайк")
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCommentsClick
                    ) {
                        Text("Комментарии")
                    }

                    Button(
                        onClick = onAddToPlaylistClick
                    ) {
                        Text("В плейлист")
                    }
                }
            }
        }
    }
}

@Composable
fun CommentsScreen(
    track: Track,
    accessToken: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var comments by remember { mutableStateOf<List<TrackComment>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun loadComments() {
        scope.launch {
            isLoading = true
            errorText = null

            try {
                comments = fetchComments(track.id)
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки комментариев"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(track.id) {
        loadComments()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextButton(
            onClick = onBack
        ) {
            Text("← Назад")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = track.title,
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = track.author,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Комментарии",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = commentText,
            onValueChange = { commentText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Ваш комментарий") },
            minLines = 2
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val trimmedText = commentText.trim()

                if (trimmedText.isBlank()) {
                    errorText = "Комментарий не должен быть пустым"
                    return@Button
                }

                scope.launch {
                    isLoading = true
                    errorText = null

                    try {
                        addComment(track.id, trimmedText, accessToken)
                        commentText = ""
                        comments = fetchComments(track.id)
                    } catch (e: Exception) {
                        errorText = e.message ?: "Ошибка отправки комментария"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading
        ) {
            Text("Отправить")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            CircularProgressIndicator()
        }

        errorText?.let {
            Text(
                text = "Ошибка: $it",
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(comments) { comment ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = comment.username,
                            style = MaterialTheme.typography.titleSmall
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = comment.text,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddToPlaylistScreen(
    track: Track,
    accessToken: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var playlistName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var successText by remember { mutableStateOf<String?>(null) }

    fun loadPlaylists() {
        scope.launch {
            isLoading = true
            errorText = null

            try {
                playlists = fetchPlaylists(accessToken)
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки плейлистов"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadPlaylists()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextButton(
            onClick = onBack
        ) {
            Text("← Назад")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Добавить в плейлист",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "${track.title} — ${track.author}",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = playlistName,
            onValueChange = { playlistName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Название нового плейлиста") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val trimmedName = playlistName.trim()

                if (trimmedName.isBlank()) {
                    errorText = "Название плейлиста не должно быть пустым"
                    return@Button
                }

                scope.launch {
                    isLoading = true
                    errorText = null
                    successText = null

                    try {
                        createPlaylist(trimmedName, accessToken)
                        playlistName = ""
                        playlists = fetchPlaylists(accessToken)
                        successText = "Плейлист создан"
                    } catch (e: Exception) {
                        errorText = e.message ?: "Ошибка создания плейлиста"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading
        ) {
            Text("Создать плейлист")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Мои плейлисты",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            CircularProgressIndicator()
        }

        successText?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary
            )
        }

        errorText?.let {
            Text(
                text = "Ошибка: $it",
                color = MaterialTheme.colorScheme.error
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(playlists) { playlist ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "Треков: ${playlist.tracksCount}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    errorText = null
                                    successText = null

                                    try {
                                        addTrackToPlaylist(
                                            playlistId = playlist.id,
                                            trackId = track.id,
                                            accessToken = accessToken
                                        )

                                        playlists = fetchPlaylists(accessToken)
                                        successText = "Трек добавлен в плейлист"
                                    } catch (e: Exception) {
                                        errorText = e.message ?: "Ошибка добавления в плейлист"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading
                        ) {
                            Text("Добавить")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistsScreen(
    accessToken: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var playlistTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var playlistName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun loadPlaylists() {
        scope.launch {
            isLoading = true
            errorText = null

            try {
                playlists = fetchPlaylists(accessToken)
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки плейлистов"
            } finally {
                isLoading = false
            }
        }
    }

    fun openPlaylist(playlist: Playlist) {
        scope.launch {
            isLoading = true
            errorText = null

            try {
                selectedPlaylist = playlist
                playlistTracks = fetchPlaylistTracks(playlist.id, accessToken)
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки треков плейлиста"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadPlaylists()
    }

    val currentPlaylist = selectedPlaylist

    if (currentPlaylist != null) {
        BackHandler {
            selectedPlaylist = null
            playlistTracks = emptyList()
            loadPlaylists()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            TextButton(
                onClick = {
                    selectedPlaylist = null
                    playlistTracks = emptyList()
                    loadPlaylists()
                }
            ) {
                Text("← Назад к плейлистам")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentPlaylist.name,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            }

            if (playlistTracks.isEmpty() && !isLoading) {
                Text("В этом плейлисте пока нет треков")
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(playlistTracks) { track ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = track.author,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            TextButton(
                onClick = onBack
            ) {
                Text("← Назад")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Мои плейлисты",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Название нового плейлиста") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val trimmedName = playlistName.trim()

                    if (trimmedName.isBlank()) {
                        errorText = "Название плейлиста не должно быть пустым"
                        return@Button
                    }

                    scope.launch {
                        isLoading = true
                        errorText = null

                        try {
                            createPlaylist(trimmedName, accessToken)
                            playlistName = ""
                            playlists = fetchPlaylists(accessToken)
                        } catch (e: Exception) {
                            errorText = e.message ?: "Ошибка создания плейлиста"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading
            ) {
                Text("Создать")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            }

            errorText?.let {
                Text(
                    text = "Ошибка: $it",
                    color = MaterialTheme.colorScheme.error
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(playlists) { playlist ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = "Треков: ${playlist.tracksCount}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    openPlaylist(playlist)
                                }
                            ) {
                                Text("Открыть")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UploadTrackScreen(
    accessToken: String,
    onBack: () -> Unit,
    onUploadSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var successText by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedFileUri = uri
        selectedFileName = uri?.let { getFileName(context, it) }
        errorText = null
        successText = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextButton(
            onClick = onBack
        ) {
            Text("← Назад")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Загрузка трека",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Название трека") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = author,
            onValueChange = { author = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Автор") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                filePickerLauncher.launch("audio/*")
            },
            enabled = !isLoading
        ) {
            Text("Выбрать аудиофайл")
        }

        selectedFileName?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Выбран файл: $it",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = uploadButton@{
                val trimmedTitle = title.trim()
                val trimmedAuthor = author.trim()
                val fileUri = selectedFileUri

                errorText = null
                successText = null

                if (trimmedTitle.isBlank()) {
                    errorText = "Введите название трека"
                    return@uploadButton
                }

                if (trimmedAuthor.isBlank()) {
                    errorText = "Введите автора трека"
                    return@uploadButton
                }

                if (fileUri == null) {
                    errorText = "Выберите аудиофайл"
                    return@uploadButton
                }

                scope.launch {
                    isLoading = true
                    errorText = null
                    successText = null

                    try {
                        uploadTrack(
                            context = context,
                            title = trimmedTitle,
                            author = trimmedAuthor,
                            fileUri = fileUri,
                            accessToken = accessToken
                        )

                        successText = "Трек успешно загружен"
                        title = ""
                        author = ""
                        selectedFileUri = null
                        selectedFileName = null

                        onUploadSuccess()
                    } catch (e: Exception) {
                        errorText = e.message ?: "Ошибка загрузки трека"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Загрузить")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            CircularProgressIndicator()
        }

        successText?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary
            )
        }

        errorText?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ошибка: $it",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

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

suspend fun authRequest(
    path: String,
    body: JSONObject
): String {
    return withContext(Dispatchers.IO) {
        val url = URL(BASE_URL + path)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")

            connection.outputStream.use { output ->
                output.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            val responseText = readResponseText(connection)

            if (responseCode !in 200..299) {
                throw RuntimeException("Backend вернул код $responseCode: $responseText")
            }

            val json = JSONObject(responseText)
            json.getString("access_token")
        } finally {
            connection.disconnect()
        }
    }
}

suspend fun fetchTracks(query: String): List<Track> {
    return withContext(Dispatchers.IO) {
        val endpoint = if (query.isBlank()) {
            "$BASE_URL/api/tracks"
        } else {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            "$BASE_URL/api/tracks/search?query=$encodedQuery"
        }

        val url = URL(endpoint)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            val responseText = readResponseText(connection)

            if (responseCode !in 200..299) {
                throw RuntimeException("Backend вернул код $responseCode: $responseText")
            }

            val jsonArray = JSONArray(responseText)
            val result = mutableListOf<Track>()

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)

                result.add(
                    Track(
                        id = item.getInt("id"),
                        title = item.getString("title"),
                        author = item.getString("author"),
                        streamUrl = item.getString("stream_url"),
                        likesCount = item.optInt("likes_count", 0),
                        commentsCount = item.optInt("comments_count", 0)
                    )
                )
            }

            result
        } finally {
            connection.disconnect()
        }
    }
}

suspend fun likeTrack(
    trackId: Int,
    accessToken: String
) {
    withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/api/tracks/$trackId/like")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            val responseText = readResponseText(connection)

            if (responseCode !in 200..299) {
                throw RuntimeException("Backend вернул код $responseCode: $responseText")
            }
        } finally {
            connection.disconnect()
        }
    }
}

suspend fun fetchComments(
    trackId: Int
): List<TrackComment> {
    return withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/api/tracks/$trackId/comments")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            val responseText = readResponseText(connection)

            if (responseCode !in 200..299) {
                throw RuntimeException("Backend вернул код $responseCode: $responseText")
            }

            val jsonArray = JSONArray(responseText)
            val result = mutableListOf<TrackComment>()

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

            result
        } finally {
            connection.disconnect()
        }
    }
}

suspend fun addComment(
    trackId: Int,
    text: String,
    accessToken: String
) {
    withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/api/tracks/$trackId/comments")
        val connection = url.openConnection() as HttpURLConnection

        val body = JSONObject()
            .put("text", text)

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")

            connection.outputStream.use { output ->
                output.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            val responseText = readResponseText(connection)

            if (responseCode !in 200..299) {
                throw RuntimeException("Backend вернул код $responseCode: $responseText")
            }
        } finally {
            connection.disconnect()
        }
    }
}

suspend fun fetchPlaylists(
    accessToken: String
): List<Playlist> {
    return withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/api/playlists")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            val responseText = readResponseText(connection)

            if (responseCode !in 200..299) {
                throw RuntimeException("Backend вернул код $responseCode: $responseText")
            }

            val jsonArray = JSONArray(responseText)
            val result = mutableListOf<Playlist>()

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)

                result.add(
                    Playlist(
                        id = item.getInt("id"),
                        name = item.getString("name"),
                        tracksCount = item.optInt("tracks_count", 0)
                    )
                )
            }

            result
        } finally {
            connection.disconnect()
        }
    }
}

suspend fun createPlaylist(
    name: String,
    accessToken: String
): Playlist {
    return withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/api/playlists")
        val connection = url.openConnection() as HttpURLConnection

        val body = JSONObject()
            .put("name", name)

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")

            connection.outputStream.use { output ->
                output.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            val responseText = readResponseText(connection)

            if (responseCode !in 200..299) {
                throw RuntimeException("Backend вернул код $responseCode: $responseText")
            }

            val item = JSONObject(responseText)

            Playlist(
                id = item.getInt("id"),
                name = item.getString("name"),
                tracksCount = item.optInt("tracks_count", 0)
            )
        } finally {
            connection.disconnect()
        }
    }
}

suspend fun addTrackToPlaylist(
    playlistId: Int,
    trackId: Int,
    accessToken: String
) {
    withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/api/playlists/$playlistId/tracks/$trackId")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            val responseText = readResponseText(connection)

            if (responseCode !in 200..299) {
                throw RuntimeException("Backend вернул код $responseCode: $responseText")
            }
        } finally {
            connection.disconnect()
        }
    }
}

suspend fun fetchPlaylistTracks(
    playlistId: Int,
    accessToken: String
): List<Track> {
    return withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/api/playlists/$playlistId/tracks")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            val responseText = readResponseText(connection)

            if (responseCode !in 200..299) {
                throw RuntimeException("Backend вернул код $responseCode: $responseText")
            }

            val jsonArray = JSONArray(responseText)
            val result = mutableListOf<Track>()

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)

                result.add(
                    Track(
                        id = item.getInt("id"),
                        title = item.getString("title"),
                        author = item.getString("author"),
                        streamUrl = item.getString("stream_url"),
                        likesCount = item.optInt("likes_count", 0),
                        commentsCount = item.optInt("comments_count", 0)
                    )
                )
            }

            result
        } finally {
            connection.disconnect()
        }
    }
}

suspend fun uploadTrack(
    context: Context,
    title: String,
    author: String,
    fileUri: Uri,
    accessToken: String
): Track {
    return withContext(Dispatchers.IO) {
        val boundary = "TrackHubBoundary${System.currentTimeMillis()}"
        val url = URL("$BASE_URL/api/tracks/upload")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty(
                "Content-Type",
                "multipart/form-data; boundary=$boundary"
            )

            val fileName = getFileName(context, fileUri).replace("\"", "")
            val contentType = context.contentResolver.getType(fileUri) ?: "audio/mpeg"

            DataOutputStream(connection.outputStream).use { output ->
                writeFormField(
                    output = output,
                    boundary = boundary,
                    name = "title",
                    value = title
                )

                writeFormField(
                    output = output,
                    boundary = boundary,
                    name = "author",
                    value = author
                )

                output.writeBytes("--$boundary\r\n")
                output.writeBytes(
                    "Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n"
                )
                output.writeBytes("Content-Type: $contentType\r\n")
                output.writeBytes("\r\n")

                context.contentResolver.openInputStream(fileUri)?.use { input ->
                    val buffer = ByteArray(8192)

                    while (true) {
                        val bytesRead = input.read(buffer)

                        if (bytesRead == -1) {
                            break
                        }

                        output.write(buffer, 0, bytesRead)
                    }
                } ?: throw RuntimeException("Не удалось открыть выбранный файл")

                output.writeBytes("\r\n")
                output.writeBytes("--$boundary--\r\n")
                output.flush()
            }

            val responseCode = connection.responseCode
            val responseText = readResponseText(connection)

            if (responseCode !in 200..299) {
                throw RuntimeException("Backend вернул код $responseCode: $responseText")
            }

            val item = JSONObject(responseText)

            Track(
                id = item.getInt("id"),
                title = item.getString("title"),
                author = item.getString("author"),
                streamUrl = item.getString("stream_url"),
                likesCount = item.optInt("likes_count", 0),
                commentsCount = item.optInt("comments_count", 0)
            )
        } finally {
            connection.disconnect()
        }
    }
}

fun writeFormField(
    output: DataOutputStream,
    boundary: String,
    name: String,
    value: String
) {
    output.writeBytes("--$boundary\r\n")
    output.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n")
    output.writeBytes("\r\n")
    output.write(value.toByteArray(Charsets.UTF_8))
    output.writeBytes("\r\n")
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

fun readResponseText(connection: HttpURLConnection): String {
    val stream = if (connection.responseCode in 200..299) {
        connection.inputStream
    } else {
        connection.errorStream
    }

    return stream?.bufferedReader()?.use { it.readText() } ?: ""
}
