package cn.dairo.music.page.musicinfo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.file.sdk.CacheInfo
import cn.dairo.file.sdk.DownloadState
import cn.dairo.music.api.ApiConst
import cn.dairo.music.api.MusicApi
import cn.dairo.music.api.model.FavoriteModel
import cn.dairo.music.api.model.MusicFileModel
import cn.dairo.music.api.model.MusicModel
import cn.dairo.music.extension.artistText
import cn.dairo.music.extension.file
import cn.dairo.music.extension.nameText
import cn.dairo.music.extension.qualityText
import cn.dairo.music.player.MusicFileResolver
import cn.dairo.music.store.FavoriteStore
import cn.dairo.music.store.MusicStore
import cn.dairo.music.store.SettingStore
import cn.dairo.music.util.DownloadQuality
import cn.dairo.music.util.FileSdkUtil
import cn.dairo.music.util.MusicDownloadUtil
import cn.dairo.music.util.ObservableObject
import cn.dairo.music.util.ServerConfig
import cn.dairo.music.util.ShareUtil
import cn.dairo.ui.root.Toast
import cn.dairo.ui.util.fileSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @param favoriteId 歌曲所在的我的歌单,不在我的歌单里时为null
 */
class MusicInfoViewModel(music: MusicModel, favoriteId: Long?) : ObservableObject() {

    companion object {

        /** 审核状态的选项 */
        val AUDIT_STATES = listOf(0 to "待审核", 1 to "通过", 2 to "拒绝")
    }

    /** 歌曲信息 */
    var music by mutableStateOf(music)

    /** 歌曲所在的我的歌单,可以从这个歌单删除 */
    val favorite: FavoriteModel? = favoriteId?.let { FavoriteStore.byId(it) }

    /** 是否"我喜欢"的歌曲 */
    var isLike by mutableStateOf(false)

    /** 下载状态:已下载、下载中,没有下载时为null */
    var downloadedText by mutableStateOf<String?>(null)

    /** 这首歌的所有下载,包括还没有下载完成的,删除下载时选择 */
    var downloads by mutableStateOf(emptyList<CacheInfo>())

    /** 当前音质和文件大小,和播放时选择文件的规则一样 */
    var displayFile by mutableStateOf<MusicFileModel?>(null)

    /** 显示的文件的说明:已下载、已缓存,在线播放时为null */
    var displayTag by mutableStateOf<String?>(null)

    /** 是否信任会员,可以查看、修改审核状态,纠错直接生效 */
    val isFriend = SettingStore.user?.isFriend == true

    /** 审核状态,只有信任会员才有,获取歌曲详情之前为null */
    var auditState by mutableStateOf<Int?>(null)

    /** 审核状态的说明 */
    val auditText: String
        get() {
            val state = this.auditState ?: return ""
            return AUDIT_STATES.firstOrNull { it.first == state }?.second ?: ""
        }

    /** 可以收藏到的歌单 */
    val favoriteList: List<FavoriteModel>
        get() = FavoriteStore.list

    init {
        this.updateLike()
        this.updateDownload()
    }

    /** 信任会员获取歌曲详情:审核状态,纠错之后的歌名、歌手、专辑 */
    fun loadDetail() {
        if (!this.isFriend || this.music.id == 0L) {
            return
        }
        val musicId = this.music.id
        MusicApi.detail(musicId).hide().fail { }.error { }.send { detail ->
            if (this.music.id != musicId) {
                return@send
            }
            this.auditState = detail.auditState
            this.music = this.music.copy(name = detail.name, artist = detail.artist, album = detail.album)
        }
    }

    /** 修改审核状态 */
    fun onAuditClick(state: Int) {
        MusicApi.audit(this.music.id, state).send {
            this.auditState = state
            Toast.show("审核状态已修改")
        }
    }

    /** 更新下载状态和显示的文件 */
    fun updateDownload() {
        this.downloads = MusicDownloadUtil.downloads(this.music.id)
        this.downloadedText = MusicDownloadUtil.downloadedText(this.music.id)
        val (file, tag) = MusicFileResolver.displayFile(this.music)
        this.displayFile = file
        this.displayTag = tag
    }

    /** 下载选择的音质 */
    fun onDownloadClick(quality: DownloadQuality) {
        MusicDownloadUtil.download(this.music, quality)
        this.downloadedText = this.downloadedText ?: "下载中"

        //添加下载任务之后刷新,可以删除正在下载的
        this.scope.launch {
            delay(500)
            updateDownload()
        }
    }

    /**
     * 删除下载的文件,正在播放的歌曲会继续播放完,下次播放时按新的规则选择文件
     * @param infos 要删除的下载
     */
    fun onDeleteDownloadClick(infos: List<CacheInfo>) {
        MusicDownloadUtil.delete(infos.map { it.id })
        Toast.show("已删除下载")
        this.updateDownload()
    }

    /** 删除下载时显示的名称:音质、大小,没有下载完成的加上"下载中" */
    fun downloadLabel(info: CacheInfo): String {
        val file = FileSdkUtil.musicFileId(info.key)?.let { this.music.file(id = it) }
        val size = (info.size?.toLong() ?: file?.size)?.fileSize ?: ""
        val state = if (info.state == DownloadState.FINISH) "" else "(下载中)"
        return MusicModel.qualityText(file?.bitRate) + "  " + size + state
    }

    /** 更新"我喜欢"状态 */
    fun updateLike() {
        this.isLike = this.music.id != 0L && MusicStore.isLike(this.music.id)
    }

    /** 喜欢点击事件 */
    fun onLikeClick() {

        //采集平台的歌曲加入曲库之后更新歌曲ID,否则按ID判断"我喜欢"状态时一直是没有喜欢
        MusicStore.toggleLike(this.music) { musicId ->
            this.music = this.music.copy(id = musicId)
            this.updateLike()
            this.updateDownload()
        }
    }

    /** 收藏到歌单 */
    fun onAddToFavoriteClick(favorite: FavoriteModel) {
        FavoriteStore.addMusic(this.music, favorite.id) { musicId ->
            this.music = this.music.copy(id = musicId)
            Toast.show("已收藏到「${favorite.name}」")
        }
    }

    /** 从歌单删除 */
    fun onRemoveClick(success: () -> Unit) {
        val favorite = this.favorite ?: return
        FavoriteStore.removeMusic(this.music.id, favorite.id) {
            Toast.show("已删除")
            success()
        }
    }

    /** 分享,分享链接是服务器上的网页,可以在线播放和下载 */
    fun onShareClick() {
        MusicStore.ensureId(this.music) { musicId ->
            this.music = this.music.copy(id = musicId)
            ShareUtil.shareUrl(SettingStore.domainNotNull + ApiConst.SHARE_MUSIC + "?id=$musicId")
        }
    }

    /** 要发送的歌曲文件:下载完成的文件,下载了多种音质时发送最高音质的 */
    val sendFile: Pair<CacheInfo, MusicFileModel?>?
        get() = this.downloads.filter { it.state == DownloadState.FINISH && it.path != null }.map { info ->
            Pair(info, FileSdkUtil.musicFileId(info.key)?.let { this.music.file(id = it) })
        }.maxByOrNull { it.second?.bitRate ?: 0 }

    /** 通过其他App发送下载的歌曲文件,VIP功能生效时只有VIP才能发送 */
    fun onSendFileClick() {
        val send = this.sendFile ?: return
        val path = send.first.path ?: return
        if (!ServerConfig.checkFileOutVip("发送文件")) {
            return
        }
        ShareUtil.sendFile(path, this.music.nameText + " - " + this.music.artistText, send.second?.format)
    }
}
