package cn.dairo.music.page.modifypwd

import androidx.compose.runtime.Composable
import cn.dairo.music.util.rememberStateObject
import cn.dairo.ui.setting.SettingButtonSingle
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.setting.SettingTextField
import cn.dairo.ui.setting.SettingTextFieldType

@Composable
fun ModifyPwdPage() {

    //密码信息
    val vm = rememberStateObject { ModifyPwdViewModel() }
    SettingStack(embedInNavigationStack = false) {
        SettingPage(title = "修改密码") {
            SettingGroup(footer = "修改之后其他设备需要重新登录") {
                SettingTextField(title = "旧密码", text = vm.oldPwd, onTextChange = { vm.oldPwd = it }, placeholder = "请输入当前密码", type = SettingTextFieldType.PASSWORD)
                SettingTextField(title = "新密码", text = vm.pwd, onTextChange = { vm.pwd = it }, placeholder = "6~32位", type = SettingTextFieldType.PASSWORD)
                SettingTextField(title = "确认密码", text = vm.repwd, onTextChange = { vm.repwd = it }, placeholder = "请确认新密码", type = SettingTextFieldType.PASSWORD)
            }
            SettingGroup {
                SettingButtonSingle("修改", onClick = vm::onModifyClick)
            }
        }
    }
}
