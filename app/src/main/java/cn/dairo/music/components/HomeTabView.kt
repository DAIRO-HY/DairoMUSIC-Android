package cn.dairo.music.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cn.dairo.music.extension.primaryText
import cn.dairo.music.page.discover.DiscoverPage
import cn.dairo.music.page.favorite.FavoritePage
import cn.dairo.music.page.mine.MinePage

/**
 * tab页面标记
 */
enum class TabPage {
    DISCOVER_PAGE,//发现页面
    FAVORITE_PAGE,//音乐页面
    MINE_PAGE,//我的页面
}

/**
 * 首页底部:迷你播放条和Tab按钮
 * @param pageTag 当前选中的页面标记
 */
@Composable
fun HomeTabView(pageTag: TabPage, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().navigationBarsPadding(), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .widthIn(max = 420.dp)
                .padding(horizontal = 16.dp)
                .padding(bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MiniPlayerBar()
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .liquidGlassBackground(RoundedCornerShape(30.dp))
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TabButton("发现", Icons.Default.Explore, pageTag == TabPage.DISCOVER_PAGE) {
                    relaunch { DiscoverPage() }
                }
                TabButton("音乐", Icons.AutoMirrored.Filled.QueueMusic, pageTag == TabPage.FAVORITE_PAGE) {
                    relaunch { FavoritePage() }
                }
                TabButton("我的", Icons.Default.Person, pageTag == TabPage.MINE_PAGE) {
                    relaunch { MinePage() }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabButton(text: String, icon: ImageVector, isSelected: Boolean, action: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier
            .weight(1f)
            .height(52.dp)
            .clip(shape)
            .background(if (isSelected) Color.primaryText.copy(alpha = 0.12f) else Color.Transparent, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (!isSelected) {
                    action()
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.primaryText, modifier = Modifier.size(22.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.primaryText, modifier = Modifier.padding(top = 3.dp))
    }
}
