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
fun HomeTabScreen(
    tracks: List<Track>,
    likedTracks: List<Track>,
    isLoading: Boolean,
    errorText: String?,
    onRetry: () -> Unit,
    onShowAllClick: () -> Unit,
    onLogout: () -> Unit,
    onPlayClick: (Track) -> Unit,
    onLikeClick: (Track) -> Unit,
    onCommentsClick: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit,
    onMoreClick: (Track) -> Unit
) {
    val likedPreviewTracks = remember(likedTracks, tracks) {
        if (likedTracks.isNotEmpty()) {
            likedTracks.take(8)
        } else {
            tracks.filter { it.isLiked }.take(8)
        }
    }

    val recentTracks = remember(tracks) {
        tracks.take(8)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(top = 42.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Главная",
                    color = TrackHubText,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )

                ProfileCircleButton(
                    onClick = onLogout
                )
            }
        }

        if (isLoading) {
            item {
                CircularProgressIndicator(
                    color = TrackHubGoldLight
                )
            }
        }

        errorText?.let {
            item {
                BackendErrorCard(
                    text = it,
                    onRetry = onRetry
                )
            }
        }

        item {
            LikedTracksSection(
                tracks = likedPreviewTracks,
                onPlayClick = onPlayClick,
                onLikeClick = onLikeClick,
                onCommentsClick = onCommentsClick,
                onAddToPlaylistClick = onAddToPlaylistClick
            )
        }

        item {
            RecentlyAddedSection(
                tracks = recentTracks,
                onShowAllClick = onShowAllClick,
                onPlayClick = onPlayClick,
                onLikeClick = onLikeClick,
                onCommentsClick = onCommentsClick,
                onAddToPlaylistClick = onAddToPlaylistClick,
                onMoreClick = onMoreClick
            )
        }

        item {
            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}

