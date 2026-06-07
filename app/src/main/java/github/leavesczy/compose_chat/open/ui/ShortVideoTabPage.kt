package github.leavesczy.compose_chat.open.ui

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import github.leavesczy.compose_chat.R
import github.leavesczy.compose_chat.open.tab.OpenTabItem
import github.leavesczy.compose_chat.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ShortVideoMode {
    NativeFeed,
    WebHome
}

private data class ShortVideoConfig(
    val webHomeUri: String,
    val allowedHosts: Set<String>
)

private data class ShortVideoItem(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val rawResId: Int,
    val durationText: String
)

@OptIn(UnstableApi::class)
@Composable
fun ShortVideoTabPage(
    modifier: Modifier = Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?
) {
    val config = remember(tab.manifest.entryUri, tab.manifest.extraConfig) {
        tab.manifest.toShortVideoConfig()
    }
    var mode by remember { mutableStateOf(ShortVideoMode.NativeFeed) }
    if (mode == ShortVideoMode.WebHome) {
        ShortVideoWebHomePage(
            modifier = modifier,
            tab = tab,
            config = config,
            onBackToFeed = { mode = ShortVideoMode.NativeFeed },
            onBackToWorkbench = onBackToWorkbench
        )
    } else {
        ShortVideoNativeFeedPage(
            modifier = modifier,
            tab = tab,
            onBackToWorkbench = onBackToWorkbench,
            onOpenWebHome = { mode = ShortVideoMode.WebHome },
            webHomeUri = config.webHomeUri
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ShortVideoNativeFeedPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?,
    onOpenWebHome: () -> Unit,
    webHomeUri: String
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val items = remember { localDemoShortVideos() }
    val pagerState = rememberPagerState(pageCount = { items.size })
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
        }
    }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var userWantsPlay by remember { mutableStateOf(true) }
    var wasPlayingBeforePause by remember { mutableStateOf(false) }

    fun loadVideo(index: Int) {
        val item = items.getOrNull(index) ?: return
        isBuffering = true
        errorMessage = null
        player.setMediaItem(MediaItem.fromUri(item.rawResourceUri()))
        player.prepare()
        player.playWhenReady = userWantsPlay
    }

    fun pauseAndBack() {
        userWantsPlay = false
        player.pause()
        onBackToWorkbench?.invoke()
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }

            override fun onIsPlayingChanged(nextIsPlaying: Boolean) {
                isPlaying = nextIsPlaying
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isBuffering = false
                isPlaying = false
                errorMessage = "视频播放失败，请重试"
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.pause()
            player.release()
        }
    }

    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    wasPlayingBeforePause = player.isPlaying
                    player.pause()
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (wasPlayingBeforePause && userWantsPlay) {
                        player.play()
                    }
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        loadVideo(index = 0)
    }

    LaunchedEffect(pagerState.currentPage) {
        if (currentIndex != pagerState.currentPage) {
            currentIndex = pagerState.currentPage
            loadVideo(index = currentIndex)
        }
    }

    BackHandler {
        pauseAndBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color.Black)
    ) {
        VerticalPager(
            modifier = Modifier.fillMaxSize(),
            state = pagerState,
            key = { page -> items[page].id }
        ) { page ->
            val item = items[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = item.backgroundBrush())
                    .clickable {
                        if (page == currentIndex) {
                            if (player.isPlaying) {
                                userWantsPlay = false
                                player.pause()
                            } else {
                                userWantsPlay = true
                                player.play()
                            }
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(page)
                            }
                        }
                    }
            ) {
                if (page == currentIndex) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { viewContext ->
                            PlayerView(viewContext).apply {
                                this.player = player
                                useController = false
                                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { playerView ->
                            playerView.player = player
                        }
                    )
                }
                ShortVideoGradientOverlay()
                ShortVideoBottomInfo(item = item)
                if (page == currentIndex) {
                    ShortVideoActionRail(
                        isPlaying = isPlaying,
                        isMuted = isMuted,
                        onTogglePlay = {
                            if (player.isPlaying) {
                                userWantsPlay = false
                                player.pause()
                            } else {
                                userWantsPlay = true
                                player.play()
                            }
                        },
                        onToggleMute = {
                            isMuted = !isMuted
                            player.volume = if (isMuted) 0f else 1f
                        },
                        onRetry = {
                            loadVideo(index = currentIndex)
                        }
                    )
                    if (!isPlaying || isBuffering || errorMessage != null) {
                        ShortVideoCenterState(
                            isBuffering = isBuffering,
                            isPlaying = isPlaying,
                            errorMessage = errorMessage,
                            onPlay = {
                                userWantsPlay = true
                                player.play()
                            },
                            onRetry = {
                                loadVideo(index = currentIndex)
                            }
                        )
                    }
                }
            }
        }
        ShortVideoTopBar(
            tab = tab,
            onBack = ::pauseAndBack,
            onOpenWebHome = {
                player.pause()
                onOpenWebHome()
            },
            onRefresh = {
                loadVideo(index = currentIndex)
            },
            webHomeUri = webHomeUri
        )
        Text(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .clip(shape = RoundedCornerShape(size = 999.dp))
                .background(color = Color.Black.copy(alpha = 0.32f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            text = "本地演示视频 · 默认静音自动播放",
            fontSize = 11.sp,
            lineHeight = 13.sp,
            color = Color.White.copy(alpha = 0.78f)
        )
    }
}

