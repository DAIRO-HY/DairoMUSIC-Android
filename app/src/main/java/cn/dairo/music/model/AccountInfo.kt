package cn.dairo.music.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * 登录信息
 */
@Serializable
data class AccountInfo(

    /** 服务器地址 */
    val domain: String,

    /** 服务器ID,同一个服务器换了地址之后依然不变 */
    val serverId: String,

    /** 会员ID */
    val userId: Long,

    /** 登录账号,有邮箱时是邮箱,只有用户名的会员是用户名,临时会员为空 */
    val account: String,

    /** 显示的名字,邮箱或者临时会员的昵称 */
    val name: String,

    /** 登录票据,退出登录之后为空;加密保存在KeychainUtil里,不保存到文件 */
    @Transient
    val token: String = "",

    /** 是否当前登录的账号 */
    val isCurrent: Boolean = false,
) {

    /** 是否同一个账号 */
    fun isSame(other: AccountInfo): Boolean {
        return this.serverId == other.serverId && this.userId == other.userId
    }

    /** 登录票据保存的名字 */
    val tokenKey: String
        get() = "TOKEN_${this.serverId}_${this.userId}"
}
