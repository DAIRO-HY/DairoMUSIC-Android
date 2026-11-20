package cn.dairo.music.page.download

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.dairo.file.sdk.CacheInfo
import cn.dairo.file.sdk.DownloadState
import cn.dairo.music.components.AlertButton
import cn.dairo.music.components.AppAlert
import cn.dairo.music.components.ButtonRole
import cn.dairo.music.components.MusicImage
import cn.dairo.music.components.NavigationPage
import cn.dairo.music.components.SwipeActionRow
import cn.dairo.music.extension.listBackground
import cn.dairo.music.extension.primaryText
import cn.dairo.music.extension.secondaryText
import cn.dairo.music.extension.theme
import cn.dairo.music.util.rememberStateObject
import cn.dairo.ui.setting.settingColors
import cn.dairo.ui.util.fileSize

/**
 * 已下载的歌曲、视频列表,显示下载进度,可以暂停、继续、删除,支持多选删除
 */
@Composable
fun DownloadListPage(type: DownloadType) {
    val vm = rememberStateObject { DownloadListViewModel(type) }

    /** 显示全部删除的确认框 */
    var showDeleteAllAlert by remember { mutableStateOf(false) }

    /** 显示删除选中的确认框 */
    var showDeleteSelectedAlert by remember { mutableStateOf(false) }

    /** 多选模式 */
    var isEditing by remember { mutableStateOf(false) }

    /** 选中的下载ID */
    var selection by remember { mutableStateOf(emptySet<String>()) }

    val isAllSelected = vm.items.isNotEmpty() && selection.size == vm.items.size

    /** 进入或退出多选模式,退出时清空选择 */
    val setEditing = { editing: Boolean ->
        isEditing = editing
        if (!editing) {
            selection = emptySet()
        }
    }

    DisposableEffect(Unit) {
        vm.start()
        onDispose { vm.stop() }
    }

    //删除或者下载记录被清理之后,去掉已经不存在的选中项
    LaunchedEffect(vm.items) {
        val ids = vm.items.map { it.id }.toSet()
        if (!ids.containsAll(selection)) {
            selection = selection.intersect(ids)
        }
        if (vm.items.isEmpty() && isEditing) {
            setEditing(false)
        }
    }

    //多选模式下返回键退出多选
    BackHandler(enabled = isEditing) {
        setEditing(false)
    }

    val background = Color.listBackground
    NavigationPage(
        title = vm.type.title,
        navigationIcon = if (isEditing) {
            {
                TextButton(onClick = { selection = if (isAllSelected) emptySet() else vm.items.map { it.id }.toSet() }) {
                    Text(if (isAllSelected) "取消全选" else "全选", color = settingColors.accent)
                }
            }
        } else {
            null
        },
        actions = {
            if (isEditing) {
                TextButton(onClick = { setEditing(false) }) {
                    Text("完成", color = settingColors.accent)
                }
            } else if (vm.items.isNotEmpty()) {
                TextButton(onClick = { setEditing(true) }) {
                    Text("选择", color = settingColors.accent)
                }
                IconButton(onClick = { showDeleteAllAlert = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "全部删除")
                }
            }
        },
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(vm.items, key = { it.id }) { info ->
                    SwipeActionRow(enabled = !isEditing, backgroundColor = background, onAction = { vm.delete(listOf(info.id)) }) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isEditing) {
                                        Modifier.clickable {
                                            selection = if (selection.contains(info.id)) selection - info.id else selection + info.id
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(start = if (isEditing) 4.dp else 16.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (isEditing) {
                                Checkbox(
                                    checked = selection.contains(info.id),
                                    onCheckedChange = {
                                        selection = if (it) selection + info.id else selection - info.id
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = settingColors.accent),
                                )
                            }
                            DownloadItemView(info, isEditing, Modifier.weight(1f)) {
                                vm.onStateClick(info)
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(start = 76.dp))
                }
            }
            if (vm.items.isEmpty()) {
                Text(vm.type.emptyText, color = Color.secondaryText, modifier = Modifier.align(Alignment.Center))
            }
        }
        if (isEditing) {
            DeleteBar(selection.size) { showDeleteSelectedAlert = true }
        }
    }
    AppAlert(
        isPresented = showDeleteAllAlert,
        title = vm.type.deleteAllTitle,
        onDismiss = { showDeleteAllAlert = false },
        buttons = listOf(
            AlertButton("删除", ButtonRole.DESTRUCTIVE, vm::deleteAll),
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
    AppAlert(
        isPresented = showDeleteSelectedAlert,
        title = "确定删除选中的${selection.size}个吗？",
        onDismiss = { showDeleteSelectedAlert = false },
        buttons = listOf(
            AlertButton("删除", ButtonRole.DESTRUCTIVE) {
                vm.delete(selection.toList())
                setEditing(false)
            },
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
}

/** 多选模式下底部的删除按钮 */
@Composable
private fun DeleteBar(count: Int, onClick: () -> Unit) {
    val enabled = count > 0
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            if (count == 0) "删除" else "删除($count)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (enabled) Color(0xFFFF3B30) else Color.Gray.copy(alpha = 0.5f))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 12.dp),
        )
    }
}

/**
 * 下载列表的一行:封面、名称、进度,右边是暂停、继续、重试按钮
 * @param isEditing 多选模式下不显示暂停、继续按钮,点一行是选中
 */
@Composable
private fun DownloadItemView(info: CacheInfo, isEditing: Boolean, modifier: Modifier, onStateClick: () -> Unit) {
    val name = DownloadName(info.name ?: "")
    Row(modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MusicImage(info.thumb, Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(name.title, color = Color.primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle(info, name),
                style = MaterialTheme.typography.bodySmall,
                color = if (info.state == DownloadState.ERROR) Color(0xFFFF3B30) else Color.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (info.state != DownloadState.FINISH) {
                LinearProgressIndicator(progress = { progress(info).toFloat() }, modifier = Modifier.fillMaxWidth(), color = Color.theme)
            }
        }
        val icon = stateIcon(info)
        if (!isEditing && icon != null) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onStateClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon.first, contentDescription = icon.second, tint = Color.theme, modifier = Modifier.size(28.dp))
            }
        }
    }
}

