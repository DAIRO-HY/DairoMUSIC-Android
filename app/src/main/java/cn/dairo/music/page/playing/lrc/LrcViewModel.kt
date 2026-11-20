package cn.dairo.music.page.playing.lrc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.media3.common.util.UnstableApi
import cn.dairo.music.api.MusicApi
import cn.dairo.music.api.http.ApiHttpBase
import cn.dairo.music.player.PlayerManager
import cn.dairo.music.store.SettingStore
import cn.dairo.music.util.ObservableObject
import cn.dairo.ui.util.LocalObjectUtil
import kotlinx.coroutines.launch

@UnstableApi
class LrcViewModel : ObservableObject() {

    /** 歌词 */
    var lines by mutableStateOf(emptyList<LrcLine>())

    /** 没有歌词时的提示 */
    var tip by mutableStateOf<String?>(null)

    /** 当前显示歌词的歌曲ID */
    private var musicId: Long? = null

    /** 获取歌词的请求 */
    private var request: ApiHttpBase? = null

    init {

        //切换歌曲时重新加载歌词
        this.scope.launch {
            snapshotFlow { PlayerManager.music?.id }.collect {
                load()
            }
        }
    }

    /** 加载当前歌曲的歌词 */
    private fun load() {
        val music = PlayerManager.music
        if (music?.id == this.musicId && (this.lines.isNotEmpty() || this.tip != null)) {
            return
        }
        this.musicId = music?.id
        this.request?.cancel()
        this.lines = emptyList()
        if (music == null || music.id == 0L) {
            this.tip = "暂无歌词"
            return
        }
        this.tip = "歌词加载中"
        val cacheName = SettingStore.accountObjectName("lrc_${music.id}")
        val cached = cacheName?.let { LocalObjectUtil.read<String>(it) }
        if (cached != null) {
            this.show(cached)
            return
        }
        this.request = MusicApi.lrc(music.id).hide().fail {
            this.tip = "歌词加载失败"
        }.error {
            this.tip = "歌词加载失败"
        }.send { list ->
            if (this.musicId != music.id) {
                return@send
            }
            val lrc = list.firstOrNull()?.lrc ?: ""
            cacheName?.let { LocalObjectUtil.write(lrc, it) }
            this.show(lrc)
        }
    }

    /** 显示歌词 */
    private fun show(lrc: String) {
        this.lines = LrcParser.parse(lrc)
        this.tip = if (this.lines.isEmpty()) "暂无歌词" else null
    }

    /** 获取当前时间应该高亮的歌词序号 */
    fun currentIndex(time: Double): Int? {
        val index = this.lines.indexOfLast { it.time <= time }
        return if (index < 0) null else index
    }

    override fun onCleared() {
        this.request?.cancel()
        super.onCleared()
    }
}
