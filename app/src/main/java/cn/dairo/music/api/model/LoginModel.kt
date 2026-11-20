package cn.dairo.music.api.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginModel(

    /** 登录票据 */
    val token: String,

    /** 会员信息 */
    val user: UserModel,
)
