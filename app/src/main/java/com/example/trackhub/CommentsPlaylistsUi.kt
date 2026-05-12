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
fun CommentsScreen(
    track: Track,
    accessToken: String,
    commentsViewModel: CommentsViewModel,
    onBack: () -> Unit
) {
    val comments = commentsViewModel.comments
    val commentText = commentsViewModel.commentText
    val isLoading = commentsViewModel.isLoading
    val isSending = commentsViewModel.isSending
    val errorText = commentsViewModel.errorText
    val hasMoreComments = commentsViewModel.hasMoreComments
    val isLoadingMoreComments = commentsViewModel.isLoadingMoreComments

    LaunchedEffect(track.id) {
        commentsViewModel.clearForTrack()
        commentsViewModel.loadComments(track.id)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .padding(top = 44.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                TrackHubBackTextButton(
                    text = "← Назад",
                    onClick = onBack
                )
            }

            item {
                CommentsTrackHeader(track = track)
            }

            item {
                Text(
                    text = "Комментарии",
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
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Оставить комментарий",
                        color = TrackHubText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CommentInputField(
                        value = commentText,
                        onValueChange = commentsViewModel::updateCommentText
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    GoldPrimaryButton(
                        text = "Отправить",
                        enabled = !isSending && !isLoading,
                        onClick = {
                            commentsViewModel.sendComment(
                                trackId = track.id,
                                accessToken = accessToken
                            )
                        }
                    )

                    if (isSending) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = TrackHubGoldLight
                            )
                        }
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
                Text(
                    text = "Список комментариев",
                    color = TrackHubText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isLoading) {
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

            if (comments.isEmpty() && !isLoading) {
                item {
                    EmptyCommentsCard()
                }
            }

            items(items = comments, key = { it.id }) { comment ->
                CommentCard(comment = comment)
            }

            if (hasMoreComments && comments.isNotEmpty()) {
                item {
                    PaginationLoadMoreButton(
                        isLoading = isLoadingMoreComments,
                        onClick = {
                            commentsViewModel.loadMoreComments(track.id)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}


@Composable
fun CommentsTrackHeader(
    track: Track
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(TrackHubSurface.copy(alpha = 0.84f))
            .border(1.dp, TrackHubBorder, RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackCoverPlaceholder(
            track = track,
            modifier = Modifier.size(62.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.title,
                color = TrackHubText,
                fontSize = 20.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = track.author,
                color = TrackHubGoldLight,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Лайков: ${track.likesCount} · Комментариев: ${track.commentsCount}",
                color = TrackHubMutedText,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CommentInputField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black.copy(alpha = 0.70f))
                .border(1.dp, TrackHubFieldBorder, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(it.take(MAX_COMMENT_LENGTH)) },
                textStyle = TextStyle(
                    color = TrackHubText,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(TrackHubGoldLight),
                modifier = Modifier.fillMaxSize(),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopStart
                    ) {
                        if (value.isBlank()) {
                            Text(
                                text = "Напишите комментарий...",
                                color = TrackHubMutedText,
                                fontSize = 16.sp,
                                lineHeight = 21.sp
                            )
                        }

                        innerTextField()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "${value.length} / $MAX_COMMENT_LENGTH",
            color = if (value.length >= MAX_COMMENT_LENGTH) TrackHubGoldLight else TrackHubMutedText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
fun CommentCard(
    comment: TrackComment
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF111111).copy(alpha = 0.90f))
            .border(1.dp, Color(0x33FFC84D), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            CommentUserIcon()

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = comment.username,
                    color = TrackHubText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                if (comment.createdAt.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = comment.createdAt.take(16).replace("T", " "),
                        color = TrackHubMutedText,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = comment.text,
            color = TrackHubText.copy(alpha = 0.92f),
            fontSize = 15.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CommentUserIcon() {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(TrackHubGold.copy(alpha = 0.12f))
            .border(1.dp, TrackHubGoldLight, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(22.dp)
        ) {
            val stroke = 2.0f
            val w = size.width
            val h = size.height

            drawCircle(
                color = TrackHubGoldLight,
                radius = w * 0.17f,
                center = Offset(w * 0.50f, h * 0.30f),
                style = Stroke(width = stroke)
            )

            val bodyPath = Path().apply {
                moveTo(w * 0.20f, h * 0.86f)
                lineTo(w * 0.20f, h * 0.78f)

                cubicTo(
                    w * 0.20f, h * 0.60f,
                    w * 0.34f, h * 0.52f,
                    w * 0.50f, h * 0.52f
                )

                cubicTo(
                    w * 0.66f, h * 0.52f,
                    w * 0.80f, h * 0.60f,
                    w * 0.80f, h * 0.78f
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
}

@Composable
fun EmptyCommentsCard() {
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
            text = "💬",
            fontSize = 34.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Комментариев пока нет",
            color = TrackHubText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Станьте первым, кто оставит комментарий к этому треку.",
            color = TrackHubMutedText,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AddToPlaylistScreen(
    track: Track,
    accessToken: String,
    playlistsViewModel: PlaylistsViewModel,
    onBack: () -> Unit
) {
    val playlists = playlistsViewModel.playlists
    val playlistName = playlistsViewModel.playlistName
    val isLoading = playlistsViewModel.isLoading
    val errorText = playlistsViewModel.errorText
    val successText = playlistsViewModel.successText
    val hasMorePlaylists = playlistsViewModel.hasMorePlaylists
    val isLoadingMorePlaylists = playlistsViewModel.isLoadingMorePlaylists

    LaunchedEffect(track.id) {
        playlistsViewModel.clearStatus()
        playlistsViewModel.loadPlaylists(accessToken)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .padding(top = 44.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                TrackHubBackTextButton(
                    text = "← Назад",
                    onClick = onBack
                )
            }

            item {
                Text(
                    text = "Добавить в плейлист",
                    color = TrackHubText,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            item {
                AddToPlaylistTrackHeader(track = track)
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(TrackHubSurface.copy(alpha = 0.84f))
                        .border(1.dp, TrackHubBorder, RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Создать новый плейлист",
                        color = TrackHubText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PlaylistNameField(
                        value = playlistName,
                        onValueChange = playlistsViewModel::updatePlaylistName
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GoldSmallButton(
                        text = "Создать плейлист",
                        onClick = {
                            playlistsViewModel.createPlaylist(accessToken)
                        }
                    )
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = TrackHubGoldLight
                        )
                    }
                }
            }

            successText?.let {
                item {
                    AddToPlaylistStatusCard(
                        text = it,
                        isError = false
                    )
                }
            }

            errorText?.let {
                item {
                    AddToPlaylistStatusCard(
                        text = it,
                        isError = true
                    )
                }
            }

            item {
                Text(
                    text = "Выберите плейлист",
                    color = TrackHubText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (playlists.isEmpty() && !isLoading) {
                item {
                    EmptyAddToPlaylistCard()
                }
            }

            items(items = playlists, key = { it.id }) { playlist ->
                AddToPlaylistCard(
                    playlist = playlist,
                    enabled = !isLoading,
                    onAddClick = {
                        playlistsViewModel.addTrackToPlaylist(
                            playlist = playlist,
                            track = track,
                            accessToken = accessToken
                        )
                    }
                )
            }

            if (hasMorePlaylists && playlists.isNotEmpty()) {
                item {
                    PaginationLoadMoreButton(
                        isLoading = isLoadingMorePlaylists,
                        onClick = {
                            playlistsViewModel.loadMorePlaylists(accessToken)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}


@Composable
fun AddToPlaylistTrackHeader(
    track: Track
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(TrackHubSurface.copy(alpha = 0.84f))
            .border(1.dp, TrackHubBorder, RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackCoverPlaceholder(
            track = track,
            modifier = Modifier.size(62.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.title,
                color = TrackHubText,
                fontSize = 20.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = track.author,
                color = TrackHubGoldLight,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Выберите плейлист, куда добавить этот трек",
                color = TrackHubMutedText,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AddToPlaylistCard(
    playlist: Playlist,
    enabled: Boolean,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(TrackHubSurface.copy(alpha = 0.84f))
            .border(1.dp, TrackHubBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LibraryCardIcon(
            iconText = "playlists",
            boxSize = 48.dp,
            iconSize = 24.dp,
            cornerRadius = 14.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = playlist.name,
                color = TrackHubText,
                fontSize = 18.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Треков: ${playlist.tracksCount}",
                color = TrackHubMutedText,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .height(38.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    if (enabled) {
                        TrackHubGold.copy(alpha = 0.95f)
                    } else {
                        Color(0xFF4E4E4E)
                    }
                )
                .clickable(enabled = enabled, onClick = onAddClick)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Добавить",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AddToPlaylistStatusCard(
    text: String,
    isError: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isError) {
                    Color(0xFF2A0808).copy(alpha = 0.86f)
                } else {
                    TrackHubSurface.copy(alpha = 0.82f)
                }
            )
            .border(
                width = 1.dp,
                color = if (isError) Color(0x66FF6B6B) else TrackHubBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = if (isError) "Ошибка: $text" else text,
            color = if (isError) Color(0xFFFF6B6B) else TrackHubGoldLight,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun EmptyAddToPlaylistCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TrackHubSurface.copy(alpha = 0.82f))
            .border(1.dp, TrackHubBorder, RoundedCornerShape(18.dp))
            .padding(vertical = 28.dp, horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LibraryCardIcon(
            iconText = "playlists",
            boxSize = 52.dp,
            iconSize = 26.dp,
            cornerRadius = 15.dp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Плейлистов пока нет",
            color = TrackHubText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Создайте новый плейлист выше, а потом добавьте в него этот трек.",
            color = TrackHubMutedText,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PlaylistsScreen(
    accessToken: String,
    playlistsViewModel: PlaylistsViewModel,
    onBack: () -> Unit
) {
    val playlists = playlistsViewModel.playlists
    val currentPlaylist = playlistsViewModel.selectedPlaylist
    val playlistTracks = playlistsViewModel.playlistTracks
    val playlistName = playlistsViewModel.playlistName
    val selectedPlaylistTrackMenu = playlistsViewModel.selectedPlaylistTrackMenu
    val selectedPlaylistForDelete = playlistsViewModel.selectedPlaylistForDelete
    val isLoading = playlistsViewModel.isLoading
    val errorText = playlistsViewModel.errorText
    val hasMorePlaylists = playlistsViewModel.hasMorePlaylists
    val hasMorePlaylistTracks = playlistsViewModel.hasMorePlaylistTracks
    val isLoadingMorePlaylists = playlistsViewModel.isLoadingMorePlaylists
    val isLoadingMorePlaylistTracks = playlistsViewModel.isLoadingMorePlaylistTracks

    LaunchedEffect(Unit) {
        playlistsViewModel.loadPlaylists(accessToken)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        if (currentPlaylist != null) {
            BackHandler {
                playlistsViewModel.closeCurrentPlaylist()
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp)
                    .padding(top = 44.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    TrackHubBackTextButton(
                        text = "← Назад к плейлистам",
                        onClick = {
                            playlistsViewModel.closeCurrentPlaylist()
                        }
                    )
                }

                item {
                    OpenPlaylistHeader(
                        playlist = currentPlaylist,
                        tracksCount = playlistTracks.size
                    )
                }

                if (isLoading) {
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

                errorText?.let {
                    item {
                        TrackHubErrorText(it)
                    }
                }

                item {
                    Text(
                        text = "Треки",
                        color = TrackHubText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (playlistTracks.isEmpty() && !isLoading) {
                    item {
                        EmptyPlaylistCard(
                            title = "В этом плейлисте пока нет треков",
                            subtitle = "Добавьте треки через кнопку «В плейлист» на карточке трека."
                        )
                    }
                }

                items(items = playlistTracks, key = { it.id }) { track ->
                    PlaylistTrackCard(
                        track = track,
                        onMoreClick = {
                            playlistsViewModel.selectPlaylistTrackMenu(track)
                        }
                    )
                }

                if (hasMorePlaylistTracks && playlistTracks.isNotEmpty()) {
                    item {
                        PaginationLoadMoreButton(
                            isLoading = isLoadingMorePlaylistTracks,
                            onClick = {
                                playlistsViewModel.loadMorePlaylistTracks(accessToken)
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(110.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp)
                    .padding(top = 44.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    TrackHubBackTextButton(
                        text = "← Назад",
                        onClick = onBack
                    )
                }

                item {
                    Text(
                        text = "Мои плейлисты",
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
                            .background(TrackHubSurface.copy(alpha = 0.82f))
                            .border(1.dp, TrackHubBorder, RoundedCornerShape(22.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Создать новый плейлист",
                            color = TrackHubText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        PlaylistNameField(
                            value = playlistName,
                            onValueChange = playlistsViewModel::updatePlaylistName
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        GoldSmallButton(
                            text = "Создать",
                            onClick = {
                                playlistsViewModel.createPlaylist(accessToken)
                            }
                        )
                    }
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = TrackHubGoldLight
                            )
                        }
                    }
                }

                errorText?.let {
                    item {
                        TrackHubErrorText(it)
                    }
                }

                item {
                    Text(
                        text = "Список плейлистов",
                        color = TrackHubText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (playlists.isEmpty() && !isLoading) {
                    item {
                        EmptyPlaylistCard(
                            title = "Плейлистов пока нет",
                            subtitle = "Создайте первый плейлист и добавьте туда свои треки."
                        )
                    }
                }

                items(items = playlists, key = { it.id }) { playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        onClick = {
                            playlistsViewModel.openPlaylist(
                                playlist = playlist,
                                accessToken = accessToken
                            )
                        },
                        onDeleteClick = {
                            playlistsViewModel.requestDeletePlaylist(playlist)
                        }
                    )
                }

                if (hasMorePlaylists && playlists.isNotEmpty()) {
                    item {
                        PaginationLoadMoreButton(
                            isLoading = isLoadingMorePlaylists,
                            onClick = {
                                playlistsViewModel.loadMorePlaylists(accessToken)
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(110.dp))
                }
            }
        }

        selectedPlaylistTrackMenu?.let { track ->
            RemoveFromPlaylistDialog(
                track = track,
                onDismiss = {
                    playlistsViewModel.dismissPlaylistTrackMenu()
                },
                onRemoveClick = {
                    playlistsViewModel.dismissPlaylistTrackMenu()
                    playlistsViewModel.removeTrackFromCurrentPlaylist(
                        track = track,
                        accessToken = accessToken
                    )
                }
            )
        }

        selectedPlaylistForDelete?.let { playlist ->
            DeletePlaylistConfirmDialog(
                playlist = playlist,
                onDismiss = {
                    playlistsViewModel.dismissDeletePlaylist()
                },
                onConfirm = {
                    playlistsViewModel.deleteSelectedPlaylist(accessToken)
                }
            )
        }
    }
}


@Composable
fun TrackHubBackTextButton(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = TrackHubGoldLight,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp)
    )
}

@Composable
fun PlaylistNameField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.70f))
            .border(1.dp, TrackHubFieldBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LibraryCardIcon(
            iconText = "playlists",
            boxSize = 34.dp,
            iconSize = 18.dp,
            cornerRadius = 10.dp
        )

        Spacer(modifier = Modifier.width(10.dp))

        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.take(MAX_PLAYLIST_NAME_LENGTH)) },
            singleLine = true,
            textStyle = TextStyle(
                color = TrackHubText,
                fontSize = 16.sp,
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
                            text = "Название плейлиста",
                            color = TrackHubMutedText,
                            fontSize = 16.sp
                        )
                    }

                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(TrackHubSurface.copy(alpha = 0.84f))
            .border(1.dp, TrackHubBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LibraryCardIcon(
            iconText = "playlists",
            boxSize = 48.dp,
            iconSize = 24.dp,
            cornerRadius = 14.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = playlist.name,
                color = TrackHubText,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Треков: ${playlist.tracksCount}",
                color = TrackHubMutedText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF2A0808).copy(alpha = 0.78f))
                .border(1.dp, Color(0x66FF6B6B), RoundedCornerShape(50))
                .clickable(onClick = onDeleteClick)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Удалить",
                color = Color(0xFFFF6B6B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        LibraryChevronIcon()
    }
}

@Composable
fun DeletePlaylistConfirmDialog(
    playlist: Playlist,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0B0B0C))
                .border(1.dp, Color(0x66FF6B6B), RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Text(
                text = "Удалить плейлист?",
                color = TrackHubText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Плейлист «${playlist.name}» будет удалён. Треки останутся в приложении.",
                color = TrackHubMutedText,
                fontSize = 14.sp,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            TrackMenuActionButton(
                text = "Удалить плейлист",
                danger = true,
                onClick = onConfirm
            )

            Spacer(modifier = Modifier.height(10.dp))

            TrackMenuActionButton(
                text = "Отмена",
                danger = false,
                onClick = onDismiss
            )
        }
    }
}

@Composable
fun OpenPlaylistHeader(
    playlist: Playlist,
    tracksCount: Int
) {
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
            LibraryCardIcon(
                iconText = "playlists",
                boxSize = 58.dp,
                iconSize = 28.dp,
                cornerRadius = 16.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = playlist.name,
                    color = TrackHubText,
                    fontSize = 28.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Треков в плейлисте: $tracksCount",
                    color = TrackHubMutedText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            TrackHubGoldLight.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Здесь отображаются треки, которые вы добавили в этот плейлист.",
            color = TrackHubMutedText,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun PlaylistTrackCard(
    track: Track,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF111111).copy(alpha = 0.90f))
            .border(1.dp, Color(0x33FFC84D), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackCoverPlaceholder(
            track = track,
            modifier = Modifier.size(54.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title,
                color = TrackHubText,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = track.author,
                color = TrackHubMutedText,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }

        TrackOptionsDotsButton(
            onClick = onMoreClick
        )
    }
}

@Composable
fun RemoveFromPlaylistDialog(
    track: Track,
    onDismiss: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0B0B0C))
                .border(1.dp, TrackHubBorder, RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Text(
                text = "Трек в плейлисте",
                color = TrackHubText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackCoverPlaceholder(
                    track = track,
                    modifier = Modifier.size(58.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = track.title,
                        color = TrackHubText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = track.author,
                        color = TrackHubGoldLight,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            TrackMenuActionButton(
                text = "Удалить из плейлиста",
                danger = true,
                onClick = onRemoveClick
            )

            Spacer(modifier = Modifier.height(10.dp))

            TrackMenuActionButton(
                text = "Отмена",
                danger = false,
                onClick = onDismiss
            )
        }
    }
}

@Composable
fun EmptyPlaylistCard(
    title: String,
    subtitle: String
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
        LibraryCardIcon(
            iconText = "playlists",
            boxSize = 52.dp,
            iconSize = 26.dp,
            cornerRadius = 15.dp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = title,
            color = TrackHubText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            color = TrackHubMutedText,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TrackHubErrorText(
    text: String
) {
    Text(
        text = "Ошибка: $text",
        color = Color(0xFFFF6B6B),
        fontSize = 14.sp,
        lineHeight = 18.sp
    )
}
