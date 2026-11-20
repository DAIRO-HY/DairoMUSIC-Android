package cn.dairo.music.page.playing.video

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import cn.dairo.file.sdk.FetchTask
import cn.dairo.music.api.MusicApi
import cn.dairo.music.api.http.ApiHttpBase
import cn.dairo.music.api.model.MusicModel
import cn.dairo.music.api.model.VideoModel
import cn.dairo.music.extension.artistText
import cn.dairo.music.extension.isSameMusic
import cn.dairo.music.extension.nameText
import cn.dairo.music.player.PlayerManager
import cn.dairo.music.player.PlayerOrderType
import cn.dairo.music.store.SettingStore
import cn.dairo.music.util.AppActivity
import cn.dairo.music.util.FileSdkUtil
import cn.dairo.music.util.FileSource
import cn.dairo.music.util.HttpUtil
import cn.dairo.music.util.NetworkMonitor
import cn.dairo.music.util.NetworkType
import cn.dairo.music.util.ObservableObject
import cn.dairo.music.util.OrientationUtil
import cn.dairo.music.util.ServerConfig
import cn.dairo.music.util.ShareUtil
import cn.dairo.ui.DairoUI
import cn.dairo.ui.root.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 视频下载之后的操作
 */
enum class VideoFileAction {

    /** 保存到相册 */
    SAVE_ALBUM,

    /** 通过其他App发送文件 */
    SEND;

    /** 操作名称,用在需要VIP的提示里 */
    val title: String
        get() = when (this) {
            SAVE_ALBUM -> "保存到相册"
            SEND -> "发送文件"
        }
}

/**
 * 播放页的视频
 * 切换到视频页面时暂停音乐,视频从头开始播放;视频播放时音乐一直暂停,离开视频页面时音乐保持暂停
 * 视频可能是后台单独上传的,和音乐的时间轴不一定同步,所以视频和音乐的播放进度互不影响
 * 视频播放结束之后按播放顺序继续:单曲循环时重新播放,随机播放时随机选下一个有视频的歌曲,列表循环时播放下一个有视频的歌曲,全屏时保持全屏
 * 默认在线播放,打开"视频边播边缓存"之后通过SDK播放;已经下载或缓存好的直接播放本地文件,不请求服务器,没有网络时也能播放
 * 视频播放时禁止自动锁屏
 */
@UnstableApi
class VideoViewModel : ObservableObject() {

    companion object {

        /** 视频接口转码、采集过程中查询的间隔(毫秒) */
        private const val VIDEO_POLL_MILLIS = 3000L

        /** 采集平台的原画视频下载过程中查询的间隔(毫秒) */
        private const val COLLECT_POLL_MILLIS = 2000L

        /** 等待采集平台的原画视频最多查询的次数 */
        private const val COLLECT_MAX_TIMES = 180

        /** 服务器没有开启视频采集,并且没有可以播放的视频时的提示 */
        private const val VIDEO_DISABLED_TIP = "该歌曲没有视频或服务器未开启视频功能"
    }

    /** 视频播放器,还没有开始播放时为null */
    var player by mutableStateOf<ExoPlayer?>(null)
        private set

    /** 播放进度和状态 */
    val state = VideoPlayerState()

    /** 是否全屏播放 */
    var isFullScreen by mutableStateOf(false)
        private set

    /** 不能播放时的提示,例如:服务器正在转码中 */
    var tip by mutableStateOf<String?>(null)
        private set

    /** 是否正在等待服务器转码、采集 */
    var isWaiting by mutableStateOf(false)
        private set

    /** 还没有收藏的歌曲只能播放原画,需要用户确认 */
    var showOriginalConfirm by mutableStateOf(false)

    /** 当前清晰度 */
    var quality by mutableIntStateOf(SettingStore.videoQuality)
        private set

    /** 是否只能播放原画(还没有收藏的歌曲) */
    var isOriginalOnly by mutableStateOf(false)
        private set

    /** 当前播放的视频是否已经下载 */
    var isDownloaded by mutableStateOf(false)
        private set

    /** 下载进度(0到100),没有下载时为null */
    var downloadPercent by mutableStateOf<Int?>(null)
        private set

