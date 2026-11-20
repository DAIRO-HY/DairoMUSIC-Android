package cn.dairo.music.store

import cn.dairo.music.api.FavoriteApi
import cn.dairo.music.api.model.FavoriteModel
import cn.dairo.music.api.model.MusicModel
import cn.dairo.music.constants.EventName
import cn.dairo.music.util.NotificationCenter
import cn.dairo.ui.util.LocalObjectUtil

/**
 * 我的歌单,保存在当前账号的本地缓存里
 */
object FavoriteStore {

    /** 本地缓存名 */
    private const val KEY_LIST = "favorite_list"

    /** 内存里的歌单,避免每次都读文件 */
    private var mList: List<FavoriteModel>? = null

    /** 我的歌单,按排序 */
    var list: List<FavoriteModel>
        get() {
            mList?.let { return it }
            val list = SettingStore.accountObjectName(KEY_LIST)?.let { LocalObjectUtil.read<List<FavoriteModel>>(it) } ?: emptyList()
            mList = list
            return list
        }
        set(value) {
            mList = value
            SettingStore.accountObjectName(KEY_LIST)?.let { LocalObjectUtil.write(value, it) }
            NotificationCenter.post(EventName.FAVORITE_CHANGE)
        }

    /** "我喜欢"歌单 */
    val likeFavorite: FavoriteModel?
        get() = this.list.firstOrNull { it.isLike }

    /** 通过歌单ID获取我的歌单 */
    fun byId(id: Long): FavoriteModel? {
        return this.list.firstOrNull { it.id == id }
    }

    /** 后台从服务器刷新歌单 */
    fun loadInBackground() {
        FavoriteApi.list().hide().send {
            this.list = it
        }
    }

    /** 切换账号时清空内存里的数据 */
    fun clearMemory() {
        mList = null
    }

    /**
     * 添加歌曲到歌单,采集平台的歌曲先加入曲库
     * @param success 成功回调,参数是歌曲ID
     */
    fun addMusic(music: MusicModel, favoriteId: Long, success: (musicId: Long) -> Unit = {}) {
        MusicStore.ensureId(music) { musicId ->
            FavoriteApi.addMusic(favoriteId, musicId).send {
                this.loadInBackground()
                MusicStore.loadInBackground()
                success(musicId)
            }
        }
    }

    /** 从歌单删除歌曲 */
    fun removeMusic(musicId: Long, favoriteId: Long, success: () -> Unit = {}) {
        FavoriteApi.removeMusic(favoriteId, musicId).send {
            this.loadInBackground()
            MusicStore.loadInBackground()
            success()
        }
    }
}
