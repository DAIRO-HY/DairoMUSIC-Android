package cn.dairo.music.page.playing.lrc

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import cn.dairo.music.player.PlayerManager
import cn.dairo.music.player.PlayerProgress
import cn.dairo.music.util.rememberStateObject
import kotlinx.coroutines.flow.first

/** 歌词列表上下的空白,第一行和最后一行也能滚动到中间 */
private val VERTICAL_PADDING = 200.dp

/**
 * 歌词,当前播放的那一行高亮并滚动到中间,点击歌词跳转到对应的位置
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun LrcView(modifier: Modifier = Modifier) {
    val vm = rememberStateObject { LrcViewModel() }
    val tip = vm.tip
    if (tip != null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(tip, color = Color.White.copy(alpha = 0.7f))
        }
        return
    }
    val currentIndex = vm.currentIndex(PlayerProgress.currentTime)
    val listState = rememberLazyListState()
    val paddingPx = with(LocalDensity.current) { VERTICAL_PADDING.toPx() }

    //当前行滚动到中间
    LaunchedEffect(currentIndex, vm.lines) {
        val index = currentIndex ?: return@LaunchedEffect

        //第一次显示时等列表布局完成
        val viewport = snapshotFlow { listState.layoutInfo.viewportSize.height }.first { it > 0 }
        val itemHeight = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }?.size ?: 0
        val offset = (paddingPx - (viewport / 2f - itemHeight / 2f)).toInt()
        listState.animateScrollToItem(index, offset)
    }
    LazyColumn(
        modifier
            .fillMaxSize()

            //上下渐隐
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(0f to Color.Transparent, 0.33f to Color.Black, 0.67f to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn,
                )
            },
        state = listState,
        contentPadding = PaddingValues(vertical = VERTICAL_PADDING, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        itemsIndexed(vm.lines) { i, line ->
            val isCurrent = i == currentIndex
            Text(
                line.text,
                style = if (isCurrent) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = Color.White.copy(alpha = if (isCurrent) 1f else 0.55f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        PlayerManager.seek(line.time)
                    },
            )
        }
    }
}
