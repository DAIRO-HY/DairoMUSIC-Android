package cn.dairo.music.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import cn.dairo.music.extension.isDarkTheme

/**
 * 浮在页面上的背景,对应iOS的液态玻璃(liquidGlassBackground)
 * Android上没有毛玻璃效果,用不透明的背景加阴影代替
 * @param shape 背景形状
 * @param color 背景色,为null时使用默认的卡片颜色
 */
@Composable
fun Modifier.liquidGlassBackground(shape: Shape, color: Color? = null): Modifier {
    val dark = isDarkTheme
    val background = color ?: if (dark) Color(0xFF2C2C2E) else MaterialTheme.colorScheme.surfaceContainer
    val border = if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)
    return this
        .shadow(8.dp, shape, ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.2f))
        .background(background, shape)
        .border(0.5.dp, border, shape)
        .clip(shape)
}
