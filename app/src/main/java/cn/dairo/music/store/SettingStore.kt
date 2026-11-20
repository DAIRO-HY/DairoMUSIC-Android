package cn.dairo.music.store

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import cn.dairo.music.api.CommonApi
import cn.dairo.music.api.UserApi
import cn.dairo.music.api.http.ApiHttp
import cn.dairo.music.api.model.ConfigModel
import cn.dairo.music.api.model.LoginModel
import cn.dairo.music.api.model.UserModel
import cn.dairo.music.api.model.VipModel
import cn.dairo.music.components.relaunch
import cn.dairo.music.model.AccountInfo
import cn.dairo.music.page.login.LoginPage
import cn.dairo.music.page.login.LoginViewModel
import cn.dairo.music.page.login.ServerPage
import cn.dairo.music.page.playing.wave.PlayingWaveStyle
import cn.dairo.music.player.PlayerManager
import cn.dairo.music.util.KeychainUtil
import cn.dairo.music.util.PlayCountUtil
import cn.dairo.music.util.ServerConfig
import cn.dairo.ui.DairoUI
import cn.dairo.ui.root.Toast
import cn.dairo.ui.util.FileUtil
import cn.dairo.ui.util.LocalObjectUtil
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * 用户设置
 */
object SettingStore {

    /** 设置保存的SharedPreferences,对应iOS的UserDefaults */
    private val preferences: SharedPreferences by lazy {
        DairoUI.context.getSharedPreferences("setting", Context.MODE_PRIVATE)
    }

    /*----------------------------------------------------------------------------------*/

    /** 会员信息 */
    private const val KEY_USER = "user"

    /** 会员信息 */
    var user: UserModel?
        get() = LocalObjectUtil.read<UserModel>(KEY_USER)
        set(value) {
            LocalObjectUtil.write(value, KEY_USER)
            ServerConfig.updateVip(value?.vip)
        }

    /** 更新本地保存的VIP状态(兑换VIP之后) */
    fun updateVip(vip: VipModel) {
        val user = this.user ?: return
        this.user = user.copy(vip = vip)
    }

    /**
     * 后台刷新会员信息,不弹出加载等待框
     * @param silent 失败时不提示(启动App、回到前台时刷新VIP状态)
     */
    fun loadUserInBackground(silent: Boolean = false, success: (user: UserModel) -> Unit = {}) {
        var request = UserApi.info().hide()
        if (silent) {
            request = request.fail { }.error { }
        }
        request.send {
            this.user = it
            this.updateCurrentAccount(it)
            success(it)
        }
    }

    /*----------------------------------------------------------------------------------*/

    /** 登录Token */
    private const val KEY_TOKEN = "TOKEN"
    private var mToken: String? = null

    /** 登录Token,加密保存 */
    var token: String
        get() {
            mToken?.let { return it }
            val token = KeychainUtil.read(KEY_TOKEN) ?: ""
            mToken = token
            return token
        }
        set(value) {
            mToken = value
            if (value.isEmpty()) {
                KeychainUtil.delete(KEY_TOKEN)
                return
            }
            KeychainUtil.write(value, KEY_TOKEN)
        }

    /*----------------------------------------------------------------------------------*/

    /** 当前服务器地址 */
    private const val KEY_DOMAIN = "DOMAIN"
    private var mDomain: String? = null

    val domainNotNull: String
        get() = this.domain ?: ""

    var domain: String?
        get() {
            if (mDomain == null) {
                mDomain = preferences.getString(KEY_DOMAIN, null)
            }
            return mDomain
        }
        set(value) {
            mDomain = value
            if (value == null) {
                preferences.edit().remove(KEY_DOMAIN).apply()
                return
            }
            preferences.edit().putString(KEY_DOMAIN, value).apply()
        }

    /** 服务器ID,作为SDK的缓存命名空间,服务器换了地址之后缓存依然有效 */
    var serverId: String by StringPreference("SERVER_ID", "")

    /*----------------------------------------------------------------------------------*/

