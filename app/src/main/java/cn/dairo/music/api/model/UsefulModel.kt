package cn.dairo.music.api.model

import kotlinx.serialization.Serializable

@Serializable
data class UsefulModel(

    /** 分组名 */
    val name: String? = null,

    /** 显示方式 1:表格 2:横向滚动 */
    val type: Int,

    /** 歌单 */
    val favorites: List<FavoriteModel>,
)
