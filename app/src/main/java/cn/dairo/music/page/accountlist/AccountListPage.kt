package cn.dairo.music.page.accountlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cn.dairo.music.R
import cn.dairo.music.api.UserApi
import cn.dairo.music.components.ActionSheet
import cn.dairo.music.components.AlertButton
import cn.dairo.music.components.AppAlert
import cn.dairo.music.components.ButtonRole
import cn.dairo.music.components.relaunch
import cn.dairo.music.extension.primaryText
import cn.dairo.music.extension.secondaryText
import cn.dairo.music.model.AccountInfo
import cn.dairo.music.page.discover.DiscoverPage
import cn.dairo.music.page.login.LoginPage
import cn.dairo.music.page.login.LoginViewModel
import cn.dairo.music.page.login.ServerPage
import cn.dairo.music.store.SettingStore
import cn.dairo.music.util.rememberStateObject
import cn.dairo.ui.root.Toast
import cn.dairo.ui.setting.SettingButtonSingle
import cn.dairo.ui.setting.SettingCustomView
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.setting.settingRowClickable

@Composable
fun AccountListPage(embedInNavigationStack: Boolean = false) {
    val vm = rememberStateObject { AccountListViewModel() }

    /** 操作按钮菜单显示 */
    var showActionMenu by remember { mutableStateOf(false) }

    /** 显示确认删除框 */
    var showDeleteAlert by remember { mutableStateOf(false) }

    SettingStack(embedInNavigationStack = embedInNavigationStack) {
        SettingPage(title = "选择账号") {
            SettingCustomView {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Image(
                        painterResource(R.drawable.logo),
                        contentDescription = null,
                        modifier = Modifier.padding(32.dp).size(80.dp).clip(RoundedCornerShape(14.dp)),
                    )
                }
            }
            SettingGroup {
                SettingCustomView {
                    Column {
                        vm.accountList.forEachIndexed { i, item ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .settingRowClickable {
                                        vm.actionIndex = i
                                        showActionMenu = true
                                    }
                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, color = Color.primaryText)
                                    Text(item.domain, style = MaterialTheme.typography.bodyMedium, color = Color.secondaryText)
                                }
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.primaryText, modifier = Modifier.alpha(if (item.isCurrent) 1f else 0.3f))
                            }
                        }
                    }
                }
            }
            SettingGroup {
                SettingButtonSingle("添加账号") {
                    relaunch { ServerPage() }
                }
            }
        }
    }
    AppAlert(
        isPresented = showDeleteAlert,
        title = "确认删除吗？",
        message = "该账户处于登录状态，删除后将会退出登录。",
        onDismiss = { showDeleteAlert = false },
        buttons = listOf(
            AlertButton("删除", ButtonRole.DESTRUCTIVE) {
                vm.onDeleteClick()

                //退出登录,让服务端删除这个票据
                UserApi.logout().hide().send {}
                SettingStore.logout()

                //删除之后重新打开该页面
                relaunch { AccountListPage(embedInNavigationStack = true) }
            },
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
    ActionSheet(
        isPresented = showActionMenu,
        title = "请选择操作",
        onDismiss = { showActionMenu = false },
        buttons = listOf(
            AlertButton("登录") {
                onSwitchAccount(vm.actionItem)
            },
            AlertButton("编辑") {
                val actionItem = vm.actionItem
                val loginVm = LoginViewModel(domain = actionItem.domain, account = actionItem.account, editAccountIndex = vm.actionIndex)
                relaunch { LoginPage(loginVm) }
            },
            AlertButton("复制") {
                val actionItem = vm.actionItem
                val loginVm = LoginViewModel(domain = actionItem.domain, account = actionItem.account)
                relaunch { LoginPage(loginVm) }
            },
            AlertButton("移除") {
                if (vm.actionItem.isCurrent) {
                    showDeleteAlert = true
                } else {
                    vm.onDeleteClick()

                    //重新加载数据
                    vm.loadData()
                }
            },
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
}

/** 切换到选择的账号 */
private fun onSwitchAccount(account: AccountInfo) {
    if (account.token.isEmpty()) {//账号已经退出登录,需要重新输入密码
        val loginVm = LoginViewModel(domain = account.domain, account = account.account)
        relaunch { LoginPage(loginVm) }
        return
    }
    SettingStore.switchAccount(account) {
        Toast.show("登录成功")
        relaunch { DiscoverPage() }
    }
}
