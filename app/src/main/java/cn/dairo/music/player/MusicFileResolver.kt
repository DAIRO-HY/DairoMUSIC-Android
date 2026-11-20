package cn.dairo.music.player

import cn.dairo.file.sdk.CacheInfo
import cn.dairo.file.sdk.CacheMode
import cn.dairo.music.api.model.MusicFileModel
import cn.dairo.music.api.model.MusicModel
import cn.dairo.music.extension.file
import cn.dairo.music.extension.settingFile
import cn.dairo.music.util.FileSdkUtil
import cn.dairo.music.util.FileSource
import java.io.File

/**
 * 本地可以直接播放的歌曲文件
 */
class LocalMusicFile(

    /** 播放地址,本地文件或者SDK的本机地址(没有扩展名的缓存文件交给SDK) */
    val url: String,

    /** 对应的音质文件,采集平台的歌曲为null */
    val file: MusicFileModel?,

    /** 是否是永久下载的文件 */
    val isDownloaded: Boolean,
)

/**
 * 选择要播放的歌曲文件,切换歌曲和歌曲信息页使用同样的规则:
 * 1. 已经下载的歌曲,使用下载的文件,下载了多种音质时使用音质最好的
 * 2. 已经缓存的文件里音质最好的,不低于设置的播放音质时,直接播放这个缓存文件(例如设置的是标准,超高品质已经缓存了)
 * 3. 都没有时,按设置的播放音质从所有音质的文件里选择,在线播放
 * 4. 离线时,设置的音质没有缓存的,使用已经缓存好的其他音质(包括比设置低的)
 */
object MusicFileResolver {

    /**
     * 本地可以直接播放的文件,不需要联网
     * @param anyQuality 设置的音质没有缓存时,是否使用已经缓存的其他音质(离线时)
     */
    fun local(music: MusicModel, anyQuality: Boolean): LocalMusicFile? {
        if (music.id == 0L) {
            return collectCached(music)
        }
        val finished = FileSdkUtil.finishedMusics(music.id, onlyDownloaded = false)
        if (finished.isEmpty()) {
            return null
        }
        best(music, finished.filter { it.mode == CacheMode.SAVE })?.let { return it }
        val cached = best(music, finished) ?: return null

        //缓存的音质不低于设置的音质时直接播放缓存
        val cachedRate = cached.file?.bitRate
        if (cachedRate != null && cachedRate >= (music.settingFile?.bitRate ?: 0)) {
            return cached
        }
        return if (anyQuality) cached else null
    }

    /**
     * 歌曲信息页显示的文件,和播放时的规则一样(不包括离线时使用比设置低的缓存)
     * @return 文件和说明(已下载、已缓存,在线播放时为null)
     */
    fun displayFile(music: MusicModel): Pair<MusicFileModel?, String?> {
        val local = local(music, anyQuality = false)
        val file = local?.file ?: return Pair(music.settingFile, null)
        return Pair(file, if (local.isDownloaded) "已下载" else "已缓存")
    }

    /** 音质最好的文件,按比特率比较,文件信息里没有的(歌曲信息过期了)按文件大小比较 */
    private fun best(music: MusicModel, list: List<CacheInfo>): LocalMusicFile? {
        val best = list.mapNotNull { info ->
            val path = info.path ?: return@mapNotNull null
            Triple(info, FileSdkUtil.musicFileId(info.key)?.let { music.file(id = it) }, path)
        }.maxWithOrNull(compareBy<Triple<CacheInfo, MusicFileModel?, String>> { it.second?.bitRate ?: 0 }.thenBy { it.first.size ?: 0UL })
            ?: return null
        return LocalMusicFile(File(best.third).toURI().toString(), best.second, best.first.mode == CacheMode.SAVE)
    }

    /** 采集平台的歌曲已经缓存好的文件 */
    private fun collectCached(music: MusicModel): LocalMusicFile? {
        val source = FileSource.collectMusic(music) ?: return null
        val path = FileSdkUtil.cachedPath(source) ?: return null
        if (source.ext.isEmpty()) {
            return FileSdkUtil.open(source)?.let { LocalMusicFile(it, null, false) }
        }
        return LocalMusicFile(File(path).toURI().toString(), null, false)
    }
}
