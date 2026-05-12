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
