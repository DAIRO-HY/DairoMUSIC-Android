package cn.dairo.music.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.dairo.music.api.model.MusicModel
import cn.dairo.music.extension.artistAlbumText
import cn.dairo.music.extension.maxBitRate
import cn.dairo.music.extension.nameText
import cn.dairo.music.extension.primaryText
import cn.dairo.music.extension.secondaryText
import cn.dairo.music.extension.theme
import cn.dairo.music.store.SettingStore
import java.util.Locale

/**
 * 歌曲列表的一行
 * @param isPlaying 是否正在播放这一首
 * @param onMoreClick 更多按钮点击事件
 */
@Composable
fun MusicItemView(music: MusicModel, isPlaying: Boolean, modifier: Modifier = Modifier, onMoreClick: () -> Unit) {
    Row(
        modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MusicImage(music.logo, Modifier.size(46.dp).clip(RoundedCornerShape(6.dp)))
        Column(Modifier.weight(1f)) {
            Text(
                music.nameText,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isPlaying) Color.theme else Color.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                if (music.id == 0L) {

                    //采集平台的歌曲,还没有加入曲库
                    MusicTag(music.collect?.platform ?: "", Color.secondaryText)
                    Spacer(Modifier.width(4.dp))
                }
                if ((music.maxBitRate ?: 0) >= SettingStore.BIT_RATE_LOSSLESS) {
                    MusicTag("SQ", Color.theme)
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    music.artistAlbumText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                //播放次数,只有曲库的搜索结果有
                val playCount = music.playCount
                if (playCount != null) {
                    Spacer(Modifier.weight(1f).width(4.dp))
                    Icon(Icons.Outlined.PlayCircle, contentDescription = null, tint = Color.secondaryText, modifier = Modifier.size(12.dp))
                    Text(playCountText(playCount), fontSize = 11.sp, color = Color.secondaryText, modifier = Modifier.padding(start = 2.dp))
                }
            }
        }
        if (isPlaying) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color.theme, modifier = Modifier.size(16.dp))
        }
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onMoreClick)
                .semantics { contentDescription = "更多操作" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.secondaryText)
        }
    }
}

/** 歌曲的小标签:采集平台、无损 */
@Composable
private fun MusicTag(text: String, color: Color) {
    Text(
        text,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        color = color,
        modifier = Modifier
            .border(0.5.dp, color, RoundedCornerShape(3.dp))
            .padding(horizontal = 3.dp),
    )
}

/** 播放次数的显示文字,1万以上显示为"1.2万" */
private fun playCountText(count: Int): String {
    if (count < 10000) {
        return count.toString()
    }
    val text = String.format(Locale.US, "%.1f", count / 10000.0)
    return (if (text.endsWith(".0")) text.dropLast(2) else text) + "万"
}
