package cn.dairo.music.page.vip

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import cn.dairo.music.components.AlertButton
import cn.dairo.music.components.AppAlert
import cn.dairo.music.components.ButtonRole
import cn.dairo.music.components.SettingInputRow
import cn.dairo.music.extension.expireDateText
import cn.dairo.music.util.ServerConfig
import cn.dairo.music.util.rememberStateObject
import cn.dairo.ui.setting.SettingButtonSingle
import cn.dairo.ui.setting.SettingCustomView
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.setting.SettingText

/**
 * VIP状态和兑换VIP
 */
@Composable
fun VipRedeemPage() {
    val vm = rememberStateObject { VipRedeemViewModel() }
    SettingStack(embedInNavigationStack = false) {
        SettingPage(title = "VIP会员") {
            SettingGroup(footer = "VIP可以听超高品质、无损音质的歌曲,看原画视频,下载歌曲和视频") {
                SettingText("VIP状态：" + statusText())
            }
            SettingGroup(header = "兑换VIP", footer = "卡号是16位数字,卡密不区分大小写") {
                SettingCustomView {
                    SettingInputRow("卡号", vm.cardNo, vm::onCardNoChange, placeholder = "16位数字", keyboardType = KeyboardType.Number)
                }
                SettingCustomView {
                    SettingInputRow(
                        "卡密", vm.cardPwd, vm::onCardPwdChange, placeholder = "请输入卡密",
                        keyboardType = KeyboardType.Ascii, capitalization = KeyboardCapitalization.Characters,
                    )
                }
            }
            SettingGroup {
                SettingButtonSingle("兑换", onClick = vm::onRedeemClick)
            }
        }
    }
    AppAlert(
        isPresented = vm.confirmMessage != null,
        title = "确认兑换",
        message = vm.confirmMessage,
        onDismiss = { vm.confirmMessage = null },
        buttons = listOf(
            AlertButton("兑换", action = vm::onConfirmClick),
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
}

/** VIP状态的说明 */
private fun statusText(): String {
    val vip = ServerConfig.vip ?: return "未开通"
    if (vip.forever) {
        return "永久VIP"
    }
    if (vip.isVip) {
        return "有效期到" + vip.expireDateText
    }
    return if (vip.expireTime == 0L) "未开通" else "已于" + vip.expireDateText + "到期"
}
