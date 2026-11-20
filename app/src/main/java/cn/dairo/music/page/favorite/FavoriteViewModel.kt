package cn.dairo.music.page.favorite

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.music.api.FavoriteApi
import cn.dairo.music.api.model.FavoriteModel
import cn.dairo.music.constants.EventName
import cn.dairo.music.store.FavoriteStore
import cn.dairo.music.store.MusicStore
import cn.dairo.music.util.NotificationCenter
import cn.dairo.music.util.ObservableObject
import cn.dairo.ui.root.Toast

class FavoriteViewModel : ObservableObject() {

    /** "我喜欢"歌单 */
    var likeFavorite by mutableStateOf<FavoriteModel?>(null)

    /** 自己创建的歌单 */
    var userFavorites by mutableStateOf(emptyList<FavoriteModel>())

    /** 最近播放的歌曲数 */
    var recentCount by mutableIntStateOf(0)

    /** 新建歌单的名字 */
    var createName by mutableStateOf("")

    /** 是否正在下拉刷新 */
    var isRefreshing by mutableStateOf(false)

    init {
        NotificationCenter.observe(this.scope, EventName.FAVORITE_CHANGE) {
            this.isRefreshing = false
            this.loadData()
        }
        NotificationCenter.observe(this.scope, EventName.RECENT_CHANGE) {
            this.recentCount = MusicStore.recentMusic.size
        }
        this.loadData()
        this.recentCount = MusicStore.recentMusic.size

        //先显示本地缓存,再从服务器更新
        FavoriteStore.loadInBackground()
    }

    /** 加载本地的歌单 */
    private fun loadData() {
        val list = FavoriteStore.list
        this.likeFavorite = list.firstOrNull { it.isLike }
        this.userFavorites = list.filter { !it.isLike }
    }

    /** 下拉刷新 */
    fun refresh() {
        this.isRefreshing = true
        FavoriteStore.loadInBackground()
        MusicStore.loadInBackground()
    }

    /** 新建歌单 */
    fun onCreateClick() {
        val name = this.createName.trim()
        if (name.isEmpty()) {
            Toast.show("请输入歌单名")
            return
        }
        FavoriteApi.add(name).send {
            Toast.show("创建成功")
            FavoriteStore.loadInBackground()
        }
    }

    /** 删除歌单 */
    fun onDeleteClick(favorite: FavoriteModel) {
        FavoriteApi.delete(favorite.id).send {
            Toast.show("删除成功")
            FavoriteStore.loadInBackground()
            MusicStore.loadInBackground()
        }
    }

    /** 拖动排序时移动歌单的位置,松开之后再保存 */
    fun onMove(from: Int, to: Int) {
        val list = this.userFavorites.toMutableList()
        list.add(to, list.removeAt(from))
        this.userFavorites = list
    }

    /** 拖动排序之后保存,"我喜欢"始终排在最前面 */
    fun onMoveEnd() {
        val ids = this.userFavorites.map { it.id }.toMutableList()
        this.likeFavorite?.let { ids.add(0, it.id) }
        FavoriteApi.sort(ids).hide().send {
            FavoriteStore.loadInBackground()
        }
    }
}
