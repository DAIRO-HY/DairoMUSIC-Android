package cn.dairo.music.page.playing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import cn.dairo.music.components.SwipeActionRow
import cn.dairo.music.extension.artistText
import cn.dairo.music.extension.listBackground
import cn.dairo.music.extension.nameText
import cn.dairo.music.extension.primaryText
import cn.dairo.music.extension.secondaryText
import cn.dairo.music.extension.theme
import cn.dairo.music.player.PlayerManager
import cn.dairo.ui.setting.settingColors

/**
 * 当前播放列表
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayListPage() {
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        listState.scrollToItem(maxOf(0, PlayerManager.index - 3))
    }
    val background = Color.listBackground
    Column(Modifier.fillMaxSize().background(background)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)) {

            //播放顺序
            Row(
                Modifier
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = PlayerManager::switchOrderType)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(PlayerManager.orderType.icon, contentDescription = null, tint = settingColors.accent, modifier = Modifier.size(20.dp))
                Text(PlayerManager.orderType.label, style = MaterialTheme.typography.bodyMedium, color = settingColors.accent, modifier = Modifier.padding(start = 4.dp))
            }
            Text(
                "播放列表(${PlayerManager.playList.size})",
                style = MaterialTheme.typography.titleMedium,
                color = Color.primaryText,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        HorizontalDivider()
        LazyColumn(Modifier.fillMaxSize(), state = listState) {
            itemsIndexed(PlayerManager.playList) { i, music ->
                val isCurrent = i == PlayerManager.index
                SwipeActionRow(backgroundColor = background, onAction = { PlayerManager.remove(i) }) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { PlayerManager.playAt(i) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(music.nameText, color = if (isCurrent) Color.theme else Color.primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(music.artistText, style = MaterialTheme.typography.bodySmall, color = Color.secondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (isCurrent) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color.theme, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(start = 16.dp))
            }
        }
    }
}
