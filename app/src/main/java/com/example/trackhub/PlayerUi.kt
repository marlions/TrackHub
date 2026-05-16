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
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun TrackHubFullPlayerScreen(
    track: Track,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    volume: Float,
    isLiked: Boolean,
    onBack: () -> Unit,
    onLikeClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    val safeDurationMs = durationMs.coerceAtLeast(1L)

    var isSeeking by remember(track.id) { mutableStateOf(false) }
    var sliderPosition by remember(track.id) { mutableStateOf(0f) }
    var showVolumeSlider by remember(track.id) { mutableStateOf(false) }
    var showTrackInfo by remember(track.id) { mutableStateOf(false) }
    var dragOffsetY by remember(track.id) { mutableStateOf(0f) }
    var horizontalDragTotal by remember(track.id) { mutableStateOf(0f) }
    var verticalDragTotal by remember(track.id) { mutableStateOf(0f) }

    BackHandler {
        onBack()
    }

    LaunchedEffect(track.id) {
        showVolumeSlider = false
        dragOffsetY = 0f
        horizontalDragTotal = 0f
        verticalDragTotal = 0f
    }

    LaunchedEffect(currentPositionMs, durationMs, isSeeking) {
        if (!isSeeking) {
            sliderPosition = currentPositionMs
                .coerceIn(0L, safeDurationMs)
                .toFloat()
        }
    }

    LaunchedEffect(showVolumeSlider, volume) {
        if (showVolumeSlider) {
            delay(3000)
            showVolumeSlider = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { translationY = dragOffsetY }
            .pointerInput(track.id) {
                detectDragGestures(
                    onDragStart = {
                        horizontalDragTotal = 0f
                        verticalDragTotal = 0f
                    },
                    onDrag = { _, dragAmount ->
                        horizontalDragTotal += dragAmount.x
                        verticalDragTotal += dragAmount.y
                        if (verticalDragTotal > 0f && kotlin.math.abs(verticalDragTotal) > kotlin.math.abs(horizontalDragTotal)) {
                            dragOffsetY = verticalDragTotal.coerceAtLeast(0f)
                        }
                    },
                    onDragEnd = {
                        when {
                            verticalDragTotal > 130f && kotlin.math.abs(verticalDragTotal) > kotlin.math.abs(horizontalDragTotal) -> onBack()
                            horizontalDragTotal < -140f && kotlin.math.abs(horizontalDragTotal) > kotlin.math.abs(verticalDragTotal) -> onNextClick()
                            horizontalDragTotal > 140f && kotlin.math.abs(horizontalDragTotal) > kotlin.math.abs(verticalDragTotal) -> onPreviousClick()
                        }
                        dragOffsetY = 0f
                        horizontalDragTotal = 0f
                        verticalDragTotal = 0f
                    },
                    onDragCancel = {
                        dragOffsetY = 0f
                        horizontalDragTotal = 0f
                        verticalDragTotal = 0f
                    }
                )
            }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF270500),
                        Color(0xFF130101),
                        Color.Black
                    )
                )
            )
    ) {
        GoldBackgroundDecorations()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 42.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    FullPlayerDownIcon()
                }

                Text(
                    text = "СЕЙЧАС ИГРАЕТ",
                    color = TrackHubText.copy(alpha = 0.90f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable {
                            showTrackInfo = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⋮",
                        color = TrackHubText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(44.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, TrackHubBorder, RoundedCornerShape(22.dp))
                    .shadow(
                        elevation = 22.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = TrackHubGold.copy(alpha = 0.12f),
                        spotColor = TrackHubGold.copy(alpha = 0.16f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                TrackCoverPlaceholder(
                    track = track,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = track.title,
                        color = TrackHubText,
                        fontSize = 28.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = track.author,
                        color = TrackHubMutedText,
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                FullPlayerHeartButton(
                    isLiked = isLiked,
                    onClick = onLikeClick
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            TrackHubCompactSlider(
                value = sliderPosition.coerceIn(0f, safeDurationMs.toFloat()),
                onValueChange = { value ->
                    isSeeking = true
                    sliderPosition = value.coerceIn(0f, safeDurationMs.toFloat())
                },
                onValueChangeFinished = {
                    onSeekTo(sliderPosition.toLong())
                    isSeeking = false
                },
                valueRange = 0f..safeDurationMs.toFloat(),
                enabled = durationMs > 0L,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
                trackHeight = 4.dp,
                thumbRadius = 6.dp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTrackTime(sliderPosition.toLong()),
                    color = TrackHubMutedText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = if (durationMs > 0L) formatTrackTime(durationMs) else "--:--",
                    color = TrackHubMutedText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FullPlayerSmallControlButton(
                    onClick = onPreviousClick
                ) {
                    PreviousTrackIcon()
                }

                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onPlayPauseClick),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        FullPlayerPauseIcon()
                    } else {
                        FullPlayerPlayIcon()
                    }
                }

                FullPlayerSmallControlButton(
                    onClick = onNextClick
                ) {
                    NextTrackIcon()
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.22f))
                        .border(1.dp, TrackHubGold.copy(alpha = 0.70f), CircleShape)
                        .clickable {
                            showVolumeSlider = !showVolumeSlider
                        },
                    contentAlignment = Alignment.Center
                ) {
                    MiniVolumeIcon(
                        isMuted = volume <= 0.01f,
                        color = TrackHubText.copy(alpha = 0.92f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                AnimatedVisibility(visible = showVolumeSlider) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(14.dp))

                        TrackHubCompactSlider(
                            value = volume.coerceIn(0f, 1f),
                            onValueChange = { newVolume ->
                                onVolumeChange(newVolume.coerceIn(0f, 1f))
                            },
                            valueRange = 0f..1f,
                            enabled = true,
                            modifier = Modifier
                                .width(230.dp)
                                .height(18.dp),
                            trackHeight = 4.dp,
                            thumbRadius = 6.dp
                        )
                    }
                }
            }
        }

        if (showTrackInfo) {
            TrackInfoDialog(
                track = track,
                onDismiss = {
                    showTrackInfo = false
                }
            )
        }
    }
}

