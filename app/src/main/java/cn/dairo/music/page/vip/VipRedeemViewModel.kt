package cn.dairo.music.page.vip

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.music.api.VipApi
import cn.dairo.music.store.SettingStore
import cn.dairo.music.util.ObservableObject
import cn.dairo.ui.root.Toast

class VipRedeemViewModel : ObservableObject() {

    companion object {

        /** 卡号的位数 */
        const val CARD_NO_LENGTH = 16

        /** 卡号只保留数字,最多16位,每4位一组 */
        private fun formatCardNo(text: String): String {
            val digits = text.filter { it in '0'..'9' }.take(CARD_NO_LENGTH)
            return digits.chunked(4).joinToString(" ")
        }
    }

    /** 卡号,每4位一组用空格隔开显示 */
    var cardNo by mutableStateOf("")
        private set

    /** 卡密,自动转成大写 */
    var cardPwd by mutableStateOf("")
        private set

    /** 已经是永久VIP时,需要会员确认的提示 */
    var confirmMessage by mutableStateOf<String?>(null)

    /** 输入卡号 */
    fun onCardNoChange(text: String) {
        this.cardNo = formatCardNo(text)
    }

    /** 输入卡密 */
    fun onCardPwdChange(text: String) {
        this.cardPwd = text.uppercase().filter { !it.isWhitespace() }
    }

    /** 兑换按钮点击事件 */
    fun onRedeemClick() {
        if (this.cardNo.count { it.isDigit() } != CARD_NO_LENGTH) {
            Toast.show("请输入16位卡号")
            return
        }
        if (this.cardPwd.isEmpty()) {
            Toast.show("请输入卡密")
            return
        }
        this.redeem(confirm = false)
    }

    /** 会员确认之后再提交一次 */
    fun onConfirmClick() {
        this.redeem(confirm = true)
    }

    /** 提交兑换 */
    private fun redeem(confirm: Boolean) {
        VipApi.redeem(this.cardNo, this.cardPwd, confirm).send { result ->
            if (result.needConfirm) {
                this.confirmMessage = result.message
                return@send
            }
            result.vip?.let { SettingStore.updateVip(it) }
            this.cardNo = ""
            this.cardPwd = ""
            Toast.show(result.message)
        }
    }
}
