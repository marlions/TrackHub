package com.example.trackhub


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle

private const val BASE_URL = "http://10.0.2.2:8000"

private val TrackHubBackground = Color(0xFF030303)
private val TrackHubSurface = Color(0xE6080809)
private val TrackHubSurfaceSoft = Color(0xFF111112)
private val TrackHubGold = Color(0xFFD09400)
private val TrackHubGoldDark = Color(0xFF9C6A00)
private val TrackHubGoldLight = Color(0xFFFFC84D)
private val TrackHubText = Color(0xFFF6F6F6)
private val TrackHubMutedText = Color(0xFF8A878E)
private val TrackHubBorder = Color(0x4AA77D18)
private val TrackHubFieldBorder = Color(0xFF473A1A)

data class Track(
    val id: Int,
    val title: String,
    val author: String,
    val streamUrl: String,
    val likesCount: Int,
    val commentsCount: Int,
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

class MainActivity : ComponentActivity() {
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF010101),
                        Color(0xFF040404),
                        Color(0xFF010101)
                    )
                )
            )
    ) {
        GoldBackgroundDecorations()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp, bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthHeader()

            Spacer(modifier = Modifier.height(10.dp))

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

            Spacer(modifier = Modifier.height(10.dp))

            AuthFormCard(
                isRegisterMode = isRegisterMode,
                username = username,
                onUsernameChange = { username = it },
                email = email,
                onEmailChange = { email = it },
                password = password,
                onPasswordChange = { password = it },
                passwordVisible = passwordVisible,
                onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
                isLoading = isLoading,
                errorText = errorText,
                onSubmit = validateAndSubmit@{
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

                    if (trimmedPassword.length < 8) {
                        errorText = "Пароль должен содержать минимум 8 символов"
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
        }
    }
}

@Composable
fun GoldBackgroundDecorations() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(TrackHubGold.copy(alpha = 0.10f), Color.Transparent)
            ),
            radius = size.width * 0.50f,
            center = Offset(size.width * 0.18f, size.height * 0.18f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(TrackHubGold.copy(alpha = 0.08f), Color.Transparent)
            ),
            radius = size.width * 0.42f,
            center = Offset(size.width * 0.88f, size.height * 0.18f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(TrackHubGold.copy(alpha = 0.05f), Color.Transparent)
            ),
            radius = size.width * 0.65f,
            center = Offset(size.width * 0.50f, size.height * 0.96f)
        )

        val noteColor = TrackHubGoldLight.copy(alpha = 0.50f)
        drawMusicNote(Offset(size.width * 0.22f, size.height * 0.11f), 1.1f, noteColor)
        drawMusicNote(Offset(size.width * 0.14f, size.height * 0.19f), 0.85f, noteColor.copy(alpha = 0.7f))
        drawMusicNote(Offset(size.width * 0.80f, size.height * 0.16f), 0.72f, noteColor.copy(alpha = 0.48f))

        drawCircle(color = TrackHubGoldLight.copy(alpha = 0.55f), radius = 3f, center = Offset(size.width * 0.14f, size.height * 0.12f))
        drawCircle(color = TrackHubGoldLight.copy(alpha = 0.48f), radius = 2.8f, center = Offset(size.width * 0.83f, size.height * 0.14f))
        drawCircle(color = TrackHubGoldLight.copy(alpha = 0.32f), radius = 2.5f, center = Offset(size.width * 0.74f, size.height * 0.20f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMusicNote(
    top: Offset,
    scale: Float,
    color: Color
) {
    val stemHeight = 40f * scale
    val stemX = top.x + 10f * scale
    val stemTop = top.y
    val stroke = 4.5f * scale

    drawLine(
        color = color,
        start = Offset(stemX, stemTop),
        end = Offset(stemX, stemTop + stemHeight),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )

    drawLine(
        color = color,
        start = Offset(stemX, stemTop),
        end = Offset(stemX + 16f * scale, stemTop + 7f * scale),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )

    drawCircle(
        color = color,
        radius = 8f * scale,
        center = Offset(stemX - 5f * scale, stemTop + stemHeight + 3f * scale)
    )
}

@Composable
fun AuthHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(820f / 385f)
    ) {
        Image(
            painter = painterResource(id = R.drawable.auth_header),
            contentDescription = "TrackHub header",
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.06f))
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(82.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            TrackHubBackground,
                            TrackHubBackground.copy(alpha = 0.90f),
                            TrackHubBackground.copy(alpha = 0.56f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(82.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            TrackHubBackground.copy(alpha = 0.56f),
                            TrackHubBackground.copy(alpha = 0.90f),
                            TrackHubBackground
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(42.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            TrackHubBackground.copy(alpha = 0.86f),
                            TrackHubBackground.copy(alpha = 0.40f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(74.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            TrackHubBackground.copy(alpha = 0.48f),
                            TrackHubBackground.copy(alpha = 0.86f),
                            TrackHubBackground
                        )
                    )
                )
        )
    }
}

@Composable
fun TrackHubRecordLogo() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        for (i in 0..5) {
            drawArc(
                color = TrackHubGoldLight.copy(alpha = 0.98f - i * 0.08f),
                startAngle = 198f,
                sweepAngle = 288f,
                useCenter = false,
                topLeft = Offset(centerX - 22f - i * 8f, centerY - 22f - i * 8f),
                size = Size(44f + i * 16f, 44f + i * 16f),
                style = Stroke(width = 4.8f, cap = StrokeCap.Round)
            )
        }

        drawCircle(color = TrackHubGoldLight, radius = 9.5f, center = Offset(centerX, centerY))
        drawCircle(color = Color.Black.copy(alpha = 0.88f), radius = 3.2f, center = Offset(centerX, centerY))

        val bars = listOf(12f, 22f, 34f, 48f, 34f, 22f, 12f)
        bars.forEachIndexed { index, barHeight ->
            val step = 13f
            val leftX = centerX - 66f - index * step
            val rightX = centerX + 66f + index * step
            val barColor = TrackHubGoldLight.copy(alpha = 0.90f)

            drawLine(
                color = barColor,
                start = Offset(leftX, centerY - barHeight / 2f),
                end = Offset(leftX, centerY + barHeight / 2f),
                strokeWidth = 4.2f,
                cap = StrokeCap.Round
            )

            drawLine(
                color = barColor,
                start = Offset(rightX, centerY - barHeight / 2f),
                end = Offset(rightX, centerY + barHeight / 2f),
                strokeWidth = 4.2f,
                cap = StrokeCap.Round
            )
        }
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
            .height(52.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(TrackHubSurfaceSoft.copy(alpha = 0.86f))
            .border(1.dp, TrackHubBorder, RoundedCornerShape(28.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        AuthTabButton(
            text = "Вход",
            icon = { MiniLoginIcon(isSelected = !isRegisterMode) },
            isSelected = !isRegisterMode,
            onClick = onLoginClick,
            modifier = Modifier.weight(1f)
        )

        AuthTabButton(
            text = "Регистрация",
            icon = { MiniRegisterIcon(isSelected = isRegisterMode) },
            isSelected = isRegisterMode,
            onClick = onRegisterClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun AuthTabButton(
    text: String,
    icon: @Composable () -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(26.dp))
            .background(
                if (isSelected) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFB67E00),
                            Color(0xFFF0BB32),
                            Color(0xFFB67E00)
                        )
                    )
                } else {
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    icon()
                }

                Spacer(modifier = Modifier.size(8.dp))

                Text(
                    text = text,
                    color = if (isSelected) Color.White else TrackHubText.copy(alpha = 0.72f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                        .size(width = 104.dp, height = 3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(TrackHubGoldLight.copy(alpha = 0.96f))
                )
            }
        }
    }
}