/** 下载进度(0到1) */
private fun progress(info: CacheInfo): Double {
    val size = info.size ?: return 0.0
    if (size == 0UL) {
        return 0.0
    }
    return minOf(info.downloaded.toDouble() / size.toDouble(), 1.0)
}

/** 歌手、音质和下载状态 */
private fun subtitle(info: CacheInfo, name: DownloadName): String {
    val detail = listOf(name.artist, name.quality).filter { it.isNotEmpty() }.joinToString(" · ")
    val sizeText = info.size?.toLong()?.fileSize
    val percent = (progress(info) * 100).toInt()
    return when (info.state) {
        DownloadState.FINISH -> listOf(detail, sizeText ?: "").filter { it.isNotEmpty() }.joinToString(" · ")
        DownloadState.DOWNLOADING -> {
            val downloaded = info.downloaded.toLong().fileSize
            "下载中 $percent% · " + (sizeText?.let { "$downloaded/$it" } ?: downloaded)
        }

        DownloadState.WAITING -> "等待下载"
        DownloadState.PAUSED -> "已暂停 $percent%"
        DownloadState.ERROR -> "下载失败" + (info.error?.let { ":$it" } ?: "")
    }
}

/** 右边的按钮,下载完成时没有 */
private fun stateIcon(info: CacheInfo): Pair<ImageVector, String>? {
    return when (info.state) {
        DownloadState.DOWNLOADING, DownloadState.WAITING -> Pair(Icons.Outlined.PauseCircle, "暂停下载")
        DownloadState.PAUSED -> Pair(Icons.Outlined.ArrowCircleDown, "继续下载")
        DownloadState.ERROR -> Pair(Icons.Outlined.Replay, "重新下载")
        DownloadState.FINISH -> null
    }
}

/**
 * 下载文件名"歌名 - 歌手 [音质].扩展名"拆分之后的各部分
 */
private class DownloadName(fileName: String) {

    /** 歌名 */
    val title: String

    /** 歌手 */
    val artist: String

    /** 音质、清晰度 */
    val quality: String

    init {
        var base = if (fileName.contains('.')) fileName.substringBeforeLast('.') else fileName
        var quality = ""
        val start = base.lastIndexOf(" [")
        if (base.endsWith("]") && start >= 0) {
            quality = base.substring(start + 2, base.length - 1)
            base = base.substring(0, start)
        }
        val separator = base.indexOf(" - ")
        if (separator >= 0) {
            this.title = base.substring(0, separator)
            this.artist = base.substring(separator + 3)
        } else {
            this.title = base
            this.artist = ""
        }
        this.quality = quality
    }
}