    /** 显示下载的选项:只下载、保存到相册、发送文件 */
    var showDownloadMenu by mutableStateOf(false)

    /** 当前播放的视频 */
    private var source by mutableStateOf<FileSource?>(null)

    /** 视频开始播放的位置(秒) */
    private var startTime = 0.0

    /** 当前视频对应的歌曲 */
    private var music: MusicModel? = null

    /** 还没有收藏的歌曲,用户已经确认播放原画 */
    private var isOriginalConfirmed = false

    /** 转码、采集过程中查询的任务 */
    private var pollJob: Job? = null

    /** 获取视频信息的请求 */
    private var request: ApiHttpBase? = null

    /** 下载任务,离开页面时只取消回调,下载会继续 */
    private var downloadTask: FetchTask? = null

    /** 原画是否只有VIP才能看,清晰度选项上显示VIP标记 */
    val isOriginalVipOnly: Boolean
        get() = ServerConfig.isVipLimited

    /** 是否显示"保存到相册",服务器的设置;非VIP显示VIP标记,点击时提示需要VIP */
    val allowSaveAlbum: Boolean
        get() = ServerConfig.allowSaveAlbum

    /** 是否显示"发送文件",服务器的设置;非VIP显示VIP标记,点击时提示需要VIP */
    val allowSendFile: Boolean
        get() = ServerConfig.allowSendFile

    /** 保存到相册、发送文件是否只有VIP才能用,选项上显示VIP标记 */
    val isFileOutVipOnly: Boolean
        get() = ServerConfig.isVipLimited

    /** 是否可以点击下载按钮,已经下载时可以保存到相册或者发送文件才可以再点 */
    val canDownload: Boolean
        get() = this.source != null && this.downloadPercent == null && (!this.isDownloaded || this.allowSaveAlbum || this.allowSendFile)

    init {

        //视频播放时暂停音乐,禁止自动锁屏
        this.state.onIsPlayingChange = { isPlaying ->
            OrientationUtil.setKeepScreenOn(isPlaying)
            if (isPlaying && PlayerManager.isPlaying) {
                PlayerManager.pause()
            }
        }

        //播放结束之后按播放顺序继续
        this.state.onEnded = {
            this.onEnded()
        }
    }

    /*----------------------------------------------------------------------------------*/

    /** 进入视频页面,从头播放当前歌曲的视频 */
    fun enter() {
        this.music = PlayerManager.music
        this.startTime = 0.0
        this.isOriginalConfirmed = false
        this.load()
    }

    /** 离开视频页面,退出全屏,停止播放 */
    fun leave() {
        this.exitFullScreen()
        this.stop()
    }

    /** 是否正在显示这首歌曲的视频 */
    fun isShowing(music: MusicModel?): Boolean {
        val current = this.music ?: return false
        music ?: return false
        return current.isSameMusic(music)
    }

    /** 暂停视频:App进入后台时视频不允许后台播放,音乐开始播放时视频暂停 */
    fun pause() {
        this.player?.pause()
    }

    /** 全屏播放,手机上横向的视频切换到横屏 */
    fun enterFullScreen() {
        this.isFullScreen = true
        OrientationUtil.setFullScreen(true)
        OrientationUtil.rotate(landscape = !this.state.isPortraitVideo)
    }

    /** 退出全屏,手机上回到竖屏 */
    fun exitFullScreen() {
        if (!this.isFullScreen) {
            return
        }
        this.isFullScreen = false
        OrientationUtil.setFullScreen(false)
        OrientationUtil.rotate(landscape = false)
    }

    /** 切换清晰度,从当前位置继续播放 */
    fun onQualityChange(quality: Int) {
        if (quality == this.quality) {
            return
        }
        if (quality == SettingStore.VIDEO_QUALITY_ORIGINAL && this.isOriginalVipOnly) {
            Toast.show("原画需要VIP")
            return
        }
        this.quality = quality
        SettingStore.videoQuality = quality
        this.reloadKeepingTime()
    }

    /** 用户确认播放原画 */
    fun onOriginalConfirm() {
        this.isOriginalConfirmed = true
        this.load()
    }

