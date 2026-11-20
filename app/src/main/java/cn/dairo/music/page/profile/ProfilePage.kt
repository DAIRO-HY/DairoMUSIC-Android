package cn.dairo.music.page.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.dairo.music.components.AlertButton
import cn.dairo.music.components.ButtonRole
import cn.dairo.music.components.TextFieldAlert
import cn.dairo.music.components.UserAvatar
import cn.dairo.music.extension.secondaryText
import cn.dairo.music.page.emailcode.EmailCodePage
import cn.dairo.music.page.emailcode.EmailCodeType
import cn.dairo.music.util.rememberStateObject
import cn.dairo.ui.image.rememberSystemImagePicker
import cn.dairo.ui.setting.SettingButtonSingle
import cn.dairo.ui.setting.SettingCustomView
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingNavigationLink
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.setting.SettingText
import cn.dairo.ui.setting.SettingTextField

@Composable
fun ProfilePage() {
    val vm = rememberStateObject { ProfileViewModel() }

    /** 显示注销账号的密码输入框 */
    var showUnregisterAlert by remember { mutableStateOf(false) }

    /** 系统图片选择器,不需要相册权限 */
    val pickImage = rememberSystemImagePicker { bitmap ->
        bitmap?.let { vm.onAvatarPick(it) }
    }
    SettingStack(embedInNavigationStack = false) {
        SettingPage(title = "个人资料") {
            SettingCustomView {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = pickImage).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        UserAvatar(vm.user?.logo, 80.dp)
                        Text("更换头像", style = MaterialTheme.typography.bodySmall, color = Color.secondaryText)
                    }
                }
            }
            SettingGroup {
                SettingTextField(title = "昵称", text = vm.nick, onTextChange = { vm.nick = it }, placeholder = "请输入昵称")
                val name = vm.user?.name
                if (!name.isNullOrEmpty()) {
                    SettingText("用户名：$name")
                }
                val email = vm.user?.email
                if (!email.isNullOrEmpty()) {
                    SettingText("邮箱：$email")
                } else if (vm.user != null) {

                    //临时会员和后台创建的只有用户名的会员都可以绑定邮箱
                    SettingNavigationLink("绑定邮箱", tip = if (vm.user?.isTemp == true) "可用邮箱登录" else "用于找回密码") {
                        EmailCodePage(EmailCodeType.BIND_EMAIL)
                    }
                }
            }
            SettingGroup {
                SettingButtonSingle("保存昵称", onClick = vm::onSaveNickClick)
            }
            if (vm.user?.isTemp == false) {
                SettingGroup(footer = if (vm.user?.email == null) "注销之后歌单都会删除" else "注销之后歌单都会删除,邮箱可以重新注册") {
                    SettingButtonSingle("注销账号") {
                        vm.unregisterPwd = ""
                        showUnregisterAlert = true
                    }
                }
            }
        }
    }
    TextFieldAlert(
        isPresented = showUnregisterAlert,
        title = "注销账号",
        message = "注销之后不能恢复,请输入密码确认",
        text = vm.unregisterPwd,
        onTextChange = { vm.unregisterPwd = it },
        placeholder = "请输入密码",
        isPassword = true,
        onDismiss = { showUnregisterAlert = false },
        buttons = listOf(
            AlertButton("注销", ButtonRole.DESTRUCTIVE, vm::onUnregisterClick),
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
}
