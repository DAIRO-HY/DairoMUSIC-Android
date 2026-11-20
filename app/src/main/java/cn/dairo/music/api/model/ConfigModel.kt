package cn.dairo.music.api.model

import kotlinx.serialization.Serializable

@Serializable
data class ConfigModel(

    /** 服务器版本 */
    val version: String,

    /** 服务器ID,服务器换了地址之后依然不变 */
    val serverId: String,

    /** 是否开放注册,关闭时隐藏注册和免注册使用 */
    val allowRegister: Boolean,

    /** 是否允许把下载的视频保存到相册,VIP功能生效时只有VIP才能用 */
    val allowSaveAlbum: Boolean,

    /** 是否允许把下载的歌曲、视频文件发送到其他App,VIP功能生效时只有VIP才能用 */
    val allowSendFile: Boolean,

    /** 服务器是否开启了视频采集,没有开启时不会采集、转码新的视频 */
    val videoEnable: Boolean,
)
