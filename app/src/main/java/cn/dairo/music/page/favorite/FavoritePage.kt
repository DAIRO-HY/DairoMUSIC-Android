package cn.dairo.music.page.favorite

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cn.dairo.music.api.model.FavoriteModel
import cn.dairo.music.components.AlertButton
import cn.dairo.music.components.AppAlert
import cn.dairo.music.components.ButtonRole
import cn.dairo.music.components.FavoriteItemView
import cn.dairo.music.components.HomeTabView
import cn.dairo.music.components.SwipeActionRow
import cn.dairo.music.components.TabPage
import cn.dairo.music.components.TextFieldAlert
import cn.dairo.music.constants.FavoriteId
import cn.dairo.music.extension.groupedBackground
import cn.dairo.music.extension.primaryText
import cn.dairo.music.extension.secondaryGroupedBackground
import cn.dairo.music.extension.secondaryText
import cn.dairo.music.extension.theme
import cn.dairo.music.page.musiclist.MusicListPage
import cn.dairo.music.util.rememberStateObject
import cn.dairo.ui.setting.LocalSettingNavigator
import cn.dairo.ui.setting.SettingNavigator
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.setting.settingColors
import cn.dairo.ui.setting.settingRowClickable

/** 分组卡片的左右边距 */
private val CARD_MARGIN = 16.dp

/** 卡片里歌单行的左右边距 */
private val ROW_PADDING = 8.dp

/** 卡片的圆角,和"我的"页面的分组(SettingGroup)一样 */
private val CORNER_RADIUS = 12.dp

