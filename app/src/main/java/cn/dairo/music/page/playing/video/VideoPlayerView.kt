package cn.dairo.music.page.playing.video

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import cn.dairo.ui.util.timeFormat
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 视频播放状态,播放进度变化时只刷新播放控件
 */
@UnstableApi
class VideoPlayerState {

    /** 当前播放位置(秒) */
    var currentTime by mutableDoubleStateOf(0.0)

    /** 视频总时长(秒) */
    var duration by mutableDoubleStateOf(0.0)
        private set

    /** 是否正在拖动进度条,拖动时不更新播放位置 */
    var isDragging by mutableStateOf(false)

    /** 是否正在播放(包括缓冲中) */
    var isPlaying by mutableStateOf(false)
        private set

    /** 是否正在缓冲 */
    var isBuffering by mutableStateOf(false)
        private set

    /** 是否已经播放结束 */
    var isEnded by mutableStateOf(false)
        private set

    /** 是否是竖向的视频,全屏时竖向的视频不切换到横屏 */
    var isPortraitVideo by mutableStateOf(false)
        private set

    /** 视频画面的宽高,还没有加载时为0 */
    var videoWidth by mutableIntStateOf(0)
        private set
    var videoHeight by mutableIntStateOf(0)
        private set

    /** 视频帧率,还没有加载时为0 */
    var frameRate by mutableFloatStateOf(0f)
        private set

    /** 播放状态变化 */
    var onIsPlayingChange: (Boolean) -> Unit = {}

    /** 播放结束 */
    var onEnded: () -> Unit = {}

    /** 当前的播放器 */
    private var player: ExoPlayer? = null

    /** 刷新播放进度的任务 */
    private var progressJob: Job? = null

    private val scope = MainScope()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            update()
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            videoWidth = videoSize.width
            videoHeight = videoSize.height
            isPortraitVideo = videoSize.height > videoSize.width
        }
    }

    /** 绑定播放器,监听播放进度和状态 */
    fun attach(player: ExoPlayer) {
        this.detach()
        this.player = player
        player.addListener(this.listener)
        this.progressJob = this.scope.launch {
            while (isActive) {
                if (!isDragging) {
                    currentTime = player.currentPosition / 1000.0
                }
                delay(500)
            }
        }
    }

    /** 播放器的状态变化 */
    private fun update() {
        val player = this.player ?: return
        val state = player.playbackState
        val isPlaying = player.playWhenReady && (state == Player.STATE_READY || state == Player.STATE_BUFFERING)
        val isEnded = state == Player.STATE_ENDED
        this.isBuffering = player.playWhenReady && state == Player.STATE_BUFFERING
        if (player.duration != C.TIME_UNSET && player.duration > 0) {
            this.duration = player.duration / 1000.0
        }
        player.videoFormat?.frameRate?.takeIf { it > 0 }?.let { this.frameRate = it }
        if (isPlaying != this.isPlaying) {
            this.isPlaying = isPlaying
            this.onIsPlayingChange(isPlaying)
        }
        if (isEnded != this.isEnded) {
            this.isEnded = isEnded
            if (isEnded) {
                this.onEnded()
            }
        }
    }

    /** 解除绑定的播放器 */
    fun detach() {
        this.player?.removeListener(this.listener)
        this.progressJob?.cancel()
        this.progressJob = null
        this.player = null
        this.currentTime = 0.0
        this.duration = 0.0
        this.isDragging = false
        if (this.isPlaying) {
            this.isPlaying = false
            this.onIsPlayingChange(false)
        }
        this.isBuffering = false
        this.isEnded = false
        this.isPortraitVideo = false
        this.videoWidth = 0
        this.videoHeight = 0
        this.frameRate = 0f
    }

    /** 分辨率和帧率,例如:1080P 30FPS、4K 60FPS,还没有加载时为null */
    val formatText: String?
        get() {
            val resolution = resolutionText(this.videoWidth, this.videoHeight) ?: return null
            if (this.frameRate <= 0) {
                return resolution
            }
            return resolution + " ${this.frameRate.roundToInt()}FPS"
        }

    /** 按长边判断分辨率,竖向和宽银幕的视频也能正确显示 */
    private fun resolutionText(width: Int, height: Int): String? {
        val long = max(width, height)
        val short = min(width, height)
        return when {
            long <= 0 -> null
            long >= 3840 -> "4K"
            long >= 2560 -> "2K"
            long >= 1920 -> "1080P"
            long >= 1280 -> "720P"
            long >= 854 -> "480P"
            else -> "${short}P"
        }
    }

    /** 播放或暂停,播放结束之后从头开始播放 */
    fun playOrPause() {
        val player = this.player ?: return
        if (this.isPlaying) {
            player.pause()
            return
        }
        if (this.isEnded) {
            player.seekTo(0)
        }
        player.play()
    }

    /** 从头重新播放,播放结束之后调用 */
    fun replay() {
        val player = this.player ?: return
        player.seekTo(0)
        player.play()
    }

    /** 跳到指定位置(秒) */
    fun seek(time: Double) {
        val player = this.player
        if (player == null) {
            this.isDragging = false
            return
        }
        player.seekTo((time * 1000).toLong())
        this.currentTime = time
        this.isDragging = false
    }
}