@Composable
private fun ShortVideoTopBar(
    tab: OpenTabItem,
    onBack: () -> Unit,
    onOpenWebHome: () -> Unit,
    onRefresh: () -> Unit,
    webHomeUri: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        ShortVideoHeaderIconButton(
            modifier = Modifier.align(alignment = Alignment.CenterStart),
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "返回工作台",
            onClick = onBack
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 92.dp),
            text = tab.displayName,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.align(alignment = Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShortVideoHeaderIconButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = "刷新",
                onClick = onRefresh
            )
            ShortVideoHeaderIconButton(
                icon = Icons.Rounded.OpenInBrowser,
                contentDescription = "打开网页",
                enabled = webHomeUri.isNotBlank(),
                onClick = onOpenWebHome
            )
        }
    }
}

@Composable
private fun ShortVideoActionRail(
    isPlaying: Boolean,
    isMuted: Boolean,
    onTogglePlay: () -> Unit,
    onToggleMute: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        ShortVideoRoundButton(
            icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            text = if (isPlaying) "暂停" else "播放",
            onClick = onTogglePlay
        )
        ShortVideoRoundButton(
            icon = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
            text = if (isMuted) "静音" else "有声",
            onClick = onToggleMute
        )
        ShortVideoRoundButton(
            icon = Icons.Rounded.Refresh,
            text = "重试",
            onClick = onRetry
        )
    }
}

@Composable
private fun ShortVideoCenterState(
    isBuffering: Boolean,
    isPlaying: Boolean,
    errorMessage: String?,
    onPlay: () -> Unit,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val text = when {
            errorMessage != null -> errorMessage
            isBuffering -> "正在加载"
            !isPlaying -> "播放"
            else -> ""
        }
        if (text.isNotBlank()) {
            Column(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(size = 999.dp))
                    .background(color = Color.Black.copy(alpha = 0.42f))
                    .clickable(onClick = if (errorMessage != null) onRetry else onPlay)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(space = 6.dp)
            ) {
                Icon(
                    modifier = Modifier.size(size = 34.dp),
                    imageVector = if (errorMessage != null) Icons.Rounded.Refresh else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
                Text(
                    text = text,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ShortVideoBottomInfo(item: ShortVideoItem) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 18.dp, end = 92.dp, bottom = 46.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = item.author,
            fontSize = 17.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            modifier = Modifier.padding(top = 6.dp),
            text = item.title,
            fontSize = 15.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = item.description,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = Color.White.copy(alpha = 0.84f)
        )
        Text(
            modifier = Modifier
                .padding(top = 10.dp)
                .clip(shape = RoundedCornerShape(size = 8.dp))
                .background(color = Color.White.copy(alpha = 0.16f))
                .padding(horizontal = 9.dp, vertical = 5.dp),
            text = "本地演示 · ${item.durationText} · 无音轨",
            fontSize = 11.sp,
            lineHeight = 13.sp,
            color = Color.White.copy(alpha = 0.86f)
        )
    }
}

@Composable
private fun ShortVideoGradientOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.46f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.78f)
                    )
                )
            )
    )
}

