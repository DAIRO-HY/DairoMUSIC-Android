package cn.dairo.music.util

import cn.dairo.file.sdk.CacheInfo
import cn.dairo.file.sdk.DownloadState
import cn.dairo.music.api.MusicApi
import cn.dairo.music.api.model.MusicModel
import cn.dairo.music.extension.downloadFiles
import cn.dairo.music.extension.file
import cn.dairo.music.extension.hasFile
import cn.dairo.music.extension.playableFiles
import cn.dairo.music.extension.qualityText
import cn.dairo.music.store.MusicStore
import cn.dairo.music.store.SettingStore
import cn.dairo.ui.root.Toast
import cn.dairo.ui.util.fileSize

/**
 * 下载音质的选项
 */
data class DownloadQuality(

    /** 显示的名称 */
    val label: String,

    /** 目标音质,从歌曲的所有音质里选择不超过这个音质的最高音质文件 */
    val bitRate: Int,

    /** 是否只有VIP才能下载,非VIP选择时提示需要VIP */
    val vipOnly: Boolean,
)

/**
 * 歌曲下载,下载的文件通过SDK永久保存,下载进度在缓存管理的"已下载的歌曲"里查看
 */
object MusicDownloadUtil {

    /** 非VIP选择VIP音质时的提示 */
    private const val VIP_TIP = "超高品质、无损音质需要VIP"

    /** 歌单全部下载时的音质选项,非VIP只能下载标准音质,其他音质显示VIP标记 */
    val listQualities: List<DownloadQuality>
        get() {
            val vip = ServerConfig.isVipLimited
            val mark = if (vip) "(VIP)" else ""
            return listOf(
                DownloadQuality("标准(128K)", SettingStore.BIT_RATE_STANDARD, false),
                DownloadQuality("超高品质(320K)$mark", 320, vip),
                DownloadQuality("无损$mark", SettingStore.BIT_RATE_LOSSLESS, vip),
            )
        }

    /**
     * 一首歌可以下载的音质,列出这首歌所有音质的文件和大小,非VIP不能下载的音质显示VIP标记
     * 还没有入库的采集平台歌曲不知道有哪些音质,显示通用的选项
     */
    fun qualities(music: MusicModel): List<DownloadQuality> {
        val files = music.downloadFiles
        if (files.isEmpty()) {
            return listQualities
        }
        val playable = music.playableFiles.map { it.id }.toSet()
        return files.map {
            val vipOnly = !playable.contains(it.id)
            DownloadQuality(MusicModel.qualityText(it.bitRate) + (if (vipOnly) "(VIP)" else "") + "  " + it.size.fileSize, it.bitRate ?: 0, vipOnly)
        }
    }

    /** 下载状态:已下载、下载中,没有下载时返回null */
    fun downloadedText(musicId: Long): String? {
        val list = downloads(musicId)
        if (list.any { it.state == DownloadState.FINISH }) {
            return "已下载"
        }
        return if (list.isEmpty()) null else "下载中"
    }

    /** 一首歌的所有下载,包括还没有下载完成的 */
    fun downloads(musicId: Long): List<CacheInfo> {
        if (musicId == 0L) {
            return emptyList()
        }
        val prefix = FileSdkUtil.musicKeyPrefix(musicId)
        return FileSdkUtil.downloads(FileSdkUtil.GROUP_MUSIC).filter { it.key.startsWith(prefix) }
    }

    /** 下载一首歌,还没有入库的歌曲先收藏到曲库,再获取歌曲的所有音质 */
    fun download(music: MusicModel, quality: DownloadQuality) {
        if (quality.vipOnly) {
            Toast.show(VIP_TIP)
            return
        }
        if (music.id != 0L && music.hasFile) {
            add(music, quality)
            return
        }
        MusicStore.ensureId(music) { musicId ->
            MusicApi.detail(musicId).send { detail ->
                add(detail, quality)
            }
        }
    }

    /** 添加一首歌的下载任务 */
    private fun add(music: MusicModel, quality: DownloadQuality) {
        val file = music.file(bitRate = quality.bitRate)
        val source = file?.let { FileSource.music(music, it) }
        if (source == null) {
            Toast.show("歌曲还在处理中,请稍后再下载")
            return
        }
        Toast.show(if (FileSdkUtil.download(source)) "已加入下载列表" else "添加下载失败")
    }

    /** 下载列表里的所有歌曲,每首歌选择不超过目标音质的最高音质文件,还没有入库或者还没有文件的歌曲跳过 */
    fun downloadAll(musicList: List<MusicModel>, quality: DownloadQuality) {
        if (quality.vipOnly) {
            Toast.show(VIP_TIP)
            return
        }
        var count = 0
        for (music in musicList) {
            if (music.id == 0L) {
                continue
            }
            val source = music.file(bitRate = quality.bitRate)?.let { FileSource.music(music, it) } ?: continue
            if (FileSdkUtil.download(source)) {
                count += 1
            }
        }
        if (count == 0) {
            Toast.show("没有可以下载的歌曲")
        } else if (count < musicList.size) {
            Toast.show("已加入下载列表${count}首,${musicList.size - count}首暂时不能下载")
        } else {
            Toast.show("已加入下载列表${count}首")
        }
    }

    /** 删除下载的文件 */
    fun delete(ids: List<String>) {
        FileSdkUtil.delete(ids)
    }
}
