package cn.dairo.music.api.model

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteModel(

    /** 歌单ID */
    val id: Long,

    /** 歌单名 */
    val name: String,

    /** 描述 */
    val description: String? = null,

    /** 封面地址,"我喜欢"歌单为空 */
    val logo: String? = null,

    /** 是否"我喜欢"歌单,不能删除、不能改名 */
    val isLike: Boolean,

    /** 歌曲数 */
    val musicCount: Int,

    /** 是否自己的歌单 */
    val isMine: Boolean,
)