    /** 登录过的账号列表的文件名 */
    private const val KEY_ACCOUNT_LIST = "account_list"

    private var mAccountList: List<AccountInfo>? = null

    /** 登录过的账号列表,每个账号的登录票据加密保存,不保存到文件 */
    var accountList: List<AccountInfo>
        get() {
            mAccountList?.let { return it }
            val list = (LocalObjectUtil.read<List<AccountInfo>>(KEY_ACCOUNT_LIST) ?: emptyList()).map {
                it.copy(token = KeychainUtil.read(it.tokenKey) ?: "")
            }
            mAccountList = list
            return list
        }
        set(value) {

            //删除已经不在列表里的账号的票据
            for (old in this.accountList) {
                if (value.none { it.isSame(old) }) {
                    KeychainUtil.delete(old.tokenKey)
                }
            }
            for (account in value) {
                if (account.token.isEmpty()) {
                    KeychainUtil.delete(account.tokenKey)
                } else {
                    KeychainUtil.write(account.token, account.tokenKey)
                }
            }
            mAccountList = value
            LocalObjectUtil.write(value, KEY_ACCOUNT_LIST)
        }

    /** 当前登录的账号 */
    val currentAccount: AccountInfo?
        get() = this.accountList.firstOrNull { it.isCurrent }

    /** 是否登录 */
    val isLogin: Boolean
        get() = this.token.isNotEmpty() && this.serverId.isNotEmpty()

    /** 获取当前账号的本地对象缓存名,不同账号的缓存互相隔离,没有登录时返回null,此时不使用缓存 */
    fun accountObjectName(name: String): String? {
        if (!this.isLogin) {
            return null
        }
        val account = this.currentAccount ?: return null
        val folder = "account_cache/${account.serverId}_${account.userId}"
        FileUtil.createDirectory(LocalObjectUtil.resolvedFolder.absolutePath + "/" + folder)
        return "$folder/$name"
    }

    /** 退出登录 */
    fun logout() {
        PlayerManager.stop()
        this.accountList = this.accountList.map {
            it.copy(token = if (it.isCurrent) "" else it.token, isCurrent = false)
        }
        this.token = ""
        this.user = null
        this.onAccountChange()
    }

    /** 退出登录,并跳转到登录页面,自动填写当前账号的信息 */
    fun toLoginPage() {
        val account = this.currentAccount
        this.logout()
        if (account != null) {
            val vm = LoginViewModel(domain = account.domain, account = account.account)
            relaunch { LoginPage(vm) }
        } else {
            relaunch { ServerPage() }
        }
    }

    /** 获取服务器的公开配置,同时确认是否DairoMUSIC服务器 */
    fun checkServer(domain: String, success: (config: ConfigModel) -> Unit) {
        CommonApi.config().domain(domain).fail {
            Toast.show(it.msg ?: "获取服务器信息失败,请确认是否DairoMUSIC服务器")
        }.send(success)
    }

    /**
     * 登录,登录类接口都返回票据和会员信息,先确认服务器地址可用再登录,登录成功之后才切换服务器
     * @param domain 服务器地址
     * @param editIndex 编辑账号时,账号在已登录列表里的序号,登录成功之后替换这个账号
     * @param request 登录请求
     */
    fun login(domain: String, editIndex: Int? = null, request: () -> ApiHttp<LoginModel>, success: () -> Unit) {
        this.checkServer(domain) { config ->
            request().domain(domain).fail {
                Toast.show(it.msg ?: "登录失败")
            }.send { loginInfo ->//登录成功
                PlayerManager.stop()
                val user = loginInfo.user
                val account = AccountInfo(
                    domain = domain, serverId = config.serverId, userId = user.id, account = this.loginAccount(user),
                    name = this.accountName(user), token = loginInfo.token, isCurrent = true,
                )
                this.accountList = this.addAccount(account, editIndex)
                this.domain = domain
                this.serverId = config.serverId
                this.token = loginInfo.token
                this.user = user
                this.onAccountChange()
                success()
            }
        }
    }

