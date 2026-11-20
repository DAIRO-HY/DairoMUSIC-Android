package cn.dairo.music.constants

import android.annotation.SuppressLint
import android.provider.Settings
import cn.dairo.music.BuildConfig
import cn.dairo.music.util.KeychainUtil
import cn.dairo.ui.DairoUI
import cn.dairo.ui.util.md5
import java.util.UUID

/**
 * 静态变量配置
 */
object Const {

    /** 客户端标识 0:WEB 1:Android 2:IOS 3:Windows 4:Mac 5:Linux */
    const val CLIENT_FLAG = 1

    /** 服务器端的GitHub项目地址 */
    const val SERVER_GITHUB_URL = "https://github.com/DAIRO-HY/DairoMUSIC"

    /** 设备标识保存的名字 */
    const val KEY_DEVICE_ID = "DEVICE_ID"

    private var mDeviceId: String? = null

    /**
     * 设备唯一标识,免注册使用时用来找回同一个临时会员
     * iOS保存在钥匙串里,卸载重装之后不变;Android卸载之后App的数据都会被删除,
     * 所以用系统的ANDROID_ID(同一台设备、同一个签名的App不变)计算,取不到时随机生成并保存
     */
    val deviceId: String
        @SuppressLint("HardwareIds")
        get() {
            mDeviceId?.let { return it }
            KeychainUtil.read(KEY_DEVICE_ID)?.let {
                mDeviceId = it
                return it
            }
            val androidId = Settings.Secure.getString(DairoUI.context.contentResolver, Settings.Secure.ANDROID_ID)
            val deviceId = if (androidId.isNullOrEmpty()) {
                UUID.randomUUID().toString().replace("-", "").lowercase()
            } else {
                ("DairoMUSIC_" + androidId).md5
            }
            KeychainUtil.write(deviceId, KEY_DEVICE_ID)
            mDeviceId = deviceId
            return deviceId
        }

    /** 整数版本号 */
    val versionCode: Int
        get() = BuildConfig.VERSION_CODE

    /** 版本名 */
    val versionName: String
        get() = BuildConfig.VERSION_NAME
}
