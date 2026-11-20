package cn.dairo.music.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import cn.dairo.music.api.MusicApi
import cn.dairo.music.api.http.ApiHttpBase
import cn.dairo.music.api.model.MusicModel
import cn.dairo.music.constants.FavoriteId
import cn.dairo.music.extension.artistText
import cn.dairo.music.extension.hasFile
import cn.dairo.music.extension.isSameMusic
import cn.dairo.music.extension.listId
import cn.dairo.music.extension.nameText
import cn.dairo.music.extension.settingFile
import cn.dairo.music.store.MusicStore
import cn.dairo.music.store.SettingStore
import cn.dairo.music.util.FileSdkUtil
import cn.dairo.music.util.FileSource
import cn.dairo.music.util.HttpUtil
import cn.dairo.music.util.NetworkMonitor
import cn.dairo.music.util.NetworkType
import cn.dairo.music.util.PlayCountUtil
import cn.dairo.ui.root.Toast
import cn.dairo.ui.util.LocalObjectUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 播放器管理,整个App共用一个播放器
 * 每首歌开始播放前按MusicFileResolver的规则选择文件:已经下载的、设置的音质已经缓存的直接播放本地文件,其他的在线播放(边播边缓存)
 * 离线模式:在线播放开始之前超时(5秒)或者失败、系统没有网络时进入离线模式,之后只播放本地有文件的歌曲,不用每首歌都等超时
 * 离线标记只保存在内存里;切换播放歌单、网络类型变化(WiFi、数据流量、没有网络之间切换)、用户在播放页取消时退出离线模式
 * 后台播放、通知栏和锁屏的播放控制由PlaybackService提供
 */
@UnstableApi
object PlayerManager {

    /** 播放信息的本地缓存名 */
    private const val KEY_PLAYING_INFO = "playing_info"

    /** 播放列表 */
    var playList by mutableStateOf(emptyList<MusicModel>())
        private set

    /** 当前播放的序号 */
    var index by mutableIntStateOf(0)
        private set

    /** 播放列表的来源,参考FavoriteId */
    var favoriteId by mutableLongStateOf(FavoriteId.NONE)
        private set

    /** 播放顺序 */
    var orderType by mutableStateOf(PlayerOrderType.LIST_LOOP)
        private set

    /** 是否正在播放,缓冲中也算正在播放 */
    var isPlaying by mutableStateOf(false)
        private set

    /** 是否正在加载 */
    var isLoading by mutableStateOf(false)
        private set

    /** 采集平台的歌曲还没有采集完成时,采集服务返回的进度说明,不需要等待采集时为null */
    var collectMsg by mutableStateOf<String?>(null)
        private set

    /** 是否处于离线模式 */
    var isOffline by mutableStateOf(false)
        private set

    /** 进入离线模式时的网络类型,网络类型变化之后退出离线模式 */
    private var offlineNetwork: NetworkType? = null

    /** 在线播放开始之前的超时检查,开始播放或者切换歌曲时取消 */
    private var startTimeoutJob: Job? = null

    /** 当前歌曲是否已经准备好开始播放,开始播放之后的失败不算断网 */
    private var isStarted = false

    /** 在线播放途中网络中断时等待缓冲的检查,恢复播放或者切换歌曲时取消 */
    private var stallJob: Job? = null

    /** 在线播放途中等待缓冲的超时时间(毫秒) */
    private const val STALL_TIMEOUT_MILLIS = 5_000L

    /** 在线播放开始之前的超时时间(毫秒),超时之后进入离线模式 */
    private const val START_TIMEOUT_MILLIS = 5_000L

    /** 当前播放的歌曲 */
    val music: MusicModel?
        get() = this.playList.getOrNull(this.index)

    /** 播放器 */
    lateinit var player: ExoPlayer
        private set

    /** 给通知栏、锁屏使用的播放器,上一曲、下一曲、播放、暂停都交给PlayerManager处理 */
    val sessionPlayer: Player by lazy { SessionPlayer(this.player) }

