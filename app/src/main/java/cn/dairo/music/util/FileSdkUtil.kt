package cn.dairo.music.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import cn.dairo.file.sdk.CacheInfo
import cn.dairo.file.sdk.CacheMode
import cn.dairo.file.sdk.CacheQuery
import cn.dairo.file.sdk.DownloadState
import cn.dairo.file.sdk.FetchListener
import cn.dairo.file.sdk.FetchTask
import cn.dairo.file.sdk.FileSdk
import cn.dairo.file.sdk.GroupStats
import cn.dairo.file.sdk.KeyMode
import cn.dairo.file.sdk.LocalSource
import cn.dairo.file.sdk.Options
import cn.dairo.file.sdk.SdkConfig
import cn.dairo.file.sdk.SdkException
import cn.dairo.music.api.model.MusicFileModel
import cn.dairo.music.api.model.MusicModel
import cn.dairo.music.api.model.VideoFileModel
import cn.dairo.music.extension.artistText
import cn.dairo.music.extension.nameText
import cn.dairo.music.extension.qualityText
import cn.dairo.music.store.SettingStore
import cn.dairo.ui.DairoUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

/**
 * 文件下载缓存SDK,整个App共用一个实例
 */
object FileSdkUtil {

    private const val TAG = "FileSdkUtil"

    /** SDK数据目录名 */
    private const val ROOT_FOLDER = "dairo-file-sdk"

    /** 歌曲缓存分组 */
    const val GROUP_MUSIC = "music"

    /** 图片缓存分组 */
    const val GROUP_IMAGE = "image"

    /** 视频缓存分组 */
    const val GROUP_VIDEO = "video"

    /** SDK实例的创建结果,创建失败时,在使用的地方抛出错误 */
    private val result: Result<FileSdk> by lazy { runCatching { create() } }

    /**
     * 获取SDK实例
     */
    fun get(): FileSdk {
        return result.getOrThrow()
    }

    /**
     * App回到前台时调用,本机HTTP服务失效时会重启,之前拿到的本机地址依然有效
     */
    fun checkServer() {
        try {
            get().checkServer()
        } catch (e: Throwable) {
            Log.e(TAG, "检查SDK本机HTTP服务失败", e)
        }
    }

    /**
     * App进入后台时调用
     */
    fun flush() {
        try {
            get().flush()
        } catch (e: Throwable) {
            Log.e(TAG, "SDK保存数据失败", e)
        }
    }

    /**
     * 获取已经缓存的文件路径,没有缓存时返回null
     */
    fun cachedPath(source: FileSource): String? {
        return runCatching { get().cachedPath(source.url, source.options) }.getOrNull()
    }

    /**
     * 请求图片用的线程,请求的都是SDK的本机服务,真正的下载并发由SDK控制
     */
    private val imageExecutor = Executors.newCachedThreadPool()

    /**
     * 读取图片数据,失败时重新获取一次地址再试(App在后台放久之后本机服务可能失效,SDK获取地址时会自动重启服务)
     * @param url 图片的下载地址
     */
    suspend fun imageData(url: String): ByteArray? {
        repeat(2) {
            imageDataOnce(url)?.let { return it }
            coroutineContext.ensureActive()
        }
        return null
    }

    /**
     * 读取一次图片数据:已经缓存好的直接读文件,不经过本机HTTP服务;没有缓存或者正在下载时通过本机HTTP服务下载
     */
    private suspend fun imageDataOnce(url: String): ByteArray? {
        val source = FileSource.image(url)
        val local = withContext(Dispatchers.IO) {
            runCatching { get().localSource(source.url, source.options) }.getOrNull()
        }
        return when (local) {
            is LocalSource.File -> {
                val data = withContext(Dispatchers.IO) {
                    runCatching { File(local.path).readBytes() }.getOrNull()
                }
                if (data != null) {
                    return data
                }

                //拿到路径之后文件刚好被清理了,重新获取会返回本机HTTP地址
                val retry = withContext(Dispatchers.IO) {
                    runCatching { get().localSource(source.url, source.options) }.getOrNull()
                }
                if (retry !is LocalSource.Http) {
                    return null
                }
                httpData(retry.url)
            }

            is LocalSource.Http -> httpData(local.url)
            null -> null
        }
    }

