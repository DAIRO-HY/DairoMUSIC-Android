package cn.dairo.music.util

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * 当前显示的Activity,屏幕方向、申请权限等需要Activity的地方使用
 */
object AppActivity {

    private var activity: WeakReference<Activity>? = null

    /** 当前的Activity,已经关闭时为null */
    val current: Activity?
        get() = this.activity?.get()

    /** Activity创建时调用 */
    fun attach(activity: Activity) {
        this.activity = WeakReference(activity)
    }
}
