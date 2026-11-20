package cn.dairo.music.page.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cn.dairo.music.R
import cn.dairo.music.components.ServerGithubLink
import cn.dairo.music.components.relaunch
import cn.dairo.music.extension.secondaryText
import cn.dairo.music.page.accountlist.AccountListPage
import cn.dairo.music.store.SettingStore
import cn.dairo.music.util.rememberStateObject
import cn.dairo.ui.setting.LocalSettingNavigator
import cn.dairo.ui.setting.SettingButtonSingle
import cn.dairo.ui.setting.SettingCustomView
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.setting.SettingTextField

/**
 * 登录的第一步:输入服务器地址,确认是DairoMUSIC服务器之后进入登录页面
 */
@Composable
fun ServerPage(vm: LoginViewModel = LoginViewModel(domain = SettingStore.domainNotNull)) {
    val vm = rememberStateObject { vm }
    SettingStack {
        val navigator = LocalSettingNavigator.current

        //确认服务器之后进入登录页面
        LaunchedEffect(vm.showLogin) {
            if (vm.showLogin) {
                vm.showLogin = false
                navigator?.push { LoginPage(vm, isRoot = false) }
            }
        }
        Box(Modifier.fillMaxSize()) {
            SettingPage(title = "服务器") {
                SettingCustomView {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Image(
                            painterResource(R.drawable.logo),
                            contentDescription = null,
                            modifier = Modifier.padding(32.dp).size(80.dp).clip(RoundedCornerShape(14.dp)),
                        )
                    }
                }
                SettingGroup(footer = "请输入DairoMUSIC服务器的地址，例如 192.168.1.100:8020、https://music.example.com。没有写http://或https://时按http://连接") {
                    SettingTextField(
                        title = "服务器",
                        text = vm.domain,
                        onTextChange = { vm.domain = it },
                        placeholder = "例 192.168.1.100:8020",
                        keyboardType = KeyboardType.Uri,
                    )
                }
                SettingGroup {
                    SettingButtonSingle("下一步", onClick = vm::onNextClick)
                }
                SettingCustomView {
                    Text(
                        "选择账号",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.secondaryText,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { relaunch { AccountListPage(embedInNavigationStack = true) } }
                            .padding(4.dp),
                    )
                }
            }

            //固定显示在屏幕最底部,输入服务器地址时隐藏
            ServerGithubLink("还没有服务器？可以按照这里的说明部署", Modifier.align(Alignment.BottomCenter))
        }
    }
}