@Composable
fun FullPlayerHeartButton(
    isLiked: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.24f))
            .border(
                width = 1.dp,
                color = if (isLiked) TrackHubGoldLight else Color.White.copy(alpha = 0.72f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        FullPlayerHeartIcon(
            isLiked = isLiked,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
fun FullPlayerHeartIcon(
    isLiked: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val heartPath = Path().apply {
            moveTo(w * 0.50f, h * 0.88f)
            cubicTo(w * 0.20f, h * 0.66f, w * 0.05f, h * 0.48f, w * 0.10f, h * 0.28f)
            cubicTo(w * 0.15f, h * 0.08f, w * 0.38f, h * 0.05f, w * 0.50f, h * 0.24f)
            cubicTo(w * 0.62f, h * 0.05f, w * 0.85f, h * 0.08f, w * 0.90f, h * 0.28f)
            cubicTo(w * 0.95f, h * 0.48f, w * 0.80f, h * 0.66f, w * 0.50f, h * 0.88f)
            close()
        }

        if (isLiked) {
            drawPath(
                path = heartPath,
                color = TrackHubGoldLight
            )
        } else {
            drawPath(
                path = heartPath,
                color = Color.White,
                style = Stroke(
                    width = w * 0.095f,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

@Composable
fun FullPlayerSmallControlButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun FullPlayerDownIcon() {
    Canvas(modifier = Modifier.size(30.dp)) {
        val stroke = 4.0f
        val color = Color.White

        drawLine(
            color = color,
            start = Offset(size.width * 0.18f, size.height * 0.36f),
            end = Offset(size.width * 0.50f, size.height * 0.68f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        drawLine(
            color = color,
            start = Offset(size.width * 0.82f, size.height * 0.36f),
            end = Offset(size.width * 0.50f, size.height * 0.68f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun PreviousTrackIcon() {
    Canvas(modifier = Modifier.size(36.dp)) {
        val color = Color.White
        val lineWidth = size.width * 0.09f

        drawLine(
            color = color,
            start = Offset(size.width * 0.20f, size.height * 0.20f),
            end = Offset(size.width * 0.20f, size.height * 0.80f),
            strokeWidth = lineWidth,
            cap = StrokeCap.Round
        )

        val path = Path().apply {
            moveTo(size.width * 0.78f, size.height * 0.18f)
            lineTo(size.width * 0.28f, size.height * 0.50f)
            lineTo(size.width * 0.78f, size.height * 0.82f)
            close()
        }

        drawPath(path = path, color = color)
    }
}

@Composable
fun NextTrackIcon() {
    Canvas(modifier = Modifier.size(36.dp)) {
        val color = Color.White
        val lineWidth = size.width * 0.09f

        drawLine(
            color = color,
            start = Offset(size.width * 0.80f, size.height * 0.20f),
            end = Offset(size.width * 0.80f, size.height * 0.80f),
            strokeWidth = lineWidth,
            cap = StrokeCap.Round
        )

        val path = Path().apply {
            moveTo(size.width * 0.22f, size.height * 0.18f)
            lineTo(size.width * 0.72f, size.height * 0.50f)
            lineTo(size.width * 0.22f, size.height * 0.82f)
            close()
        }

        drawPath(path = path, color = color)
    }
}

@Composable
fun FullPlayerPlayIcon() {
    Canvas(
        modifier = Modifier
            .size(34.dp)
            .offset(x = 2.dp)
    ) {
        val path = Path().apply {
            moveTo(size.width * 0.22f, size.height * 0.12f)
            lineTo(size.width * 0.22f, size.height * 0.88f)
            lineTo(size.width * 0.86f, size.height * 0.50f)
            close()
        }

        drawPath(path = path, color = Color.Black)
    }
}

@Composable
fun FullPlayerPauseIcon() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black)
        )

        Box(
            modifier = Modifier
                .width(8.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black)
        )
    }
}

@Composable
fun TrackHubMiniPlayer(
    track: Track,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    volume: Float,
    onSeekTo: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenPlayerClick: () -> Unit,
    onPlayPauseClick: () -> Unit
) {
    val safeDurationMs = durationMs.coerceAtLeast(1L)

    var isSeeking by remember(track.id) { mutableStateOf(false) }
    var sliderPosition by remember(track.id) { mutableStateOf(0f) }
    var showVolumeSlider by remember { mutableStateOf(false) }

    LaunchedEffect(track.id) {
        showVolumeSlider = false
        dragOffsetY = 0f
        horizontalDragTotal = 0f
        verticalDragTotal = 0f
    }

    LaunchedEffect(currentPositionMs, durationMs, isSeeking) {
        if (!isSeeking) {
            sliderPosition = currentPositionMs
                .coerceIn(0L, safeDurationMs)
                .toFloat()
        }
    }

    LaunchedEffect(showVolumeSlider, volume) {
        if (showVolumeSlider) {
            delay(3000)
            showVolumeSlider = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF210200),
                        Color(0xFF3A0700),
                        Color(0xFF120000)
                    )
                )
            )
            .border(1.dp, TrackHubBorder, RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenPlayerClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackCoverPlaceholder(
                    track = track,
                    modifier = Modifier.size(54.dp)
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

                    Text(
                        text = track.author,
                        color = TrackHubGoldLight,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            AnimatedVisibility(
                visible = showVolumeSlider,
                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrackHubCompactSlider(
                        value = volume.coerceIn(0f, 1f),
                        onValueChange = { newVolume ->
                            onVolumeChange(newVolume.coerceIn(0f, 1f))
                        },
                        valueRange = 0f..1f,
                        enabled = true,
                        modifier = Modifier
                            .width(92.dp)
                            .height(22.dp),
                        trackHeight = 3.dp,
                        thumbRadius = 5.dp
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.26f))
                    .border(1.dp, TrackHubGold.copy(alpha = 0.62f), CircleShape)
                    .clickable {
                        showVolumeSlider = !showVolumeSlider
                    },
                contentAlignment = Alignment.Center
            ) {
                MiniVolumeIcon(
                    isMuted = volume <= 0.01f,
                    color = TrackHubGoldLight,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, TrackHubGoldLight, CircleShape)
                    .clickable(onClick = onPlayPauseClick),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    PauseGoldIcon()
                } else {
                    PlayGoldIcon()
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(42.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatTrackTime(sliderPosition.toLong()),
                    color = TrackHubMutedText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            TrackHubCompactSlider(
                value = sliderPosition.coerceIn(0f, safeDurationMs.toFloat()),
                onValueChange = { value ->
                    isSeeking = true
                    sliderPosition = value.coerceIn(0f, safeDurationMs.toFloat())
                },
                onValueChangeFinished = {
                    onSeekTo(sliderPosition.toLong())
                    isSeeking = false
                },
                valueRange = 0f..safeDurationMs.toFloat(),
                enabled = durationMs > 0L,
                modifier = Modifier
                    .weight(1f)
                    .height(14.dp),
                trackHeight = 4.dp,
                thumbRadius = 5.dp
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier.width(42.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (durationMs > 0L) formatTrackTime(durationMs) else "--:--",
                    color = TrackHubMutedText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun TrackHubCompactSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 4.dp,
    thumbRadius: Dp = 5.dp,
    onValueChangeFinished: (() -> Unit)? = null
) {
    var widthPx by remember { mutableStateOf(1) }

    val minValue = valueRange.start
    val maxValue = valueRange.endInclusive
    val safeMax = if (maxValue > minValue) maxValue else minValue + 1f
    val safeValue = value.coerceIn(minValue, safeMax)

    fun valueFromOffset(x: Float): Float {
        val fraction = (x / widthPx.toFloat()).coerceIn(0f, 1f)
        return minValue + (safeMax - minValue) * fraction
    }

    Box(
        modifier = modifier
            .onSizeChanged { size ->
                widthPx = size.width.coerceAtLeast(1)
            }
            .pointerInput(enabled, minValue, safeMax, widthPx) {
                if (!enabled) {
                    return@pointerInput
                }

                detectTapGestures { offset ->
                    onValueChange(valueFromOffset(offset.x))
                    onValueChangeFinished?.invoke()
                }
            }
            .pointerInput(enabled, minValue, safeMax, widthPx) {
                if (!enabled) {
                    return@pointerInput
                }

                detectDragGestures(
                    onDragStart = { offset ->
                        onValueChange(valueFromOffset(offset.x))
                    },
                    onDrag = { change, _ ->
                        onValueChange(valueFromOffset(change.position.x))
                    },
                    onDragEnd = {
                        onValueChangeFinished?.invoke()
                    },
                    onDragCancel = {
                        onValueChangeFinished?.invoke()
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackHeightPx = trackHeight.toPx()
            val thumbRadiusPx = thumbRadius.toPx()
            val centerY = size.height / 2f
            val progressFraction = ((safeValue - minValue) / (safeMax - minValue)).coerceIn(0f, 1f)
            val progressWidth = size.width * progressFraction
            val inactiveColor = if (enabled) {
                Color.White.copy(alpha = 0.18f)
            } else {
                Color.White.copy(alpha = 0.10f)
            }
            val activeColor = if (enabled) {
                TrackHubGoldLight
            } else {
                TrackHubMutedText.copy(alpha = 0.50f)
            }

            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(0f, centerY - trackHeightPx / 2f),
                size = Size(size.width, trackHeightPx),
                cornerRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
            )

            drawRoundRect(
                color = activeColor,
                topLeft = Offset(0f, centerY - trackHeightPx / 2f),
                size = Size(progressWidth, trackHeightPx),
                cornerRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
            )

            if (enabled) {
                drawCircle(
                    color = TrackHubGoldLight,
                    radius = thumbRadiusPx,
                    center = Offset(progressWidth.coerceIn(thumbRadiusPx, size.width - thumbRadiusPx), centerY)
                )
            }
        }
    }
}

@Composable
fun MiniVolumeIcon(
    isMuted: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.105f

        val speakerPath = Path().apply {
            moveTo(w * 0.10f, h * 0.38f)
            lineTo(w * 0.28f, h * 0.38f)
            lineTo(w * 0.52f, h * 0.18f)
            lineTo(w * 0.52f, h * 0.82f)
            lineTo(w * 0.28f, h * 0.62f)
            lineTo(w * 0.10f, h * 0.62f)
            close()
        }

        drawPath(
            path = speakerPath,
            color = color
        )

        if (isMuted) {
            drawLine(
                color = color,
                start = Offset(w * 0.68f, h * 0.34f),
                end = Offset(w * 0.92f, h * 0.66f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(w * 0.92f, h * 0.34f),
                end = Offset(w * 0.68f, h * 0.66f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        } else {
            drawArc(
                color = color,
                startAngle = -38f,
                sweepAngle = 76f,
                useCenter = false,
                topLeft = Offset(w * 0.48f, h * 0.30f),
                size = Size(w * 0.34f, h * 0.40f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            drawArc(
                color = color.copy(alpha = 0.75f),
                startAngle = -42f,
                sweepAngle = 84f,
                useCenter = false,
                topLeft = Offset(w * 0.54f, h * 0.18f),
                size = Size(w * 0.48f, h * 0.64f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}
