package cn.dairo.music.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 向左滑动显示操作按钮的一行,对应iOS列表的swipeActions
 * @param enabled 不能操作时不响应滑动
 * @param actionText 按钮文字
 * @param backgroundColor 这一行的背景色,滑动时盖住后面的按钮
 * @param onAction 按钮点击事件
 */
@Composable
fun SwipeActionRow(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    actionText: String = "删除",
    backgroundColor: Color,
    onAction: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        Box(modifier.fillMaxWidth().background(backgroundColor)) { content() }
        return
    }
    val actionWidth = 80.dp
    val actionWidthPx = with(LocalDensity.current) { actionWidth.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(actionWidth)
                .background(Color(0xFFFF3B30))
                .clickable {
                    scope.launch { offset.animateTo(0f) }
                    onAction()
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(actionText, color = Color.White)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .background(backgroundColor)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch { offset.animateTo(if (offset.value < -actionWidthPx / 2) -actionWidthPx else 0f) }
                        },
                        onDragCancel = {
                            scope.launch { offset.animateTo(0f) }
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch { offset.snapTo((offset.value + dragAmount).coerceIn(-actionWidthPx, 0f)) }
                    }
                },
        ) {
            content()
        }
    }
}
