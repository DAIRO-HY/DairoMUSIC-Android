package cn.dairo.music.api

import cn.dairo.music.api.http.ApiHttp
import cn.dairo.music.api.model.VipRedeemModel

object VipApi {

    /** 兑换VIP,cardNo:卡号(可以带空格、横线) cardPwd:卡密(不区分大小写) confirm:会员已经确认(已经是永久VIP时需要确认) */
    fun redeem(cardNo: String, cardPwd: String, confirm: Boolean): ApiHttp<VipRedeemModel> {
        val parameter = mutableMapOf<String, Any?>("cardNo" to cardNo, "cardPwd" to cardPwd)
        if (confirm) {
            parameter["confirm"] = 1
        }
        return ApiHttp(ApiConst.VIP_REDEEM, "POST", parameter)
    }
}
