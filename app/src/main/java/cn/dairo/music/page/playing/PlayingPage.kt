package cn.dairo.music.page.playing

import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import cn.dairo.music.components.MusicImageCache
import cn.dairo.music.util.BlurUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import cn.dairo.music.components.AlertButton
import cn.dairo.music.components.AppAlert
import cn.dairo.music.components.AppRoot
import cn.dairo.music.components.AppSheet
import cn.dairo.music.components.ButtonRole
import cn.dairo.music.components.MusicImage
import cn.dairo.music.constants.EventName
import cn.dairo.music.extension.artistAlbumText
import cn.dairo.music.extension.isDarkTheme
import cn.dairo.music.extension.like
import cn.dairo.music.extension.nameText
import cn.dairo.music.page.musicinfo.MusicInfoSheet
import cn.dairo.music.page.playing.lrc.LrcView
import cn.dairo.music.page.playing.video.VideoPlayerView
import cn.dairo.music.page.playing.video.VideoView
import cn.dairo.music.page.playing.video.VideoViewModel
import cn.dairo.music.page.playing.wave.PlayingStarsView
import cn.dairo.music.page.playing.wave.PlayingWaveStyle
import cn.dairo.music.page.playing.wave.PlayingWaveView
import cn.dairo.music.player.AudioSpectrum
import cn.dairo.music.player.PlayerManager
import cn.dairo.music.player.PlayerProgress
import cn.dairo.music.store.MusicStore
import cn.dairo.music.store.SettingStore
import cn.dairo.music.util.OnReceive
import cn.dairo.music.util.rememberStateObject
import cn.dairo.ui.root.RootView
import cn.dairo.ui.theme.DairoTheme
import cn.dairo.ui.theme.ThemeMode
import cn.dairo.ui.util.timeFormat
import kotlin.math.max
import kotlin.math.min

/** 视频页面 */
private const val TAB_VIDEO = 0

/** 封面页面 */
private const val TAB_COVER = 1

/** 歌词页面 */
private const val TAB_LRC = 2

/** 竖屏时内容的最大宽度,避免平板上控制按钮拉得太开 */
private val PORTRAIT_MAX_WIDTH = 640.dp

/** 横屏时右侧歌曲信息和控制栏的最大宽度 */
private val LANDSCAPE_CONTROL_MAX_WIDTH = 420.dp

/** 歌手、专辑的文字颜色 */
private val ARTIST_COLOR = Color(220 / 255f, 225 / 255f, 1f, 0.6f)

