package cn.dairo.music.util

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.music.api.CommonApi
import cn.dairo.music.api.model.VipModel
import cn.dairo.music.store.SettingStore
import cn.dairo.ui.root.Toast

/**
 * 服务器的配置和当前会员的VIP状态,进入首页和App回到前台时刷新,管理后台修改之后不用重新登录
 * 属性都是Compose的状态,页面读取之后,变化时自动刷新
 */
object ServerConfig {

    /** 是否允许把下载的视频保存到相册,不允许时不显示入口;VIP功能生效时只有VIP才能用 */
    var allowSaveAlbum by mutableStateOf(false)
        private set

    /** 是否允许发送下载的歌曲、视频文件,不允许时不显示入口;VIP功能生效时只有VIP才能用 */
    var allowSendFile by mutableStateOf(false)
        private set

    /** 服务器是否开启了视频采集,获取到配置之前按开启处理 */
    var videoEnable by mutableStateOf(true)
        private set

    /** 当前会员的VIP状态,会员信息保存时更新;没有登录时为null,按VIP功能没有生效处理 */
    var vip by mutableStateOf<VipModel?>(null)
        private set

    /** 是否已经读取了本地保存的VIP状态 */
    private var isVipLoaded = false

    private val handler = Handler(Looper.getMainLooper())

    /** App启动时读取本地保存的VIP状态 */
    fun init() {
        if (!isVipLoaded) {
            isVipLoaded = true
            this.vip = SettingStore.user?.vip
        }
    }

    /** VIP功能对当前会员是否生效,没有生效时不显示任何VIP相关的内容 */
    val isVipEnable: Boolean
        get() = this.vip?.enable == true

    /** 是否受非VIP的限制:只能听、下载标准音质和流畅版视频,不能发送文件、保存到相册 */
    val isVipLimited: Boolean
        get() {
            val vip = this.vip ?: return false
            return vip.enable && !vip.isVip
        }

    /**
     * 文件出App(发送文件、保存到相册)是VIP特权,非VIP点击时提示需要开通VIP
     * @param action 提示里的操作名称,例如"发送文件"
     * @return 是否可以继续
     */
    fun checkFileOutVip(action: String): Boolean {
        if (this.isVipLimited) {
            Toast.show(action + "需要开通VIP")
            return false
        }
        return true
    }

    /** 更新VIP状态 */
    fun updateVip(vip: VipModel?) {
        isVipLoaded = true
        if (Looper.myLooper() == Looper.getMainLooper()) {
            this.vip = vip
        } else {
            handler.post { this.vip = vip }
        }
    }

    /** 从服务器获取配置,没有登录时不处理 */
    fun load() {
        if (!SettingStore.isLogin) {
            return
        }
        CommonApi.config().hide().fail { }.error { }.send { config ->
            this.allowSaveAlbum = config.allowSaveAlbum
            this.allowSendFile = config.allowSendFile
            this.videoEnable = config.videoEnable
        }
    }
}
