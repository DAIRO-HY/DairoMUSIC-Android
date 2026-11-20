package cn.dairo.music.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import cn.dairo.music.extension.artistText
import cn.dairo.music.extension.nameText
import cn.dairo.music.extension.primaryText
import cn.dairo.music.extension.secondaryText
import cn.dairo.music.page.playing.PlayListPage
import cn.dairo.music.player.PlayerManager
import cn.dairo.ui.root.RootView

/**
 * 迷你播放条,点击打开播放页面,没有歌曲时不显示
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun MiniPlayerBar(modifier: Modifier = Modifier) {

    /** 显示播放列表 */
    var showPlayList by remember { mutableStateOf(false) }
    val music = PlayerManager.music ?: return
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .liquidGlassBackground(RoundedCornerShape(28.dp))
            .clickable { AppRoot.showPlaying() }
            .padding(start = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MusicImage(music.logo, Modifier.size(40.dp).clip(CircleShape))
        Column(Modifier.weight(1f)) {
            Text(music.nameText, style = MaterialTheme.typography.bodyMedium, color = Color.primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                PlayerManager.collectMsg ?: music.artistText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        MiniPlayerButton(
            if (PlayerManager.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            if (PlayerManager.isPlaying) "暂停" else "播放",
        ) {
            PlayerManager.playOrPause()
        }
        MiniPlayerButton(Icons.AutoMirrored.Filled.List, "播放列表") {
            showPlayList = true
        }
    }
    if (showPlayList) {
        AppSheet(onDismiss = { showPlayList = false }, heightFraction = 0.6f) {
            RootView {
                PlayListPage()
            }
        }
    }
}

@Composable
private fun MiniPlayerButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = label, tint = Color.primaryText, modifier = Modifier.size(26.dp))
    }
}
