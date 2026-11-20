package cn.dairo.music.components

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.util.UnstableApi
import cn.dairo.music.page.discover.DiscoverPage
import cn.dairo.music.page.login.ServerPage
import cn.dairo.music.page.playing.PlayingPage
import cn.dairo.music.store.SettingStore
import cn.dairo.ui.root.RootView

/**
 * App的根页面,对应iOS的WindowGroup
 * relaunch替换整个页面(关闭其他所有页面),播放页盖在所有页面上面
 */
object AppRoot {

    /** 当前的根页面 */
    internal var page by mutableStateOf<@Composable () -> Unit>({
        if (SettingStore.isLogin) {
            DiscoverPage()
        } else {
            ServerPage()
        }
    })

    /** 每次relaunch都重新创建页面,之前页面的状态全部丢弃 */
    internal var pageKey by mutableIntStateOf(0)

    /** 是否显示播放页,对应iOS迷你播放条弹出的fullScreenCover */
    var isPlayingShowing by mutableStateOf(false)
        private set

    /** 打开播放页 */
    fun showPlaying() {
        this.isPlayingShowing = true
    }

    /** 关闭播放页 */
    fun dismissPlaying() {
        this.isPlayingShowing = false
    }

    private val handler = Handler(Looper.getMainLooper())

    internal fun relaunch(page: @Composable () -> Unit) {
        val block = {
            this.page = page
            this.pageKey += 1
            this.isPlayingShowing = false
        }
        if (Looper.myLooper() == Looper.getMainLooper()) block() else handler.post(block)
    }
}

/**
 * 关闭其他所有页面,跳转到这个页面,对应iOS的View.relaunch()
 */
fun relaunch(page: @Composable () -> Unit) {
    AppRoot.relaunch(page)
}

/**
 * App的根视图
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun AppRootView() {
    RootView {
        key(AppRoot.pageKey) {
            AppRoot.page()
        }
        AnimatedVisibility(
            visible = AppRoot.isPlayingShowing,
            enter = slideInVertically(tween(300)) { it },
            exit = slideOutVertically(tween(300)) { it },
        ) {
            PlayingPage()
        }
    }
}
