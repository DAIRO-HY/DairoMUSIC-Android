package cn.dairo.music.page.cache

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import cn.dairo.file.sdk.CacheMode
import cn.dairo.file.sdk.DownloadState
import cn.dairo.music.components.AlertButton
import cn.dairo.music.components.AppAlert
import cn.dairo.music.components.ButtonRole
import cn.dairo.music.components.OnPageAppear
import cn.dairo.music.extension.theme
import cn.dairo.music.page.download.DownloadListPage
import cn.dairo.music.page.download.DownloadType
import cn.dairo.music.store.SettingStore
import cn.dairo.music.util.FileSdkUtil
import cn.dairo.music.util.ObservableObject
import cn.dairo.music.util.rememberStateObject
import cn.dairo.ui.root.Loading
import cn.dairo.ui.root.Toast
import cn.dairo.ui.setting.SettingButton
import cn.dairo.ui.setting.SettingButtonSingle
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingIcon
import cn.dairo.ui.setting.SettingNavigationLink
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingPicker
import cn.dairo.ui.setting.SettingPickerData
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.setting.SettingToggle
import cn.dairo.ui.util.fileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 设置项的图标 */
private fun icon(icon: ImageVector, color: Color) = SettingIcon.System(icon, backgroundColor = color)

/**
 * 缓存管理,播放过的歌曲和看过的图片都缓存在SDK里
 */
