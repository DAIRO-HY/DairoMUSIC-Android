package cn.dairo.music.api.model

import kotlinx.serialization.Serializable

@Serializable
data class UserModel(

    /** 会员ID */
    val id: Long,

    /** 邮箱,临时会员为空;后台创建的只有用户名的会员也为空 */
    val email: String? = null,

    /** 用户名,只能由后台设置,可以用来登录 */
    val name: String? = null,

    /** 昵称 */
    val nick: String? = null,

    /** 头像地址 */
    val logo: String? = null,

    /** 是否临时会员(还没有绑定邮箱) */
    val isTemp: Boolean,

    /** 是否信赖用户(纠错直接生效) */
    val isFriend: Boolean,

    /** VIP状态 */
    val vip: VipModel,
)

@Serializable
data class VipModel(

    /** VIP功能对这个会员是否生效,为false时没有任何限制,不显示VIP相关的内容 */
    val enable: Boolean,

    /** 现在是否VIP */
    val isVip: Boolean,

    /** 是否永久VIP */
    val forever: Boolean,

    /** VIP到期时间(毫秒),0:从来不是VIP,永久VIP时没有意义 */
    val expireTime: Long,
)

@Serializable
data class VipRedeemModel(

    /** 是否需要会员确认,已经是永久VIP时兑换没有实际效果,确认之后带上confirm=1再提交 */
    val needConfirm: Boolean,

    /** 提示文字 */
    val message: String,

    /** 兑换之后的VIP状态 */
    val vip: VipModel? = null,
)
