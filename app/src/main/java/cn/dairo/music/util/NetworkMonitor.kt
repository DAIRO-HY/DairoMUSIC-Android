package cn.dairo.music.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import cn.dairo.ui.DairoUI

/**
 * 网络类型
 */
enum class NetworkType {

    /** 还没有获取到网络状态 */
    UNKNOWN,

    /** 没有网络 */
    NONE,

    /** WiFi */
    WIFI,

    /** 数据流量 */
    CELLULAR,

    /** 其他(有线网络等) */
    OTHER,
}

/**
 * 当前的网络类型,App启动时开始监听,只记录最新的状态,切换歌曲前读取
 */
object NetworkMonitor {

    /** 从没有网络恢复到有网络时的通知,在主线程发送 */
    const val AVAILABLE = "NetworkMonitor.AVAILABLE"

    private val handler = Handler(Looper.getMainLooper())

    /** 最新的网络类型,在监听线程里更新 */
    @Volatile
    private var type = NetworkType.UNKNOWN

    /** 开始监听,App启动时调用 */
    fun start() {
        val manager = DairoUI.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        this.type = typeOf(manager, manager.activeNetwork)
        manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                update(typeOf(capabilities))
            }

            override fun onLost(network: Network) {
                update(NetworkType.NONE)
            }
        })
    }

    /** 网络类型变化 */
    private fun update(type: NetworkType) {
        val old = this.type
        this.type = type

        //第一次获取到网络状态时不算恢复
        if (old != NetworkType.UNKNOWN && old == NetworkType.NONE && type != NetworkType.NONE) {
            handler.post { NotificationCenter.post(AVAILABLE) }
        }
    }

    /** 当前的网络类型 */
    val current: NetworkType
        get() = this.type

    private fun typeOf(manager: ConnectivityManager, network: Network?): NetworkType {
        network ?: return NetworkType.NONE
        val capabilities = manager.getNetworkCapabilities(network) ?: return NetworkType.NONE
        return typeOf(capabilities)
    }

    private fun typeOf(capabilities: NetworkCapabilities): NetworkType {
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return NetworkType.NONE
        }
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return NetworkType.WIFI
        }
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return NetworkType.CELLULAR
        }
        return NetworkType.OTHER
    }
}