    /** 切换到下一个有视频的歌曲 */
    fun onNextClick() {
        if (!this.playNext()) {
            Toast.show("播放列表里没有其他有视频的歌曲")
        }
    }

    /** 切换到上一个有视频的歌曲 */
    fun onPreviousClick() {
        val index = PlayerManager.previousVideoIndex
        if (index == null) {
            Toast.show("播放列表里没有其他有视频的歌曲")
            return
        }
        PlayerManager.selectWithoutPlaying(index)
        this.enter()
    }

    /** 播放播放列表里下一个有视频的歌曲,没有时返回false */
    private fun playNext(): Boolean {
        val index = PlayerManager.nextVideoIndex ?: return false
        PlayerManager.selectWithoutPlaying(index)
        this.enter()
        return true
    }

    /** 重新加载当前歌曲的视频,从当前位置继续播放 */
    private fun reloadKeepingTime() {
        this.player?.let { this.startTime = it.currentPosition / 1000.0 }
        this.load()
    }

    /*----------------------------------------------------------------------------------*/

    /** 加载视频 */
    private fun load() {
        this.stop()

        //非VIP播放流畅版时只临时改了清晰度,每次加载都按设置重新选择
        this.quality = SettingStore.videoQuality
        val music = this.music
        if (music == null) {
            this.tip = "没有正在播放的歌曲"
            return
        }
        if (music.id == 0L) {
            this.loadCollect(music)
            return
        }
        this.isOriginalOnly = false

        //当前清晰度已经下载或缓存好的直接播放,不请求服务器
        val finished = FileSdkUtil.finishedVideo(music.id, this.quality)
        if (finished != null) {
            this.play(FileSource.video(finished))
            return
        }

        //离线模式(或者系统没有网络)时不请求服务器,播放已经缓存的其他清晰度
        if (PlayerManager.isOffline || NetworkMonitor.current == NetworkType.NONE) {
            val any = FileSdkUtil.finishedVideo(music.id, null)
            if (any != null) {
                this.play(FileSource.video(any))
            } else {
                this.tip = "离线模式下没有缓存的视频"
            }
            return
        }
        if (music.videoState == 3 || music.videoState == 4) {
            this.tip = "该歌曲没有视频"
            return
        }
        this.requestVideo(music)
    }

    /** 还没有收藏的歌曲,播放采集平台的原画视频 */
    private fun loadCollect(music: MusicModel) {
        this.isOriginalOnly = true
        val source = FileSource.collectVideo(music)
        val infoUrl = music.collect?.videoInfoUrl
        if (source == null || infoUrl.isNullOrEmpty()) {

            //服务器没有开启视频采集时没有视频地址
            this.tip = if (ServerConfig.videoEnable) "该歌曲没有视频" else VIDEO_DISABLED_TIP
            return
        }

        //已经缓存过的不需要再确认,当前清晰度已经是原画时也不需要确认
        if (FileSdkUtil.cachedPath(source) != null) {
            this.play(source)
            return
        }
        if (!this.isOriginalConfirmed && this.quality != SettingStore.VIDEO_QUALITY_ORIGINAL) {
            this.tip = "当前音乐未收藏,只能播放原画版"
            this.showOriginalConfirm = true
            return
        }

        //查询采集平台的视频是否可以播放,第一次请求时开始下载
        this.isWaiting = true
        this.tip = "正在准备视频"
        this.pollJob = this.scope.launch {
            repeat(COLLECT_MAX_TIMES) {
                val msg = HttpUtil.text(infoUrl) ?: "网络连接失败,正在重试"
                if (!isActive) {
                    return@launch
                }
                if (msg == "OK") {
                    play(source)
                    return@launch
                }
                tip = msg
                if (msg.startsWith("采集失败")) {
                    isWaiting = false
                    return@launch
                }
                delay(COLLECT_POLL_MILLIS)
            }
            isWaiting = false
            tip = "视频准备超时,请稍后再试"
        }
    }

