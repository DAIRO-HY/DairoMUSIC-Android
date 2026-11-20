package cn.dairo.music.page.playing

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.dairo.music.extension.theme

/**
 * 播放页的操作引导,只在第一次进入播放页时显示:向右滑动看视频,向左滑动看歌词
 * @param onDismiss 关闭引导
 */
@Composable
fun PlayingGuideView(onDismiss: () -> Unit) {

    //箭头左右晃动的动画
    val transition = rememberInfiniteTransition(label = "guide")
    val moving by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "moving")
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(48.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                GuideItem(Icons.Default.SmartDisplay, "视频", "向右滑动", 1f, moving, Modifier.weight(1f))
                GuideItem(Icons.Default.FormatQuote, "歌词", "向左滑动", -1f, moving, Modifier.weight(1f))
            }
            Text(
                "播放页左右滑动可以切换视频、封面和歌词",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Text(
                "知道了",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.theme)
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 40.dp, vertical = 12.dp),
            )
        }
    }
}

/**
 * 一边的说明,视频在左边要向右滑,歌词在右边要向左滑
 * @param direction 手指滑动的方向,箭头朝这个方向晃动,-1:向左 1:向右
 */
@Composable
private fun GuideItem(icon: ImageVector, title: String, tip: String, direction: Float, moving: Float, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        Row(Modifier.offset(x = (direction * 8 * moving).dp), verticalAlignment = Alignment.CenterVertically) {
            if (direction < 0) {
                Icon(Icons.Default.KeyboardDoubleArrowLeft, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Text(tip, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            if (direction > 0) {
                Icon(Icons.Default.KeyboardDoubleArrowRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}