/**
 * 播放页面,左右滑动切换视频、封面和歌词
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayingPage() {

    //状态栏图标在深色背景上用浅色,关闭时恢复
    val appIsDark = isDarkTheme
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = !appIsDark
            controller?.isAppearanceLightNavigationBars = !appIsDark
        }
    }

    //播放页固定使用深色主题
    DairoTheme(themeMode = ThemeMode.DARK) {
        PlayingContent()
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun PlayingContent() {

    /** 视频 */
    val videoVm = rememberStateObject { VideoViewModel() }

    /** 当前显示的页面 */
    val pagerState = rememberPagerState(initialPage = TAB_COVER) { 3 }
    val selectedTab = pagerState.currentPage

    /** 上一次显示的页面,离开视频页面时用来判断 */
    var lastTab by remember { mutableIntStateOf(TAB_COVER) }

    /** 用来判断"我喜欢"状态,歌单变化时刷新 */
    var likeVersion by remember { mutableIntStateOf(0) }

    /** 显示播放列表 */
    var showPlayList by remember { mutableStateOf(false) }

    /** 显示歌曲信息 */
    var showInfo by remember { mutableStateOf(false) }

    /** 第一次进入播放页时提示左边是视频、右边是歌词 */
    var showGuide by remember { mutableStateOf(!SettingStore.playingGuideShown) }

    /** 显示取消离线模式的确认框 */
    var showOfflineAlert by remember { mutableStateOf(false) }

    /** App是否在前台,在后台时停止背景和波形的动画 */
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val isAppActive = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)

    //页面显示时才动画,播放页关闭之后这里就不在了
    val isAnimating = isAppActive
    val waveStyle = PlayingWaveStyle.of(SettingStore.playingWaveStyle)
    val isWaveAnimating = isAnimating && selectedTab == TAB_COVER

    //只有封面页的波形显示时才计算频谱,关闭波形时不计算
    LaunchedEffect(isWaveAnimating, waveStyle) {
        AudioSpectrum.isEnabled = isWaveAnimating && waveStyle != PlayingWaveStyle.OFF
    }
    DisposableEffect(Unit) {
        onDispose {
            AudioSpectrum.isEnabled = false
        }
    }

    //全屏播放视频时返回键退出全屏,否则关闭播放页
    BackHandler {
        if (videoVm.isFullScreen) {
            videoVm.exitFullScreen()
        } else {
            AppRoot.dismissPlaying()
        }
    }

    OnReceive(EventName.MUSIC_CHANGE) {
        likeVersion += 1
    }
    OnReceive(EventName.SLEEP_TIMER_END) {
        videoVm.pause()
    }

    //播放列表清空之后关闭播放页
    val isEmpty = PlayerManager.music == null
    LaunchedEffect(isEmpty) {
        if (isEmpty) {
            AppRoot.dismissPlaying()
        }
    }

    //切换页面,进入视频页面时暂停音乐,视频从头开始播放;离开视频页面时停止视频,音乐保持暂停
    //视频可能是后台单独上传的,和音乐的时间轴不一定同步,所以两边的播放进度互不影响
    LaunchedEffect(selectedTab) {
        if (selectedTab == lastTab) {
            return@LaunchedEffect
        }
        if (selectedTab == TAB_VIDEO) {
            PlayerManager.pause()
            videoVm.enter()
        } else if (lastTab == TAB_VIDEO) {
            videoVm.leave()
        }
        lastTab = selectedTab
    }

    //在视频页面通过播放列表切换了歌曲,视频跟着切换
    val index = PlayerManager.index
    LaunchedEffect(index) {
        if (selectedTab == TAB_VIDEO && !videoVm.isShowing(PlayerManager.music)) {
            PlayerManager.pause()
            videoVm.enter()
        }
    }

    //音乐开始播放时(例如通知栏点了播放)视频暂停
    val isPlaying = PlayerManager.isPlaying
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            videoVm.pause()
        }
    }

    //App进入后台时暂停视频
    LaunchedEffect(lifecycleState) {
        if (!lifecycleState.isAtLeast(Lifecycle.State.STARTED)) {
            videoVm.pause()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            if (lastTab == TAB_VIDEO) {
                videoVm.leave()
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color(7 / 255f, 11 / 255f, 24 / 255f))) {
        Background(PlayerManager.music?.logo)
        PlayingStarsView(isAnimating, Modifier.fillMaxSize())
        BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding()) {
            val isLandscape = maxWidth > maxHeight
            val controlWidth = min(maxWidth.value * 0.4f, LANDSCAPE_CONTROL_MAX_WIDTH.value).dp
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxHeight()
                    .then(if (isLandscape) Modifier.fillMaxWidth() else Modifier.widthIn(max = PORTRAIT_MAX_WIDTH).fillMaxWidth()),
            ) {
                TopBar(selectedTab, onClose = AppRoot::dismissPlaying, onMoreClick = { showInfo = true })
                if (isLandscape) {

                    //横屏(平板):左边视频/封面/歌词,右边歌曲信息和控制栏
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TabView(pagerState, videoVm, waveStyle, isWaveAnimating, Modifier.weight(1f))
                        Column(
                            Modifier.width(controlWidth).fillMaxHeight().padding(end = 16.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            InfoBar(likeVersion) { showOfflineAlert = true }
                            BottomBar(selectedTab) { showPlayList = true }
                        }
                    }
                } else {
                    TabView(pagerState, videoVm, waveStyle, isWaveAnimating, Modifier.weight(1f))
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        InfoBar(likeVersion) { showOfflineAlert = true }
                        BottomBar(selectedTab) { showPlayList = true }
                    }
                }
            }
        }

        //全屏播放视频,切换到下一个视频的过程中保持全屏,显示加载状态
        if (videoVm.isFullScreen) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                contentAlignment = Alignment.Center,
            ) {
                val player = videoVm.player
                if (player != null) {
                    VideoPlayerView(
                        player, videoVm.state, isFullScreen = true, onFullScreenClick = videoVm::exitFullScreen,
                        onPreviousClick = videoVm::onPreviousClick, onNextClick = videoVm::onNextClick, modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (videoVm.isWaiting) {
                            CircularProgressIndicator(color = Color.White)
                        }
                        Text(videoVm.tip ?: "", style = MaterialTheme.typography.bodyMedium, color = Color.White, textAlign = TextAlign.Center)
                        OutlinedButton(onClick = videoVm::exitFullScreen) {
                            Text("退出全屏", color = Color.White)
                        }
                    }
                }
            }
        }
        AnimatedVisibility(visible = showGuide, exit = fadeOut()) {
            LaunchedEffect(Unit) {
                SettingStore.playingGuideShown = true
            }
            PlayingGuideView { showGuide = false }
        }
    }
    AppAlert(
        isPresented = showOfflineAlert,
        title = "当前处于离线模式，是否取消离线模式？",
        message = "离线模式下只播放已经下载或缓存的歌曲，取消之后从下一首开始尝试在线播放",
        onDismiss = { showOfflineAlert = false },
        buttons = listOf(
            AlertButton("取消离线模式", action = PlayerManager::exitOffline),
            AlertButton("保持", ButtonRole.CANCEL),
        ),
    )
    if (showPlayList) {
        AppSheet(onDismiss = { showPlayList = false }, heightFraction = 0.6f) {
            RootView(themeMode = ThemeMode.DARK) {
                PlayListPage()
            }
        }
    }
    val infoMusic = PlayerManager.music
    if (showInfo && infoMusic != null) {
        val favoriteId = PlayerManager.favoriteId
        MusicInfoSheet(infoMusic, if (favoriteId > 0) favoriteId else null) { showInfo = false }
    }
}

