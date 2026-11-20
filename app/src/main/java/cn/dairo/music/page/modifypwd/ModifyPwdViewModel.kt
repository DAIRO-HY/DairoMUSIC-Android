package cn.dairo.music.page.modifypwd

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.music.api.UserApi
import cn.dairo.music.util.ObservableObject
import cn.dairo.ui.root.Toast

class ModifyPwdViewModel : ObservableObject() {

    /** 旧密码 */
    var oldPwd by mutableStateOf("")

    /** 新密码 */
    var pwd by mutableStateOf("")

    /** 确认新密码 */
    var repwd by mutableStateOf("")

    /** 修改按钮点击事件 */
    fun onModifyClick() {
        if (this.oldPwd.isEmpty()) {
            Toast.show("请输入旧密码")
            return
        }
        if (this.pwd.length < 6 || this.pwd.length > 32) {
            Toast.show("新密码长度为6~32位")
            return
        }
        if (this.pwd != this.repwd) {
            Toast.show("确认密码和新密码不一致")
            return
        }
        UserApi.modifyPwd(this.oldPwd, this.pwd).send {
            Toast.show("修改成功")
            this.oldPwd = ""
            this.pwd = ""
            this.repwd = ""
        }
    }
}
