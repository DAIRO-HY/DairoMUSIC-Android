package cn.dairo.music.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import cn.dairo.ui.setting.LocalSettingNavigator

/**
 * 页面显示时调用,对应iOS的onAppear:第一次显示、从上面打开的页面返回时都会调用
 * SettingStack里下层的页面一直保持存活,用导航栈的层数判断页面是否在最上面
 */
@Composable
fun OnPageAppear(block: () -> Unit) {
    val navigator = LocalSettingNavigator.current
    val myDepth = remember { navigator?.depth ?: 0 }
    val isTop = (navigator?.depth ?: 0) == myDepth
    val currentBlock by rememberUpdatedState(block)
    LaunchedEffect(isTop) {
        if (isTop) {
            currentBlock()
        }
    }
}

/**
 * 当前页面是否可以返回(不是导航栈的根页面),决定是否显示返回按钮
 */
@Composable
fun rememberCanGoBack(): Boolean {
    val navigator = LocalSettingNavigator.current
    return remember { (navigator?.depth ?: 0) > 0 }
}
