package cn.dairo.music.api

import cn.dairo.music.api.http.ApiHttp
import cn.dairo.music.api.http.EmptyModel
import cn.dairo.music.api.model.ConfigModel
import cn.dairo.music.constants.Const

object CommonApi {

    /** 获取服务器的公开配置,不需要登录 */
    fun config(): ApiHttp<ConfigModel> {
        return ApiHttp<ConfigModel>(ApiConst.COMMON_CONFIG, "GET").noAuth()
    }

    /** 发送邮箱验证码 type:register:注册 reset_pwd:重置密码 bind_email:绑定邮箱 */
    fun sendEmailCode(email: String, type: String): ApiHttp<EmptyModel> {
        return ApiHttp<EmptyModel>(ApiConst.EMAIL_SEND_CODE, "POST", mapOf("email" to email, "type" to type)).noAuth()
    }

    /** 意见反馈,返回给用户看的提示文字 */
    fun feedback(content: String): ApiHttp<String> {
        return ApiHttp(ApiConst.FEEDBACK_ADD, "POST", mapOf("content" to content, "clientFlag" to Const.CLIENT_FLAG))
    }
}
