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

@Composable
fun TrackHubBottomNavigation(
    currentTab: MainTab,
    onTabClick: (MainTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .background(Color.Black.copy(alpha = 0.96f))
            .border(1.dp, Color(0x16FFFFFF))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            tab = MainTab.HOME,
            currentTab = currentTab,
            title = "Главная",
            onClick = onTabClick
        )

        BottomNavItem(
            tab = MainTab.SEARCH,
            currentTab = currentTab,
            title = "Поиск",
            onClick = onTabClick
        )

        BottomNavItem(
            tab = MainTab.LIBRARY,
            currentTab = currentTab,
            title = "Моя медиатека",
            onClick = onTabClick
        )

        BottomNavItem(
            tab = MainTab.CREATE,
            currentTab = currentTab,
            title = "Создать",
            onClick = onTabClick
        )
    }
}

@Composable
fun RowScope.BottomNavItem(
    tab: MainTab,
    currentTab: MainTab,
    title: String,
    onClick: (MainTab) -> Unit
) {
    val selected = tab == currentTab

    Column(
        modifier = Modifier
            .weight(1f)
            .clickable {
                onClick(tab)
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BottomNavIcon(
            tab = tab,
            selected = selected
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            color = if (selected) TrackHubGoldLight else TrackHubMutedText,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun SearchTabScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    tracks: List<Track>,
    isLoading: Boolean,
    errorText: String?,
    currentTrack: Track?,
    isPlaying: Boolean,
    hasMoreTracks: Boolean,
    isLoadingMoreTracks: Boolean,
    onSearchClick: () -> Unit,
    onAllTracksClick: () -> Unit,
    onPlayClick: (Track) -> Unit,
    onLikeClick: (Track) -> Unit,
    onCommentsClick: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit,
    onMoreClick: (Track) -> Unit,
    onLoadMoreClick: () -> Unit
) {
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            return@LaunchedEffect
        }

        delay(450)
        onSearchClick()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(top = 48.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Поиск",
                color = TrackHubText,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        item {
            TrackHubSearchField(
                value = searchQuery,
                onValueChange = onSearchQueryChange
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DarkSmallButton(
                    text = "Все треки",
                    onClick = onAllTracksClick
                )
            }
        }

        errorText?.let {
            item {
                BackendErrorCard(
                    text = it,
                    onRetry = onSearchClick
                )
            }
        }

        item {
            Text(
                text = if (searchQuery.isBlank()) "Все треки" else "Результаты поиска",
                color = TrackHubText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isLoading && tracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = TrackHubGoldLight
                    )
                }
            }
        }

        if (tracks.isEmpty() && !isLoading && errorText == null) {
            item {
                EmptySearchResultCard(
                    searchQuery = searchQuery
                )
            }
        }

        items(items = tracks, key = { it.id }) { track ->
            SearchTrackResultCard(
                track = track,
                isCurrentTrack = currentTrack?.id == track.id,
                isPlaying = currentTrack?.id == track.id && isPlaying,
                onPlayClick = { onPlayClick(track) },
                onLikeClick = { onLikeClick(track) },
                onCommentsClick = { onCommentsClick(track) },
                onAddToPlaylistClick = { onAddToPlaylistClick(track) },
                onMoreClick = { onMoreClick(track) }
            )
        }

        if (hasMoreTracks && tracks.isNotEmpty()) {
            item {
                PaginationLoadMoreButton(
                    isLoading = isLoadingMoreTracks,
                    onClick = onLoadMoreClick
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}

@Composable
fun SearchTrackResultCard(
    track: Track,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF111111).copy(alpha = 0.90f))
            .border(1.dp, TrackHubBorder, RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPlayClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackCoverPlaceholder(
                track = track,
                modifier = Modifier.size(58.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.title,
                    color = TrackHubText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = track.author,
                    color = TrackHubMutedText,
                    fontSize = 14.sp,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Лайков: ${track.likesCount} · Комментариев: ${track.commentsCount}",
                    color = TrackHubMutedText.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, TrackHubGoldLight, CircleShape)
                    .clickable(onClick = onPlayClick),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrentTrack && isPlaying) {
                    PauseGoldIcon()
                } else {
                    PlayGoldIcon()
                }
            }

            if (onMoreClick != null) {
                Spacer(modifier = Modifier.width(6.dp))

                TrackOptionsDotsButton(
                    onClick = onMoreClick
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchActionButton(
                text = "Лайк",
                onClick = onLikeClick,
                modifier = Modifier.weight(1f)
            )

            SearchActionButton(
                text = "Комментарии",
                onClick = onCommentsClick,
                modifier = Modifier.weight(1.45f)
            )

            SearchActionButton(
                text = "В плейлист",
                onClick = onAddToPlaylistClick,
                modifier = Modifier.weight(1.35f)
            )
        }
    }
}

@Composable
fun SearchActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.42f))
            .border(1.dp, Color(0x44FFC84D), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = TrackHubText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun EmptySearchResultCard(
    searchQuery: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TrackHubSurface.copy(alpha = 0.80f))
            .border(1.dp, TrackHubBorder, RoundedCornerShape(18.dp))
            .padding(vertical = 28.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TrackHubPngIcon(
            drawableId = R.drawable.search_icon,
            size = 42.dp,
            color = TrackHubGoldLight,
            contentDescription = null
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Треки не найдены",
            color = TrackHubText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (searchQuery.isBlank()) {
                "Пока нет доступных треков"
            } else {
                "Попробуйте изменить поисковый запрос"
            },
            color = TrackHubMutedText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LibraryTabScreen(
    tracks: List<Track>,
    likedTracksCount: Int,
    onPlaylistsClick: () -> Unit,
    onLikedTracksClick: () -> Unit,
    onMyTracksClick: () -> Unit,
    onAllTracksClick: () -> Unit
) {
    val allTracksCount = tracks.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(top = 48.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Моя медиатека",
                color = TrackHubText,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        item {
            LibraryActionCard(
                title = "Мои плейлисты",
                subtitle = "Открыть созданные плейлисты",
                iconText = "playlists",
                onClick = onPlaylistsClick
            )
        }

        item {
            LibraryActionCard(
                title = "Любимые треки",
                subtitle = "Треков с лайками: $likedTracksCount",
                iconText = "likes",
                onClick = onLikedTracksClick
            )
        }

        item {
            LibraryActionCard(
                title = "Мои треки",
                subtitle = "Показать только мои загруженные треки",
                iconText = "tracks",
                onClick = onMyTracksClick
            )
        }

        item {
            LibraryActionCard(
                title = "Все треки",
                subtitle = "Всего треков: $allTracksCount",
                iconText = "tracks",
                onClick = onAllTracksClick
            )
        }

        item {
            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}

@Composable
fun LibraryTracksListScreen(
    title: String,
    subtitle: String,
    emptyText: String,
    tracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    hasMoreTracks: Boolean,
    isLoadingMoreTracks: Boolean,
    onBack: () -> Unit,
    onPlayClick: (Track) -> Unit,
    onLikeClick: (Track) -> Unit,
    onCommentsClick: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit,
    onMoreClick: (Track) -> Unit,
    onLoadMoreClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(top = 44.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TextButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 0.dp)
            ) {
                Text(
                    text = "← Назад",
                    color = TrackHubGoldLight,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item {
            Column {
                Text(
                    text = title,
                    color = TrackHubText,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    color = TrackHubMutedText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (tracks.isEmpty()) {
            item {
                EmptyLibraryTracksCard(
                    text = emptyText
                )
            }
        }

        items(items = tracks, key = { it.id }) { track ->
            SearchTrackResultCard(
                track = track,
                isCurrentTrack = currentTrack?.id == track.id,
                isPlaying = currentTrack?.id == track.id && isPlaying,
                onPlayClick = { onPlayClick(track) },
                onLikeClick = { onLikeClick(track) },
                onCommentsClick = { onCommentsClick(track) },
                onAddToPlaylistClick = { onAddToPlaylistClick(track) },
                onMoreClick = { onMoreClick(track) }
            )
        }

        if (hasMoreTracks && tracks.isNotEmpty()) {
            item {
                PaginationLoadMoreButton(
                    isLoading = isLoadingMoreTracks,
                    onClick = onLoadMoreClick
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}

@Composable
fun EmptyLibraryTracksCard(
    text: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TrackHubSurface.copy(alpha = 0.82f))
            .border(1.dp, TrackHubBorder, RoundedCornerShape(18.dp))
            .padding(vertical = 28.dp, horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "♪",
            color = TrackHubGoldLight,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = text,
            color = TrackHubText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Когда здесь появятся треки, они будут отображаться списком.",
            color = TrackHubMutedText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CreateTabScreen(
    accessToken: String,
    onUploadSuccess: (Track) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSizeBytes by remember { mutableStateOf<Long?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var successText by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedFileUri = uri
        selectedFileName = uri?.let { getFileName(context, it) }
        selectedFileSizeBytes = uri?.let { getFileSizeBytes(context, it).takeIf { size -> size >= 0L } }
        errorText = null
        successText = null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(top = 48.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Создать",
                color = TrackHubText,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(TrackHubSurface.copy(alpha = 0.84f))
                    .border(1.dp, TrackHubBorder, RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CreateTrackIcon()

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Загрузить трек",
                            color = TrackHubText,
                            fontSize = 23.sp,
                            lineHeight = 25.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "Добавьте аудиофайл в TrackHub",
                            color = TrackHubMutedText,
                            fontSize = 13.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                TrackHubInputLabel("Название трека")

                Spacer(modifier = Modifier.height(8.dp))

                TrackHubTextField(
                    value = title,
                    onValueChange = {
                        title = it.take(MAX_TRACK_TITLE_LENGTH)
                        errorText = null
                        successText = null
                    },
                    placeholder = "Введите название трека",
                    keyboardType = KeyboardType.Text,
                    leadingContent = {
                        Text(
                            text = "♪",
                            color = TrackHubGoldLight,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TrackHubInputLabel("Автор")

                Spacer(modifier = Modifier.height(8.dp))

                TrackHubTextField(
                    value = author,
                    onValueChange = {
                        author = it.take(MAX_TRACK_AUTHOR_LENGTH)
                        errorText = null
                        successText = null
                    },
                    placeholder = "Введите автора",
                    keyboardType = KeyboardType.Text,
                    leadingContent = {
                        MiniAuthorIcon()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TrackHubInputLabel("Аудиофайл")

                Spacer(modifier = Modifier.height(8.dp))

                AudioFilePickerCard(
                    selectedFileName = selectedFileName,
                    selectedFileSizeBytes = selectedFileSizeBytes,
                    enabled = !isLoading,
                    onClick = {
                        filePickerLauncher.launch("audio/*")
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                GoldPrimaryButton(
                    text = "Загрузить",
                    enabled = !isLoading,
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

                        if (trimmedTitle.length > MAX_TRACK_TITLE_LENGTH) {
                            errorText = "Название трека должно быть не длиннее 100 символов"
                            return@uploadButton
                        }

                        if (trimmedAuthor.isBlank()) {
                            errorText = "Введите автора трека"
                            return@uploadButton
                        }

                        if (trimmedAuthor.length > MAX_TRACK_AUTHOR_LENGTH) {
                            errorText = "Автор должен быть не длиннее 50 символов"
                            return@uploadButton
                        }

                        if (fileUri == null) {
                            errorText = "Выберите аудиофайл"
                            return@uploadButton
                        }

                        val safeFileName = selectedFileName.orEmpty()
                        val extension = safeFileName.substringAfterLast('.', "").lowercase()

                        if (extension !in setOf("mp3", "wav")) {
                            errorText = "Можно загрузить только MP3 или WAV файл"
                            return@uploadButton
                        }

                        val fileSizeBytes = selectedFileSizeBytes ?: getFileSizeBytes(context, fileUri)

                        if (fileSizeBytes > MAX_AUDIO_FILE_SIZE_BYTES) {
                            errorText = "Размер аудиофайла не должен превышать 50 МБ"
                            return@uploadButton
                        }

                        scope.launch {
                            isLoading = true
                            errorText = null
                            successText = null

                            try {
                                val uploadedTrack = uploadTrack(
                                    context = context,
                                    title = trimmedTitle,
                                    author = trimmedAuthor,
                                    fileUri = fileUri,
                                    accessToken = accessToken
                                )

                                title = ""
                                author = ""
                                selectedFileUri = null
                                selectedFileName = null
                                selectedFileSizeBytes = null
                                successText = "Трек успешно загружен"

                                onUploadSuccess(uploadedTrack)
                            } catch (e: Exception) {
                                errorText = e.message ?: "Ошибка загрузки трека"
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                )

                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = TrackHubGoldLight
                        )
                    }
                }

                successText?.let {
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = it,
                        color = TrackHubGoldLight,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                errorText?.let {
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Ошибка: $it",
                        color = Color(0xFFFF6B6B),
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        item {
            CreatePlaylistHintCard()
        }

        item {
            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}

@Composable
fun CreateTrackIcon() {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TrackHubGold.copy(alpha = 0.12f))
            .border(1.3.dp, TrackHubGoldLight, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "♪",
            color = TrackHubGoldLight,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MiniAuthorIcon() {
    Canvas(
        modifier = Modifier.size(20.dp)
    ) {
        val stroke = 2.0f
        val w = size.width
        val h = size.height

        drawCircle(
            color = TrackHubGoldLight,
            radius = w * 0.17f,
            center = Offset(w * 0.50f, h * 0.28f),
            style = Stroke(width = stroke)
        )

        val bodyPath = Path().apply {
            moveTo(w * 0.20f, h * 0.86f)
            lineTo(w * 0.20f, h * 0.76f)

            cubicTo(
                w * 0.20f, h * 0.58f,
                w * 0.34f, h * 0.50f,
                w * 0.50f, h * 0.50f
            )

            cubicTo(
                w * 0.66f, h * 0.50f,
                w * 0.80f, h * 0.58f,
                w * 0.80f, h * 0.76f
            )

            lineTo(w * 0.80f, h * 0.86f)
        }

        drawPath(
            path = bodyPath,
            color = TrackHubGoldLight,
            style = Stroke(
                width = stroke,
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
fun AudioFilePickerCard(
    selectedFileName: String?,
    selectedFileSizeBytes: Long?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.70f))
            .border(1.dp, TrackHubFieldBorder, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TrackHubGold.copy(alpha = 0.10f))
                .border(1.dp, TrackHubGoldLight, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "♪",
                color = TrackHubGoldLight,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = selectedFileName ?: "Выбрать аудиофайл",
                color = if (selectedFileName == null) TrackHubMutedText else TrackHubText,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = selectedFileSizeBytes?.let { "MP3/WAV • ${formatFileSize(it)}" } ?: "MP3 или WAV аудиофайл до 50 МБ",
                color = TrackHubMutedText.copy(alpha = 0.85f),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "Выбрать",
            color = TrackHubGoldLight,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CreatePlaylistHintCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TrackHubSurface.copy(alpha = 0.76f))
            .border(1.dp, TrackHubBorder, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LibraryCardIcon(
            iconText = "playlists",
            boxSize = 44.dp,
            iconSize = 22.dp,
            cornerRadius = 13.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Плейлисты",
                color = TrackHubText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "Создавать и открывать плейлисты можно в разделе «Моя медиатека».",
                color = TrackHubMutedText,
                fontSize = 13.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
fun LibraryActionCard(
    title: String,
    subtitle: String,
    iconText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(TrackHubSurface.copy(alpha = 0.82f))
            .border(1.dp, TrackHubBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LibraryCardIcon(iconText = iconText)

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = TrackHubText,
                fontSize = 19.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                color = TrackHubMutedText,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        LibraryChevronIcon()
    }
}

@Composable
fun LibraryCardIcon(
    iconText: String,
    boxSize: androidx.compose.ui.unit.Dp = 48.dp,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 14.dp
) {
    Box(
        modifier = Modifier
            .size(boxSize)
            .clip(RoundedCornerShape(cornerRadius))
            .background(TrackHubGold.copy(alpha = 0.10f))
            .border(1.2.dp, TrackHubGoldLight, RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        when (iconText) {
            "playlists", "▤" -> {
                Canvas(modifier = Modifier.size(iconSize)) {
                    val color = TrackHubGoldLight
                    val stroke = 2.0f
                    val w = size.width
                    val h = size.height

                    drawLine(
                        color = color,
                        start = Offset(w * 0.22f, h * 0.25f),
                        end = Offset(w * 0.82f, h * 0.25f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        color = color,
                        start = Offset(w * 0.22f, h * 0.50f),
                        end = Offset(w * 0.82f, h * 0.50f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        color = color,
                        start = Offset(w * 0.22f, h * 0.75f),
                        end = Offset(w * 0.82f, h * 0.75f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        color = color,
                        start = Offset(w * 0.22f, h * 0.15f),
                        end = Offset(w * 0.22f, h * 0.85f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }

            "likes", "♡" -> {
                TrackHubPngIcon(
                    drawableId = R.drawable.heart_icon,
                    size = iconSize,
                    color = null,
                    contentDescription = "Любимые треки"
                )
            }

            "tracks", "♪" -> {
                Text(
                    text = "♪",
                    color = TrackHubGoldLight,
                    fontSize = (iconSize.value + 2).sp,
                    fontWeight = FontWeight.Bold
                )
            }

            "+" -> {
                Text(
                    text = "+",
                    color = TrackHubGoldLight,
                    fontSize = (iconSize.value + 6).sp,
                    fontWeight = FontWeight.Light
                )
            }

            else -> {
                Text(
                    text = iconText,
                    color = TrackHubGoldLight,
                    fontSize = iconSize.value.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LibraryChevronIcon() {
    Canvas(
        modifier = Modifier.size(20.dp)
    ) {
        val color = TrackHubGoldLight
        val stroke = 2.6f

        drawLine(
            color = color,
            start = Offset(size.width * 0.35f, size.height * 0.22f),
            end = Offset(size.width * 0.68f, size.height * 0.50f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        drawLine(
            color = color,
            start = Offset(size.width * 0.35f, size.height * 0.78f),
            end = Offset(size.width * 0.68f, size.height * 0.50f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun TrackHubPngIcon(
    drawableId: Int,
    size: androidx.compose.ui.unit.Dp,
    color: Color? = null,
    contentDescription: String? = null
) {
    Image(
        painter = painterResource(id = drawableId),
        contentDescription = contentDescription,
        modifier = Modifier.size(size),
        contentScale = ContentScale.Fit,
        colorFilter = color?.let { ColorFilter.tint(it) }
    )
}

@Composable
fun TrackHubSearchField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black.copy(alpha = 0.72f))
            .border(1.dp, TrackHubBorder, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackHubPngIcon(
            drawableId = R.drawable.search_icon,
            size = 24.dp,
            color = TrackHubGoldLight,
            contentDescription = "Поиск"
        )

        Spacer(modifier = Modifier.width(12.dp))

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = TrackHubText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            ),
            cursorBrush = SolidColor(TrackHubGoldLight),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isBlank()) {
                        Text(
                            text = "Поиск трека",
                            color = TrackHubMutedText,
                            fontSize = 18.sp
                        )
                    }

                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun PaginationLoadMoreButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.48f))
            .border(1.dp, TrackHubBorder, RoundedCornerShape(50))
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = TrackHubGoldLight,
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "Загрузить ещё",
                color = TrackHubGoldLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GoldSmallButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(50))
            .background(TrackHubGold)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DarkSmallButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF181818))
            .border(1.dp, Color(0x33FFC84D), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = TrackHubText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun EmptySectionText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = TrackHubMutedText,
            fontSize = 15.sp
        )
    }
}

@Composable
fun ProfileCircleButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        TrackHubPngIcon(
            drawableId = R.drawable.profile_icon,
            size = 58.dp,
            color = null,
            contentDescription = "Профиль"
        )
    }
}

@Composable
fun HeartStackIcon() {
    TrackHubPngIcon(
        drawableId = R.drawable.heart_icon,
        size = 44.dp,
        color = null,
        contentDescription = "Ваши лайки"
    )
}

@Composable
fun ShuffleCircleIcon() {
    Box(
        modifier = Modifier.size(44.dp),
        contentAlignment = Alignment.Center
    ) {
        TrackHubPngIcon(
            drawableId = R.drawable.shuffle_icon,
            size = 44.dp,
            color = null,
            contentDescription = "Перемешать"
        )
    }
}

@Composable
fun ClockGoldIcon() {
    Box(
        modifier = Modifier.size(42.dp),
        contentAlignment = Alignment.Center
    ) {
        TrackHubPngIcon(
            drawableId = R.drawable.time_icon,
            size = 42.dp,
            color = null,
            contentDescription = "Недавно добавленные"
        )
    }
}

@Composable
fun PlayGoldIcon() {
    Canvas(
        modifier = Modifier
            .size(20.dp)
            .offset(x = 1.dp)
    ) {
        val playPath = Path().apply {
            moveTo(size.width * 0.20f, size.height * 0.12f)
            lineTo(size.width * 0.20f, size.height * 0.88f)
            lineTo(size.width * 0.86f, size.height * 0.50f)
            close()
        }

        drawPath(
            path = playPath,
            color = Color.White
        )
    }
}

@Composable
fun PauseGoldIcon() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White)
        )

        Box(
            modifier = Modifier
                .width(6.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White)
        )
    }
}

@Composable
fun BottomNavIcon(
    tab: MainTab,
    selected: Boolean
) {
    val iconColor = if (selected) {
        TrackHubGoldLight
    } else {
        TrackHubMutedText
    }

    val icon = when (tab) {
        MainTab.HOME -> R.drawable.home_icon
        MainTab.SEARCH -> R.drawable.search_icon
        MainTab.LIBRARY -> R.drawable.library_icon
        MainTab.CREATE -> R.drawable.create_icon
    }

    val size = when (tab) {
        MainTab.HOME -> 30.dp
        MainTab.SEARCH -> 31.dp
        MainTab.LIBRARY -> 31.dp
        MainTab.CREATE -> 34.dp
    }

    TrackHubPngIcon(
        drawableId = icon,
        size = size,
        color = iconColor,
        contentDescription = null
    )
}