/** 播放时控制按钮自动隐藏的时间(毫秒) */
private const val HIDE_CONTROLS_MILLIS = 3000L

/**
 * 视频播放控件:画面、播放暂停按钮、进度条和全屏按钮,点击画面显示或隐藏控制按钮
 * @param isFullScreen 是否是全屏显示
 * @param onFullScreenClick 全屏、退出全屏按钮点击事件
 * @param onPreviousClick 上一个视频按钮点击事件
 * @param onNextClick 下一个视频按钮点击事件
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    player: ExoPlayer,
    state: VideoPlayerState,
    isFullScreen: Boolean,
    onFullScreenClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    /** 是否显示控制按钮 */
    var showControls by remember { mutableStateOf(true) }

    /** 点击次数,每次点击重新计算自动隐藏的时间 */
    var tapCount by remember { mutableIntStateOf(0) }

    //正在播放并且没有拖动进度条时,一段时间之后自动隐藏控制按钮
    LaunchedEffect(showControls, state.isPlaying, state.isDragging, tapCount) {
        if (!showControls || !state.isPlaying || state.isDragging) {
            return@LaunchedEffect
        }
        delay(HIDE_CONTROLS_MILLIS)
        showControls = false
    }
    Box(
        modifier
            .background(Color.Black)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                showControls = !showControls
                tapCount += 1
            },
    ) {
        ContentFrame(player, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        AnimatedVisibility(visible = showControls || !state.isPlaying, enter = fadeIn(), exit = fadeOut()) {
            Controls(state, isFullScreen, onFullScreenClick, onPreviousClick, onNextClick)
        }
    }
}

/** 控制按钮 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun Controls(state: VideoPlayerState, isFullScreen: Boolean, onFullScreenClick: () -> Unit, onPreviousClick: () -> Unit, onNextClick: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f))) {

        //上一个、播放暂停(缓冲时显示加载中)、下一个
        Row(
            Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(if (isFullScreen) 56.dp else 36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControlButton(Icons.Default.SkipPrevious, "上一个视频", 48, 26, onClick = onPreviousClick)
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                if (state.isBuffering) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    ControlButton(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (state.isPlaying) "暂停视频" else "播放视频",
                        64, 36,
                        background = Color.Black.copy(alpha = 0.4f),
                        onClick = state::playOrPause,
                    )
                }
            }
            ControlButton(Icons.Default.SkipNext, "下一个视频", 48, 26, onClick = onNextClick)
        }
        Column(Modifier.fillMaxSize()) {

            //分辨率和帧率显示在左上角,和控制按钮一起显示、隐藏
            val formatText = state.formatText
            if (formatText != null) {
                Text(
                    formatText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(top = if (isFullScreen) 16.dp else 8.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            BottomBar(state, isFullScreen, onFullScreenClick)
        }
    }
}

/** 播放时间、进度条和全屏按钮 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun BottomBar(state: VideoPlayerState, isFullScreen: Boolean, onFullScreenClick: () -> Unit) {
    val textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Color.White)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = if (isFullScreen) 16.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(format(state.currentTime), style = textStyle)
        val range = 0f..max(state.duration, 1.0).toFloat()
        val sliderState = remember(range) { SliderState(0f, 0, range) }

        //外部的播放进度同步给进度条(受控组件)
        sliderState.value = min(state.currentTime, max(state.duration, 0.0)).toFloat()
        Slider(
            state = sliderState,
            onValueChange = {
                state.isDragging = true
                state.currentTime = it.toDouble()
            },
            onValueChangeFinished = { state.seek(state.currentTime) },
            enabled = state.duration > 0,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.3f)),
            modifier = Modifier.weight(1f),
        )
        Text(format(state.duration), style = textStyle)
        ControlButton(
            if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
            if (isFullScreen) "退出全屏" else "全屏",
            36, 24,
            onClick = onFullScreenClick,
        )
    }
}

/** 视频上的图标按钮 */
@Composable
private fun ControlButton(icon: ImageVector, label: String, size: Int, iconSize: Int, background: Color = Color.Transparent, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(iconSize.dp))
    }
}

/** 格式化播放时间 */
private fun format(seconds: Double): String {
    return ((if (seconds.isFinite()) seconds else 0.0) * 1000).timeFormat
}