@Composable
private fun ShortVideoHeaderIconButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    backgroundColor: Color = Color.Black.copy(alpha = if (enabled) 0.36f else 0.18f),
    contentColor: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(size = 38.dp)
            .clip(shape = RoundedCornerShape(size = 999.dp))
            .background(color = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.42f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(size = 18.dp),
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor.copy(alpha = if (enabled) 1f else 0.42f)
        )
    }
}

@Composable
private fun ShortVideoRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(top = 13.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size = 48.dp)
                .clip(shape = RoundedCornerShape(size = 999.dp))
                .background(color = Color.Black.copy(alpha = 0.36f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(size = 25.dp),
                imageVector = icon,
                contentDescription = null,
                tint = Color.White
            )
        }
        Text(
            text = text,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun ShortVideoWebHomePage(
    modifier: Modifier,
    tab: OpenTabItem,
    config: ShortVideoConfig,
    onBackToFeed: () -> Unit,
    onBackToWorkbench: (() -> Unit)?
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var progress by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }

    fun back() {
        val view = webView
        if (view?.canGoBack() == true) {
            view.goBack()
        } else {
            onBackToFeed()
        }
    }

    BackHandler {
        back()
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }

    LaunchedEffect(loading, config.webHomeUri) {
        if (loading) {
            delay(timeMillis = 8000)
            if (loading) {
                message = "TikTok 网页仍在加载，可先回到本地演示。"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            ShortVideoHeaderIconButton(
                modifier = Modifier.align(alignment = Alignment.CenterStart),
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = if (canGoBack) "网页返回" else "本地演示",
                backgroundColor = Color.Transparent,
                contentColor = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color,
                onClick = ::back
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 122.dp),
                text = "TikTok 网页入口",
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.align(alignment = Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShortVideoHeaderIconButton(
                    icon = Icons.Rounded.Refresh,
                    contentDescription = "重载",
                    backgroundColor = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color,
                    contentColor = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color,
                    onClick = {
                        loading = true
                        message = null
                        webView?.loadUrl(config.webHomeUri)
                    }
                )
                ShortVideoHeaderIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "返回工作台",
                    enabled = onBackToWorkbench != null,
                    backgroundColor = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color,
                    contentColor = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color,
                    onClick = { onBackToWorkbench?.invoke() }
                )
                ShortVideoHeaderIconButton(
                    icon = Icons.Rounded.OpenInBrowser,
                    contentDescription = "外部浏览器打开",
                    backgroundColor = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color,
                    contentColor = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color,
                    onClick = {
                        runCatching {
                            webView?.context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(config.webHomeUri)))
                        }.onFailure {
                            message = "未找到可打开该网页的外部应用。"
                        }
                    }
                )
            }
        }
        message?.let { text ->
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color(color = 0xFFFFF7ED))
                    .clickable(onClick = onBackToFeed)
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                text = text,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Color(color = 0xFFB45309)
            )
        }
        Box(modifier = Modifier.weight(weight = 1f)) {
            if (config.webHomeUri.isBlank()) {
                Text(
                    modifier = Modifier.padding(all = 18.dp),
                    text = "未配置 TikTok 网页地址。",
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                )
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            webView = this
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progress = newProgress
                                    loading = newProgress < 100
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val uri = request?.url ?: return false
                                    if (uri.isAllowedShortVideoWebNavigation(config.allowedHosts)) {
                                        return false
                                    }
                                    message = "已拦截非白名单链接：${uri.host.orEmpty().ifBlank { uri.scheme.orEmpty() }}"
                                    return true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    loading = false
                                    canGoBack = view?.canGoBack() == true
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        loading = false
                                        canGoBack = view?.canGoBack() == true
                                        message = "TikTok 网页暂不可用，已可切回本地演示。"
                                    }
                                }

                                override fun onReceivedHttpError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    errorResponse: WebResourceResponse?
                                ) {
                                    val statusCode = errorResponse?.statusCode ?: return
                                    if (request?.isForMainFrame == true && statusCode >= 400) {
                                        loading = false
                                        canGoBack = view?.canGoBack() == true
                                        message = "TikTok 网页返回 $statusCode，可切回本地演示。"
                                    }
                                }
                            }
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.loadsImagesAutomatically = true
                            settings.javaScriptCanOpenWindowsAutomatically = false
                            settings.setSupportMultipleWindows(false)
                            settings.setSupportZoom(false)
                            settings.builtInZoomControls = false
                            settings.displayZoomControls = false
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            loadUrl(config.webHomeUri)
                        }
                    }
                )
            }
        }
    }
}