    /** 获取歌曲的视频,没有采集时服务器会开始采集,采集和转码过程中定时查询 */
    private fun requestVideo(music: MusicModel) {
        this.isWaiting = true
        if (this.tip == null) {
            this.tip = "正在获取视频"
        }
        this.request = MusicApi.video(music.id).hide().fail {
            this.onRequestError(music, it.msg ?: "获取视频失败")
        }.error {
            this.onRequestError(music, it)
        }.send { video ->
            if (this.music?.id != music.id) {
                return@send
            }
            this.onVideo(video, music)
        }
    }

    /** 获取视频信息失败(例如没有网络)时,有其他清晰度已经缓存好的先播放,没有时显示错误 */
    private fun onRequestError(music: MusicModel, msg: String) {
        if (this.music?.id != music.id) {
            return
        }
        val any = FileSdkUtil.finishedVideo(music.id, null)
        if (any != null) {
            this.play(FileSource.video(any))
            return
        }
        this.isWaiting = false
        this.tip = msg
    }

    /** 处理视频信息 */
    private fun onVideo(video: VideoModel, music: MusicModel) {
        when (video.state) {
            2 -> {//有视频
                var file = if (this.quality == SettingStore.VIDEO_QUALITY_ORIGINAL) video.original else video.smooth

                //非VIP不能看原画,改为播放流畅版(不修改设置,成为VIP之后还是播放原画)
                val smooth = video.smooth
                if (file?.vipOnly == true && smooth != null && smooth.vipOnly != true) {
                    this.quality = SettingStore.VIDEO_QUALITY_SMOOTH
                    file = smooth
                }
                if (file == null) {
                    this.waitAndRetry(music, "服务器正在转码中")
                    return
                }
                when (file.state) {
                    1 -> {
                        val source = FileSource.video(file, music)
                        if (source == null) {
                            this.showTip("视频地址不正确")
                            return
                        }
                        this.play(source)
                    }

                    2 -> this.showTip("视频转码失败")
                    else -> this.waitAndRetry(music, "服务器正在转码中")
                }
            }

            3, 4 -> this.showTip("该歌曲没有视频")
            5 -> this.showTip("视频采集失败")
            else -> this.waitAndRetry(music, "服务器正在采集视频")//未采集、采集中,请求接口时服务器已经开始采集
        }
    }

    /** 显示提示,不再等待 */
    private fun showTip(tip: String) {
        this.isWaiting = false
        this.tip = tip
    }

    /**
     * 显示提示,一段时间之后重新获取视频信息
     * 服务器没有开启视频采集时,未采集、采集中、转码中的视频不会再有变化,直接提示,不再查询
     */
    private fun waitAndRetry(music: MusicModel, tip: String) {
        if (!ServerConfig.videoEnable) {
            this.showTip(VIDEO_DISABLED_TIP)
            return
        }
        this.isWaiting = true
        this.tip = tip
        this.pollJob = this.scope.launch {
            delay(VIDEO_POLL_MILLIS)
            if (this@VideoViewModel.music?.id != music.id) {
                return@launch
            }
            requestVideo(music)
        }
    }

    /*----------------------------------------------------------------------------------*/