/**
 * 我的歌单
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritePage() {
    val vm = rememberStateObject { FavoriteViewModel() }

    /** 显示新建歌单的输入框 */
    var showCreateAlert by remember { mutableStateOf(false) }

    /** 要删除的歌单 */
    var deleteFavorite by remember { mutableStateOf<FavoriteModel?>(null) }

    /** 是否排序模式,拖动排序 */
    var isEditing by remember { mutableStateOf(false) }

    SettingStack {
        val navigator = LocalSettingNavigator.current
        Column(Modifier.fillMaxSize().background(Color.groupedBackground)) {
            PullToRefreshBox(
                isRefreshing = vm.isRefreshing,
                onRefresh = vm::refresh,
                modifier = Modifier.weight(1f).fillMaxWidth().statusBarsPadding(),
            ) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
                    Text(
                        "音乐",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.primaryText,
                        modifier = Modifier.padding(horizontal = CARD_MARGIN + ROW_PADDING).padding(top = 8.dp, bottom = 12.dp),
                    )

                    //我喜欢、最近播放
                    Card {
                        vm.likeFavorite?.let { like ->
                            FavoriteRow(onClick = { navigator?.push { MusicListPage(like.id, like.name) } }) {
                                FavoriteItemView(like)
                            }
                            CardDivider()
                        }
                        FavoriteRow(onClick = { navigator?.push { MusicListPage(FavoriteId.RECENT, "最近播放") } }) {
                            FavoriteItemView(logo = null, icon = Icons.Default.Schedule, iconColor = Color.theme, name = "最近播放", musicCount = vm.recentCount)
                        }
                    }

                    //我的歌单标题,和卡片里的封面对齐
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = CARD_MARGIN + ROW_PADDING).padding(top = 24.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("我的歌单", style = MaterialTheme.typography.bodyMedium, color = Color.secondaryText, modifier = Modifier.weight(1f))
                        if (vm.userFavorites.isNotEmpty()) {
                            Text(
                                if (isEditing) "完成" else "排序",
                                style = MaterialTheme.typography.bodyMedium,
                                color = settingColors.accent,
                                modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { isEditing = !isEditing }.padding(4.dp),
                            )
                        }
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "新建歌单",
                            tint = settingColors.accent,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    vm.createName = ""
                                    showCreateAlert = true
                                }
                                .padding(2.dp)
                                .size(22.dp),
                        )
                    }
                    if (vm.userFavorites.isEmpty()) {

                        //没有歌单时的提示
                        Text(
                            "点击右上角的+新建歌单",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.secondaryText,
                            modifier = Modifier.padding(horizontal = CARD_MARGIN + ROW_PADDING),
                        )
                    } else {
                        UserFavorites(vm, isEditing, navigator) { deleteFavorite = it }
                    }
                }
            }
            HomeTabView(TabPage.FAVORITE_PAGE)
        }
    }
    TextFieldAlert(
        isPresented = showCreateAlert,
        title = "新建歌单",
        text = vm.createName,
        onTextChange = { vm.createName = it },
        placeholder = "请输入歌单名",
        onDismiss = { showCreateAlert = false },
        buttons = listOf(
            AlertButton("创建", action = vm::onCreateClick),
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
    AppAlert(
        isPresented = deleteFavorite != null,
        title = "确定删除歌单「${deleteFavorite?.name ?: ""}」吗？",
        onDismiss = { deleteFavorite = null },
        buttons = listOf(
            AlertButton("删除", ButtonRole.DESTRUCTIVE) {
                deleteFavorite?.let { vm.onDeleteClick(it) }
            },
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
}

/** 自己创建的歌单,左滑删除,排序模式下拖动右边的把手排序 */
@Composable
private fun UserFavorites(vm: FavoriteViewModel, isEditing: Boolean, navigator: SettingNavigator?, onDelete: (FavoriteModel) -> Unit) {

    /** 正在拖动的歌单序号 */
    var draggingIndex by remember { mutableIntStateOf(-1) }

    /** 拖动的距离 */
    var dragOffset by remember { mutableFloatStateOf(0f) }

    /** 一行的高度 */
    var rowHeight by remember { mutableIntStateOf(1) }

    val background = Color.secondaryGroupedBackground
    Card {
        vm.userFavorites.forEachIndexed { i, favorite ->

            //拖动排序时这一行跟着歌单移动,拖动的手势不会中断
            key(favorite.id) {
            val isDragging = i == draggingIndex
            Box(
                Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffset else 0f
                        shadowElevation = if (isDragging) 8f else 0f
                    }
                    .onSizeChanged { rowHeight = maxOf(1, it.height) },
            ) {
                Column {
                    SwipeActionRow(enabled = !isEditing, backgroundColor = background, onAction = { onDelete(favorite) }) {
                        FavoriteRow(onClick = if (isEditing) null else ({ navigator?.push { MusicListPage(favorite.id, favorite.name) } })) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FavoriteItemView(favorite, Modifier.weight(1f))
                                if (isEditing) {
                                    Icon(
                                        Icons.Default.DragHandle,
                                        contentDescription = "拖动排序",
                                        tint = Color.secondaryText,
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .size(28.dp)
                                            .pointerInput(favorite.id) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        draggingIndex = vm.userFavorites.indexOfFirst { it.id == favorite.id }
                                                        dragOffset = 0f
                                                    },
                                                    onDragEnd = {
                                                        draggingIndex = -1
                                                        dragOffset = 0f
                                                        vm.onMoveEnd()
                                                    },
                                                    onDragCancel = {
                                                        draggingIndex = -1
                                                        dragOffset = 0f
                                                        vm.onMoveEnd()
                                                    },
                                                ) { change, amount ->
                                                    change.consume()
                                                    dragOffset += amount.y

                                                    //拖过半行时和相邻的歌单交换位置
                                                    val index = draggingIndex
                                                    if (dragOffset > rowHeight / 2f && index < vm.userFavorites.size - 1) {
                                                        vm.onMove(index, index + 1)
                                                        draggingIndex = index + 1
                                                        dragOffset -= rowHeight
                                                    } else if (dragOffset < -rowHeight / 2f && index > 0) {
                                                        vm.onMove(index, index - 1)
                                                        draggingIndex = index - 1
                                                        dragOffset += rowHeight
                                                    }
                                                }
                                            },
                                    )
                                }
                            }
                        }
                    }
                    if (i < vm.userFavorites.size - 1) {
                        CardDivider()
                    }
                }
            }
            }
        }
    }
}

/** 圆角卡片,和"我的"页面的分组一样 */
@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = CARD_MARGIN)
            .fillMaxWidth()
            .clip(RoundedCornerShape(CORNER_RADIUS))
            .background(Color.secondaryGroupedBackground),
    ) {
        content()
    }
}

/** 卡片里的一行 */
@Composable
private fun FavoriteRow(onClick: (() -> Unit)?, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.settingRowClickable(onClick = onClick) else Modifier)
            .padding(horizontal = ROW_PADDING, vertical = 6.dp),
    ) {
        content()
    }
}

/** 卡片里的分隔线,和歌单名对齐 */
@Composable
private fun CardDivider() {
    HorizontalDivider(Modifier.padding(start = ROW_PADDING + FavoriteItemView.TEXT_LEADING), thickness = 0.5.dp)
}
