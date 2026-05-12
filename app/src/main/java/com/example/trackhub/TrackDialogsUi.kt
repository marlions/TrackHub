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
fun TrackOptionsDotsButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
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

@Composable
fun TrackOptionsDialog(
    track: Track,
    onDismiss: () -> Unit,
    onInfoClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onDeleteTrackClick: () -> Unit
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
                        fontSize = 19.sp,
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
                text = "Информация о треке",
                danger = false,
                onClick = onInfoClick
            )

            Spacer(modifier = Modifier.height(10.dp))

            TrackMenuActionButton(
                text = "Комментарии",
                danger = false,
                onClick = onCommentsClick
            )

            Spacer(modifier = Modifier.height(10.dp))

            TrackMenuActionButton(
                text = "Добавить в плейлист",
                danger = false,
                onClick = onAddToPlaylistClick
            )

            Spacer(modifier = Modifier.height(10.dp))

            TrackMenuActionButton(
                text = "Удалить трек",
                danger = true,
                onClick = onDeleteTrackClick
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
fun TrackMenuActionButton(
    text: String,
    danger: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (danger) {
                    Color(0xFF2A0808).copy(alpha = 0.90f)
                } else {
                    Color.Black.copy(alpha = 0.55f)
                }
            )
            .border(
                width = 1.dp,
                color = if (danger) Color(0x66FF6B6B) else TrackHubBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (danger) Color(0xFFFF6B6B) else TrackHubText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TrackInfoDialog(
    track: Track,
    onDismiss: () -> Unit
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
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackCoverPlaceholder(
                    track = track,
                    modifier = Modifier.size(60.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Информация о треке",
                        color = TrackHubText,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = track.title,
                        color = TrackHubGoldLight,
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            TrackInfoRow(label = "Название", value = track.title)
            TrackInfoRow(label = "Автор", value = track.author)
            TrackInfoRow(label = "Дата загрузки", value = formatTrackDate(track.createdAt))
            TrackInfoRow(label = "Длительность", value = formatDurationSeconds(track.durationSeconds))
            TrackInfoRow(label = "Размер файла", value = formatFileSize(track.fileSizeBytes))
            TrackInfoRow(label = "Прослушиваний", value = track.playCount.toString())

            Spacer(modifier = Modifier.height(14.dp))

            TrackMenuActionButton(
                text = "Закрыть",
                danger = false,
                onClick = onDismiss
            )
        }
    }
}

@Composable
fun DeleteTrackConfirmDialog(
    track: Track,
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
                text = "Удалить трек?",
                color = TrackHubText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Трек «${track.title}» будет удалён из приложения, а аудиофайл будет удалён с сервера.",
                color = TrackHubMutedText,
                fontSize = 14.sp,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            TrackMenuActionButton(
                text = "Удалить",
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
fun TrackInfoRow(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = TrackHubMutedText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = value,
            color = TrackHubText,
            fontSize = 15.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(7.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TrackHubBorder.copy(alpha = 0.55f))
        )
    }
}

fun formatTrackDate(createdAt: String): String {
    if (createdAt.isBlank()) {
        return "Неизвестно"
    }

    return createdAt
        .take(16)
        .replace("T", " ")
}

fun formatDurationSeconds(seconds: Int): String {
    if (seconds <= 0) {
        return "Неизвестно"
    }

    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%d:%02d".format(minutes, secs)
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) {
        return "Неизвестно"
    }

    val kb = bytes / 1024.0
    val mb = kb / 1024.0

    return if (mb >= 1.0) {
        "%.1f MB".format(mb)
    } else {
        "%.0f KB".format(kb)
    }
}
