package cn.dairo.music.page.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.dairo.music.R
import cn.dairo.music.components.relaunch
import cn.dairo.music.extension.secondaryBackground
import cn.dairo.music.extension.secondaryText
import cn.dairo.music.page.emailcode.EmailCodePage
import cn.dairo.music.page.emailcode.EmailCodeType
import cn.dairo.music.util.rememberStateObject
import cn.dairo.ui.setting.LocalSettingNavigator
import cn.dairo.ui.setting.SettingButtonSingle
import cn.dairo.ui.setting.SettingCustomView
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.setting.SettingTextField
import cn.dairo.ui.setting.SettingTextFieldType

/**
 * 登录的第二步:服务器已经确定,输入邮箱或用户名和密码
 * @param isRoot 是否直接作为根页面打开(登录失效、编辑账号),这时没有上一页,要显示更换服务器
 */
@Composable
fun LoginPage(vm: LoginViewModel, isRoot: Boolean = true) {
    val vm = rememberStateObject { vm }
    LaunchedEffect(Unit) {
        vm.loadConfig()
    }
    SettingStack(embedInNavigationStack = isRoot) {
        val navigator = LocalSettingNavigator.current
        SettingPage(title = if (vm.editAccountIndex == null) "登录" else "编辑账号") {
            SettingCustomView {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Image(painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(14.dp)))
                    ServerLabel(vm.domain)
                }
            }
            SettingGroup {
                SettingTextField(title = "账号", text = vm.account, onTextChange = { vm.account = it }, placeholder = "邮箱或用户名")
                SettingTextField(title = "密码", text = vm.pwd, onTextChange = { vm.pwd = it }, placeholder = "请输入密码", type = SettingTextFieldType.PASSWORD)
            }
            SettingGroup {
                SettingButtonSingle(if (vm.editAccountIndex == null) "登录" else "保存", onClick = vm::onLoginClick)
            }
            SettingCustomView {

                //注册在最左边,忘记密码在最右边,更换服务器在正中间
                Row(
                    Modifier
                        .fillMaxWidth()

                        //左右和上面的输入框对齐
                        .padding(horizontal = 20.dp)

                        //和下面的"免注册使用"拉开距离
                        .padding(bottom = 24.dp),
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (vm.allowRegister == true) {
                            LinkText("注册") {
                                navigator?.push { EmailCodePage(EmailCodeType.REGISTER, vm.domain) }
                            }
                        }
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (isRoot) {
                            LinkText("更换服务器") {
                                val serverVm = LoginViewModel(domain = vm.domain, account = vm.account, editAccountIndex = vm.editAccountIndex)
                                relaunch { ServerPage(serverVm) }
                            }
                        }
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        LinkText("忘记密码") {
                            navigator?.push { EmailCodePage(EmailCodeType.RESET_PWD, vm.domain) }
                        }
                    }
                }
            }
            if (vm.allowRegister == true) {
                SettingGroup(footer = "不注册也可以使用,之后可以在个人资料里绑定邮箱") {
                    SettingButtonSingle("免注册使用", onClick = vm::onTempLoginClick)
                }
            }
        }
    }
}

/** 登录页面底部的文字按钮 */
@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.secondaryText,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    )
}

/**
 * 服务器地址标签,登录、注册、忘记密码页面显示当前要登录的服务器
 */
@Composable
fun ServerLabel(domain: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(Color.secondaryBackground, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Dns, contentDescription = null, tint = Color.secondaryText, modifier = Modifier.size(14.dp))
        Text(
            domain,
            style = MaterialTheme.typography.bodySmall,
            color = Color.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