    /**
     * 通过本机HTTP服务下载,状态码不是200时返回null;页面关闭时断开连接,SDK会自动取消下载
     */
    private suspend fun httpData(url: String): ByteArray? = suspendCancellableCoroutine { cont ->
        val connection = runCatching { URL(url).openConnection() as HttpURLConnection }.getOrNull()
        if (connection == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        cont.invokeOnCancellation {
            imageExecutor.execute { connection.disconnect() }
        }
        imageExecutor.execute {
            val data = runCatching {
                connection.connectTimeout = 15_000
                connection.readTimeout = 60_000
                if (connection.responseCode != 200) {
                    null
                } else {
                    connection.inputStream.use { input ->
                        val output = ByteArrayOutputStream()
                        input.copyTo(output)
                        output.toByteArray()
                    }
                }
            }.getOrNull()
            connection.disconnect()
            if (cont.isActive) {
                cont.resume(data)
            }
        }
    }

    /**
     * 获取给播放器使用的本机地址,支持边下边播
     */
    fun open(source: FileSource): String? {
        return runCatching { get().open(source.url, source.options) }.getOrNull()
    }

    /**
     * 获取本机地址,不等待下载,通知栏封面等只能传地址的地方使用
     */
    fun localUrl(source: FileSource): String? {
        return runCatching { get().localUrl(source.url, source.options) }.getOrNull()
    }

    /**
     * 是否已经永久下载完成
     */
    fun isDownloaded(source: FileSource): Boolean {
        return runCatching {
            val sdk = get()
            val info = sdk.get(sdk.cacheId(source.url, source.options)) ?: return false
            info.mode == CacheMode.SAVE && info.state == DownloadState.FINISH
        }.getOrDefault(false)
    }

    /**
     * 下载并永久保存,已经缓存完成时直接转为永久保存,不会重新下载
     * 回调都在主线程,取消之后不会再回调,但下载任务会继续
     * @param progress 下载进度(0到100)
     * @param finish 下载结束,成功时返回文件路径,失败时返回错误信息
     */
    fun fetchSave(source: FileSource, progress: (percent: Int) -> Unit, finish: (path: String?, error: String?) -> Unit): FetchTask? {
        return try {
            val options = source.options.copy(cache = CacheMode.SAVE)
            get().fetch(source.url, options, FileSdkFetchListener(progress, finish))
        } catch (e: Throwable) {
            finish(null, e.message)
            null
        }
    }

    /**
     * 各分组的统计
     * @param mode CACHE:缓存 SAVE:永久下载
     */
    fun stats(mode: CacheMode): List<GroupStats> {
        return runCatching { get().stats(mode) }.getOrDefault(emptyList())
    }

    /*----------------------------------------------------------------------------------*/

    /**
     * 歌曲文件缓存key的前缀,后面加文件ID;同一首歌不同音质的文件前缀相同
     */
    fun musicKeyPrefix(musicId: Long): String {
        return "/music/$musicId/file/"
    }

    /**
     * 视频文件缓存key的前缀,后面加文件ID;同一首歌不同清晰度的视频前缀相同
     */
    fun videoKeyPrefix(musicId: Long): String {
        return "/video/$musicId/file/"
    }

    /**
     * 视频清晰度的名称,文件名里用来区分清晰度
     */
    fun videoQualityText(quality: Int): String {
        return if (quality == SettingStore.VIDEO_QUALITY_ORIGINAL) "原画" else "流畅"
    }

    /**
     * 已经下载或者边播边缓存完成的视频
     * @param quality 清晰度,为null时不限清晰度,有多个时优先永久下载的
     */
    fun finishedVideo(musicId: Long, quality: Int?): CacheInfo? {
        val prefix = videoKeyPrefix(musicId)
        val qualityName = quality?.let { "[" + videoQualityText(it) + "]" }
        return list(GROUP_VIDEO, null)
            .filter { it.key.startsWith(prefix) && it.state == DownloadState.FINISH && it.path != null }
            .filter { info -> qualityName?.let { (info.name ?: "").contains(it) } ?: true }
            .sortedBy { if (it.mode == CacheMode.SAVE) 0 else 1 }
            .firstOrNull()
    }

    /**
     * 添加永久保存的后台下载任务,已经缓存完成时直接转为永久保存,不会重新下载
     */
    fun download(source: FileSource): Boolean {
        return try {
            get().download(source.url, source.options.copy(cache = CacheMode.SAVE))
            true
        } catch (e: Throwable) {
            Log.e(TAG, "添加下载任务失败", e)
            false
        }
    }

    /**
     * 当前服务器永久下载的文件,包括还没有下载完成的,最近添加的在前面
     * @param group 分组
     */
    fun downloads(group: String): List<CacheInfo> {
        return list(group, CacheMode.SAVE)
    }

    /**
     * 当前服务器的缓存记录,包括还没有下载完成的,最近添加的在前面
     * @param mode 缓存方式,为null时包括缓存和永久下载
     */
    private fun list(group: String, mode: CacheMode?): List<CacheInfo> {
        val sdk = runCatching { get() }.getOrNull() ?: return emptyList()
        val serverId = SettingStore.serverId
        val result = ArrayList<CacheInfo>()
        var offset = 0UL
        val limit = 500UL
        while (true) {
            val page = runCatching { sdk.list(CacheQuery(group = group, mode = mode, offset = offset, limit = limit)) }.getOrNull() ?: break
            result.addAll(page.filter { it.namespace == serverId })
            if (page.size.toULong() < limit) {
                break
            }
            offset += limit
        }
        return result
    }

    /**
     * 歌曲在本地已经完成的文件,包括永久下载和边播边缓存的
     * @param onlyDownloaded 是否只要永久下载的
     */
    fun finishedMusics(musicId: Long, onlyDownloaded: Boolean): List<CacheInfo> {
        val prefix = musicKeyPrefix(musicId)
        return list(GROUP_MUSIC, if (onlyDownloaded) CacheMode.SAVE else null)
            .filter { it.key.startsWith(prefix) && it.state == DownloadState.FINISH && it.path != null }
    }

    /**
     * 从歌曲缓存key里取出文件ID,不是曲库歌曲的key时返回null
     */
    fun musicFileId(key: String): Long? {
        val index = key.indexOf("/file/")
        if (index < 0) {
            return null
        }
        return key.substring(index + "/file/".length).toLongOrNull()
    }

    /**
     * 本地有已经完成的文件(下载或者缓存)的歌曲,离线时用来查找可以播放的歌曲,一次查出来,不用每首歌查一次
     * @return 曲库歌曲的ID和采集平台歌曲的缓存key
     */
    fun localMusicKeys(): Pair<Set<Long>, Set<String>> {
        val musicIds = HashSet<Long>()
        val collectKeys = HashSet<String>()
        for (info in list(GROUP_MUSIC, null)) {
            if (info.state != DownloadState.FINISH || info.path == null) {
                continue
            }
            if (info.key.startsWith("/collect/")) {
                collectKeys.add(info.key)
                continue
            }

            //key的格式:/music/{歌曲ID}/file/{文件ID}
            val parts = info.key.split("/").filter { it.isNotEmpty() }
            if (parts.size >= 2 && parts[0] == "music") {
                parts[1].toLongOrNull()?.let { musicIds.add(it) }
            }
        }
        return Pair(musicIds, collectKeys)
    }

    /**
     * 删除缓存或下载的文件,正在下载的会先停止
     */
    fun delete(ids: List<String>) {
        try {
            get().delete(ids)
        } catch (e: Throwable) {
            Log.e(TAG, "删除下载失败", e)
        }
    }

    /**
     * 暂停下载
     */
    fun pause(ids: List<String>) {
        runCatching { get().pause(ids) }
    }

    /**
     * 继续下载,下载失败的重新下载
     */
    fun resume(ids: List<String>) {
        runCatching { get().resume(ids) }
    }

    /**
     * 清理缓存,不包括正在使用的缓存,会读写文件,不能在主线程调用
     * @param group 缓存分组,为null时清理所有分组
     */
    fun clearCache(group: String?) {
        runCatching { get().clearCache(group, null) }
    }

    /** 缓存保存期限的默认值(天),和SDK的默认值一样 */
    const val DEFAULT_CACHE_EXPIRE_DAYS = 90

    /**
     * 设置缓存保存期限,超过期限没有访问的缓存由SDK定期清理,下载的文件不会被清理
     * @param days 天数,0表示永久
     */
    fun setCacheExpireDays(days: Int) {
        runCatching { get().setCacheExpireDays(days.toUInt()) }
    }

    /**
     * 设置缓存容量上限,超过之后自动清理最久没有使用的缓存
     * @param mb 容量上限(MB),0表示不限制
     */
    fun setMaxCacheSize(mb: Int) {
        runCatching { get().setMaxCacheSize(mb.toULong() * 1024UL * 1024UL) }
    }

    /**
     * 创建SDK实例,数据目录用noBackupFilesDir,不会被系统清理,也不会备份
     */
    private fun create(): FileSdk {
        val root = File(DairoUI.context.noBackupFilesDir, ROOT_FOLDER)
        root.mkdirs()
        val sdk = FileSdk(SdkConfig(rootDir = root.absolutePath))

        //SDK不负责保存设置,启动时再设置一次
        if (SettingStore.fileSdkMaxCachingCount > 0) {
            sdk.setMaxCachingCount(SettingStore.fileSdkMaxCachingCount.toUInt())
        }
        SettingStore.fileSdkCacheExpireDays?.let {
            sdk.setCacheExpireDays(it.toUInt())
        }
        if (SettingStore.fileSdkMaxCacheSize > 0) {
            sdk.setMaxCacheSize(SettingStore.fileSdkMaxCacheSize.toULong() * 1024UL * 1024UL)
        }
        return sdk
    }
}

/**
 * 要通过SDK访问的文件,歌曲和图片都是公开地址,不需要登录票据
 */
class FileSource(

    /** 下载地址 */
    val url: String,

    /** SDK访问参数 */
    val options: Options,

    /** 缓存文件的扩展名,未知时为空,此时缓存文件没有扩展名 */
    val ext: String,
) {
    companion object {

        /**
         * 曲库里歌曲的一个音质的文件
         * 用歌曲ID和文件ID作为缓存key,DFS换了地址或者重新生成公开路径之后缓存依然有效,可以按歌曲ID找到已经下载、缓存的文件;有MD5时下载完成后校验
         * 文件名带上歌手和音质,下载列表里显示
         */
        fun music(music: MusicModel, file: MusicFileModel): FileSource? {
            val url = file.url
            if (url.isNullOrEmpty()) {
                return null
            }
            val displayName = "${music.nameText} - ${music.artistText} [${MusicModel.qualityText(file.bitRate)}].${file.format}"
            return FileSource(
                url, Options(
                    group = FileSdkUtil.GROUP_MUSIC, namespace = SettingStore.serverId,
                    cacheKey = FileSdkUtil.musicKeyPrefix(music.id) + file.id, name = displayName,
                    md5 = file.md5, thumb = music.logo,
                ), file.format
            )
        }

        /**
         * 还没有入库的采集平台歌曲
         * 采集平台的播放地址可能每次都不一样,用采集参数作为缓存key;扩展名从地址里获取,获取不到时为空
         */
        fun collectMusic(music: MusicModel): FileSource? {
            val collect = music.collect ?: return null
            val url = collect.url
            if (url.isNullOrEmpty()) {
                return null
            }
            val name = music.name ?: "${music.id}"
            val path = runCatching { URL(url).path }.getOrNull() ?: ""
            val fileName = path.substringAfterLast('/')
            val ext = if (fileName.contains('.')) fileName.substringAfterLast('.').lowercase() else ""
            return FileSource(
                url, Options(
                    group = FileSdkUtil.GROUP_MUSIC, namespace = SettingStore.serverId,
                    cacheKey = "/collect/${collect.platform}/${collect.extra}",
                    name = if (ext.isEmpty()) name else "$name.$ext",
                ), ext
            )
        }

        /** 歌曲视频,用歌曲ID和文件ID作为缓存key,换了地址之后缓存和下载依然有效,没有网络时可以按歌曲ID找到缓存好的视频 */
        fun video(file: VideoFileModel, music: MusicModel): FileSource? {
            val url = file.url
            if (url.isNullOrEmpty()) {
                return null
            }
            return FileSource(
                url, Options(
                    group = FileSdkUtil.GROUP_VIDEO, namespace = SettingStore.serverId,
                    cacheKey = FileSdkUtil.videoKeyPrefix(music.id) + file.id,
                    name = "${music.nameText} - ${music.artistText} [${FileSdkUtil.videoQualityText(file.quality)}].mp4",
                    md5 = file.md5, thumb = music.logo,
                ), "mp4"
            )
        }

        /** 已经缓存好的视频,重新生成访问参数,播放时直接读缓存文件 */
        fun video(info: CacheInfo): FileSource {
            return FileSource(
                info.url, Options(group = info.group, namespace = info.namespace, cacheKey = info.key, name = info.name, thumb = info.thumb),
                "mp4"
            )
        }

        /** 采集平台的原画视频,还没有入库的歌曲使用,用采集参数作为缓存key */
        fun collectVideo(music: MusicModel): FileSource? {
            val collect = music.collect ?: return null
            val url = collect.videoUrl
            if (url.isNullOrEmpty()) {
                return null
            }
            return FileSource(
                url, Options(
                    group = FileSdkUtil.GROUP_VIDEO, namespace = SettingStore.serverId,
                    cacheKey = "/collect-video/${collect.platform}/${collect.extra}",
                    name = "${music.nameText} - ${music.artistText} [原画].mp4", thumb = music.logo,
                ), "mp4"
            )
        }

        /** 图片,采集平台的图片地址可能用参数区分,缓存key包含参数部分 */
        fun image(url: String): FileSource {
            return FileSource(url, Options(group = FileSdkUtil.GROUP_IMAGE, keyMode = KeyMode.PATH_AND_QUERY, priority = 1), "")
        }
    }
}

/**
 * 下载回调,转发到主线程
 */
private class FileSdkFetchListener(
    private val progress: (percent: Int) -> Unit,
    private val finish: (path: String?, error: String?) -> Unit,
) : FetchListener {

    private val handler = Handler(Looper.getMainLooper())

    override fun onProgress(size: ULong?, downloaded: ULong) {
        if (size == null || size == 0UL) {
            return
        }
        val percent = (downloaded.toDouble() / size.toDouble() * 100).toInt()
        this.handler.post { this.progress(percent) }
    }

    override fun onSuccess(path: String) {
        this.handler.post { this.finish(path, null) }
    }

    override fun onError(error: SdkException) {
        if (error is SdkException.Cancelled) {
            return
        }
        this.handler.post { this.finish(null, error.message ?: error.toString()) }
    }
}
