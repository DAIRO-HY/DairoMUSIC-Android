package cn.dairo.music.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import cn.dairo.music.extension.secondaryBackground
import cn.dairo.music.extension.tertiaryText

/**
 * 图片还没有显示出来时的占位图,加载中、加载失败、服务器还没有生成缩略图时使用
 * 图标大小跟随占位区域,大格子和小格子看起来比例一样
 */
@Composable
fun ImagePlaceholder(icon: ImageVector, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.background(Color.secondaryBackground), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = Color.tertiaryText, modifier = Modifier.size(minOf(maxWidth, maxHeight) * 0.3f))
    }
}
