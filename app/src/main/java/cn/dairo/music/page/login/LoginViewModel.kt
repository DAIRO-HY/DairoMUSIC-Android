package cn.dairo.music.page.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.music.api.CommonApi
import cn.dairo.music.api.UserApi
import cn.dairo.music.components.relaunch
import cn.dairo.music.page.discover.DiscoverPage
import cn.dairo.music.store.SettingStore
import cn.dairo.music.util.ObservableObject
import cn.dairo.ui.root.Toast

/**
 * 服务器页面和登录页面共用
 * @param editAccountIndex 编辑模式时,当前账号所在的索引
 */
class LoginViewModel(domain: String = "", account: String = "", val editAccountIndex: Int? = null) : ObservableObject() {

    /** 服务器地址,在服务器页面输入,登录页面只显示 */
    var domain by mutableStateOf(domain)

    /** 登录账号:邮箱或用户名 */
    var account by mutableStateOf(account)

    /** 密码 */
    var pwd by mutableStateOf("")

    /** 服务器是否开放注册,关闭时隐藏注册和免注册使用,还没有获取到时为null */
    var allowRegister by mutableStateOf<Boolean?>(null)

    /** 从服务器页面跳转到登录页面 */
    var showLogin by mutableStateOf(false)

    /** 服务器页面下一步点击事件,确认是DairoMUSIC服务器之后跳转到登录页面 */
    fun onNextClick() {
        var domain = this.domain.trim()
        if (domain.isEmpty()) {
            Toast.show("请输入服务器地址")
            return
        }
        if (!domain.startsWith("http://") && !domain.startsWith("https://")) {

            //补全url
            domain = "http://$domain"
        }

        //去掉最后的/
        domain = domain.trimEnd('/')
        SettingStore.checkServer(domain) { config ->
            this.domain = domain
            this.allowRegister = config.allowRegister
            this.showLogin = true
        }
    }

    /** 直接打开登录页面时(登录失效、编辑账号),获取服务器是否开放注册 */
    fun loadConfig() {
        if (this.allowRegister != null || this.domain.isEmpty()) {
            return
        }
        CommonApi.config().domain(this.domain).hide().fail { }.error { }.send {
            this.allowRegister = it.allowRegister
        }
    }

    /** 登录按钮点击事件 */
    fun onLoginClick() {
        val account = this.account.trim()
        if (account.isEmpty()) {
            Toast.show("请输入邮箱或用户名")
            return
        }
        val pwd = this.pwd
        if (pwd.isEmpty()) {
            Toast.show("密码必填")
            return
        }
        SettingStore.login(this.domain, this.editAccountIndex, request = {
            UserApi.login(account, pwd)
        }, success = {
            relaunch { DiscoverPage() }
        })
    }

    /** 免注册使用点击事件,同一台设备再次使用时找回同一个临时会员 */
    fun onTempLoginClick() {
        SettingStore.login(this.domain, this.editAccountIndex, request = {
            UserApi.tempLogin()
        }, success = {
            relaunch { DiscoverPage() }
        })
    }
}
