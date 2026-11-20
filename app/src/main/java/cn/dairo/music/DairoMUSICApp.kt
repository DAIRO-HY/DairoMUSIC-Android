package cn.dairo.music

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.util.UnstableApi
import cn.dairo.music.player.PlayerManager
import cn.dairo.music.store.SettingStore
import cn.dairo.music.util.FileSdkUtil
import cn.dairo.music.util.NetworkMonitor
import cn.dairo.music.util.PlayCountUtil
import cn.dairo.music.util.ServerConfig

@UnstableApi
class DairoMUSICApp : Application() {

    override fun onCreate() {
        super.onCreate()

        //提前创建SDK实例
        runCatching { FileSdkUtil.get() }

        //开始监听网络类型,切换歌曲时判断离线模式
        NetworkMonitor.start()

        //读取本地保存的VIP状态
        ServerConfig.init()

        //恢复上次的播放列表,不自动播放
        PlayerManager.init(this)
        PlayerManager.restore()

        //提交上次没有提交成功的播放记录,网络恢复时再提交
        PlayCountUtil.start()

        //App前后台状态
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {

            //回到前台
            override fun onStart(owner: LifecycleOwner) {
                FileSdkUtil.checkServer()
                ServerConfig.load()

                //VIP状态可能变化了(到期、兑换、后台修改)
                if (SettingStore.isLogin) {
                    SettingStore.loadUserInBackground(silent = true)
                }
            }

            //进入后台
            override fun onStop(owner: LifecycleOwner) {
                PlayerManager.savePlayingInfo()
                FileSdkUtil.flush()
            }
        })
    }
}
