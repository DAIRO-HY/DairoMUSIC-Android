package cn.dairo.music.page.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.util.UnstableApi
import cn.dairo.music.api.MusicApi
import cn.dairo.music.api.http.ApiHttpBase
import cn.dairo.music.api.model.MusicModel
import cn.dairo.music.constants.FavoriteId
import cn.dairo.music.player.PlayerManager
import cn.dairo.music.store.SettingStore
import cn.dairo.music.util.ObservableObject
import cn.dairo.ui.util.LocalObjectUtil

@UnstableApi
class SearchViewModel : ObservableObject() {

    companion object {

        /** 搜索历史的本地缓存名 */
        private const val KEY_HISTORY = "search_history"

        /** 最多保存的搜索历史数 */
        private const val MAX_HISTORY_COUNT = 20
    }

    /** 搜索关键词 */
    var key by mutableStateOf("")

    /** 曲库的搜索结果 */
    var musicList by mutableStateOf(emptyList<MusicModel>())

    /** 采集平台的搜索结果 */
    var collectList by mutableStateOf(emptyList<MusicModel>())

    /** 是否正在搜索采集平台 */
    var isCollectSearching by mutableStateOf(false)

    /** 是否已经搜索过,没有搜索时显示搜索历史 */
    var isSearched by mutableStateOf(false)

    /** 搜索历史 */
    var history by mutableStateOf(emptyList<String>())

    /** 上一次的请求,重新搜索时取消 */
    private var requests = emptyList<ApiHttpBase>()

    init {
        this.history = SettingStore.accountObjectName(KEY_HISTORY)?.let { LocalObjectUtil.read<List<String>>(it) } ?: emptyList()
    }

    /** 搜索,曲库和采集平台同时搜索,曲库的结果先显示 */
    fun search() {
        val key = this.key.trim()
        if (key.isEmpty()) {
            return
        }
        this.addHistory(key)
        this.requests.forEach { it.cancel() }
        this.isSearched = true
        this.musicList = emptyList()
        this.collectList = emptyList()
        this.isCollectSearching = true
        val musicRequest = MusicApi.search(key).send {
            this.musicList = it
        }
        val collectRequest = MusicApi.searchCollect(key).hide().fail { }.error { }.finish {
            this.isCollectSearching = false
        }.send {
            this.collectList = it
        }
        this.requests = listOf(musicRequest, collectRequest)
    }

    /** 点击搜索历史 */
    fun onHistoryClick(key: String) {
        this.key = key
        this.search()
    }

    /** 关键词清空时回到搜索历史 */
    fun onKeyChange(key: String) {
        this.key = key
        if (key.isEmpty()) {
            this.isSearched = false
        }
    }

    /** 播放搜索结果 */
    fun play(list: List<MusicModel>, index: Int) {
        PlayerManager.prepareAndPlay(list, index, FavoriteId.SEARCH)
    }

    /** 添加到搜索历史 */
    private fun addHistory(key: String) {
        this.history = (listOf(key) + this.history.filter { it != key }).take(MAX_HISTORY_COUNT)
        this.saveHistory()
    }

    /** 清空搜索历史 */
    fun onClearHistoryClick() {
        this.history = emptyList()
        this.saveHistory()
    }

    private fun saveHistory() {
        SettingStore.accountObjectName(KEY_HISTORY)?.let { LocalObjectUtil.write(this.history, it) }
    }

    override fun onCleared() {
        this.requests.forEach { it.cancel() }
        super.onCleared()
    }
}
