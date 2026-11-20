package cn.dairo.music.extension

import cn.dairo.music.api.model.MusicFileModel
import cn.dairo.music.api.model.MusicModel
import cn.dairo.music.store.SettingStore
import cn.dairo.music.util.ServerConfig

/** 显示的歌名 */
val MusicModel.nameText: String
    get() = this.name ?: "未知歌曲"

/** 显示的歌手 */
val MusicModel.artistText: String
    get() = this.artist ?: "未知歌手"

/** 歌手和专辑 */
val MusicModel.artistAlbumText: String
    get() {
        val album = this.album
        if (album.isNullOrEmpty()) {
            return this.artistText
        }
        return this.artistText + " - " + album
    }

/** 是否同一首歌,采集平台的歌曲ID为0,用采集参数判断 */
fun MusicModel.isSameMusic(other: MusicModel): Boolean {
    if (this.id != 0L || other.id != 0L) {
        return this.id == other.id
    }
    return this.collect?.platform == other.collect?.platform && this.collect?.extra == other.collect?.extra
}

/** 是否已经采集完成,有歌曲文件 */
val MusicModel.hasFile: Boolean
    get() = !this.files.isNullOrEmpty()

/** 最高音质,还没有采集完成时为null */
val MusicModel.maxBitRate: Int?
    get() = this.files?.mapNotNull { it.bitRate }?.maxOrNull()

/**
 * 可以在线播放、下载的文件,按音质从低到高排序
 * 只从有播放地址的文件里选;非VIP只能用最低音质(标准音质),歌单等本地缓存的歌曲信息可能是VIP到期之前获取的,这里也要限制
 */
val MusicModel.playableFiles: List<MusicFileModel>
    get() {
        val files = (this.files ?: emptyList()).filter { it.url != null }.sortedBy { it.bitRate ?: 0 }
        return if (ServerConfig.isVipLimited) files.take(1) else files
    }

/** 下载选项里列出的文件:有播放地址的,和非VIP时只有VIP才能下载的(显示VIP标记),按音质从低到高排序 */
val MusicModel.downloadFiles: List<MusicFileModel>
    get() = (this.files ?: emptyList()).filter { it.url != null || it.vipOnly == true }.sortedBy { it.bitRate ?: 0 }

/** 按目标音质选择歌曲文件:不超过目标音质的最高音质,所有文件都超过时选最低音质 */
fun MusicModel.file(bitRate: Int): MusicFileModel? {
    val files = this.playableFiles
    return files.lastOrNull { (it.bitRate ?: 0) <= bitRate } ?: files.firstOrNull()
}

/** 按设置的播放音质选择的歌曲文件 */
val MusicModel.settingFile: MusicFileModel?
    get() = this.file(bitRate = SettingStore.bitRate)

/** 按文件ID找歌曲文件 */
fun MusicModel.file(id: Long): MusicFileModel? {
    return this.files?.firstOrNull { it.id == id }
}

/** 列表里使用的唯一标识,同一首歌在不同歌单里也算不同的条目 */
val MusicModel.listId: String
    get() {
        if (this.id != 0L) {
            return "${this.id}-${this.favoriteId ?: 0}"
        }
        return "${this.collect?.platform ?: ""}-${this.collect?.extra ?: ""}"
    }

/** 获取音质的文字说明 */
fun MusicModel.Companion.qualityText(bitRate: Int?): String {
    if (bitRate == null || bitRate <= 0) {
        return "未知"
    }
    if (bitRate >= SettingStore.BIT_RATE_LOSSLESS) {
        return "无损"
    }
    if (bitRate >= 320) {
        return "超高品质(${bitRate}K)"
    }
    if (bitRate > 128) {
        return "高品质(${bitRate}K)"
    }
    return "标准(${bitRate}K)"
}
