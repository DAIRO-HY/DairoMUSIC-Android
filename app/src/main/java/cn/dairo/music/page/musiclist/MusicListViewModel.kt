package cn.dairo.music.page.musiclist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.util.UnstableApi
import cn.dairo.music.api.FavoriteApi
import cn.dairo.music.api.model.MusicModel
import cn.dairo.music.constants.EventName
import cn.dairo.music.constants.FavoriteId
import cn.dairo.music.player.PlayerManager
import cn.dairo.music.store.FavoriteStore
import cn.dairo.music.store.MusicStore
import cn.dairo.music.util.NotificationCenter
import cn.dairo.music.util.ObservableObject
import cn.dairo.ui.root.Toast

/**
 * @param favoriteId 歌单ID,最近播放时为FavoriteId.RECENT
 */
@UnstableApi
class MusicListViewModel(val favoriteId: Long, name: String) : ObservableObject() {

    /** 歌单名 */
    var name by mutableStateOf(name)

    /** 歌单描述 */
    var description by mutableStateOf<String?>(null)

    /** 封面 */
    var logo by mutableStateOf<String?>(null)

    /** 歌曲列表 */
    var musicList by mutableStateOf(emptyList<MusicModel>())

    /** 是否正在从服务器加载 */
    var isLoading by mutableStateOf(false)

    /** 是否我的歌单,可以删除里面的歌曲 */
    var isMine by mutableStateOf(false)

    /** 是否"我喜欢"歌单,不能修改 */
    var isLike by mutableStateOf(false)

    init {
        if (favoriteId == FavoriteId.RECENT) {
            NotificationCenter.observe(this.scope, EventName.RECENT_CHANGE) {
                this.loadData()
            }
        } else {
            NotificationCenter.observe(this.scope, EventName.MUSIC_CHANGE, EventName.FAVORITE_CHANGE) {
                this.loadData()
            }
        }
        this.loadData()
        if (this.isMine) {

            //我的歌单先显示本地缓存,再从服务器更新
            MusicStore.loadInBackground()
        }
    }

    /** 加载数据 */
    fun loadData() {
        if (this.favoriteId == FavoriteId.RECENT) {
            this.musicList = MusicStore.recentMusic
            return
        }
        val favorite = FavoriteStore.byId(this.favoriteId)
        if (favorite != null) {//我的歌单
            this.isMine = true
            this.isLike = favorite.isLike
            this.name = favorite.name
            this.description = favorite.description
            this.logo = favorite.logo
            this.musicList = MusicStore.musics(this.favoriteId)
            return
        }
        if (this.isMine || this.isLoading) {
            return
        }

        //推荐的歌单从服务器获取
        this.isLoading = true
        FavoriteApi.detail(this.favoriteId).hide().send { favorite ->
            this.name = favorite.name
            this.description = favorite.description
            this.logo = favorite.logo
        }
        FavoriteApi.musics(this.favoriteId).hide().finish {
            this.isLoading = false
        }.send {
            this.musicList = it
        }
    }

    /** 播放列表里的歌曲 */
    fun play(index: Int) {
        PlayerManager.prepareAndPlay(this.musicList, index, this.favoriteId)
    }

    /** 从歌单删除歌曲 */
    fun remove(music: MusicModel) {
        FavoriteStore.removeMusic(music.id, this.favoriteId) {
            Toast.show("已删除")
        }
    }

    /** 清空最近播放 */
    fun clearRecent() {
        MusicStore.clearRecent()
    }
}
