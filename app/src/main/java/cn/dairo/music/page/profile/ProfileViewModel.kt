package cn.dairo.music.page.profile

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.music.api.UserApi
import cn.dairo.music.components.relaunch
import cn.dairo.music.extension.toUploadJpeg
import cn.dairo.music.page.login.ServerPage
import cn.dairo.music.store.SettingStore
import cn.dairo.music.util.ObservableObject
import cn.dairo.ui.root.Toast

class ProfileViewModel : ObservableObject() {

    /** 当前登录的会员信息 */
    var user by mutableStateOf(SettingStore.user)

    /** 昵称 */
    var nick by mutableStateOf(SettingStore.user?.nick ?: "")

    /** 注销账号时验证的密码 */
    var unregisterPwd by mutableStateOf("")

    /** 保存昵称点击事件 */
    fun onSaveNickClick() {
        val nick = this.nick.trim()
        if (nick.isEmpty()) {
            Toast.show("请输入昵称")
            return
        }
        UserApi.edit(nick).send {
            Toast.show("保存成功")
            this.loadUser()
        }
    }

    /** 选择头像之后上传 */
    fun onAvatarPick(image: Bitmap) {
        val jpeg = image.toUploadJpeg()
        if (jpeg == null) {
            Toast.show("读取图片失败")
            return
        }
        UserApi.avatar(jpeg).send {
            this.loadUser()
        }
    }

    /** 注销账号点击事件 */
    fun onUnregisterClick() {
        UserApi.unregister(this.unregisterPwd).send {
            Toast.show("账号已注销")
            val account = SettingStore.currentAccount
            SettingStore.logout()

            //注销之后这个账号不能再登录,从已登录列表里删除
            if (account != null) {
                SettingStore.accountList = SettingStore.accountList.filter { !it.isSame(account) }
            }
            relaunch { ServerPage() }
        }
    }

    /** 刷新会员信息 */
    private fun loadUser() {
        SettingStore.loadUserInBackground {
            this.user = it
        }
    }
}