@Composable
fun AuthFormCard(
    isRegisterMode: Boolean,
    username: String,
    onUsernameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: () -> Unit,
    isLoading: Boolean,
    errorText: String?,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(30.dp),
                ambientColor = Color.Black,
                spotColor = TrackHubGold.copy(alpha = 0.14f)
            )
            .clip(RoundedCornerShape(30.dp))
            .background(TrackHubSurface)
            .border(1.dp, TrackHubBorder, RoundedCornerShape(30.dp))
            .padding(horizontal = 22.dp, vertical = 22.dp)
    ) {
        if (isRegisterMode) {
            TrackHubInputLabel("Имя пользователя")
            Spacer(modifier = Modifier.height(8.dp))
            TrackHubTextField(
                value = username,
                onValueChange = onUsernameChange,
                placeholder = "Введите имя пользователя",
                keyboardType = KeyboardType.Text,
                leadingContent = { MiniRegisterIcon(isSelected = true) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        TrackHubInputLabel("Email")
        Spacer(modifier = Modifier.height(8.dp))
        TrackHubTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = "Введите ваш email",
            keyboardType = KeyboardType.Email,
            leadingContent = { MiniEnvelopeIcon() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TrackHubInputLabel("Пароль")
        Spacer(modifier = Modifier.height(8.dp))
        TrackHubTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "Введите ваш пароль",
            keyboardType = KeyboardType.Password,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            leadingContent = { MiniLockIcon() },
            trailingContent = {
                if (passwordVisible) {
                    MiniEyeIcon()
                } else {
                    MiniEyeOffIcon()
                }
            },
            onTrailingClick = onPasswordVisibilityChange
        )

        if (!isRegisterMode) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Забыли пароль?",
                color = TrackHubGoldLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.End)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        GoldPrimaryButton(
            text = if (isRegisterMode) "Зарегистрироваться" else "Войти",
            enabled = !isLoading,
            onClick = onSubmit
        )

        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(
                color = TrackHubGoldLight,
                modifier = Modifier.align(Alignment.CenterHorizontally)
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

@Composable
fun TrackHubInputLabel(text: String) {
    Text(
        text = text,
        color = TrackHubText,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun TrackHubTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    leadingContent: @Composable () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
    onTrailingClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        singleLine = true,
        placeholder = {
            Text(
                text = placeholder,
                color = TrackHubMutedText,
                fontSize = 15.sp
            )
        },
        leadingIcon = {
            Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                leadingContent()
            }
        },
        trailingIcon = if (trailingContent != null) {
            {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(enabled = onTrailingClick != null) {
                            onTrailingClick?.invoke()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    trailingContent()
                }
            }
        } else null,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(19.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TrackHubText,
            unfocusedTextColor = TrackHubText,
            focusedContainerColor = Color(0xFF060606),
            unfocusedContainerColor = Color(0xFF060606),
            focusedBorderColor = TrackHubGoldLight,
            unfocusedBorderColor = TrackHubFieldBorder,
            cursorColor = TrackHubGoldLight,
            focusedLeadingIconColor = TrackHubGoldLight,
            unfocusedLeadingIconColor = TrackHubGoldLight,
            focusedTrailingIconColor = TrackHubGoldLight,
            unfocusedTrailingIconColor = TrackHubGoldLight
        )
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
            .height(52.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = TrackHubGold.copy(alpha = 0.25f),
                spotColor = TrackHubGold.copy(alpha = 0.28f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (enabled) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFB67A00),
                            Color(0xFFF2B83A),
                            Color(0xFFB67A00)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF4E4E4E),
                            Color(0xFF5E5E5E)
                        )
                    )
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            MiniChevronRightIcon()
        }
    }
}