@Composable
fun CachePage() {
    val vm = rememberStateObject { CacheViewModel() }

    /** 要清理的缓存分组,空字符串表示全部 */
    var clearGroup by remember { mutableStateOf<String?>(null) }

    //从下载列表删除之后回来会更新
    OnPageAppear {
        vm.loadSize()
    }
    SettingStack(embedInNavigationStack = false) {
        SettingPage(title = "缓存管理") {
            SettingGroup(footer = "关闭之后没有缓存的歌曲、视频直接在线播放,已经缓存好的仍然播放本地文件") {
                SettingToggle(title = "音乐边播边缓存", isOn = SettingStore.musicCache, onIsOnChange = { SettingStore.musicCache = it })
                SettingToggle(title = "视频边播边缓存", isOn = SettingStore.videoCache, onIsOnChange = { SettingStore.videoCache = it })
            }
            SettingGroup(footer = "下载的歌曲和视频永久保存,清理缓存时不会删除") {
                SettingNavigationLink("已下载的歌曲", tip = vm.downloadMusicText, icon = icon(Icons.Default.Download, Color(0xFF34C759))) {
                    DownloadListPage(DownloadType.MUSIC)
                }
                SettingNavigationLink("已下载的视频", tip = vm.downloadVideoText, icon = icon(Icons.Default.Download, Color(0xFFFF9500))) {
                    DownloadListPage(DownloadType.VIDEO)
                }
            }
            SettingGroup(footer = "正在播放的歌曲不会被清理") {
                SettingButton("歌曲缓存", tip = vm.musicSize, icon = icon(Icons.Default.MusicNote, Color.theme)) {
                    clearGroup = FileSdkUtil.GROUP_MUSIC
                }
                SettingButton("视频缓存", tip = vm.videoSize, icon = icon(Icons.Default.Movie, Color(0xFFFF9500))) {
                    clearGroup = FileSdkUtil.GROUP_VIDEO
                }
                SettingButton("图片缓存", tip = vm.imageSize, icon = icon(Icons.Default.Photo, Color(0xFF007AFF))) {
                    clearGroup = FileSdkUtil.GROUP_IMAGE
                }
            }
            SettingGroup(footer = "超过保存期限没有播放的缓存会定期清理,超过上限时清理最久没有播放的缓存;下载的歌曲和视频不会被清理") {
                SettingPicker(
                    "缓存保存期限", data = CacheViewModel.EXPIRE_DAYS_DATA, value = vm.expireDays, onValueChange = { vm.expireDays = it },
                    icon = icon(Icons.Default.Timer, Color(0xFFFF9500)), shouldChange = vm::onExpireDaysChange,
                )
                SettingPicker(
                    "缓存上限", data = CacheViewModel.MAX_SIZE_DATA, value = vm.maxSize, onValueChange = { vm.maxSize = it },
                    icon = icon(Icons.Default.Storage, Color(0xFF5856D6)), shouldChange = vm::onMaxSizeChange,
                )
            }
            SettingGroup {
                SettingButtonSingle("清理全部缓存") {
                    clearGroup = ""
                }
            }
        }
    }
    AppAlert(
        isPresented = clearGroup != null,
        title = clearTitle(clearGroup),
        onDismiss = { clearGroup = null },
        buttons = listOf(
            AlertButton("清理", ButtonRole.DESTRUCTIVE) {
                clearGroup?.let { vm.onClearClick(it.ifEmpty { null }) }
            },
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
}

/** 清理确认框的标题 */
private fun clearTitle(group: String?): String {
    return when (group) {
        FileSdkUtil.GROUP_MUSIC -> "确定清理歌曲缓存吗？"
        FileSdkUtil.GROUP_IMAGE -> "确定清理图片缓存吗？"
        FileSdkUtil.GROUP_VIDEO -> "确定清理视频缓存吗？"
        else -> "确定清理全部缓存吗？"
    }
}

class CacheViewModel : ObservableObject() {

    companion object {

        /** 缓存上限的选项(MB),0表示不限制 */
        val MAX_SIZE_DATA = listOf(
            SettingPickerData("1GB", 1024),
            SettingPickerData("2GB", 2 * 1024),
            SettingPickerData("5GB", 5 * 1024),
            SettingPickerData("10GB", 10 * 1024),
            SettingPickerData("不限制", 0),
        )

        /** 缓存保存期限的选项(天),0表示永久 */
        val EXPIRE_DAYS_DATA = listOf(
            SettingPickerData("1星期", 7),
            SettingPickerData("1个月(30天)", 30),
            SettingPickerData("3个月(90天)", 90),
            SettingPickerData("6个月(180天)", 180),
            SettingPickerData("1年(365天)", 365),
            SettingPickerData("永久", 0),
        )
    }

    /** 歌曲缓存大小 */
    var musicSize by mutableStateOf("计算中")

    /** 图片缓存大小 */
    var imageSize by mutableStateOf("计算中")

    /** 视频缓存大小,不包括已经下载的视频 */
    var videoSize by mutableStateOf("计算中")

    /** 已下载的歌曲数量和大小 */
    var downloadMusicText by mutableStateOf("计算中")

    /** 已下载的视频数量和大小 */
    var downloadVideoText by mutableStateOf("计算中")

    /** 缓存上限(MB) */
    var maxSize by mutableIntStateOf(SettingStore.fileSdkMaxCacheSize)

    /** 缓存保存期限(天),没有设置过时是SDK的默认值90天 */
    var expireDays by mutableIntStateOf(SettingStore.fileSdkCacheExpireDays ?: FileSdkUtil.DEFAULT_CACHE_EXPIRE_DAYS)

    /** 统计缓存和下载的大小,页面显示时刷新 */
    fun loadSize() {
        this.scope.launch {
            val result = withContext(Dispatchers.IO) {
                val stats = FileSdkUtil.stats(CacheMode.CACHE)
                val sizeOf = { group: String -> (stats.firstOrNull { it.group == group }?.size ?: 0UL).toLong().fileSize }

                //下载按当前服务器统计,包括还没有下载完成的
                val downloadText = { group: String, unit: String ->
                    val list = FileSdkUtil.downloads(group)
                    if (list.isEmpty()) {
                        "无"
                    } else {
                        val size = list.sumOf { if (it.state == DownloadState.FINISH) (it.size ?: it.downloaded) else it.downloaded }
                        "${list.size}$unit · " + size.toLong().fileSize
                    }
                }
                listOf(
                    sizeOf(FileSdkUtil.GROUP_MUSIC),
                    sizeOf(FileSdkUtil.GROUP_IMAGE),
                    sizeOf(FileSdkUtil.GROUP_VIDEO),
                    downloadText(FileSdkUtil.GROUP_MUSIC, "首"),
                    downloadText(FileSdkUtil.GROUP_VIDEO, "个"),
                )
            }
            musicSize = result[0]
            imageSize = result[1]
            videoSize = result[2]
            downloadMusicText = result[3]
            downloadVideoText = result[4]
        }
    }

    /**
     * 清理缓存,SDK清理时会读写文件,不能在主线程调用
     * @param group 缓存分组,为null时清理全部
     */
    fun onClearClick(group: String?) {
        Loading.show()
        this.scope.launch {
            withContext(Dispatchers.IO) {
                FileSdkUtil.clearCache(group)
            }
            Loading.hide()
            Toast.show("清理完成")
            loadSize()
        }
    }

    /** 修改缓存保存期限 */
    fun onExpireDaysChange(value: Int): Boolean {
        SettingStore.fileSdkCacheExpireDays = value
        FileSdkUtil.setCacheExpireDays(value)
        return true
    }

    /** 修改缓存上限 */
    fun onMaxSizeChange(value: Int): Boolean {
        SettingStore.fileSdkMaxCacheSize = value
        FileSdkUtil.setMaxCacheSize(value)
        return true
    }
}
