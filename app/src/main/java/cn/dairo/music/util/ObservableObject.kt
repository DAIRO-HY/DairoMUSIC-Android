package cn.dairo.music.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

/**
 * 页面的视图模型,对应iOS的ObservableObject
 * 属性用mutableStateOf保存,修改之后页面自动刷新;页面关闭时调用onCleared,停止scope里的任务
 */
abstract class ObservableObject {

    /** 视图模型的协程,在主线程,页面关闭时取消 */
    protected val scope: CoroutineScope = MainScope()

    /** 页面关闭时调用 */
    open fun onCleared() {
        this.scope.cancel()
    }
}

/**
 * 创建页面的视图模型,对应iOS的@StateObject:页面存在期间只创建一次,页面关闭时调用onCleared
 */
@Composable
fun <T : ObservableObject> rememberStateObject(create: () -> T): T {
    val vm = remember { create() }
    DisposableEffect(vm) {
        onDispose { vm.onCleared() }
    }
    return vm
}
