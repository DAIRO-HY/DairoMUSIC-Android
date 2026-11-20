package cn.dairo.music.util

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * 屏幕控制:手机上只支持竖屏,视频全屏播放时切换到横屏;平板支持所有方向
 * 视频播放时禁止自动锁屏也在这里控制
 */
object OrientationUtil {

    /** Activity创建时调用 */
    fun attach(activity: Activity) {
        AppActivity.attach(activity)
        activity.requestedOrientation = if (isPad(activity)) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    /** 是否是平板 */
    private fun isPad(activity: Activity): Boolean {
        return activity.resources.configuration.smallestScreenWidthDp >= 600
    }

    /**
     * 切换手机的屏幕方向,平板不处理
     * @param landscape true:横屏 false:竖屏
     */
    fun rotate(landscape: Boolean) {
        val activity = AppActivity.current ?: return
        if (isPad(activity)) {
            return
        }
        activity.requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    /** 全屏播放视频时隐藏状态栏和导航栏 */
    fun setFullScreen(fullScreen: Boolean) {
        val activity = AppActivity.current ?: return
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (fullScreen) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    /** 视频播放时禁止自动锁屏,对应iOS的isIdleTimerDisabled */
    fun setKeepScreenOn(keepScreenOn: Boolean) {
        val window = AppActivity.current?.window ?: return
        if (keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /** 当前是否横屏 */
    fun isLandscape(configuration: Configuration): Boolean {
        return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }
}
