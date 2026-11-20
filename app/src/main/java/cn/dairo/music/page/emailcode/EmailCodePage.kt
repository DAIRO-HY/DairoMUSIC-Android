package cn.dairo.music.page.emailcode

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.dairo.music.components.SettingInputRow
import cn.dairo.music.page.login.ServerLabel
import cn.dairo.music.util.rememberStateObject
import cn.dairo.ui.setting.SettingButtonSingle
import cn.dairo.ui.setting.SettingCustomView
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.setting.SettingTextField
import cn.dairo.ui.setting.SettingTextFieldType
import cn.dairo.ui.setting.settingColors

/**
 * 注册、重置密码、绑定邮箱,都需要邮箱验证码
 */
@Composable
fun EmailCodePage(type: EmailCodeType, domain: String = "") {
    val vm = rememberStateObject { EmailCodeViewModel(type, domain) }
    SettingStack(embedInNavigationStack = false) {
        SettingPage(title = vm.type.title) {
            if (vm.type != EmailCodeType.BIND_EMAIL) {
                SettingCustomView {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        ServerLabel(vm.domain)
                    }
                }
            }
            SettingGroup(footer = footer(vm)) {
                SettingTextField(title = "邮箱", text = vm.email, onTextChange = { vm.email = it }, placeholder = "请输入邮箱", keyboardType = KeyboardType.Email)
                SettingCustomView {
                    SettingInputRow("验证码", vm.code, { vm.code = it }, placeholder = "请输入验证码", keyboardType = KeyboardType.Number, verticalPadding = 11) {
                        val enabled = vm.countdown <= 0
                        Text(
                            if (vm.countdown > 0) "${vm.countdown}秒" else "发送验证码",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (enabled) settingColors.accent else settingColors.secondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .widthIn(min = 72.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(enabled = enabled, onClick = vm::onSendCodeClick)
                                .padding(vertical = 4.dp),
                        )
                    }
                }
                if (vm.isVerifyPwd) {
                    SettingTextField(title = "登录密码", text = vm.pwd, onTextChange = { vm.pwd = it }, placeholder = "请输入当前的登录密码", type = SettingTextFieldType.PASSWORD)
                } else {
                    SettingTextField(
                        title = if (vm.type == EmailCodeType.RESET_PWD) "新密码" else "密码",
                        text = vm.pwd, onTextChange = { vm.pwd = it }, placeholder = "6~32位", type = SettingTextFieldType.PASSWORD,
                    )
                    SettingTextField(title = "确认密码", text = vm.repwd, onTextChange = { vm.repwd = it }, placeholder = "请再次输入密码", type = SettingTextFieldType.PASSWORD)
                }
            }
            SettingGroup {
                SettingButtonSingle(vm.type.title, onClick = vm::onSubmitClick)
            }
        }
    }
}

/** 绑定邮箱的说明 */
private fun footer(vm: EmailCodeViewModel): String? {
    if (vm.type != EmailCodeType.BIND_EMAIL) {
        return null
    }
    if (vm.isVerifyPwd) {
        return "绑定之后可以使用邮箱或用户名登录,密码不变,忘记密码时可以用邮箱找回"
    }
    return "绑定之后可以使用邮箱和密码登录,歌单都会保留"
}