private fun localDemoShortVideos(): List<ShortVideoItem> {
    return listOf(
        ShortVideoItem(
            id = "local-demo-01",
            title = "本地演示 01",
            author = "@AI-OnCall Demo",
            description = "本地内置短视频，用于离线播放和交互验收。",
            rawResId = R.raw.short_demo_01,
            durationText = "20 秒"
        ),
        ShortVideoItem(
            id = "local-demo-02",
            title = "本地演示 02",
            author = "@AI-OnCall Demo",
            description = "用于验证上下滑动、播放暂停和静音状态。",
            rawResId = R.raw.short_demo_02,
            durationText = "9 秒"
        ),
        ShortVideoItem(
            id = "local-demo-03",
            title = "本地演示 03",
            author = "@AI-OnCall Demo",
            description = "用于验证连续播放、切换和兜底体验。",
            rawResId = R.raw.short_demo_03,
            durationText = "待实测"
        )
    )
}

private fun ShortVideoItem.rawResourceUri(): Uri {
    return Uri.parse("rawresource:///$rawResId")
}

private fun ShortVideoItem.backgroundBrush(): Brush {
    val colors = when (id) {
        "local-demo-01" -> listOf(Color(color = 0xFF111827), Color(color = 0xFF2563EB))
        "local-demo-02" -> listOf(Color(color = 0xFF18181B), Color(color = 0xFF16A34A))
        else -> listOf(Color(color = 0xFF0F172A), Color(color = 0xFFDB2777))
    }
    return Brush.verticalGradient(colors = colors)
}

private fun github.leavesczy.compose_chat.open.model.TabManifest.toShortVideoConfig(): ShortVideoConfig {
    val webHomeUri = extraConfig["webHomeUri"]?.trim().orEmpty()
        .ifBlank { entryUri.orEmpty() }
    return ShortVideoConfig(
        webHomeUri = webHomeUri,
        allowedHosts = extraConfig.toShortVideoAllowedHosts(entryUri = webHomeUri)
    )
}

private fun Map<String, String>.toShortVideoAllowedHosts(entryUri: String): Set<String> {
    val configuredHosts = get("allowedHosts")
        ?.split(",", ";")
        ?.map { host -> host.normalizedShortVideoHost() }
        ?.filter { host -> host.isNotBlank() && host != "unknown" }
        .orEmpty()
    val entryHost = runCatching {
        Uri.parse(entryUri).host.orEmpty().normalizedShortVideoHost()
    }.getOrDefault("")
    return (configuredHosts + entryHost).filter { host -> host.isNotBlank() }.toSet()
}

private fun Uri.isAllowedShortVideoWebNavigation(allowedHosts: Set<String>): Boolean {
    val normalizedScheme = scheme.orEmpty().lowercase()
    if (normalizedScheme == "about" || normalizedScheme == "data") {
        return true
    }
    if (normalizedScheme != "http" && normalizedScheme != "https") {
        return false
    }
    val requestHost = host.orEmpty().normalizedShortVideoHost()
    return allowedHosts.isEmpty() || allowedHosts.any { allowedHost ->
        requestHost == allowedHost || requestHost.endsWith(suffix = ".$allowedHost")
    }
}

private fun String.normalizedShortVideoHost(): String {
    return trim()
        .lowercase()
        .removePrefix("www.")
}
