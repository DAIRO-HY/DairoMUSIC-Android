package cn.dairo.music.api.model

import kotlinx.serialization.Serializable

@Serializable
data class MusicModel(

    /** 歌曲ID,采集平台的搜索结果为0 */
    val id: Long,

    /** 歌名 */
    val name: String? = null,

    /** 歌手 */
    val artist: String? = null,

    /** 专辑 */
    val album: String? = null,

    /** 封面地址 */
    val logo: String? = null,

    /** 时长(毫秒) */
    val duration: Long? = null,

    /** 所在的歌单ID,只有歌单里的歌曲才有 */
    val favoriteId: Long? = null,

    /** 所有音质的歌曲文件,为空表示还没有采集完成 */
    val files: List<MusicFileModel>? = null,

    /** 采集信息 */
    val collect: MusicCollectModel? = null,

    /** 视频状态 0:未采集 1:采集中 2:有视频 3:没有视频 4:视频已删除 5:采集失败 */
    val videoState: Int? = null,

    /** 播放次数,只有曲库的搜索结果才有 */
    val playCount: Int? = null,

    /** 审核状态 0:待审核 1:通过 2:拒绝(曲库里搜索不到),只有信任会员获取歌曲详情时才有 */
    val auditState: Int? = null,
)

@Serializable
data class MusicFileModel(

    /** 文件ID,不会变,用作缓存key */
    val id: Long,

    /** 文件的MD5,部分老数据为空 */
    val md5: String? = null,

    /** 比特率 */
    val bitRate: Int? = null,

    /** 采样率 */
    val sampleRate: Int? = null,

    /** 文件大小 */
    val size: Long,

    /** 格式 */
    val format: String,

    /** 播放地址(支持断点续传),非VIP时只有最低音质有地址 */
    val url: String? = null,

    /** 是否只有VIP可以播放,非VIP时为true的文件没有播放地址 */
    val vipOnly: Boolean? = null,
)

@Serializable
data class MusicCollectModel(

    /** 采集平台 */
    val platform: String,

    /** 采集参数 */
    val extra: String,

    /** 采集平台的播放地址 */
    val url: String? = null,

    /** 采集平台的原画视频地址,可以直接播放 */
    val videoUrl: String? = null,

    /** 查询视频是否可以播放,返回OK时可以播放,其他内容是进度说明,第一次请求时开始下载 */
    val videoInfoUrl: String? = null,
)
