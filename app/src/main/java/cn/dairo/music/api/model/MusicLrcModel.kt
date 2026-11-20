package cn.dairo.music.api.model

import kotlinx.serialization.Serializable

@Serializable
data class MusicLrcModel(

    /** 歌词ID */
    val id: Long,

    /** 歌词标题 */
    val title: String? = null,

    /** 歌词内容,LRC或者WEBVTT格式 */
    val lrc: String? = null,
)
