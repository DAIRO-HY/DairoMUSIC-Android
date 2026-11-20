package cn.dairo.music.api.model

import kotlinx.serialization.Serializable

@Serializable
data class AppVersionModel(

    /** 版本名 */
    val versionName: String,

    /** 版本号 */
    val versionCode: Int,

    /** 安装方式 1:手动安装 2:弹框提示,后台下载 3:强制更新,后台下载 */
    val installType: Int,

    /** 更新说明 */
    val instruction: String? = null,

    /** 下载地址 */
    val url: String,
)
