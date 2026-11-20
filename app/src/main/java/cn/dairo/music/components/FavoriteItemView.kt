package cn.dairo.music.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.dairo.music.api.model.FavoriteModel
import cn.dairo.music.extension.like
import cn.dairo.music.extension.primaryText
import cn.dairo.music.extension.secondaryBackground
import cn.dairo.music.extension.secondaryText

/**
 * 歌单列表的一行
 */
object FavoriteItemView {

    /** 封面的大小 */
    val LOGO_SIZE = 52.dp

    /** 封面和歌单名之间的距离 */
    val SPACING = 12.dp

    /** 歌单名到这一行左边的距离,分隔线和歌单名对齐 */
    val TEXT_LEADING = LOGO_SIZE + SPACING
}

/** 我的歌单,"我喜欢"用固定的图标 */
@Composable
fun FavoriteItemView(favorite: FavoriteModel, modifier: Modifier = Modifier) {
    FavoriteItemView(
        logo = if (favorite.isLike) null else favorite.logo,
        icon = if (favorite.isLike) Icons.Default.Favorite else Icons.AutoMirrored.Filled.QueueMusic,
        iconColor = if (favorite.isLike) Color.like else Color.secondaryText,
        name = favorite.name,
        musicCount = favorite.musicCount,
        modifier = modifier,
    )
}

/**
 * @param logo 封面图片,"我喜欢"和"最近播放"用固定的图标
 * @param icon 没有封面时的图标
 * @param iconColor 图标颜色
 */
@Composable
fun FavoriteItemView(logo: String?, icon: ImageVector, iconColor: Color, name: String, musicCount: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FavoriteItemView.SPACING),
    ) {
        Box(Modifier.size(FavoriteItemView.LOGO_SIZE).clip(RoundedCornerShape(8.dp))) {
            if (logo != null) {
                MusicImage(logo, Modifier.fillMaxSize(), placeholder = icon)
            } else {
                Box(Modifier.fillMaxSize().background(Color.secondaryBackground), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(26.dp))
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, color = Color.primaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${musicCount}首", style = MaterialTheme.typography.bodySmall, color = Color.secondaryText, modifier = Modifier.padding(top = 3.dp))
        }
    }
}
