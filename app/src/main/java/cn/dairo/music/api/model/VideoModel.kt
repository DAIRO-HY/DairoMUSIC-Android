package cn.dairo.music.api.model

import kotlinx.serialization.Serializable

@Serializable
data class VideoModel(

    /** 视频状态 0:未采集 1:采集中 2:有视频 3:没有视频 4:视频已删除 5:采集失败 */
    val state: Int,

    /** 原画 */
    val original: VideoFileModel? = null,

    /** 流畅(480p 25fps),原画不高于480p 25fps时和原画是同一个文件 */
    val smooth: VideoFileModel? = null,
)

@Serializable
data class VideoFileModel(

    /** 文件ID,不会变,用作缓存key */
    val id: Long,

    /** 清晰度 1:原画 2:流畅 */
    val quality: Int,

    /** 状态 0:转码中 1:可以播放 2:失败 */
    val state: Int,

    /** 宽 */
    val width: Int? = null,

    /** 高 */
    val height: Int? = null,

    /** 比特率(kbps) */
    val bitRate: Int? = null,

    /** 时长(毫秒) */
    val duration: Long? = null,

    /** 文件大小 */
    val size: Long? = null,

    /** 文件MD5 */
    val md5: String? = null,

    /** 播放地址 */
    val url: String? = null,

    /** 是否只有VIP可以播放,非VIP时原画为true,没有播放地址 */
    val vipOnly: Boolean? = null,
)
