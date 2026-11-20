package cn.dairo.music.extension

import cn.dairo.music.api.model.VipModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** VIP状态的说明:永久、到期日期、已到期、未开通 */
val VipModel.statusText: String
    get() {
        if (this.forever) {
            return "永久"
        }
        if (this.expireTime == 0L) {
            return "未开通"
        }
        if (!this.isVip) {
            return "已到期"
        }
        return this.expireDateText + "到期"
    }

/** 到期日期 */
val VipModel.expireDateText: String
    get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(this.expireTime))
