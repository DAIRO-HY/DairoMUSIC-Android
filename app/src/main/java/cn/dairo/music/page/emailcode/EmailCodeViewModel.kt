package cn.dairo.music.page.emailcode

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.music.api.CommonApi
import cn.dairo.music.api.UserApi
import cn.dairo.music.components.relaunch
import cn.dairo.music.page.discover.DiscoverPage
import cn.dairo.music.page.login.LoginPage
import cn.dairo.music.page.login.LoginViewModel
import cn.dairo.music.page.mine.MinePage
import cn.dairo.music.store.SettingStore
import cn.dairo.music.util.ObservableObject
import cn.dairo.ui.root.Toast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 需要邮箱验证码的操作
 */
enum class EmailCodeType(val value: String) {

    /** 注册 */
    REGISTER("register"),

    /** 重置密码 */
    RESET_PWD("reset_pwd"),

    /** 绑定邮箱:临时会员,或者后台创建的只有用户名的会员 */
    BIND_EMAIL("bind_email");

    /** 页面标题 */
    val title: String
        get() = when (this) {
            REGISTER -> "注册"
            RESET_PWD -> "重置密码"
            BIND_EMAIL -> "绑定邮箱"
        }
}

/**
 * @param domain 服务器地址,从登录页面传过来,绑定邮箱时不需要
 */
class EmailCodeViewModel(val type: EmailCodeType, val domain: String) : ObservableObject() {

    companion object {

        /** 重新发送验证码的等待时间(秒) */
        private const val RESEND_SECONDS = 60
    }

    var email by mutableStateOf("")

    var code by mutableStateOf("")

    var pwd by mutableStateOf("")

    var repwd by mutableStateOf("")

    /** 距离可以重新发送验证码的秒数,0表示可以发送 */
    var countdown by mutableIntStateOf(0)

    private var timer: Job? = null

    /** 只有用户名的会员绑定邮箱:密码填当前的登录密码确认身份,不修改密码 */
    val isVerifyPwd = type == EmailCodeType.BIND_EMAIL && SettingStore.user?.isTemp == false

    /** 发送验证码点击事件 */
    fun onSendCodeClick() {
        val email = this.email.trim()
        if (email.isEmpty()) {
            Toast.show("请输入邮箱")
            return
        }
        val onSent = { _: Any ->
            Toast.show("验证码已发送,请查收邮件")
            this.startCountdown()
        }
        if (this.type == EmailCodeType.BIND_EMAIL) {
            CommonApi.sendEmailCode(email, this.type.value).send(onSent)
            return
        }

        //注册和重置密码时还没有登录,请求登录页面选择的服务器
        val domain = this.domain
        SettingStore.checkServer(domain) { config ->
            if (this.type == EmailCodeType.REGISTER && !config.allowRegister) {
                Toast.show("该服务器没有开放注册")
                return@checkServer
            }
            CommonApi.sendEmailCode(email, this.type.value).domain(domain).send(onSent)
        }
    }

    /** 开始倒计时 */
    private fun startCountdown() {
        this.countdown = RESEND_SECONDS
        this.timer?.cancel()
        this.timer = this.scope.launch {
            while (countdown > 0) {
                delay(1000)
                countdown -= 1
            }
        }
    }

    /** 提交按钮点击事件 */
    fun onSubmitClick() {
        val email = this.email.trim()
        if (email.isEmpty()) {
            Toast.show("请输入邮箱")
            return
        }
        if (this.code.trim().isEmpty()) {
            Toast.show("请输入验证码")
            return
        }
        if (this.isVerifyPwd) {
            if (this.pwd.isEmpty()) {
                Toast.show("请输入当前的登录密码")
                return
            }
        } else if (this.pwd.length < 6 || this.pwd.length > 32) {
            Toast.show("密码长度为6~32位")
            return
        } else if (this.pwd != this.repwd) {
            Toast.show("两次输入的密码不一致")
            return
        }
        val code = this.code.trim()
        val pwd = this.pwd
        when (this.type) {
            EmailCodeType.REGISTER -> SettingStore.login(this.domain, request = {
                UserApi.register(email, pwd, code)
            }, success = {
                Toast.show("注册成功")
                relaunch { DiscoverPage() }
            })

            EmailCodeType.RESET_PWD -> {
                val domain = this.domain
                UserApi.resetPwd(email, code, pwd).domain(domain).send {
                    Toast.show("密码已重置,请重新登录")
                    val vm = LoginViewModel(domain = domain, account = email)
                    relaunch { LoginPage(vm) }
                }
            }

            EmailCodeType.BIND_EMAIL -> UserApi.bindEmail(email, code, pwd).send {
                Toast.show("绑定成功")
                SettingStore.loadUserInBackground()
                relaunch { MinePage() }
            }
        }
    }
}
