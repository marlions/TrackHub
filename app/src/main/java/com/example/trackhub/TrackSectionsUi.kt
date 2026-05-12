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
fun LikedTracksSection(
    tracks: List<Track>,
    onPlayClick: (Track) -> Unit,
    onLikeClick: (Track) -> Unit,
    onCommentsClick: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(TrackHubSurface.copy(alpha = 0.78f))
            .border(1.dp, TrackHubBorder, RoundedCornerShape(22.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeartStackIcon()

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Ваши лайки",
                color = TrackHubText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            ShuffleCircleIcon()
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (tracks.isEmpty()) {
            EmptySectionText("Вы пока не лайкнули ни одного трека")
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tracks.take(8).chunked(2).forEach { rowTracks ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowTracks.forEach { track ->
                            HomeSmallTrackCard(
                                track = track,
                                modifier = Modifier.weight(1f),
                                onPlayClick = { onPlayClick(track) },
                                onLikeClick = { onLikeClick(track) },
                                onCommentsClick = { onCommentsClick(track) },
                                onAddToPlaylistClick = { onAddToPlaylistClick(track) }
                            )
                        }

                        if (rowTracks.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentlyAddedSection(
    tracks: List<Track>,
    onShowAllClick: () -> Unit,
    onPlayClick: (Track) -> Unit,
    onLikeClick: (Track) -> Unit,
    onCommentsClick: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit,
    onMoreClick: (Track) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(TrackHubSurface.copy(alpha = 0.78f))
            .border(1.dp, TrackHubBorder, RoundedCornerShape(22.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClockGoldIcon()

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Недавно добавленные",
                color = TrackHubText,
                fontSize = 22.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "Показать все ›",
                color = TrackHubGoldLight,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onShowAllClick)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (tracks.isEmpty()) {
            EmptySectionText("Пока нет загруженных треков")
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tracks.take(4).forEach { track ->
                    RecentlyAddedWideTrackCard(
                        track = track,
                        onPlayClick = { onPlayClick(track) },
                        onMoreClick = { onMoreClick(track) }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeSmallTrackCard(
    track: Track,
    modifier: Modifier = Modifier,
    onPlayClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF141414))
            .border(1.dp, Color(0x22FFC84D), RoundedCornerShape(10.dp))
            .clickable(onClick = onPlayClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackCoverPlaceholder(
            track = track,
            modifier = Modifier.size(54.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.title,
                color = TrackHubText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = track.author,
                color = TrackHubMutedText,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun RecentlyAddedWideTrackCard(
    track: Track,
    onPlayClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF111111).copy(alpha = 0.88f))
            .border(1.dp, Color(0x33FFC84D), RoundedCornerShape(12.dp))
            .clickable(onClick = onPlayClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackCoverPlaceholder(
            track = track,
            modifier = Modifier.size(50.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.title,
                color = TrackHubText,
                fontSize = 16.sp,
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
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onMoreClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⋮",
                color = TrackHubMutedText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TrackCoverPlaceholder(
    track: Track,
    modifier: Modifier = Modifier
) {
    val colors = remember(track.id) {
        when (track.id % 5) {
            0 -> listOf(Color(0xFF3A0A0A), Color(0xFFD99B00))
            1 -> listOf(Color(0xFF111111), Color(0xFF6A1111))
            2 -> listOf(Color(0xFF1B1B1B), Color(0xFF444444))
            3 -> listOf(Color(0xFF101020), Color(0xFFB7860B))
            else -> listOf(Color(0xFF080808), Color(0xFF322000))
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(colors)
            )
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "♪",
            color = TrackHubGoldLight,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