    /** 当前加载到播放器里的歌曲,没有加载时为null(对应iOS的player为nil) */
    private var current: LoadedMusic? = null

    /** 这一遍播放是否被拖动到了快结束的位置,拖动到结尾的不算播放次数 */
    private var isSeekedToEnd = false

    /** 拖动进度之后离结尾少于这个时间(秒)时,播放结束不算播放次数 */
    private const val PLAY_COUNT_MIN_SECONDS = 10.0

    /** 连续播放失败的次数,超过列表里的歌曲数时停止播放 */
    private var errorCount = 0

    /** 歌曲还没有播放地址时,获取歌曲详情的请求 */
    private var detailRequest: ApiHttpBase? = null

    /** 等待采集完成的任务,切换歌曲时取消 */
    private var collectJob: Job? = null

    /** 刷新播放进度的任务 */
    private var progressJob: Job? = null

    /** 等待采集完成的最多次数,每秒查询一次 */
    private const val COLLECT_MAX_TIMES = 180

    private val scope = MainScope()

    private val progress: PlayerProgress
        get() = PlayerProgress

    /**
     * 创建播放器,App启动时调用
     * 来电等打断时由播放器自动暂停,结束之后自动继续;拔出耳机时暂停
     */
    fun init(context: Context) {

        //在音频处理链上接一个处理节点,给播放页的环形波形计算频谱
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioOutputPlaybackParameters(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(TeeAudioProcessor(AudioSpectrum)))
                    .build()
            }
        }
        val attributes = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build()
        this.player = ExoPlayer.Builder(context, renderersFactory)
            .setAudioAttributes(attributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        this.player.addListener(PlayerListener())
    }

    /*----------------------------------------------------------------------------------*/

    /** 恢复当前账号上次的播放列表,不自动播放 */
    fun restore() {
        val info = SettingStore.accountObjectName(KEY_PLAYING_INFO)?.let { LocalObjectUtil.read<PlayingInfo>(it) } ?: PlayingInfo()
        this.playList = info.playList
        this.index = if (info.index in info.playList.indices) info.index else 0
        this.favoriteId = info.favoriteId
        this.orderType = info.orderType
        this.progress.currentTime = info.currentTime
        this.progress.duration = (this.music?.duration ?: 0) / 1000.0
    }

    /** 保存播放信息 */
    fun savePlayingInfo() {
        val name = SettingStore.accountObjectName(KEY_PLAYING_INFO) ?: return
        val info = PlayingInfo(this.playList, this.index, this.favoriteId, this.progress.currentTime, this.orderType)
        LocalObjectUtil.write(info, name)
    }

    /*----------------------------------------------------------------------------------*/

    /**
     * 播放一个列表
     * @param favoriteId 播放列表的来源,参考FavoriteId
     */
    fun prepareAndPlay(list: List<MusicModel>, index: Int = 0, favoriteId: Long) {
        if (index !in list.indices) {
            return
        }

        //切换了播放歌单时退出离线模式,在同一个歌单里点另一首歌不算
        if (favoriteId != this.favoriteId || list.map { it.listId } != this.playList.map { it.listId }) {
            this.exitOffline()
        }
        this.playList = list
        this.index = index
        this.favoriteId = favoriteId
        this.errorCount = 0
        this.load(0.0)
    }

    /** 播放列表里的某一首 */
    fun playAt(index: Int) {
        if (index !in this.playList.indices) {
            return
        }
        this.index = index
        this.errorCount = 0
        this.load(0.0)
    }

    /** 播放或暂停 */
    fun playOrPause() {
        if (this.isPlaying) {
            this.pause()
        } else {
            this.play()
        }
    }

    /** 继续播放,还没有加载歌曲时从上次的位置开始播放 */
    fun play() {
        if (this.current != null) {
            if (this.player.playbackState == Player.STATE_ENDED) {
                this.player.seekTo(0)
            }
            this.player.play()
            return
        }
        this.load(this.progress.currentTime)
    }

    /** 暂停 */
    fun pause() {

        //歌曲还在采集、获取详情时取消,避免准备好之后自动播放,之后点播放时重新加载
        if (this.current == null) {
            this.releasePlayer()
        }
        this.player.pause()
        this.savePlayingInfo()
    }

    /**
     * 下一曲
     * @param auto 是否播放结束之后自动切换,单曲循环时自动切换会重新播放当前歌曲
     */
    fun next(auto: Boolean = false) {
        if (this.playList.isEmpty()) {
            return
        }
        if (auto && this.orderType == PlayerOrderType.ONE_LOOP) {
            this.seek(0.0)
            this.player.play()
            return
        }
        this.index = this.nextIndex
        this.load(0.0)
    }

    /** 上一曲 */
    fun previous() {
        if (this.playList.isEmpty()) {
            return
        }
        this.index = (this.index - 1 + this.playList.size) % this.playList.size
        this.load(0.0, direction = -1)
    }

    /** 退出离线模式,下一首开始尝试在线播放,正在播放的歌曲不中断 */
    fun exitOffline() {
        this.isOffline = false
        this.offlineNetwork = null
    }

    /** 下一曲的序号 */
    private val nextIndex: Int
        get() {
            val count = this.playList.size
            if (this.orderType == PlayerOrderType.RANDOM_LOOP && count > 1) {
                var index = this.index
                while (index == this.index) {
                    index = (0 until count).random()
                }
                return index
            }
            return (this.index + 1) % count
        }

    /** 下一首有视频的歌曲序号,随机播放时随机选一首,其他按列表顺序往后找,没有其他有视频的歌曲时返回null */
    val nextVideoIndex: Int?
        get() {
            val indices = this.playList.indices.filter { it != this.index && this.playList[it].videoState == 2 }
            if (indices.isEmpty()) {
                return null
            }
            if (this.orderType == PlayerOrderType.RANDOM_LOOP) {
                return indices.random()
            }
            return indices.firstOrNull { it > this.index } ?: indices.first()
        }

    /** 上一首有视频的歌曲序号,随机播放时随机选一首,其他按列表顺序往前找,没有其他有视频的歌曲时返回null */
    val previousVideoIndex: Int?
        get() {
            val indices = this.playList.indices.filter { it != this.index && this.playList[it].videoState == 2 }
            if (indices.isEmpty()) {
                return null
            }
            if (this.orderType == PlayerOrderType.RANDOM_LOOP) {
                return indices.random()
            }
            return indices.lastOrNull { it < this.index } ?: indices.last()
        }

    /**
     * 切换当前歌曲但不播放音乐,视频页面切换下一个视频时使用
     * @param addRecent 是否加入最近播放,定时停止时切换的歌曲还没有播放,不加入
     */
    fun selectWithoutPlaying(index: Int, addRecent: Boolean = true) {
        if (index !in this.playList.indices) {
            return
        }
        this.releasePlayer()
        this.index = index
        this.progress.currentTime = 0.0
        this.progress.duration = (this.music?.duration ?: 0) / 1000.0
        val music = this.music
        if (music != null && addRecent) {
            MusicStore.addRecent(music)
        }
        this.savePlayingInfo()
    }

    /** 跳转到指定位置 */
    fun seek(seconds: Double) {
        val duration = this.progress.duration
        this.isSeekedToEnd = duration > 0 && duration - seconds < PLAY_COUNT_MIN_SECONDS
        this.progress.currentTime = seconds
        if (this.current != null) {
            this.player.seekTo((seconds * 1000).toLong())
        }
    }

    /** 切换到下一种播放顺序 */
    fun switchOrderType() {
        this.orderType = this.orderType.next
        this.savePlayingInfo()
    }

    /** 从播放列表移除 */
    fun remove(index: Int) {
        if (index !in this.playList.indices) {
            return
        }
        this.playList = this.playList.toMutableList().also { it.removeAt(index) }
        if (this.playList.isEmpty()) {
            this.stop()
            return
        }
        if (index < this.index) {
            this.index -= 1
        } else if (index == this.index) {

            //移除的是正在播放的歌曲,接着播放下一首
            this.index = minOf(index, this.playList.size - 1)
            if (this.isPlaying) {
                this.load(0.0)
            } else {
                this.releasePlayer()
                this.progress.currentTime = 0.0
                this.progress.duration = (this.music?.duration ?: 0) / 1000.0
            }
        }
        this.savePlayingInfo()
    }

    /** 清空播放列表,停止播放 */
    fun stop() {
        this.releasePlayer()
        this.playList = emptyList()
        this.index = 0
        this.favoriteId = FavoriteId.NONE
        this.progress.currentTime = 0.0
        this.progress.duration = 0.0
        this.savePlayingInfo()
    }

    /** 采集平台的歌曲加入曲库之后,更新播放列表里的歌曲ID */
    fun onCollected(music: MusicModel, musicId: Long) {
        var changed = false
        this.playList = this.playList.map {
            if (it.id == 0L && it.isSameMusic(music)) {
                changed = true
                it.copy(id = musicId)
            } else {
                it
            }
        }
        if (changed) {
            this.savePlayingInfo()
        }
    }

    /*----------------------------------------------------------------------------------*/

    /**
     * 加载当前歌曲并播放
     * @param startTime 开始播放的位置(秒)
     * @param direction 离线时当前歌曲不能播放,往哪个方向找能播放的歌曲,1:往后 -1:往前
     */
    private fun load(startTime: Double, direction: Int = 1) {
        this.releasePlayer()
        this.isSeekedToEnd = false
        val music = this.music ?: return
        this.progress.currentTime = startTime
        this.progress.duration = (music.duration ?: 0) / 1000.0

        //本地有合适的文件(已经下载的、设置的音质已经缓存的)直接播放,不需要联网,不检查网络
        val local = MusicFileResolver.local(music, anyQuality = false)
        if (local != null) {
            this.startPlay(music, local.url, startTime, isOnline = false)
            return
        }
        this.checkOffline()
        if (this.isOffline) {
            this.loadOffline(music, startTime, direction)
            return
        }
        if (music.id == 0L) {
            this.loadCollect(music, startTime)
            return
        }
        val source = music.settingFile?.let { FileSource.music(music, it) }
        if (source == null) {

            //歌曲列表里的信息可能是采集完成之前的,重新获取歌曲详情
            this.loadDetail(music, startTime)
            return
        }
        this.startPlay(music, source, startTime)
    }

    /** 切换歌曲前检查离线模式:网络类型变化之后退出离线模式,系统没有网络时进入离线模式 */
    private fun checkOffline() {
        val network = NetworkMonitor.current
        val offlineNetwork = this.offlineNetwork
        if (this.isOffline && offlineNetwork != null && network != offlineNetwork && network != NetworkType.UNKNOWN) {
            this.exitOffline()
        }
        if (!this.isOffline && network == NetworkType.NONE) {
            this.enterOffline("当前没有网络,已切换到离线模式")
        }
    }

    /** 进入离线模式,记下当时的网络类型 */
    private fun enterOffline(msg: String) {
        if (this.isOffline) {
            return
        }
        this.isOffline = true
        this.offlineNetwork = NetworkMonitor.current
        Toast.show(msg)
    }

    /** 离线时播放已经缓存的其他音质,没有缓存的按播放顺序找下一首本地有文件的歌曲 */
    private fun loadOffline(music: MusicModel, startTime: Double, direction: Int) {
        val local = MusicFileResolver.local(music, anyQuality = true)
        if (local != null) {
            this.startPlay(music, local.url, startTime, isOnline = false)
            return
        }
        val index = this.offlinePlayableIndex(direction)
        if (index == null) {
            this.stopOffline()
            return
        }
        this.index = index
        this.load(0.0, direction)
    }

    /**
     * 离线时按播放顺序找一首本地有文件的歌曲,当前这首不能播放,最多找一遍列表,找不到时返回null
     * 单曲循环:不切换到其他歌曲;列表循环:按方向依次往后(往前)找;随机播放:从本地有文件的歌曲里随机选
     */
    private fun offlinePlayableIndex(direction: Int): Int? {
        if (this.orderType == PlayerOrderType.ONE_LOOP) {
            return null
        }
        val (musicIds, collectKeys) = FileSdkUtil.localMusicKeys()
        val isPlayable = { music: MusicModel ->
            if (music.id != 0L) {
                musicIds.contains(music.id)
            } else {
                FileSource.collectMusic(music)?.let { collectKeys.contains(it.options.cacheKey ?: "") } ?: false
            }
        }
        val count = this.playList.size
        if (this.orderType == PlayerOrderType.RANDOM_LOOP) {
            return this.playList.indices.filter { it != this.index && isPlayable(this.playList[it]) }.randomOrNull()
        }
        for (step in 1 until maxOf(count, 1)) {
            val index = ((this.index + direction * step) % count + count) % count
            if (isPlayable(this.playList[index])) {
                return index
            }
        }
        return null
    }

    /** 离线时整个列表都没有可以播放的歌曲,停止播放,停在当前歌曲 */
    private fun stopOffline() {
        this.releasePlayer()
        this.savePlayingInfo()
        Toast.show("离线模式下没有可以播放的歌曲")
    }

    /** 采集平台的歌曲,还没有缓存时要等采集服务采集完成之后才能播放 */
    private fun loadCollect(music: MusicModel, startTime: Double) {
        val source = FileSource.collectMusic(music)
        val collectUrl = music.collect?.url
        if (source == null || collectUrl == null) {
            this.onError("歌曲没有播放地址")
            return
        }
        this.waitCollect(collectUrl, source, startTime)
    }

    /** 在线播放:按设置边播边缓存,或者直接在线播放 */
    private fun startPlay(music: MusicModel, source: FileSource, startTime: Double) {
        val url = if (SettingStore.musicCache) FileSdkUtil.open(source) else source.url
        if (url == null) {
            this.onError("歌曲加载失败")
            return
        }
        this.startPlay(music, url, startTime, isOnline = true)
    }

    /**
     * 开始播放指定地址的歌曲
     * @param isOnline 是否在线播放,在线播放开始之前超时或者失败时进入离线模式
     */
    private fun startPlay(music: MusicModel, url: String, startTime: Double, isOnline: Boolean) {
        val loaded = LoadedMusic(isOnline, startTime)
        this.current = loaded
        this.player.setMediaItem(this.mediaItem(music, url), (startTime * 1000).toLong())
        this.player.prepare()
        this.player.play()
        this.startProgress()
        MusicStore.addRecent(music)
        this.savePlayingInfo()
        if (isOnline) {
            this.startTimeoutJob = this.scope.launch {
                delay(START_TIMEOUT_MILLIS)
                if (current !== loaded || isStarted) {
                    return@launch
                }
                onStartFailed(startTime)
            }
        }
    }

    /** 生成播放器的歌曲,带上通知栏、锁屏显示的歌名、歌手和封面 */
    private fun mediaItem(music: MusicModel, url: String): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(music.nameText)
            .setArtist(music.artistText)
            .setAlbumTitle(music.album)
            .setArtworkUri(music.logo?.let { FileSdkUtil.localUrl(FileSource.image(it)) }?.toUri())
            .build()
        return MediaItem.Builder().setUri(url).setMediaId(music.listId).setMediaMetadata(metadata).build()
    }

    /** 定时刷新播放进度 */
    private fun startProgress() {
        if (this.progressJob?.isActive == true) {
            return
        }
        this.progressJob = this.scope.launch {
            while (isActive) {
                if (current != null && !progress.isDragging && player.playbackState != Player.STATE_IDLE) {
                    progress.currentTime = player.currentPosition / 1000.0
                }
                delay(500)
            }
        }
    }

    /** 开始等待缓冲的超时检查,已经在检查时不重新开始 */
    private fun startStallTimeout() {
        if (this.stallJob != null) {
            return
        }
        val loaded = this.current
        this.stallJob = this.scope.launch {
            delay(STALL_TIMEOUT_MILLIS)
            if (current !== loaded || !player.playWhenReady || player.playbackState != Player.STATE_BUFFERING) {
                return@launch
            }
            onStalled()
        }
    }

    /**
     * 在线播放途中网络中断,等待缓冲超时:提示并播放下一曲
     * 播放途中的超时不算断网,不进入离线模式
     */
    private fun onStalled() {
        this.onError("网络连接超时")
    }

    /**
     * 在线播放开始之前超时或者失败,进入离线模式,当前歌曲按离线规则重新加载
     * 播放途中的失败不算断网
     */
    private fun onStartFailed(startTime: Double) {
        this.releasePlayer()
        this.enterOffline("网络连接超时,已切换到离线模式")
        this.load(startTime)
    }

    /**
     * 等待采集服务采集完成,每秒查询一次进度,第一次查询时采集服务开始采集
     * 查询地址是"播放地址/collect_info",采集完成时返回OK,没有完成时返回进度说明
     */
    private fun waitCollect(collectUrl: String, source: FileSource, startTime: Double) {
        val infoUrl = "$collectUrl/collect_info"
        this.isLoading = true
        this.collectMsg = "准备采集"
        this.collectJob = this.scope.launch {
            repeat(COLLECT_MAX_TIMES) {
                val msg = HttpUtil.text(infoUrl)
                if (!isActive) {
                    return@launch
                }
                if (msg == "OK") {
                    collectMsg = null
                    music?.let { startPlay(it, source, startTime) }
                    return@launch
                }
                collectMsg = msg ?: "网络连接失败,正在重试"
                delay(1000)
            }
            collectMsg = null
            onError("采集超时")
        }
    }

    /** 歌曲列表里的信息可能是采集完成之前的,重新获取歌曲详情之后再播放;网络连接失败时进入离线模式 */
    private fun loadDetail(music: MusicModel, startTime: Double) {
        this.isLoading = true
        this.detailRequest = MusicApi.detail(music.id).hide().fail {
            this.isLoading = false
            this.onError(it.msg ?: "获取歌曲信息失败")
        }.error {
            this.isLoading = false
            this.onStartFailed(startTime)
        }.send { detail ->
            if (this.music?.id != detail.id) {
                return@send
            }
            this.isLoading = false
            if (!detail.hasFile) {
                this.onError("歌曲还在处理中,请稍后再试")
                return@send
            }
            this.playList = this.playList.toMutableList().also { it[this.index] = detail.copy(favoriteId = music.favoriteId) }
            this.load(startTime)
        }
    }

    /** 播放器的状态变化,以播放器的实际状态为准(来电等情况播放器会暂停) */
    private fun updateStatus() {
        val loaded = this.current
        if (loaded == null) {
            this.isPlaying = false
            return
        }
        val state = this.player.playbackState
        val isWaiting = this.player.playWhenReady && state == Player.STATE_BUFFERING
        this.isPlaying = this.player.playWhenReady && (state == Player.STATE_READY || state == Player.STATE_BUFFERING)
        this.isLoading = isWaiting

        //在线播放途中一直在等待缓冲(网络中断)时,超时之后处理,不能一直卡在那里
        if (loaded.isOnline && this.isStarted && isWaiting) {
            this.startStallTimeout()
        } else {
            this.stallJob?.cancel()
            this.stallJob = null
        }
    }

    /** 歌曲播放结束,自动播放下一曲;定时停止正在等歌曲播放结束时,切换到下一曲但不播放 */
    private fun onSongEnd() {

        //自然播放到结尾的算一次播放
        val music = this.music
        if (!this.isSeekedToEnd && music != null) {
            PlayCountUtil.add(music)
        }
        if (!SleepTimer.consumeSongEnd()) {
            this.next(auto = true)
            return
        }
        if (this.orderType == PlayerOrderType.ONE_LOOP) {
            this.seek(0.0)
            this.pause()
        } else {
            this.selectWithoutPlaying(this.nextIndex, addRecent = false)
        }
    }

    /** 播放失败时自动播放下一曲,整个列表都失败时停止 */
    private fun onError(msg: String) {
        this.releasePlayer()
        this.errorCount += 1
        if (this.errorCount >= this.playList.size) {
            Toast.show(msg)
            return
        }
        Toast.show("$msg,自动播放下一曲")
        this.index = this.nextIndex
        this.load(0.0)
    }

    /** 释放当前歌曲 */
    private fun releasePlayer() {
        this.current = null
        this.isStarted = false
        this.stallJob?.cancel()
        this.stallJob = null
        this.startTimeoutJob?.cancel()
        this.startTimeoutJob = null
        this.detailRequest?.cancel()
        this.detailRequest = null
        this.collectJob?.cancel()
        this.collectJob = null
        this.collectMsg = null
        this.progressJob?.cancel()
        this.progressJob = null
        this.player.stop()
        this.player.clearMediaItems()
        this.isPlaying = false
        this.isLoading = false
    }

    /**
     * 加载到播放器里的歌曲
     * @param isOnline 在线播放开始之前失败时进入离线模式
     * @param startTime 开始播放的位置(秒)
     */
    private class LoadedMusic(val isOnline: Boolean, val startTime: Double)

    /**
     * 监听播放器的状态
     */
    private class PlayerListener : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            current ?: return
            if (playbackState == Player.STATE_READY) {
                if (!isStarted) {
                    isStarted = true
                    errorCount = 0
                    startTimeoutJob?.cancel()
                }
                val duration = player.duration
                if (duration != C.TIME_UNSET && duration > 0) {
                    progress.duration = duration / 1000.0
                }
            }
            updateStatus()
            if (playbackState == Player.STATE_ENDED) {
                onSongEnd()
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            updateStatus()
        }

        override fun onPlayerError(error: PlaybackException) {
            val loaded = current ?: return
            if (loaded.isOnline && !isStarted) {
                onStartFailed(loaded.startTime)
            } else {
                onError(if (isStarted) "网络连接失败" else "歌曲加载失败")
            }
        }
    }

    /**
     * 给通知栏、锁屏、耳机按键使用的播放器
     * 播放列表由PlayerManager管理,播放器里只有当前一首,上一曲、下一曲按钮要自己提供
     */
    private class SessionPlayer(player: Player) : ForwardingPlayer(player) {

        override fun getAvailableCommands(): Player.Commands {
            return super.getAvailableCommands().buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_PLAY_PAUSE)
                .build()
        }

        override fun isCommandAvailable(command: Int): Boolean {
            return this.availableCommands.contains(command)
        }

        override fun play() {
            PlayerManager.play()
        }

        override fun setPlayWhenReady(playWhenReady: Boolean) {
            if (playWhenReady) PlayerManager.play() else PlayerManager.pause()
        }

        override fun pause() {
            PlayerManager.pause()
        }

        override fun seekToNext() {
            PlayerManager.next()
        }

        override fun seekToNextMediaItem() {
            PlayerManager.next()
        }

        override fun seekToPrevious() {
            PlayerManager.previous()
        }

        override fun seekToPreviousMediaItem() {
            PlayerManager.previous()
        }

        override fun seekTo(positionMs: Long) {
            PlayerManager.seek(positionMs / 1000.0)
        }
    }
}
