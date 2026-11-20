package cn.dairo.music.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * 全局通知,对应iOS的NotificationCenter,只在主线程使用
 */
object NotificationCenter {

    private val flow = MutableSharedFlow<String>(extraBufferCapacity = 64)

    /** 发送通知 */
    fun post(name: String) {
        this.flow.tryEmit(name)
    }

    /**
     * 监听通知,scope结束时停止监听
     * @param names 要监听的通知名,可以有多个
     */
    fun observe(scope: CoroutineScope, vararg names: String, block: (name: String) -> Unit): Job {
        return scope.launch {
            flow.filter { it in names }.collect { block(it) }
        }
    }
}

/**
 * 页面上监听通知,对应iOS的onReceive,页面关闭时停止监听
 */
@Composable
fun OnReceive(vararg names: String, block: (name: String) -> Unit) {
    val currentBlock = rememberUpdatedState(block)
    val key = names.joinToString(",")
    LaunchedEffect(key) {
        NotificationCenter.observe(this, *names) { currentBlock.value(it) }
    }
}