    /** 将登录成功的账号保存到已登录列表,同一个服务器的同一个会员只保留一条,编辑时保持账号原来的位置 */
    private fun addAccount(account: AccountInfo, editIndex: Int?): List<AccountInfo> {
        val list = this.accountList.map { it.copy(isCurrent = false) }.toMutableList()
        if (editIndex == null || editIndex !in list.indices) {
            list.removeAll { it.isSame(account) }
            list.add(account)
            return list
        }
        list[editIndex] = account
        return list.filterIndexed { i, it -> i == editIndex || !it.isSame(account) }
    }

    /** 切换到已经登录过的账号,票据失效时会跳转到登录页面 */
    fun switchAccount(account: AccountInfo, success: () -> Unit) {
        val oldDomain = this.domain
        val oldToken = this.token
        val oldServerId = this.serverId
        val oldAccountList = this.accountList
        val onFail = { msg: String ->
            this.domain = oldDomain
            this.accountList = oldAccountList
            this.serverId = oldServerId
            this.token = oldToken
            Toast.show(msg)
        }
        this.domain = account.domain
        this.accountList = oldAccountList.map { it.copy(isCurrent = it.isSame(account)) }
        this.serverId = account.serverId
        this.token = account.token
        UserApi.info().error(onFail).fail {
            onFail(it.msg ?: "切换账号失败")
        }.send {
            PlayerManager.stop()
            this.user = it
            this.updateCurrentAccount(it)
            this.onAccountChange()
            success()
        }
    }

    /** 会员信息变化之后,更新已登录列表里显示的名字,绑定邮箱之后也会更新登录账号 */
    private fun updateCurrentAccount(user: UserModel) {
        val list = this.accountList.toMutableList()
        val index = list.indexOfFirst { it.isCurrent }
        if (index < 0) {
            return
        }
        list[index] = list[index].copy(account = this.loginAccount(user), name = this.accountName(user))
        this.accountList = list
    }

    /** 登录账号,有邮箱时用邮箱,没有邮箱时用用户名,临时会员为空 */
    private fun loginAccount(user: UserModel): String {
        val email = user.email
        if (!email.isNullOrEmpty()) {
            return email
        }
        return user.name ?: ""
    }

    /** 账号显示的名字,有登录账号时显示登录账号,临时会员显示昵称 */
    private fun accountName(user: UserModel): String {
        val account = this.loginAccount(user)
        if (account.isNotEmpty()) {
            return account
        }
        return user.nick ?: "临时会员"
    }

    /** 账号变化之后,清空内存里上一个账号的数据,加载新账号的播放列表 */
    private fun onAccountChange() {
        FavoriteStore.clearMemory()
        MusicStore.clearMemory()
        PlayerManager.restore()

        //提交新账号上次没有提交成功的播放记录
        PlayCountUtil.submit()
    }

    /*----------------------------------------------------------------------------------*/

    /** 无损音质的比特率 */
    const val BIT_RATE_LOSSLESS = 999

    /** 标准音质的比特率,非VIP只能听标准音质 */
    const val BIT_RATE_STANDARD = 128

    /** 目标音质,返回不超过这个音质的最高音质文件 */
    var bitRate: Int by IntPreference("BIT_RATE", 320)

    /*----------------------------------------------------------------------------------*/

    /** 视频清晰度:原画 */
    const val VIDEO_QUALITY_ORIGINAL = 1

    /** 视频清晰度:流畅 */
    const val VIDEO_QUALITY_SMOOTH = 2

    /** 视频清晰度,记住上次的选择,默认流畅 */
    var videoQuality: Int by IntPreference("VIDEO_QUALITY", VIDEO_QUALITY_SMOOTH)

    /** 播放页环形波形的样式,值是PlayingWaveStyle,页面读取时自动刷新 */
    var playingWaveStyle: Int by IntPreference("PLAYING_WAVE_STYLE", PlayingWaveStyle.BARS.value)