/** 背景:模糊的封面压暗,中间偏上透出一点紫色;颜色跟着每首歌的封面变 */
@Composable
private fun Background(logo: String?) {

    //饱和度提高一些
    val saturation = remember { ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(1.25f) }) }
    Box(Modifier.fillMaxSize()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MusicImage(logo, Modifier.fillMaxSize().blur(46.dp), colorFilter = saturation)
        } else {

            //Android 12以下不支持Modifier.blur,把封面缩小、模糊之后放大显示
            var blurred by remember(logo) { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(logo) {
                val image = logo?.let { MusicImageCache.load(it) } ?: return@LaunchedEffect
                blurred = withContext(Dispatchers.Default) { BlurUtil.blur(image.asAndroidBitmap()).asImageBitmap() }
            }
            blurred?.let {
                Image(it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, colorFilter = saturation)
            }
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.radialGradient(
                    listOf(Color(40 / 255f, 30 / 255f, 120 / 255f, 0.15f), Color(5 / 255f, 7 / 255f, 20 / 255f, 0.82f)),
                    center = Offset(size.width * 0.5f, size.height * 0.36f),
                    radius = max(size.width, size.height) * 0.6f,
                ),
            )
        }
    }
}

/** 视频、封面、歌词左右滑动切换 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun TabView(
    pagerState: androidx.compose.foundation.pager.PagerState,
    videoVm: VideoViewModel,
    waveStyle: PlayingWaveStyle,
    isWaveAnimating: Boolean,
    modifier: Modifier,
) {
    HorizontalPager(pagerState, modifier.fillMaxWidth(), beyondViewportPageCount = 1) { page ->
        when (page) {
            TAB_VIDEO -> VideoView(videoVm, Modifier.padding(horizontal = 16.dp))

            //封面:圆形封面外面一圈跟着音乐起伏的波形
            TAB_COVER -> PlayingWaveView(
                logo = PlayerManager.music?.logo,
                isPlaying = PlayerManager.isPlaying,
                style = waveStyle,
                shockwave = SettingStore.playingWaveShockwave,
                coverColor = SettingStore.playingWaveCoverColor,
                isActive = isWaveAnimating,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            else -> LrcView(Modifier.padding(horizontal = 16.dp))
        }
    }
}

/** 顶部关闭按钮和页面指示 */
@Composable
private fun TopBar(selectedTab: Int, onClose: () -> Unit, onMoreClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp)) {
        Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0 until 3) {
                val width by animateDpAsState(if (selectedTab == i) 18.dp else 6.dp, label = "indicator")
                val color by animateColorAsState(Color.White.copy(alpha = if (selectedTab == i) 1f else 0.4f), label = "indicator")
                Box(Modifier.size(width, 6.dp).clip(RoundedCornerShape(3.dp)).background(color))
            }
        }
        IconButton(Icons.Default.KeyboardArrowDown, "关闭", 30, Modifier.align(Alignment.CenterStart), onClick = onClose)

        //歌曲信息
        IconButton(Icons.Default.MoreHoriz, "更多操作", 28, Modifier.align(Alignment.CenterEnd), onClick = onMoreClick)
    }
}

