package cn.dairo.music.api

import cn.dairo.music.api.http.ApiHttp
import cn.dairo.music.api.http.EmptyModel
import cn.dairo.music.api.model.LoginModel
import cn.dairo.music.api.model.UserModel
import cn.dairo.music.constants.Const

object UserApi {

    /** 登录时需要的设备信息 */
    private val deviceParameter: Map<String, Any?>
        get() = mapOf("deviceId" to Const.deviceId, "clientFlag" to Const.CLIENT_FLAG, "clientVersion" to Const.versionCode)

    /** 免注册使用,同一台设备找回同一个临时会员 */
    fun tempLogin(): ApiHttp<LoginModel> {
        return ApiHttp<LoginModel>(ApiConst.USER_TEMP_LOGIN, "POST", deviceParameter).noAuth()
    }

    /** 登录,account为邮箱或用户名(用户名不区分大小写),pwd为明文密码 */
    fun login(account: String, pwd: String): ApiHttp<LoginModel> {
        val parameter = deviceParameter + mapOf("account" to account, "pwd" to pwd)
        return ApiHttp<LoginModel>(ApiConst.USER_LOGIN, "POST", parameter).noAuth()
    }

    /** 注册,注册后直接登录 */
    fun register(email: String, pwd: String, code: String): ApiHttp<LoginModel> {
        val parameter = deviceParameter + mapOf("email" to email, "pwd" to pwd, "code" to code)
        return ApiHttp<LoginModel>(ApiConst.USER_REGISTER, "POST", parameter).noAuth()
    }

    /** 重置密码,所有设备需要重新登录 */
    fun resetPwd(email: String, code: String, pwd: String): ApiHttp<EmptyModel> {
        return ApiHttp<EmptyModel>(ApiConst.USER_RESET_PWD, "POST", mapOf("email" to email, "code" to code, "pwd" to pwd)).noAuth()
    }

    /** 绑定邮箱:临时会员绑定之后成为正式会员,pwd为新的登录密码;只有用户名没有邮箱的会员pwd为当前的登录密码,用来确认身份,密码不会修改 */
    fun bindEmail(email: String, code: String, pwd: String): ApiHttp<UserModel> {
        return ApiHttp(ApiConst.USER_BIND_EMAIL, "POST", mapOf("email" to email, "code" to code, "pwd" to pwd))
    }

    /** 会员信息 */
    fun info(): ApiHttp<UserModel> {
        return ApiHttp(ApiConst.USER_INFO, "GET")
    }

    /** 修改昵称 */
    fun edit(nick: String): ApiHttp<EmptyModel> {
        return ApiHttp(ApiConst.USER_EDIT, "POST", mapOf("nick" to nick))
    }

    /** 上传头像,返回头像地址;不限大小,服务器会居中裁切成正方形并缩小 */
    fun avatar(jpeg: ByteArray): ApiHttp<String> {
        return ApiHttp<String>(ApiConst.USER_AVATAR, "POST").file("file", "avatar.jpg", "image/jpeg", jpeg)
    }

    /** 修改密码,其他设备需要重新登录 */
    fun modifyPwd(oldPwd: String, pwd: String): ApiHttp<EmptyModel> {
        return ApiHttp(ApiConst.USER_MODIFY_PWD, "POST", mapOf("oldPwd" to oldPwd, "pwd" to pwd))
    }

    /** 注销账号 */
    fun unregister(pwd: String): ApiHttp<EmptyModel> {
        return ApiHttp(ApiConst.USER_UNREGISTER, "POST", mapOf("pwd" to pwd))
    }

    /** 退出登录 */
    fun logout(): ApiHttp<EmptyModel> {
        return ApiHttp(ApiConst.USER_LOGOUT, "DELETE")
    }
}
