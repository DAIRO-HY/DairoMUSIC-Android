package cn.dairo.music.player

import cn.dairo.music.api.model.MusicModel
import cn.dairo.music.constants.FavoriteId
import kotlinx.serialization.Serializable

/**
 * 保存到本地的播放信息,App重启之后恢复
 */
@Serializable
data class PlayingInfo(

    /** 播放列表 */
    val playList: List<MusicModel> = emptyList(),

    /** 当前播放的序号 */
    val index: Int = 0,

    /** 播放列表的来源,参考FavoriteId */
    val favoriteId: Long = FavoriteId.NONE,

    /** 当前播放位置(秒) */
    val currentTime: Double = 0.0,

    /** 播放顺序 */
    val orderType: PlayerOrderType = PlayerOrderType.LIST_LOOP,
)
