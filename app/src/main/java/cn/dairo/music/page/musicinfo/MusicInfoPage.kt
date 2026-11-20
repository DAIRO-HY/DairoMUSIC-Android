package cn.dairo.music.page.musicinfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
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
import cn.dairo.music.api.model.MusicModel
import cn.dairo.music.components.ActionSheet
import cn.dairo.music.components.AlertButton
import cn.dairo.music.components.AppAlert
import cn.dairo.music.components.AppSheet
import cn.dairo.music.components.ButtonRole
import cn.dairo.music.components.MusicImage
import cn.dairo.music.components.OnPageAppear
import cn.dairo.music.constants.EventName
import cn.dairo.music.extension.artistText
import cn.dairo.music.extension.like
import cn.dairo.music.extension.maxBitRate
import cn.dairo.music.extension.nameText
import cn.dairo.music.extension.primaryText
import cn.dairo.music.extension.qualityText
import cn.dairo.music.extension.secondaryText
import cn.dairo.music.extension.theme
import cn.dairo.music.page.musiccomplaint.MusicComplaintPage
import cn.dairo.music.page.musiccorrect.MusicCorrectPage
import cn.dairo.music.util.MusicDownloadUtil
import cn.dairo.music.util.OnReceive
import cn.dairo.music.util.ServerConfig
import cn.dairo.music.util.rememberStateObject
import cn.dairo.ui.root.RootView
import cn.dairo.ui.setting.SettingButton
import cn.dairo.ui.setting.SettingCustomView
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingIcon
import cn.dairo.ui.setting.SettingNavigationLink
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.setting.SettingText
import cn.dairo.ui.util.fileSize
import cn.dairo.ui.util.timeFormat

/** 设置项的图标 */
private fun icon(icon: ImageVector, color: Color) = SettingIcon.System(icon, backgroundColor = color)

/**
 * 从下方弹出歌曲信息,对应iOS的.sheet(item:)
 * @param favoriteId 歌曲所在的我的歌单,不在我的歌单里时为null
 */
@Composable
fun MusicInfoSheet(music: MusicModel, favoriteId: Long?, onDismiss: () -> Unit) {
    AppSheet(onDismiss = onDismiss) {
        RootView {
            MusicInfoPage(music, favoriteId, onDismiss)
        }
    }
}

/**
 * 歌曲信息和操作,从下方弹出
 * @param favoriteId 歌曲所在的我的歌单,不在我的歌单里时为null
 * @param dismiss 关闭页面
 */