@Composable
fun MiniEnvelopeIcon() {
    Image(
        painter = painterResource(id = R.drawable.screenshot_8),
        contentDescription = "Email icon",
        modifier = Modifier.size(20.dp),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun MiniLockIcon() {
    Image(
        painter = painterResource(id = R.drawable.screenshot_9),
        contentDescription = "Password icon",
        modifier = Modifier.size(18.dp),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun MiniEyeIcon() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val path = Path().apply {
            moveTo(1f, size.height / 2f)
            quadraticTo(size.width / 2f, 1f, size.width - 1f, size.height / 2f)
            quadraticTo(size.width / 2f, size.height - 1f, 1f, size.height / 2f)
        }
        drawPath(path = path, color = TrackHubGoldLight, style = Stroke(width = 2f))
        drawCircle(color = TrackHubGoldLight, radius = 2.5f, center = Offset(size.width / 2f, size.height / 2f))
    }
}

@Composable
fun MiniEyeOffIcon() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val path = Path().apply {
            moveTo(1f, size.height / 2f)
            quadraticTo(size.width / 2f, 1f, size.width - 1f, size.height / 2f)
            quadraticTo(size.width / 2f, size.height - 1f, 1f, size.height / 2f)
        }
        drawPath(path = path, color = TrackHubGoldLight, style = Stroke(width = 2f))
        drawLine(
            color = TrackHubGoldLight,
            start = Offset(3f, size.height - 2f),
            end = Offset(size.width - 3f, 2f),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun MiniLoginIcon(isSelected: Boolean) {
    val iconColor = if (isSelected) {
        Color.White
    } else {
        TrackHubText.copy(alpha = 0.72f)
    }

    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = 2.3f
        val w = size.width
        val h = size.height

        // Контур двери
        drawLine(
            color = iconColor,
            start = Offset(w * 0.48f, h * 0.17f),
            end = Offset(w * 0.82f, h * 0.17f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        drawLine(
            color = iconColor,
            start = Offset(w * 0.82f, h * 0.17f),
            end = Offset(w * 0.82f, h * 0.83f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        drawLine(
            color = iconColor,
            start = Offset(w * 0.82f, h * 0.83f),
            end = Offset(w * 0.48f, h * 0.83f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        drawLine(
            color = iconColor,
            start = Offset(w * 0.48f, h * 0.17f),
            end = Offset(w * 0.48f, h * 0.36f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        drawLine(
            color = iconColor,
            start = Offset(w * 0.48f, h * 0.64f),
            end = Offset(w * 0.48f, h * 0.83f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Стрелка вправо
        drawLine(
            color = iconColor,
            start = Offset(w * 0.13f, h * 0.50f),
            end = Offset(w * 0.64f, h * 0.50f),
            strokeWidth = stroke + 0.2f,
            cap = StrokeCap.Round
        )

        drawLine(
            color = iconColor,
            start = Offset(w * 0.48f, h * 0.34f),
            end = Offset(w * 0.64f, h * 0.50f),
            strokeWidth = stroke + 0.2f,
            cap = StrokeCap.Round
        )

        drawLine(
            color = iconColor,
            start = Offset(w * 0.48f, h * 0.66f),
            end = Offset(w * 0.64f, h * 0.50f),
            strokeWidth = stroke + 0.2f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun MiniRegisterIcon(isSelected: Boolean) {
    val iconColor = if (isSelected) {
        Color.White
    } else {
        TrackHubText.copy(alpha = 0.72f)
    }

    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = 2.35f
        val w = size.width
        val h = size.height

        // Голова
        drawCircle(
            color = iconColor,
            radius = w * 0.155f,
            center = Offset(w * 0.34f, h * 0.28f),
            style = Stroke(width = stroke)
        )

        // Корпус пользователя — ровная плавная форма
        val bodyPath = Path().apply {
            moveTo(w * 0.10f, h * 0.86f)

            lineTo(w * 0.10f, h * 0.76f)

            cubicTo(
                w * 0.10f, h * 0.60f,
                w * 0.22f, h * 0.52f,
                w * 0.36f, h * 0.52f
            )

            lineTo(w * 0.50f, h * 0.52f)
        }

        drawPath(
            path = bodyPath,
            color = iconColor,
            style = Stroke(
                width = stroke,
                cap = StrokeCap.Round
            )
        )

        // Нижняя линия корпуса
        drawLine(
            color = iconColor,
            start = Offset(w * 0.10f, h * 0.86f),
            end = Offset(w * 0.50f, h * 0.86f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Плюс справа — строго ровный
        drawLine(
            color = iconColor,
            start = Offset(w * 0.72f, h * 0.38f),
            end = Offset(w * 0.72f, h * 0.82f),
            strokeWidth = stroke + 0.15f,
            cap = StrokeCap.Round
        )

        drawLine(
            color = iconColor,
            start = Offset(w * 0.52f, h * 0.60f),
            end = Offset(w * 0.92f, h * 0.60f),
            strokeWidth = stroke + 0.15f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun MiniChevronRightIcon() {
    Canvas(
        modifier = Modifier.size(15.dp)
    ) {
        val stroke = 2.2f
        val color = Color.White.copy(alpha = 0.95f)

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

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    accessToken: String,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var myTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var libraryInnerScreen by remember { mutableStateOf(LibraryInnerScreen.MAIN) }
    var currentTrack by remember { mutableStateOf<Track?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var playerVolume by remember { mutableStateOf(1f) }
    var showFullPlayerScreen by remember { mutableStateOf(false) }

    var selectedTrackForComments by remember { mutableStateOf<Track?>(null) }
    var selectedTrackForPlaylist by remember { mutableStateOf<Track?>(null) }
    var selectedTrackMenu by remember { mutableStateOf<Track?>(null) }
    var selectedTrackInfo by remember { mutableStateOf<Track?>(null) }
    var trackPendingDelete by remember { mutableStateOf<Track?>(null) }
    var showPlaylistsScreen by remember { mutableStateOf(false) }
    var showUploadTrackScreen by remember { mutableStateOf(false) }
    var showProfileScreen by remember { mutableStateOf(false) }
    var showUserSearchScreen by remember { mutableStateOf(false) }
    var showFollowingScreen by remember { mutableStateOf(false) }

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

    fun loadMyTracks() {
        scope.launch {
            isLoading = true
            errorText = null

            try {
                myTracks = fetchMyTracks(accessToken)
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка загрузки моих треков"
            } finally {
                isLoading = false
            }
        }
    }

    fun refreshTracksSilently(query: String = searchQuery) {
        scope.launch {
            try {
                delay(900)
                val freshTracks = fetchTracks(query)
                tracks = freshTracks

                currentTrack?.let { playingTrack ->
                    freshTracks.firstOrNull { it.id == playingTrack.id }?.let { updatedTrack ->
                        currentTrack = updatedTrack
                    }
                }
            } catch (_: Exception) {
                // Тихое обновление не должно ломать интерфейс, если сервер временно недоступен.
            }
        }
    }

    fun playTrack(track: Track) {
        val fullStreamUrl = if (track.streamUrl.startsWith("http")) {
            track.streamUrl
        } else {
            BASE_URL + track.streamUrl
        }

        val mediaItem = MediaItem.fromUri(Uri.parse(fullStreamUrl))

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        currentTrack = track
        currentPositionMs = 0L
        durationMs = 0L
        isPlaying = true

        refreshTracksSilently()
    }

    fun togglePlayPause() {
        if (currentTrack == null) return

        if (player.isPlaying) {
            player.pause()
            isPlaying = false
        } else {
            player.play()
            isPlaying = true
        }
    }

    fun playAdjacentTrack(direction: Int) {
        if (tracks.isEmpty()) return

        val currentId = currentTrack?.id
        val currentIndex = tracks.indexOfFirst { it.id == currentId }
        val safeIndex = if (currentIndex >= 0) currentIndex else 0
        val nextIndex = (safeIndex + direction + tracks.size) % tracks.size

        playTrack(tracks[nextIndex])
    }

    fun likeAndReload(track: Track) {
        scope.launch {
            errorText = null

            try {
                likeTrack(track.id, accessToken)

                val freshTracks = fetchTracks(searchQuery)
                tracks = freshTracks

                currentTrack?.let { playingTrack ->
                    freshTracks.firstOrNull { it.id == playingTrack.id }?.let { updatedTrack ->
                        currentTrack = updatedTrack
                    }
                }
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка лайка"
            }
        }
    }

    fun deleteTrackAndReload(track: Track) {
        scope.launch {
            errorText = null

            try {
                deleteTrack(track.id, accessToken)

                if (currentTrack?.id == track.id) {
                    player.stop()
                    currentTrack = null
                    isPlaying = false
                    currentPositionMs = 0L
                    durationMs = 0L
                }

                loadTracks(searchQuery)

                if (libraryInnerScreen == LibraryInnerScreen.MY_TRACKS) {
                    loadMyTracks()
                }
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка удаления трека"
            }
        }
    }

    LaunchedEffect(Unit) {
        loadTracks()
    }

    LaunchedEffect(currentTrack?.id) {
        if (currentTrack == null) return@LaunchedEffect

        while (true) {
            currentPositionMs = player.currentPosition.coerceAtLeast(0L)

            val playerDuration = player.duration
            durationMs = if (playerDuration > 0L) {
                playerDuration
            } else {
                0L
            }

            isPlaying = player.isPlaying

            if (player.playbackState == Player.STATE_ENDED && tracks.size > 1) {
                playAdjacentTrack(1)
                break
            }

            delay(300)
        }
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
        return
    }

    if (selectedPlaylistTrack != null) {
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
        return
    }

    if (showPlaylistsScreen) {
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
        return
    }

    if (showUploadTrackScreen) {
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
        return
    }

    if (showUserSearchScreen) {
        BackHandler {
            showUserSearchScreen = false
        }

        UserSearchScreen(
            accessToken = accessToken,
            onBack = {
                showUserSearchScreen = false
            }
        )
        return
    }

    if (showFollowingScreen) {
        BackHandler {
            showFollowingScreen = false
        }

        FollowingScreen(
            accessToken = accessToken,
            onBack = {
                showFollowingScreen = false
            }
        )
        return
    }

    if (showProfileScreen) {
        BackHandler {
            showProfileScreen = false
        }

        ProfileScreen(
            accessToken = accessToken,
            tracksCount = tracks.size,
            likedTracksCount = tracks.count { it.likesCount > 0 },
            onFindUsersClick = {
                showUserSearchScreen = true
            },
            onFollowingClick = {
                showFollowingScreen = true
            },
            onBack = {
                showProfileScreen = false
            },
            onLogout = {
                player.pause()
                showProfileScreen = false
                onLogout()
            }
        )
        return
    }

    if (showFullPlayerScreen && currentTrack != null) {
        val track = currentTrack!!

        TrackHubFullPlayerScreen(
            track = track,
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            volume = playerVolume,
            isLiked = track.likesCount > 0,
            onBack = {
                showFullPlayerScreen = false
            },
            onLikeClick = {
                likeAndReload(track)
            },
            onSeekTo = { positionMs ->
                val targetPosition = if (durationMs > 0L) {
                    positionMs.coerceIn(0L, durationMs)
                } else {
                    positionMs.coerceAtLeast(0L)
                }

                player.seekTo(targetPosition)
                currentPositionMs = targetPosition
            },
            onVolumeChange = { newVolume ->
                val safeVolume = newVolume.coerceIn(0f, 1f)

                playerVolume = safeVolume
                player.volume = safeVolume
            },
            onPlayPauseClick = {
                togglePlayPause()
            },
            onPreviousClick = {
                playAdjacentTrack(-1)
            },
            onNextClick = {
                playAdjacentTrack(1)
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        GoldBackgroundDecorations()

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            BackHandler(
                enabled = currentTab == MainTab.LIBRARY && libraryInnerScreen != LibraryInnerScreen.MAIN
            ) {
                libraryInnerScreen = LibraryInnerScreen.MAIN
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentTab) {
                    MainTab.HOME -> {
                        HomeTabScreen(
                            tracks = tracks,
                            isLoading = isLoading,
                            errorText = errorText,
                            onRetry = {
                                loadTracks(searchQuery)
                            },
                            onShowAllClick = {
                                currentTab = MainTab.LIBRARY
                                libraryInnerScreen = LibraryInnerScreen.ALL_TRACKS
                            },
                            onLogout = {
                                showProfileScreen = true
                            },
                            onPlayClick = { track ->
                                playTrack(track)
                            },
                            onLikeClick = { track ->
                                likeAndReload(track)
                            },
                            onCommentsClick = { track ->
                                selectedTrackForComments = track
                            },
                            onAddToPlaylistClick = { track ->
                                selectedTrackForPlaylist = track
                            },
                            onMoreClick = { track ->
                                selectedTrackMenu = track
                            }
                        )
                    }

                    MainTab.SEARCH -> {
                        SearchTabScreen(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            tracks = tracks,
                            isLoading = isLoading,
                            errorText = errorText,
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            onSearchClick = {
                                loadTracks(searchQuery)
                            },
                            onAllTracksClick = {
                                searchQuery = ""
                                loadTracks()
                            },
                            onPlayClick = { track ->
                                if (currentTrack?.id == track.id) {
                                    togglePlayPause()
                                } else {
                                    playTrack(track)
                                }
                            },
                            onLikeClick = { track ->
                                likeAndReload(track)
                            },
                            onCommentsClick = { track ->
                                selectedTrackForComments = track
                            },
                            onAddToPlaylistClick = { track ->
                                selectedTrackForPlaylist = track
                            },
                            onMoreClick = { track ->
                                selectedTrackMenu = track
                            }
                        )
                    }

                    MainTab.LIBRARY -> {
                        when (libraryInnerScreen) {
                            LibraryInnerScreen.MAIN -> {
                                LibraryTabScreen(
                                    tracks = tracks,
                                    onPlaylistsClick = {
                                        libraryInnerScreen = LibraryInnerScreen.PLAYLISTS
                                    },
                                    onLikedTracksClick = {
                                        libraryInnerScreen = LibraryInnerScreen.LIKED_TRACKS
                                    },
                                    onMyTracksClick = {
                                        libraryInnerScreen = LibraryInnerScreen.MY_TRACKS
                                        loadMyTracks()
                                    },
                                    onAllTracksClick = {
                                        libraryInnerScreen = LibraryInnerScreen.ALL_TRACKS
                                    }
                                )
                            }

                            LibraryInnerScreen.PLAYLISTS -> {
                                PlaylistsScreen(
                                    accessToken = accessToken,
                                    onBack = {
                                        libraryInnerScreen = LibraryInnerScreen.MAIN
                                    }
                                )
                            }

                            LibraryInnerScreen.LIKED_TRACKS -> {
                                LibraryTracksListScreen(
                                    title = "Любимые треки",
                                    subtitle = "Треки, которые получили лайки",
                                    emptyText = "Пока нет любимых треков",
                                    tracks = tracks.filter { it.likesCount > 0 },
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    onBack = {
                                        libraryInnerScreen = LibraryInnerScreen.MAIN
                                    },
                                    onPlayClick = { track ->
                                        if (currentTrack?.id == track.id) {
                                            togglePlayPause()
                                        } else {
                                            playTrack(track)
                                        }
                                    },
                                    onLikeClick = { track ->
                                        likeAndReload(track)
                                    },
                                    onCommentsClick = { track ->
                                        selectedTrackForComments = track
                                    },
                                    onAddToPlaylistClick = { track ->
                                        selectedTrackForPlaylist = track
                                    },
                                    onMoreClick = { track ->
                                        selectedTrackMenu = track
                                    }
                                )
                            }

                            LibraryInnerScreen.MY_TRACKS -> {
                                LibraryTracksListScreen(
                                    title = "Мои треки",
                                    subtitle = "Треки, которые загрузили именно вы",
                                    emptyText = "Вы пока не загрузили ни одного трека",
                                    tracks = myTracks,
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    onBack = {
                                        libraryInnerScreen = LibraryInnerScreen.MAIN
                                    },
                                    onPlayClick = { track ->
                                        if (currentTrack?.id == track.id) {
                                            togglePlayPause()
                                        } else {
                                            playTrack(track)
                                        }
                                    },
                                    onLikeClick = { track ->
                                        likeAndReload(track)
                                    },
                                    onCommentsClick = { track ->
                                        selectedTrackForComments = track
                                    },
                                    onAddToPlaylistClick = { track ->
                                        selectedTrackForPlaylist = track
                                    },
                                    onMoreClick = { track ->
                                        selectedTrackMenu = track
                                    }
                                )
                            }

                            LibraryInnerScreen.ALL_TRACKS -> {
                                LibraryTracksListScreen(
                                    title = "Все треки",
                                    subtitle = "Полный список доступных треков",
                                    emptyText = "Пока нет загруженных треков",
                                    tracks = tracks,
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    onBack = {
                                        libraryInnerScreen = LibraryInnerScreen.MAIN
                                    },
                                    onPlayClick = { track ->
                                        if (currentTrack?.id == track.id) {
                                            togglePlayPause()
                                        } else {
                                            playTrack(track)
                                        }
                                    },
                                    onLikeClick = { track ->
                                        likeAndReload(track)
                                    },
                                    onCommentsClick = { track ->
                                        selectedTrackForComments = track
                                    },
                                    onAddToPlaylistClick = { track ->
                                        selectedTrackForPlaylist = track
                                    },
                                    onMoreClick = { track ->
                                        selectedTrackMenu = track
                                    }
                                )
                            }
                        }
                    }

                    MainTab.CREATE -> {
                        CreateTabScreen(
                            accessToken = accessToken,
                            onUploadSuccess = {
                                loadTracks(searchQuery)
                            }
                        )
                    }

                }
            }

            currentTrack?.let { track ->
                TrackHubMiniPlayer(
                    track = track,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    volume = playerVolume,
                    onSeekTo = { positionMs ->
                        val targetPosition = if (durationMs > 0L) {
                            positionMs.coerceIn(0L, durationMs)
                        } else {
                            positionMs.coerceAtLeast(0L)
                        }

                        player.seekTo(targetPosition)
                        currentPositionMs = targetPosition
                    },
                    onVolumeChange = { newVolume ->
                        val safeVolume = newVolume.coerceIn(0f, 1f)

                        playerVolume = safeVolume
                        player.volume = safeVolume
                    },
                    onOpenPlayerClick = {
                        showFullPlayerScreen = true
                    },
                    onPlayPauseClick = {
                        togglePlayPause()
                    }
                )
            }

            TrackHubBottomNavigation(
                currentTab = currentTab,
                onTabClick = { tab ->
                    currentTab = tab

                    if (tab != MainTab.LIBRARY) {
                        libraryInnerScreen = LibraryInnerScreen.MAIN
                    }
                }
            )
        }
        selectedTrackMenu?.let { track ->
            TrackOptionsDialog(
                track = track,
                onDismiss = {
                    selectedTrackMenu = null
                },
                onInfoClick = {
                    selectedTrackMenu = null
                    selectedTrackInfo = track
                },
                onCommentsClick = {
                    selectedTrackMenu = null
                    selectedTrackForComments = track
                },
                onAddToPlaylistClick = {
                    selectedTrackMenu = null
                    selectedTrackForPlaylist = track
                },
                onDeleteTrackClick = {
                    selectedTrackMenu = null
                    trackPendingDelete = track
                }
            )
        }

        selectedTrackInfo?.let { track ->
            TrackInfoDialog(
                track = track,
                onDismiss = {
                    selectedTrackInfo = null
                }
            )
        }

        trackPendingDelete?.let { track ->
            DeleteTrackConfirmDialog(
                track = track,
                onDismiss = {
                    trackPendingDelete = null
                },
                onConfirm = {
                    trackPendingDelete = null
                    deleteTrackAndReload(track)
                }
            )
        }
    }

}

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

@Composable
fun HomeTabScreen(
    tracks: List<Track>,
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
    val likedTracks = tracks
        .filter { it.likesCount > 0 }
        .take(8)

    val recentTracks = tracks.take(8)

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
                tracks = likedTracks,
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
        GoldBackgroundDecorations()

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
                if (user.isFollowing) {
                    unfollowUser(user.id, accessToken)
                } else {
                    followUser(user.id, accessToken)
                }

                users = searchUsers(
                    accessToken = accessToken,
                    query = query
                )
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка изменения подписки"
            }
        }
    }

    LaunchedEffect(Unit) {
        loadUsers("")
    }

    LaunchedEffect(query) {
        delay(350)
        loadUsers(query)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        GoldBackgroundDecorations()

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

            items(users) { user ->
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
                following = fetchMyFollowing(accessToken)
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
        GoldBackgroundDecorations()

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

            items(following) { user ->
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
fun RecentTrackCard(
    track: Track,
    onPlayClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(132.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF121212))
            .border(1.dp, Color(0x33FFC84D), RoundedCornerShape(12.dp))
            .clickable(onClick = onPlayClick)
            .padding(10.dp)
    ) {
        TrackCoverPlaceholder(
            track = track,
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = track.title,
            color = TrackHubText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = track.author,
            color = TrackHubMutedText,
            fontSize = 13.sp,
            maxLines = 1
        )
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
    val colors = when (track.id % 5) {
        0 -> listOf(Color(0xFF3A0A0A), Color(0xFFD99B00))
        1 -> listOf(Color(0xFF111111), Color(0xFF6A1111))
        2 -> listOf(Color(0xFF1B1B1B), Color(0xFF444444))
        3 -> listOf(Color(0xFF101020), Color(0xFFB7860B))
        else -> listOf(Color(0xFF080808), Color(0xFF322000))
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
    var showVolumeSlider by remember(track.id) { mutableStateOf(true) }
    var showTrackInfo by remember(track.id) { mutableStateOf(false) }

    BackHandler {
        onBack()
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
    val density = LocalDensity.current
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
    onSearchClick: () -> Unit,
    onAllTracksClick: () -> Unit,
    onPlayClick: (Track) -> Unit,
    onLikeClick: (Track) -> Unit,
    onCommentsClick: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit,
    onMoreClick: (Track) -> Unit
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

        items(tracks) { track ->
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
    onPlaylistsClick: () -> Unit,
    onLikedTracksClick: () -> Unit,
    onMyTracksClick: () -> Unit,
    onAllTracksClick: () -> Unit
) {
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
                subtitle = "Треков с лайками: ${tracks.count { it.likesCount > 0 }}",
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
                subtitle = "Всего треков: ${tracks.size}",
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
    onBack: () -> Unit,
    onPlayClick: (Track) -> Unit,
    onLikeClick: (Track) -> Unit,
    onCommentsClick: (Track) -> Unit,
    onAddToPlaylistClick: (Track) -> Unit,
    onMoreClick: (Track) -> Unit
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

        items(tracks) { track ->
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
                        title = it
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
                        author = it
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

                        if (trimmedTitle.length > 100) {
                            errorText = "Название трека должно быть не длиннее 100 символов"
                            return@uploadButton
                        }

                        if (trimmedAuthor.isBlank()) {
                            errorText = "Введите автора трека"
                            return@uploadButton
                        }

                        if (trimmedAuthor.length > 50) {
                            errorText = "Автор должен быть не длиннее 50 символов"
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

                                title = ""
                                author = ""
                                selectedFileUri = null
                                selectedFileName = null
                                successText = "Трек успешно загружен"

                                onUploadSuccess()
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
                text = "MP3, WAV или другой audio-файл",
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
fun DarkTrackListCard(
    track: Track,
    onPlayClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF111111))
            .border(1.dp, Color(0x33FFC84D), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
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

                Text(
                    text = track.author,
                    color = TrackHubMutedText,
                    fontSize = 14.sp,
                    maxLines = 1
                )

                Text(
                    text = "Лайков: ${track.likesCount} · Комментариев: ${track.commentsCount}",
                    color = TrackHubMutedText,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(50))
                    .background(TrackHubGold)
                    .clickable(onClick = onPlayClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DarkSmallButton("Лайк", onLikeClick)
            DarkSmallButton("Комментарии", onCommentsClick)
            DarkSmallButton("В плейлист", onAddToPlaylistClick)
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
    var isSending by remember { mutableStateOf(false) }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        GoldBackgroundDecorations()

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
                        onValueChange = {
                            commentText = it
                            errorText = null
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    GoldPrimaryButton(
                        text = "Отправить",
                        enabled = !isSending && !isLoading,
                        onClick = sendComment@{
                            val trimmedText = commentText.trim()

                            if (trimmedText.isBlank()) {
                                errorText = "Комментарий не должен быть пустым"
                                return@sendComment
                            }

                            if (trimmedText.length > 500) {
                                errorText = "Комментарий должен быть не длиннее 500 символов"
                                return@sendComment
                            }

                            scope.launch {
                                isSending = true
                                errorText = null

                                try {
                                    addComment(track.id, trimmedText, accessToken)
                                    commentText = ""
                                    comments = fetchComments(track.id)
                                } catch (e: Exception) {
                                    errorText = e.message ?: "Ошибка отправки комментария"
                                } finally {
                                    isSending = false
                                }
                            }
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

            items(comments) { comment ->
                CommentCard(comment = comment)
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
            onValueChange = onValueChange,
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        GoldBackgroundDecorations()

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
                        onValueChange = {
                            playlistName = it
                            errorText = null
                            successText = null
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GoldSmallButton(
                        text = "Создать плейлист",
                        onClick = {
                            val trimmedName = playlistName.trim()

                            if (trimmedName.isBlank()) {
                                errorText = "Название плейлиста не должно быть пустым"
                            } else if (trimmedName.length > 100) {
                                errorText = "Название плейлиста должно быть не длиннее 100 символов"
                            } else {
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
                            }
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

            items(playlists) { playlist ->
                AddToPlaylistCard(
                    playlist = playlist,
                    enabled = !isLoading,
                    onAddClick = {
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
                                successText = "Трек добавлен в плейлист «${playlist.name}»"
                            } catch (e: Exception) {
                                errorText = e.message ?: "Ошибка добавления в плейлист"
                            } finally {
                                isLoading = false
                            }
                        }
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
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var playlistTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var playlistName by remember { mutableStateOf("") }
    var selectedPlaylistTrackMenu by remember { mutableStateOf<Track?>(null) }
    var selectedPlaylistForDelete by remember { mutableStateOf<Playlist?>(null) }
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

    fun removeTrackFromCurrentPlaylist(track: Track) {
        val playlist = selectedPlaylist ?: return

        scope.launch {
            isLoading = true
            errorText = null

            try {
                removeTrackFromPlaylist(
                    playlistId = playlist.id,
                    trackId = track.id,
                    accessToken = accessToken
                )

                playlistTracks = fetchPlaylistTracks(playlist.id, accessToken)
                playlists = fetchPlaylists(accessToken)
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка удаления трека из плейлиста"
            } finally {
                isLoading = false
            }
        }
    }

    fun deletePlaylistAndReload(playlist: Playlist) {
        scope.launch {
            isLoading = true
            errorText = null

            try {
                deletePlaylist(playlist.id, accessToken)

                if (selectedPlaylist?.id == playlist.id) {
                    selectedPlaylist = null
                    playlistTracks = emptyList()
                }

                playlists = fetchPlaylists(accessToken)
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка удаления плейлиста"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadPlaylists()
    }

    val currentPlaylist = selectedPlaylist

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        GoldBackgroundDecorations()

        if (currentPlaylist != null) {
            BackHandler {
                selectedPlaylist = null
                playlistTracks = emptyList()
                loadPlaylists()
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
                            selectedPlaylist = null
                            playlistTracks = emptyList()
                            loadPlaylists()
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

                items(playlistTracks) { track ->
                    PlaylistTrackCard(
                        track = track,
                        onMoreClick = {
                            selectedPlaylistTrackMenu = track
                        }
                    )
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
                            onValueChange = { playlistName = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        GoldSmallButton(
                            text = "Создать",
                            onClick = {
                                val trimmedName = playlistName.trim()

                                if (trimmedName.isBlank()) {
                                    errorText = "Название плейлиста не должно быть пустым"
                                    return@GoldSmallButton
                                }

                                if (trimmedName.length > 100) {
                                    errorText = "Название плейлиста должно быть не длиннее 100 символов"
                                    return@GoldSmallButton
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

                items(playlists) { playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        onClick = {
                            openPlaylist(playlist)
                        },
                        onDeleteClick = {
                            selectedPlaylistForDelete = playlist
                        }
                    )
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
                    selectedPlaylistTrackMenu = null
                },
                onRemoveClick = {
                    selectedPlaylistTrackMenu = null
                    removeTrackFromCurrentPlaylist(track)
                }
            )
        }

        selectedPlaylistForDelete?.let { playlist ->
            DeletePlaylistConfirmDialog(
                playlist = playlist,
                onDismiss = {
                    selectedPlaylistForDelete = null
                },
                onConfirm = {
                    selectedPlaylistForDelete = null
                    deletePlaylistAndReload(playlist)
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
            onValueChange = onValueChange,
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

                if (trimmedTitle.length > 100) {
                    errorText = "Название трека должно быть не длиннее 100 символов"
                    return@uploadButton
                }

                if (trimmedAuthor.isBlank()) {
                    errorText = "Введите автора трека"
                    return@uploadButton
                }

                if (trimmedAuthor.length > 50) {
                    errorText = "Автор должен быть не длиннее 50 символов"
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

suspend fun fetchCurrentUser(
    accessToken: String
): UserProfile {
    return withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/api/auth/me")
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

            val item = JSONObject(responseText)

            UserProfile(
                id = item.getInt("id"),
                username = item.getString("username"),
                email = item.getString("email")
            )
        } finally {
            connection.disconnect()
        }
    }
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
                        commentsCount = item.optInt("comments_count", 0),
                        createdAt = item.optString("created_at", ""),
                        fileSizeBytes = item.optLong("file_size_bytes", 0L),
                        durationSeconds = item.optInt("duration_seconds", 0),
                        playCount = item.optInt("play_count", 0)
                    )
                )
            }

            result
        } finally {
            connection.disconnect()
        }
    }
}

suspend fun fetchMyTracks(
    accessToken: String
): List<Track> {
    return withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/api/tracks/my")
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
                        commentsCount = item.optInt("comments_count", 0),
                        createdAt = item.optString("created_at", ""),
                        fileSizeBytes = item.optLong("file_size_bytes", 0L),
                        durationSeconds = item.optInt("duration_seconds", 0),
                        playCount = item.optInt("play_count", 0)
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

suspend fun deleteTrack(
    trackId: Int,
    accessToken: String
) {
    withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/api/tracks/$trackId")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "DELETE"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode

            if (responseCode !in 200..299) {
                val responseText = readResponseText(connection)
                throw RuntimeException("Backend вернул код $responseCode: $responseText")
            }
        } finally {
            connection.disconnect()
        }
    }
}

suspend fun deletePlaylist(
    playlistId: Int,
    accessToken: String
) {
    withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/api/playlists/$playlistId")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "DELETE"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode

            if (responseCode !in 200..299) {
                val responseText = readResponseText(connection)
                throw RuntimeException("Backend вернул код $responseCode: $responseText")
            }
        } finally {
            connection.disconnect()
        }
    }
}

suspend fun removeTrackFromPlaylist(
    playlistId: Int,
    trackId: Int,
    accessToken: String
) {
    withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/api/playlists/$playlistId/tracks/$trackId")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "DELETE"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode

            if (responseCode !in 200..299) {
                val responseText = readResponseText(connection)
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
                        commentsCount = item.optInt("comments_count", 0),
                        createdAt = item.optString("created_at", ""),
                        fileSizeBytes = item.optLong("file_size_bytes", 0L),
                        durationSeconds = item.optInt("duration_seconds", 0),
                        playCount = item.optInt("play_count", 0)
                    )
                )
            }

            result
        } finally {
            connection.disconnect()
        }
    }
}


suspend fun searchUsers(
    accessToken: String,
    query: String
): List<UserPublic> {
    return withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = URL("$BASE_URL/api/users/search?query=$encodedQuery")
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
            val result = mutableListOf<UserPublic>()

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

            result
        } finally {
            connection.disconnect()
        }
    }
}

suspend fun fetchMyFollowing(
    accessToken: String
): List<FollowUser> {
    return withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/api/users/me/following")
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
            val result = mutableListOf<FollowUser>()

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

            result
        } finally {
            connection.disconnect()
        }
    }
}

suspend fun followUser(
    userId: Int,
    accessToken: String
) {
    withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/api/users/$userId/follow")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode

            if (responseCode !in 200..299) {
                val responseText = readResponseText(connection)
                throw RuntimeException("Backend вернул код $responseCode: $responseText")
            }
        } finally {
            connection.disconnect()
        }
    }
}

suspend fun unfollowUser(
    userId: Int,
    accessToken: String
) {
    withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/api/users/$userId/follow")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "DELETE"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode

            if (responseCode !in 200..299) {
                val responseText = readResponseText(connection)
                throw RuntimeException("Backend вернул код $responseCode: $responseText")
            }
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
                commentsCount = item.optInt("comments_count", 0),
                createdAt = item.optString("created_at", ""),
                fileSizeBytes = item.optLong("file_size_bytes", 0L),
                durationSeconds = item.optInt("duration_seconds", 0),
                playCount = item.optInt("play_count", 0)
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

fun formatTrackTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L

    return "%d:%02d".format(minutes, seconds)
}

fun readResponseText(connection: HttpURLConnection): String {
    val stream = if (connection.responseCode in 200..299) {
        connection.inputStream
    } else {
        connection.errorStream
    }

    return stream?.bufferedReader()?.use { it.readText() } ?: ""
}
