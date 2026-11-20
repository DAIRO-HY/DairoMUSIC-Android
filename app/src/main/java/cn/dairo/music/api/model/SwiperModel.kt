package cn.dairo.music.api.model

import kotlinx.serialization.Serializable

@Serializable
data class SwiperModel(

    /** 轮播ID */
    val id: Long,

    /** 标题 */
    val name: String,

    /** 图片地址 */
    val image: String? = null,

    /** 类型 1:推荐的歌单(data为歌单ID) 2:推荐单曲(data为歌曲ID) 3:拨打电话 4:跳转网页 */
    val type: Int,

    /** 数据 */
    val data: String? = null,
)