    /** 播放页是否显示低音冲击波,页面读取时自动刷新 */
    var playingWaveShockwave: Boolean by BooleanPreference("PLAYING_WAVE_SHOCKWAVE", true)

    /** 播放页波形颜色是否跟随封面,页面读取时自动刷新 */
    var playingWaveCoverColor: Boolean by BooleanPreference("PLAYING_WAVE_COVER_COLOR", true)

    /** 播放页左右滑动的引导是否已经显示过 */
    var playingGuideShown: Boolean by BooleanPreference("PLAYING_GUIDE_SHOWN", false)

    /** 音乐是否边播边缓存,默认开启;关闭时没有缓存的歌曲直接在线播放 */
    var musicCache: Boolean by BooleanPreference("MUSIC_CACHE", true)

    /** 视频是否边播边缓存,默认在线播放 */
    var videoCache: Boolean by BooleanPreference("VIDEO_CACHE", false)

    /*----------------------------------------------------------------------------------*/

    /** 同时缓存文件数,0表示没有设置过 */
    var fileSdkMaxCachingCount: Int by IntPreference("FILE_SDK_MAX_CACHING_COUNT", 0)

    /** 缓存文件保存天数,null表示没有设置过,0表示永久 */
    var fileSdkCacheExpireDays: Int?
        get() = if (preferences.contains("FILE_SDK_CACHE_EXPIRE_DAYS")) preferences.getInt("FILE_SDK_CACHE_EXPIRE_DAYS", 0) else null
        set(value) {
            if (value == null) {
                preferences.edit().remove("FILE_SDK_CACHE_EXPIRE_DAYS").apply()
            } else {
                preferences.edit().putInt("FILE_SDK_CACHE_EXPIRE_DAYS", value).apply()
            }
        }

    /** 缓存容量上限(MB),0表示没有设置过 */
    var fileSdkMaxCacheSize: Int by IntPreference("FILE_SDK_MAX_CACHE_SIZE", 0)

    /*----------------------------------------------------------------------------------*/

    /**
     * 保存在SharedPreferences里的设置,对应iOS页面上的@AppStorage
     * 值同时保存在Compose的状态里,页面读取之后,值变化时页面自动刷新
     */
    private abstract class Preference<T>(private val key: String, private val defaultValue: T) : ReadWriteProperty<Any?, T> {

        private var state: MutableState<T>? = null

        abstract fun get(preferences: SharedPreferences, key: String, defaultValue: T): T

        abstract fun put(editor: SharedPreferences.Editor, key: String, value: T)

        private fun state(): MutableState<T> {
            this.state?.let { return it }
            val state = mutableStateOf(this.get(preferences, this.key, this.defaultValue))
            this.state = state
            return state
        }

        override fun getValue(thisRef: Any?, property: KProperty<*>): T = this.state().value

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            this.state().value = value
            val editor = preferences.edit()
            this.put(editor, this.key, value)
            editor.apply()
        }
    }

    private class IntPreference(key: String, defaultValue: Int) : Preference<Int>(key, defaultValue) {
        override fun get(preferences: SharedPreferences, key: String, defaultValue: Int) = preferences.getInt(key, defaultValue)
        override fun put(editor: SharedPreferences.Editor, key: String, value: Int) {
            editor.putInt(key, value)
        }
    }

    private class BooleanPreference(key: String, defaultValue: Boolean) : Preference<Boolean>(key, defaultValue) {
        override fun get(preferences: SharedPreferences, key: String, defaultValue: Boolean) = preferences.getBoolean(key, defaultValue)
        override fun put(editor: SharedPreferences.Editor, key: String, value: Boolean) {
            editor.putBoolean(key, value)
        }
    }

    private class StringPreference(key: String, defaultValue: String) : Preference<String>(key, defaultValue) {
        override fun get(preferences: SharedPreferences, key: String, defaultValue: String) = preferences.getString(key, defaultValue) ?: defaultValue
        override fun put(editor: SharedPreferences.Editor, key: String, value: String) {
            editor.putString(key, value)
        }
    }
}
