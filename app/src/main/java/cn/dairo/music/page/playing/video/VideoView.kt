package cn.dairo.music.page.playing.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import cn.dairo.music.components.ActionSheet
import cn.dairo.music.components.AlertButton
import cn.dairo.music.components.AppAlert
import cn.dairo.music.components.ButtonRole
import cn.dairo.music.store.SettingStore

/**
 * 播放页的视频,全屏时视频显示在播放页的最上层,这里只显示黑色背景
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoView(vm: VideoViewModel, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16 / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            val player = vm.player
            if (player != null) {
                if (!vm.isFullScreen) {
                    VideoPlayerView(
                        player, vm.state, isFullScreen = false, onFullScreenClick = vm::enterFullScreen,
                        onPreviousClick = vm::onPreviousClick, onNextClick = vm::onNextClick, modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                Column(
                    Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (vm.isWaiting) {
                        CircularProgressIndicator(color = Color.White)
                    }
                    Text(vm.tip ?: "", style = MaterialTheme.typography.bodyMedium, color = Color.White, textAlign = TextAlign.Center)
                    if (vm.isOriginalOnly && !vm.isWaiting && vm.tip == "当前音乐未收藏,只能播放原画版") {
                        OutlinedButton(onClick = { vm.showOriginalConfirm = true }) {
                            Text("播放原画版", color = Color.White)
                        }
                    }

                    //没有视频、等待转码时也可以切换到上一个、下一个视频
                    if (!vm.isWaiting) {
                        Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                            SmallButton(Icons.Default.SkipPrevious, "上一个视频", vm::onPreviousClick)
                            SmallButton(Icons.Default.SkipNext, "下一个视频", vm::onNextClick)
                        }
                    }
                }
            }
        }
        Toolbar(vm)
    }
    AppAlert(
        isPresented = vm.showOriginalConfirm,
        title = "提示",
        message = "当前音乐未收藏，只能播放原画版，原画需要消耗更多流量，是否继续播放？",
        onDismiss = { vm.showOriginalConfirm = false },
        buttons = listOf(
            AlertButton("继续播放", action = vm::onOriginalConfirm),
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
    val vip = if (vm.isFileOutVipOnly) "(VIP)" else ""
    ActionSheet(
        isPresented = vm.showDownloadMenu,
        title = if (vm.isDownloaded) "已下载" else "下载视频",
        onDismiss = { vm.showDownloadMenu = false },
        buttons = buildList {
            if (!vm.isDownloaded) {
                add(AlertButton("只下载") { vm.onDownloadAction(null) })
            }
            if (vm.allowSaveAlbum) {
                add(AlertButton((if (vm.isDownloaded) "保存到相册" else "下载并保存到相册") + vip) { vm.onDownloadAction(VideoFileAction.SAVE_ALBUM) })
            }
            if (vm.allowSendFile) {
                add(AlertButton((if (vm.isDownloaded) "发送文件" else "下载并发送文件") + vip) { vm.onDownloadAction(VideoFileAction.SEND) })
            }
            add(AlertButton("取消", ButtonRole.CANCEL))
        },
    )
}

/** 清晰度、下载 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun Toolbar(vm: VideoViewModel) {
    var showQualityMenu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box {
            val qualityText = if (vm.isOriginalOnly || vm.quality == SettingStore.VIDEO_QUALITY_ORIGINAL) "原画" else "流畅"
            ToolbarButton(Icons.Outlined.HighQuality, qualityText, enabled = !vm.isOriginalOnly) {
                showQualityMenu = true
            }
            DropdownMenu(expanded = showQualityMenu, onDismissRequest = { showQualityMenu = false }) {
                DropdownMenuItem(text = { Text("流畅") }, onClick = {
                    showQualityMenu = false
                    vm.onQualityChange(SettingStore.VIDEO_QUALITY_SMOOTH)
                })
                DropdownMenuItem(text = { Text(if (vm.isOriginalVipOnly) "原画(VIP)" else "原画") }, onClick = {
                    showQualityMenu = false
                    vm.onQualityChange(SettingStore.VIDEO_QUALITY_ORIGINAL)
                })
            }
        }
        Spacer(Modifier.weight(1f))
        val percent = vm.downloadPercent
        if (percent != null) {
            ToolbarButton(Icons.Outlined.ArrowCircleDown, "$percent%", enabled = false) {}
        } else {
            ToolbarButton(
                if (vm.isDownloaded) Icons.Outlined.CheckCircle else Icons.Outlined.ArrowCircleDown,
                if (vm.isDownloaded) "已下载" else "下载",
                enabled = vm.canDownload,
                onClick = vm::onDownloadClick,
            )
        }
    }
}

/** 视频下面的文字按钮 */
@Composable
private fun ToolbarButton(icon: ImageVector, text: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.White, modifier = Modifier.padding(start = 4.dp))
    }
}

/** 没有视频时的上一个、下一个按钮 */
@Composable
private fun SmallButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}
