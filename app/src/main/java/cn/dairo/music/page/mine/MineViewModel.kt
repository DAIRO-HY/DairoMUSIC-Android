package cn.dairo.music.page.mine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.music.api.UserApi
import cn.dairo.music.components.relaunch
import cn.dairo.music.model.AccountInfo
import cn.dairo.music.page.login.ServerPage
import cn.dairo.music.store.SettingStore
import cn.dairo.music.util.ObservableObject
import cn.dairo.music.util.ServerConfig
import cn.dairo.ui.root.Toast

class MineViewModel : ObservableObject() {

    /** 当前登录的账号信息 */
    var account by mutableStateOf<AccountInfo?>(SettingStore.currentAccount)

    /** 当前登录的会员信息 */
    var user by mutableStateOf(SettingStore.user)

    /** 目标音质 */
    var bitRate by mutableIntStateOf(SettingStore.bitRate)

    /** 显示的名字 */
    val displayName: String
        get() {
            val nick = this.user?.nick
            if (!nick.isNullOrEmpty()) {
                return nick
            }
            return this.account?.name ?: ""
        }

    /** 刷新会员信息,昵称和头像可能在其他设备上修改过 */
    fun loadUser() {
        this.user = SettingStore.user
        SettingStore.loadUserInBackground {
            this.user = it
            this.account = SettingStore.currentAccount
        }
    }

    /**
     * 修改播放音质,歌曲里有所有音质的文件,每首歌开始播放前按设置选择,不用重新获取歌单
     * 非VIP只能选标准音质,不保存,保留自己设置的音质,开通VIP之后恢复
     */
    fun onBitRateChange(bitRate: Int): Boolean {
        if (ServerConfig.isVipLimited) {
            if (bitRate > SettingStore.BIT_RATE_STANDARD) {
                Toast.show("超高品质、无损音质需要VIP")
                return false
            }
            return true
        }
        SettingStore.bitRate = bitRate
        return true
    }

    /** 退出登录点击事件 */
    fun onLogoutClick() {

        //让服务端删除这个票据
        UserApi.logout().hide().send {}
        SettingStore.logout()
        relaunch { ServerPage() }
    }
}
