package cn.dairo.music.page.musiclist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.media3.common.util.UnstableApi
import cn.dairo.music.api.model.MusicModel
import cn.dairo.music.components.ActionSheet
import cn.dairo.music.components.AlertButton
import cn.dairo.music.components.AppAlert
import cn.dairo.music.components.AppRoot
import cn.dairo.music.components.ButtonRole
import cn.dairo.music.components.MiniPlayerBar
import cn.dairo.music.components.MusicImage
import cn.dairo.music.components.MusicItemView
import cn.dairo.music.components.NavigationPage
import cn.dairo.music.components.SwipeActionRow
import cn.dairo.music.constants.FavoriteId
import cn.dairo.music.extension.isSameMusic
import cn.dairo.music.extension.like
import cn.dairo.music.extension.listBackground
import cn.dairo.music.extension.listId
import cn.dairo.music.extension.primaryText
import cn.dairo.music.extension.secondaryBackground
import cn.dairo.music.extension.secondaryText
import cn.dairo.music.extension.theme
import cn.dairo.music.page.favoriteedit.FavoriteEditPage
import cn.dairo.music.page.musicinfo.MusicInfoSheet
import cn.dairo.music.player.PlayerManager
import cn.dairo.music.util.MusicDownloadUtil
import cn.dairo.music.util.rememberStateObject
import cn.dairo.ui.setting.LocalSettingNavigator

/**
 * 歌单里的歌曲列表,我的歌单、推荐的歌单、最近播放都使用这个页面
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun MusicListPage(favoriteId: Long, name: String) {
    val vm = rememberStateObject { MusicListViewModel(favoriteId, name) }
    val navigator = LocalSettingNavigator.current

    /** 要显示详情的歌曲 */
    var infoMusic by remember { mutableStateOf<MusicModel?>(null) }

    /** 显示清空最近播放的确认框 */
    var showClearAlert by remember { mutableStateOf(false) }

    /** 显示全部下载的音质选择 */
    var showDownloadPicker by remember { mutableStateOf(false) }

    val background = Color.listBackground
    NavigationPage(
        title = vm.name,
        actions = {
            if (vm.isMine && !vm.isLike) {
                IconButton(onClick = { navigator?.push { FavoriteEditPage(vm.favoriteId) } }) {
                    Icon(Icons.Default.EditNote, contentDescription = "编辑歌单")
                }
            }
            if (vm.favoriteId == FavoriteId.RECENT && vm.musicList.isNotEmpty()) {
                IconButton(onClick = { showClearAlert = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "清空最近播放")
                }
            }
        },
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Header(vm) { showDownloadPicker = true }
                }
                if (vm.musicList.isEmpty() && !vm.isLoading) {
                    item {
                        Text(
                            "没有歌曲",
                            color = Color.secondaryText,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
                itemsIndexed(vm.musicList, key = { _, music -> music.listId }) { i, music ->
                    SwipeActionRow(enabled = vm.isMine, backgroundColor = background, onAction = { vm.remove(music) }) {
                        MusicItemView(
                            music,
                            isPlaying = isPlaying(vm, music),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    vm.play(i)

                                    //点击歌曲之后打开播放页面
                                    AppRoot.showPlaying()
                                }
                                .padding(horizontal = 16.dp),
                        ) {
                            infoMusic = music
                        }
                    }
                    HorizontalDivider(Modifier.padding(start = 74.dp))
                }
            }
            if (vm.isLoading && vm.musicList.isEmpty()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
        MiniPlayerBar(Modifier.navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 4.dp))
    }
    infoMusic?.let {
        MusicInfoSheet(it, favoriteId = if (vm.isMine) vm.favoriteId else null) { infoMusic = null }
    }

    //下载音质选项读取了VIP状态,VIP状态变化时自动刷新VIP标记
    ActionSheet(
        isPresented = showDownloadPicker,
        title = "选择下载的音质",
        message = "歌曲没有选择的音质时下载最接近的音质",
        onDismiss = { showDownloadPicker = false },
        buttons = MusicDownloadUtil.listQualities.map { quality ->
            AlertButton(quality.label) {
                MusicDownloadUtil.downloadAll(vm.musicList, quality)
            }
        } + AlertButton("取消", ButtonRole.CANCEL),
    )
    AppAlert(
        isPresented = showClearAlert,
        title = "确定清空最近播放吗？",
        onDismiss = { showClearAlert = false },
        buttons = listOf(
            AlertButton("清空", ButtonRole.DESTRUCTIVE, vm::clearRecent),
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
}

/** 歌单信息 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun Header(vm: MusicListViewModel, onDownloadClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(96.dp).clip(RoundedCornerShape(10.dp))) {
            if (vm.isLike) {
                HeaderIcon(Icons.Default.Favorite, Color.like)
            } else if (vm.favoriteId == FavoriteId.RECENT) {
                HeaderIcon(Icons.Default.Schedule, Color.theme)
            } else {
                MusicImage(vm.logo, Modifier.fillMaxSize(), placeholder = Icons.AutoMirrored.Filled.QueueMusic)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(vm.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.primaryText, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val description = vm.description
            if (!description.isNullOrEmpty()) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.secondaryText,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${vm.musicList.size}首", style = MaterialTheme.typography.bodySmall, color = Color.secondaryText, modifier = Modifier.weight(1f))
                if (vm.musicList.isNotEmpty()) {
                    Row(
                        Modifier.clip(RoundedCornerShape(4.dp)).clickable(onClick = onDownloadClick).padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.theme, modifier = Modifier.size(16.dp))
                        Text("全部下载", style = MaterialTheme.typography.bodySmall, color = Color.theme, modifier = Modifier.padding(start = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderIcon(icon: ImageVector, color: Color) {
    Box(Modifier.fillMaxSize().background(Color.secondaryBackground), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(40.dp))
    }
}

/** 是否正在播放的歌曲,只在播放的是这个列表时显示 */
@androidx.annotation.OptIn(UnstableApi::class)
private fun isPlaying(vm: MusicListViewModel, music: MusicModel): Boolean {
    if (PlayerManager.favoriteId != vm.favoriteId) {
        return false
    }
    val playing = PlayerManager.music ?: return false
    return playing.isSameMusic(music)
}