@Composable
fun MusicInfoPage(music: MusicModel, favoriteId: Long?, dismiss: () -> Unit) {
    val vm = rememberStateObject { MusicInfoViewModel(music, favoriteId) }

    /** 显示收藏到歌单的选择 */
    var showFavoritePicker by remember { mutableStateOf(false) }

    /** 显示从歌单删除的确认框 */
    var showRemoveAlert by remember { mutableStateOf(false) }

    /** 显示下载音质的选择 */
    var showDownloadPicker by remember { mutableStateOf(false) }

    /** 显示删除下载的确认(只下载了一种音质时) */
    var showDeleteDownloadAlert by remember { mutableStateOf(false) }

    /** 显示删除哪种音质的选择(下载了多种音质时) */
    var showDeleteDownloadPicker by remember { mutableStateOf(false) }

    /** 显示审核状态的选择 */
    var showAuditPicker by remember { mutableStateOf(false) }

    /** 显示拒绝的确认框 */
    var showRejectAlert by remember { mutableStateOf(false) }

    OnReceive(EventName.MUSIC_CHANGE) {
        vm.updateLike()
    }
    val current = vm.music
    SettingStack {

        //纠错页面返回时也刷新,信任会员的纠错直接生效
        OnPageAppear {
            vm.loadDetail()
        }
        SettingPage(title = "歌曲信息") {
            SettingCustomView {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MusicImage(current.logo, Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            current.nameText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.primaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(current.artistText, style = MaterialTheme.typography.bodyMedium, color = Color.secondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            SettingGroup {
                SettingButton(
                    if (vm.isLike) "取消喜欢" else "喜欢",
                    icon = icon(if (vm.isLike) Icons.Default.HeartBroken else Icons.Default.Favorite, Color.like),
                    onClick = vm::onLikeClick,
                )
                SettingButton("收藏到歌单", icon = icon(Icons.Default.Add, Color.theme)) {
                    showFavoritePicker = true
                }
                val favorite = vm.favorite
                if (favorite != null) {
                    SettingButton("从「${favorite.name}」删除", icon = icon(Icons.Default.Delete, Color(0xFFFF3B30))) {
                        showRemoveAlert = true
                    }
                }
                SettingButton("分享", icon = icon(Icons.Default.Share, Color(0xFF007AFF)), onClick = vm::onShareClick)
                SettingButton("下载", tip = vm.downloadedText, icon = icon(Icons.Default.Download, Color(0xFF34C759))) {
                    showDownloadPicker = true
                }

                //下载完成之后可以通过其他App发送歌曲文件,服务器不允许发送时不显示,非VIP点击时提示需要VIP
                if (ServerConfig.allowSendFile && vm.sendFile != null) {
                    SettingButton(
                        "发送歌曲文件",
                        tip = if (ServerConfig.isVipLimited) "需要VIP" else "发送到其他App",
                        icon = icon(Icons.AutoMirrored.Filled.Send, Color(0xFFFF9500)),
                        onClick = vm::onSendFileClick,
                    )
                }

                //已经下载的可以删除,不允许下载时也可以删除
                if (vm.downloads.isNotEmpty()) {
                    SettingButton("删除下载", icon = icon(Icons.Default.DeleteForever, Color(0xFFFF3B30))) {
                        if (vm.downloads.size == 1) {
                            showDeleteDownloadAlert = true
                        } else {
                            showDeleteDownloadPicker = true
                        }
                    }
                }
            }
            SettingGroup {
                SettingText("歌手：" + current.artistText)
                SettingText("专辑：" + (current.album ?: "未知专辑"))
                SettingText("最高音质：" + MusicModel.qualityText(current.maxBitRate))
                SettingText("当前音质：" + MusicModel.qualityText(vm.displayFile?.bitRate) + (vm.displayTag?.let { "($it)" } ?: ""))
                SettingText("文件大小：" + (vm.displayFile?.size?.fileSize ?: "未知"))
                SettingText("时长：" + (current.duration?.timeFormat ?: "未知"))
            }
            if (current.id != 0L) {
                SettingGroup {

                    //只有信任会员可以审核
                    if (vm.isFriend) {
                        SettingButton("审核状态", tip = vm.auditText, icon = icon(Icons.Default.VerifiedUser, Color(0xFF30B0C7))) {
                            showAuditPicker = true
                        }
                    }
                    SettingNavigationLink("纠错", tip = "歌名、歌手、专辑不对", icon = icon(Icons.Default.Edit, Color(0xFFFF9500))) {
                        MusicCorrectPage(current)
                    }
                    SettingNavigationLink("投诉", tip = "音质、内容有问题", icon = icon(Icons.Default.Feedback, Color(0xFF8E8E93))) {
                        MusicComplaintPage(current.id)
                    }
                }
            }
        }
    }
    ActionSheet(
        isPresented = showFavoritePicker,
        title = "收藏到歌单",
        onDismiss = { showFavoritePicker = false },
        buttons = vm.favoriteList.map { favorite ->
            AlertButton(favorite.name) { vm.onAddToFavoriteClick(favorite) }
        } + AlertButton("取消", ButtonRole.CANCEL),
    )
    ActionSheet(
        isPresented = showDownloadPicker,
        title = "选择下载的音质",
        onDismiss = { showDownloadPicker = false },
        buttons = MusicDownloadUtil.qualities(current).map { quality ->
            AlertButton(quality.label) { vm.onDownloadClick(quality) }
        } + AlertButton("取消", ButtonRole.CANCEL),
    )
    AppAlert(
        isPresented = showDeleteDownloadAlert,
        title = "确定删除下载的歌曲吗？",
        message = vm.downloads.firstOrNull()?.let { vm.downloadLabel(it) },
        onDismiss = { showDeleteDownloadAlert = false },
        buttons = listOf(
            AlertButton("删除", ButtonRole.DESTRUCTIVE) { vm.onDeleteDownloadClick(vm.downloads) },
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
    ActionSheet(
        isPresented = showDeleteDownloadPicker,
        title = "删除下载",
        onDismiss = { showDeleteDownloadPicker = false },
        buttons = vm.downloads.map { info ->
            AlertButton(vm.downloadLabel(info), ButtonRole.DESTRUCTIVE) { vm.onDeleteDownloadClick(listOf(info)) }
        } + AlertButton("全部删除", ButtonRole.DESTRUCTIVE) { vm.onDeleteDownloadClick(vm.downloads) } + AlertButton("取消", ButtonRole.CANCEL),
    )
    ActionSheet(
        isPresented = showAuditPicker,
        title = "审核状态",
        onDismiss = { showAuditPicker = false },
        buttons = MusicInfoViewModel.AUDIT_STATES.map { state ->
            AlertButton(state.second) {
                if (state.first == 2) {

                    //选项框关闭之后再弹出确认框
                    showRejectAlert = true
                } else {
                    vm.onAuditClick(state.first)
                }
            }
        } + AlertButton("取消", ButtonRole.CANCEL),
    )
    AppAlert(
        isPresented = showRejectAlert,
        title = "确定拒绝这首歌吗？",
        message = "拒绝之后所有人从曲库都搜索不到这首歌,已经在歌单里的不受影响",
        onDismiss = { showRejectAlert = false },
        buttons = listOf(
            AlertButton("拒绝", ButtonRole.DESTRUCTIVE) { vm.onAuditClick(2) },
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
    AppAlert(
        isPresented = showRemoveAlert,
        title = "确定从歌单删除这首歌吗？",
        onDismiss = { showRemoveAlert = false },
        buttons = listOf(
            AlertButton("删除", ButtonRole.DESTRUCTIVE) { vm.onRemoveClick(dismiss) },
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
}