/** 歌名、歌手、喜欢和离线模式按钮 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun InfoBar(likeVersion: Int, onOfflineClick: () -> Unit) {
    val music = PlayerManager.music
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {

        //歌名单独一行,长歌名能显示得更完整
        Text(
            music?.nameText ?: "",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                PlayerManager.collectMsg ?: music?.artistAlbumText ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = ARTIST_COLOR,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            //离线模式时显示,点击之后可以取消离线模式
            if (PlayerManager.isOffline) {
                IconButton(Icons.Default.WifiOff, "离线模式", 24, tint = Color(0xFFFF9500), onClick = onOfflineClick)
            }

            //是否"我喜欢"的歌曲,歌单变化时刷新
            @Suppress("UNUSED_EXPRESSION")
            likeVersion
            val isLike = music != null && music.id != 0L && MusicStore.isLike(music.id)
            IconButton(
                if (isLike) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                if (isLike) "取消喜欢" else "喜欢",
                28,
                tint = if (isLike) Color.like else Color.White,
            ) {
                music?.let { MusicStore.toggleLike(it) }
            }
        }
    }
}

/** 进度条和播放控制 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun BottomBar(selectedTab: Int, onPlayListClick: () -> Unit) {
    if (selectedTab == TAB_VIDEO) {

        //视频有自己的进度条和控制按钮
        Spacer(Modifier.height(24.dp))
        return
    }
    PlayingProgressView()
    Row(
        Modifier.fillMaxWidth().padding(vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(PlayerManager.orderType.icon, PlayerManager.orderType.label, 26, onClick = PlayerManager::switchOrderType)
        IconButton(Icons.Default.SkipPrevious, "上一曲", 40, onClick = PlayerManager::previous)
        Box(
            Modifier.clip(CircleShape).clickable(onClick = PlayerManager::playOrPause),
        ) {
            PlayingPlayButton(PlayerManager.isPlaying, PlayerManager.isLoading)
        }
        IconButton(Icons.Default.SkipNext, "下一曲", 40) { PlayerManager.next() }
        IconButton(Icons.AutoMirrored.Filled.List, "播放列表", 26, onClick = onPlayListClick)
    }
}

/** 白色的图标按钮 */
@Composable
private fun IconButton(icon: ImageVector, label: String, iconSize: Int, modifier: Modifier = Modifier, tint: Color = Color.White, onClick: () -> Unit) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(iconSize.dp))
    }
}

/** 播放进度条,两边显示时间,点击、拖动跳到对应位置 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun PlayingProgressView() {
    val progress = PlayerProgress
    val textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Color.White.copy(alpha = 0.6f))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(format(progress.currentTime), style = textStyle, modifier = Modifier.width(44.dp))
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .height(24.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        if (progress.duration <= 0) {
                            return@awaitEachGesture
                        }
                        val width = size.width.toFloat()
                        progress.isDragging = true
                        progress.currentTime = min(1f, max(0f, down.position.x / width)) * progress.duration
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                break
                            }
                            change.consume()
                            progress.currentTime = min(1f, max(0f, change.position.x / width)) * progress.duration
                        }
                        progress.isDragging = false
                        PlayerManager.seek(progress.currentTime)
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            val ratio = if (progress.duration > 0) min(1.0, max(0.0, progress.currentTime / progress.duration)).toFloat() else 0f
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.18f)))
            Box(Modifier.width(maxWidth * ratio).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(226 / 255f, 230 / 255f, 1f)))
            Box(
                Modifier
                    .padding(start = max(0f, (maxWidth * ratio).value - 7).dp)
                    .size(14.dp)
                    .shadow(6.dp, CircleShape, ambientColor = Color(170 / 255f, 180 / 255f, 1f), spotColor = Color(170 / 255f, 180 / 255f, 1f))
                    .background(Color.White, CircleShape),
            )
        }
        Text(format(progress.duration), style = textStyle, textAlign = TextAlign.End, modifier = Modifier.width(44.dp))
    }
}

/** 播放、暂停按钮:蓝色渐变的圆形按钮,下方有蓝色光晕,外面一圈淡淡的光环,暂停时光晕变暗 */
@Composable
private fun PlayingPlayButton(isPlaying: Boolean, isLoading: Boolean) {
    val blue = Color(75 / 255f, 111 / 255f, 240 / 255f)
    val lightBlue = Color(127 / 255f, 180 / 255f, 1f)
    Box(
        Modifier
            .size(80.dp)
            .background(lightBlue.copy(alpha = if (isPlaying) 0.12f else 0.06f), CircleShape)
            .padding(5.dp)
            .shadow(if (isPlaying) 18.dp else 12.dp, CircleShape, ambientColor = blue, spotColor = blue)
            .background(Brush.linearGradient(listOf(lightBlue, blue)), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
        } else {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

/** 格式化播放时间 */
private fun format(seconds: Double): String {
    return ((if (seconds.isFinite()) seconds else 0.0) * 1000).timeFormat
}