@Composable
fun ProfileScreen(
    accessToken: String,
    tracksCount: Int,
    likedTracksCount: Int,
    onFindUsersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var playlistsCount by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun loadProfileInfo() {
        scope.launch {
            isLoading = true
            errorText = null

            try {
                userProfile = fetchCurrentUser(accessToken)
                playlistsCount = fetchPlaylists(accessToken).size
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки профиля"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadProfileInfo()
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
                    text = "Профиль",
                    color = TrackHubText,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(TrackHubSurface.copy(alpha = 0.84f))
                        .border(1.dp, TrackHubBorder, RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TrackHubPngIcon(
                        drawableId = R.drawable.profile_icon,
                        size = 78.dp,
                        color = null,
                        contentDescription = "Профиль"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = userProfile?.username ?: "TrackHub",
                        color = TrackHubText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = userProfile?.email ?: "Активный пользователь",
                        color = TrackHubMutedText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )

                    if (isLoading) {
                        Spacer(modifier = Modifier.height(16.dp))

                        CircularProgressIndicator(
                            color = TrackHubGoldLight,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    errorText?.let {
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Не удалось загрузить данные профиля",
                            color = Color(0xFFFF6B6B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

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

                    Spacer(modifier = Modifier.height(18.dp))

                    ProfileStatsGrid(
                        tracksCount = tracksCount,
                        likedTracksCount = likedTracksCount,
                        playlistsCount = playlistsCount
                    )
                }
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
                        text = "Пользователи",
                        color = TrackHubText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileActionRow(
                        title = "Найти пользователей",
                        subtitle = "Поиск пользователей по имени",
                        iconText = "+",
                        onClick = onFindUsersClick
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ProfileActionRow(
                        title = "Мои подписки",
                        subtitle = "Пользователи, на которых вы подписаны",
                        iconText = "playlists",
                        onClick = onFollowingClick
                    )
                }
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
                        text = "Аккаунт",
                        color = TrackHubText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF2A0808).copy(alpha = 0.86f))
                            .border(1.dp, Color(0x66FF6B6B), RoundedCornerShape(18.dp))
                            .clickable(onClick = onLogout),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Выйти из аккаунта",
                            color = Color(0xFFFF6B6B),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}


@Composable
fun ProfileActionRow(
    title: String,
    subtitle: String,
    iconText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.48f))
            .border(1.dp, TrackHubBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LibraryCardIcon(
            iconText = iconText,
            boxSize = 42.dp,
            iconSize = 21.dp,
            cornerRadius = 12.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = TrackHubText,
                fontSize = 17.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = subtitle,
                color = TrackHubMutedText,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }

        LibraryChevronIcon()
    }
}

@Composable
fun UserSearchScreen(
    accessToken: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<UserPublic>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun loadUsers(searchText: String = query) {
        scope.launch {
            isLoading = true
            errorText = null

            try {
                users = searchUsers(
                    accessToken = accessToken,
                    query = searchText
                )
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка поиска пользователей"
            } finally {
                isLoading = false
            }
        }
    }

    fun toggleFollow(user: UserPublic) {
        scope.launch {
            errorText = null

            try {
                val status = if (user.isFollowing) {
                    unfollowUser(user.id, accessToken)
                } else {
                    followUser(user.id, accessToken)
                }

                users = users.map { item ->
                    if (item.id == user.id) {
                        item.copy(
                            isFollowing = status.isFollowing,
                            followersCount = status.followersCount
                        )
                    } else {
                        item
                    }
                }
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка изменения подписки"
            }
        }
    }

    LaunchedEffect(query) {
        delay(350)
        isLoading = true
        errorText = null

        try {
            users = searchUsers(
                accessToken = accessToken,
                query = query
            )
        } catch (e: Exception) {
            errorText = e.message ?: "Ошибка поиска пользователей"
        } finally {
            isLoading = false
        }
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
                    text = "Найти пользователей",
                    color = TrackHubText,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            item {
                UserSearchField(
                    value = query,
                    onValueChange = {
                        query = it
                    }
                )
            }

            errorText?.let {
                item {
                    BackendErrorCard(
                        text = it,
                        onRetry = {
                            loadUsers(query)
                        }
                    )
                }
            }

            if (isLoading && users.isEmpty()) {
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

            if (users.isEmpty() && !isLoading && errorText == null) {
                item {
                    EmptyUsersCard(
                        title = "Пользователи не найдены",
                        subtitle = if (query.isBlank()) {
                            "Пока нет других пользователей для подписки."
                        } else {
                            "Попробуйте изменить поисковый запрос."
                        }
                    )
                }
            }

            items(items = users, key = { it.id }) { user ->
                UserPublicCard(
                    user = user,
                    onFollowClick = {
                        toggleFollow(user)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun FollowingScreen(
    accessToken: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var following by remember { mutableStateOf<List<FollowUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun loadFollowing() {
        scope.launch {
            isLoading = true
            errorText = null

            try {
                following = fetchMyFollowing(accessToken)
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки подписок"
            } finally {
                isLoading = false
            }
        }
    }

    fun unfollowAndReload(user: FollowUser) {
        scope.launch {
            errorText = null

            try {
                unfollowUser(user.id, accessToken)
                following = following.filterNot { it.id == user.id }
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка отписки"
            }
        }
    }

    LaunchedEffect(Unit) {
        loadFollowing()
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
                    text = "Мои подписки",
                    color = TrackHubText,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            item {
                Text(
                    text = "Пользователи, на которых вы подписаны",
                    color = TrackHubMutedText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            errorText?.let {
                item {
                    BackendErrorCard(
                        text = it,
                        onRetry = {
                            loadFollowing()
                        }
                    )
                }
            }

            if (isLoading && following.isEmpty()) {
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

            if (following.isEmpty() && !isLoading && errorText == null) {
                item {
                    EmptyUsersCard(
                        title = "Подписок пока нет",
                        subtitle = "Откройте поиск пользователей и подпишитесь на интересных авторов."
                    )
                }
            }

            items(items = following, key = { it.id }) { user ->
                FollowingUserCard(
                    user = user,
                    onUnfollowClick = {
                        unfollowAndReload(user)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun UserSearchField(
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
        MiniUserSearchIcon()

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
                            text = "Поиск по имени",
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
fun UserPublicCard(
    user: UserPublic,
    onFollowClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(TrackHubSurface.copy(alpha = 0.84f))
            .border(1.dp, TrackHubBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatarIcon(
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = user.username,
                color = TrackHubText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Подписчики: ${user.followersCount}",
                color = TrackHubMutedText,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = "Подписки: ${user.followingCount}",
                color = TrackHubMutedText,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        FollowButton(
            isFollowing = user.isFollowing,
            onClick = onFollowClick
        )
    }
}

@Composable
fun FollowingUserCard(
    user: FollowUser,
    onUnfollowClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(TrackHubSurface.copy(alpha = 0.84f))
            .border(1.dp, TrackHubBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatarIcon(
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = user.username,
                color = TrackHubText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (user.followedAt.isBlank()) {
                    "Вы подписаны на пользователя"
                } else {
                    "Подписка с ${formatTrackDate(user.followedAt)}"
                },
                color = TrackHubMutedText,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .height(38.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF2A0808).copy(alpha = 0.90f))
                .border(1.dp, Color(0x66FF6B6B), RoundedCornerShape(50))
                .clickable(onClick = onUnfollowClick)
                .padding(horizontal = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Отписаться",
                color = Color(0xFFFF6B6B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun FollowButton(
    isFollowing: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (isFollowing) {
                    Color.Black.copy(alpha = 0.55f)
                } else {
                    TrackHubGold.copy(alpha = 0.95f)
                }
            )
            .border(
                width = 1.dp,
                color = if (isFollowing) TrackHubBorder else TrackHubGoldLight,
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isFollowing) "Отписаться" else "Подписаться",
            color = if (isFollowing) TrackHubText else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun EmptyUsersCard(
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
        UserAvatarIcon(
            size = 54.dp
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
fun UserAvatarIcon(
    size: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(TrackHubGold.copy(alpha = 0.12f))
            .border(1.dp, TrackHubGoldLight, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        MiniUserSearchIcon(
            modifier = Modifier.size(size * 0.46f)
        )
    }
}

@Composable
fun MiniUserSearchIcon(
    modifier: Modifier = Modifier.size(24.dp)
) {
    Canvas(modifier = modifier) {
        val stroke = size.width * 0.09f
        val w = size.width
        val h = size.height

        drawCircle(
            color = TrackHubGoldLight,
            radius = w * 0.16f,
            center = Offset(w * 0.42f, h * 0.30f),
            style = Stroke(width = stroke)
        )

        val bodyPath = Path().apply {
            moveTo(w * 0.16f, h * 0.76f)
            cubicTo(
                w * 0.16f,
                h * 0.57f,
                w * 0.30f,
                h * 0.50f,
                w * 0.42f,
                h * 0.50f
            )
            cubicTo(
                w * 0.54f,
                h * 0.50f,
                w * 0.68f,
                h * 0.57f,
                w * 0.68f,
                h * 0.76f
            )
        }

        drawPath(
            path = bodyPath,
            color = TrackHubGoldLight,
            style = Stroke(
                width = stroke,
                cap = StrokeCap.Round
            )
        )

        drawCircle(
            color = TrackHubGoldLight,
            radius = w * 0.14f,
            center = Offset(w * 0.76f, h * 0.72f),
            style = Stroke(width = stroke * 0.80f)
        )

        drawLine(
            color = TrackHubGoldLight,
            start = Offset(w * 0.86f, h * 0.82f),
            end = Offset(w * 0.98f, h * 0.94f),
            strokeWidth = stroke * 0.80f,
            cap = StrokeCap.Round
        )
    }
}


@Composable
fun ProfileStatsGrid(
    tracksCount: Int,
    likedTracksCount: Int,
    playlistsCount: Int?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProfileStatCard(
                title = "Треки",
                value = tracksCount.toString(),
                modifier = Modifier.weight(1f)
            )

            ProfileStatCard(
                title = "Лайки",
                value = likedTracksCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        ProfileStatCard(
            title = "Плейлисты",
            value = playlistsCount?.toString() ?: "—",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ProfileStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.48f))
            .border(1.dp, TrackHubBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = TrackHubGoldLight,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = title,
            color = TrackHubMutedText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun BackendErrorCard(
    text: String,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF2A0808).copy(alpha = 0.86f))
            .border(1.dp, Color(0x66FF6B6B), RoundedCornerShape(20.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Сервер недоступен",
            color = Color(0xFFFF6B6B),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Проверьте, что backend запущен, база данных работает, а адрес API указан правильно.",
            color = TrackHubText.copy(alpha = 0.88f),
            fontSize = 14.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = text,
            color = TrackHubMutedText,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center
        )

        if (onRetry != null) {
            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(TrackHubGold.copy(alpha = 0.95f))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Повторить",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