    /** 播放视频,已经下载或缓存好的直接播放本地文件,其他的按设置在线播放或者边播边缓存 */
    private fun play(source: FileSource) {
        val path = FileSdkUtil.cachedPath(source)
        val url = when {
            path != null -> File(path).toURI().toString()
            SettingStore.videoCache -> FileSdkUtil.open(source)
            else -> source.url
        }
        if (url == null) {
            this.showTip("视频地址不正确")
            return
        }
        this.source = source
        this.isDownloaded = FileSdkUtil.isDownloaded(source)
        this.isWaiting = false
        this.tip = null
        val attributes = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build()
        val player = ExoPlayer.Builder(DairoUI.context).setAudioAttributes(attributes, true).build()
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (this@VideoViewModel.player !== player) {
                    return
                }
                stop()
                showTip("视频加载失败,请检查网络")
            }
        })
        player.setMediaItem(MediaItem.fromUri(url), (this.startTime * 1000).toLong())
        player.prepare()
        this.state.attach(player)
        player.play()
        this.player = player
    }

    /** 播放结束:单曲循环时重新播放,其他按播放顺序播放下一个视频,播放列表里没有其他视频时重新播放 */
    private fun onEnded() {
        if (PlayerManager.orderType == PlayerOrderType.ONE_LOOP || !this.playNext()) {
            this.state.replay()
        }
    }

    /** 停止播放和查询,全屏时保持全屏(播放下一个视频时依然全屏) */
    private fun stop() {
        this.pollJob?.cancel()
        this.pollJob = null
        this.request?.cancel()
        this.request = null
        OrientationUtil.setKeepScreenOn(false)
        this.state.detach()
        this.player?.release()
        this.player = null
        this.source = null
        this.tip = null
        this.isWaiting = false
        this.downloadTask?.cancel()
        this.downloadTask = null
        this.downloadPercent = null
    }

    /*----------------------------------------------------------------------------------*/

    /** 下载按钮点击事件:可以保存到相册或者发送文件时先选择,否则直接下载 */
    fun onDownloadClick() {
        if (this.allowSaveAlbum || this.allowSendFile) {
            this.showDownloadMenu = true
            return
        }
        this.onDownloadAction(null)
    }

    /**
     * 选择下载的选项:没有下载时先下载当前清晰度的视频,下载完成之后保存到相册或者发送文件
     * 保存到相册、发送文件是VIP特权,非VIP提示需要VIP,也不下载
     * @param action 下载之后的操作,为null时只下载
     */
    fun onDownloadAction(action: VideoFileAction?) {
        if (action != null && !ServerConfig.checkFileOutVip(action.title)) {
            return
        }
        val source = this.source
        val path = source?.let { FileSdkUtil.cachedPath(it) }
        if (this.isDownloaded && action != null && path != null) {
            this.perform(action, path)
            return
        }
        this.download(action)
    }

    /**
     * 下载当前清晰度的视频,已经缓存好的直接转为下载,不会重新下载
     * @param action 下载完成之后的操作,为null时只下载
     */
    private fun download(action: VideoFileAction?) {
        val source = this.source ?: return
        this.downloadPercent = 0
        this.downloadTask = FileSdkUtil.fetchSave(source, progress = {
            this.downloadPercent = it
        }, finish = { path, error ->
            this.downloadPercent = null
            if (path == null) {
                Toast.show("下载失败:" + (error ?: ""))
                return@fetchSave
            }
            this.isDownloaded = true
            if (action != null) {
                this.perform(action, path)
            } else {
                Toast.show("下载完成")
            }
        })
    }

    /** 对下载好的视频文件执行保存到相册或者发送 */
    private fun perform(action: VideoFileAction, path: String) {
        val name = this.music?.let { it.nameText + " - " + it.artistText } ?: "视频"
        when (action) {
            VideoFileAction.SAVE_ALBUM -> this.saveToAlbum(path, name)
            VideoFileAction.SEND -> ShareUtil.sendFile(path, name, null)
        }
    }

    /** 保存到相册,Android 10开始不需要权限,Android 9及以下需要存储权限 */
    private fun saveToAlbum(path: String, name: String) {
        val context = DairoUI.context
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            AppActivity.current?.let { ActivityCompat.requestPermissions(it, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0) }
            Toast.show("没有存储权限,允许之后请再保存一次")
            return
        }
        val fileName = name.replace("/", "_") + ".mp4"
        this.scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val values = ContentValues().apply {
                            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Dairo音乐")
                            put(MediaStore.Video.Media.IS_PENDING, 1)
                        }
                        val resolver = context.contentResolver
                        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: error("创建文件失败")
                        resolver.openOutputStream(uri)?.use { output -> File(path).inputStream().use { it.copyTo(output) } }
                        values.clear()
                        values.put(MediaStore.Video.Media.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                    } else {
                        @Suppress("DEPRECATION")
                        val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Dairo音乐")
                        folder.mkdirs()
                        val target = File(folder, fileName)
                        File(path).copyTo(target, overwrite = true)
                        MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf("video/mp4"), null)
                    }
                }
            }
            Toast.show(if (result.isSuccess) "已保存到相册" else "保存到相册失败:" + (result.exceptionOrNull()?.message ?: ""))
        }
    }

    override fun onCleared() {
        this.leave()
        super.onCleared()
    }
}
