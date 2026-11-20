package cn.dairo.music.page.download

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.file.sdk.CacheInfo
import cn.dairo.file.sdk.DownloadState
import cn.dairo.music.util.FileSdkUtil
import cn.dairo.music.util.ObservableObject
import cn.dairo.ui.root.Loading
import cn.dairo.ui.root.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 下载的类型
 */
enum class DownloadType {

    /** 歌曲 */
    MUSIC,

    /** 视频 */
    VIDEO;

    /** SDK的分组 */
    val group: String
        get() = if (this == MUSIC) FileSdkUtil.GROUP_MUSIC else FileSdkUtil.GROUP_VIDEO

    /** 页面标题 */
    val title: String
        get() = if (this == MUSIC) "已下载的歌曲" else "已下载的视频"

    /** 没有下载时的提示 */
    val emptyText: String
        get() = if (this == MUSIC) "没有下载的歌曲" else "没有下载的视频"

    /** 全部删除的确认框标题 */
    val deleteAllTitle: String
        get() = if (this == MUSIC) "确定删除全部已下载的歌曲吗？" else "确定删除全部已下载的视频吗？"
}

class DownloadListViewModel(val type: DownloadType) : ObservableObject() {

    companion object {

        /** 刷新下载进度的间隔(毫秒) */
        private const val REFRESH_MILLIS = 1000L
    }

    /** 下载列表,最近添加的在前面 */
    var items by mutableStateOf(emptyList<CacheInfo>())
        private set

    /** 定时刷新下载进度 */
    private var timer: Job? = null

    /** 页面显示时开始定时刷新 */
    fun start() {
        this.timer?.cancel()
        this.timer = this.scope.launch {
            while (isActive) {
                refresh()
                delay(REFRESH_MILLIS)
            }
        }
    }

    /** 页面关闭时停止刷新 */
    fun stop() {
        this.timer?.cancel()
        this.timer = null
    }

    /** 从SDK读取下载列表,读数据库不在主线程 */
    private suspend fun refresh() {
        val group = this.type.group
        val items = withContext(Dispatchers.IO) { FileSdkUtil.downloads(group) }
        if (items != this.items) {
            this.items = items
        }
    }

    /** 暂停、继续、重试 */
    fun onStateClick(info: CacheInfo) {
        when (info.state) {
            DownloadState.DOWNLOADING, DownloadState.WAITING -> FileSdkUtil.pause(listOf(info.id))
            DownloadState.PAUSED, DownloadState.ERROR -> FileSdkUtil.resume(listOf(info.id))
            DownloadState.FINISH -> {}
        }
        this.scope.launch { refresh() }
    }

    /** 删除选中的 */
    fun delete(ids: List<String>) {
        val idSet = ids.toSet()
        this.items = this.items.filter { !idSet.contains(it.id) }
        this.scope.launch(Dispatchers.IO) {
            FileSdkUtil.delete(ids)
        }
    }

    /** 全部删除 */
    fun deleteAll() {
        val ids = this.items.map { it.id }
        this.items = emptyList()
        Loading.show()
        this.scope.launch {
            withContext(Dispatchers.IO) {
                FileSdkUtil.delete(ids)
            }
            Loading.hide()
            Toast.show("已删除")
            refresh()
        }
    }
}
