package cn.dairo.music.store

import cn.dairo.music.api.FavoriteApi
import cn.dairo.music.api.MusicApi
import cn.dairo.music.api.model.MusicModel
import cn.dairo.music.constants.EventName
import cn.dairo.music.extension.isSameMusic
import cn.dairo.music.player.PlayerManager
import cn.dairo.music.util.NotificationCenter
import cn.dairo.ui.root.Toast
import cn.dairo.ui.util.LocalObjectUtil

/**
 * 我的歌单里的歌曲和最近播放,保存在当前账号的本地缓存里
 */
object MusicStore {

    /** 我的歌单里所有歌曲的缓存名 */
    private const val KEY_FAVORITE_MUSIC = "favorite_music"

    /** 最近播放的缓存名 */
    private const val KEY_RECENT_MUSIC = "recent_music"

    /** 最近播放最多保存的歌曲数 */
    private const val RECENT_MAX_COUNT = 100

    /** 内存里的歌曲,歌曲很多时解析很慢 */
    private var mUserMusic: List<MusicModel>? = null

    /** "我喜欢"歌单里的歌曲ID */
    private var mLikeMusicIds: Set<Long>? = null

    /** 我所有歌单里的歌曲,同一首歌在多个歌单里时有多条,用favoriteId区分 */
    var userMusic: List<MusicModel>
        get() {
            mUserMusic?.let { return it }
            val list = SettingStore.accountObjectName(KEY_FAVORITE_MUSIC)?.let { LocalObjectUtil.read<List<MusicModel>>(it) } ?: emptyList()
            mUserMusic = list
            return list
        }
        set(value) {
            mUserMusic = value
            mLikeMusicIds = null
            SettingStore.accountObjectName(KEY_FAVORITE_MUSIC)?.let { LocalObjectUtil.write(value, it) }
            NotificationCenter.post(EventName.MUSIC_CHANGE)
        }

    /** 获取我的歌单里的歌曲 */
    fun musics(favoriteId: Long): List<MusicModel> {
        return this.userMusic.filter { it.favoriteId == favoriteId }
    }

    /** 判断是否"我喜欢"的歌曲 */
    fun isLike(musicId: Long): Boolean {
        var ids = mLikeMusicIds
        if (ids == null) {
            val likeId = FavoriteStore.likeFavorite?.id
            ids = this.userMusic.filter { it.favoriteId == likeId }.map { it.id }.toSet()
            mLikeMusicIds = ids
        }
        return ids.contains(musicId)
    }

    /** 后台从服务器刷新歌曲 */
    fun loadInBackground() {
        FavoriteApi.allMusics().hide().send {
            this.userMusic = it
        }
    }

    /** 切换账号时清空内存里的数据 */
    fun clearMemory() {
        mUserMusic = null
        mLikeMusicIds = null
    }

    /**
     * 添加或删除"我喜欢"
     * @param onAdded 添加到"我喜欢"成功之后回调歌曲ID,采集平台的歌曲加入曲库之后才有ID
     */
    fun toggleLike(music: MusicModel, onAdded: (musicId: Long) -> Unit = {}) {
        val like = FavoriteStore.likeFavorite
        if (like == null) {
            FavoriteStore.loadInBackground()
            Toast.show("歌单还没有加载完成,请稍后再试")
            return
        }
        if (music.id != 0L && this.isLike(music.id)) {
            FavoriteStore.removeMusic(music.id, like.id)
        } else {
            FavoriteStore.addMusic(music, like.id, onAdded)
        }
    }

    /** 确保歌曲已经在曲库里,采集平台搜索到的歌曲ID为0,需要先加入曲库 */
    fun ensureId(music: MusicModel, success: (musicId: Long) -> Unit) {
        if (music.id != 0L) {
            success(music.id)
            return
        }
        val collect = music.collect
        if (collect == null) {
            Toast.show("歌曲信息不完整")
            return
        }
        MusicApi.collect(music.name ?: "", music.logo, collect.platform, collect.extra).send { musicId ->
            PlayerManager.onCollected(music, musicId)
            success(musicId)
        }
    }

    /*----------------------------------------------------------------------------------*/

    /** 最近播放 */
    var recentMusic: List<MusicModel>
        get() = SettingStore.accountObjectName(KEY_RECENT_MUSIC)?.let { LocalObjectUtil.read<List<MusicModel>>(it) } ?: emptyList()
        set(value) {
            SettingStore.accountObjectName(KEY_RECENT_MUSIC)?.let { LocalObjectUtil.write(value, it) }
            NotificationCenter.post(EventName.RECENT_CHANGE)
        }

    /** 添加到最近播放,已经存在时移到最前面 */
    fun addRecent(music: MusicModel) {
        val list = this.recentMusic.filter { !it.isSameMusic(music) }.toMutableList()
        list.add(0, music.copy(favoriteId = null))
        this.recentMusic = list.take(RECENT_MAX_COUNT)
    }

    /** 清空最近播放 */
    fun clearRecent() {
        this.recentMusic = emptyList()
    }
}
