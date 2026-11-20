package cn.dairo.music.page.playing.wave

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.media3.common.util.UnstableApi
import cn.dairo.music.components.MusicImage
import kotlin.math.sin

/**
 * 播放页封面:圆形封面跟着播放慢慢转,外面一圈跟着实时频谱起伏的波形
 * 可以叠加低音冲击波(鼓点时从封面往外扩散一圈光环),波形颜色可以跟随封面的主色
 * @param logo 封面地址
 * @param isPlaying 是否正在播放,暂停时波形全部落回、封面停止转动
 * @param style 波形样式
 * @param shockwave 是否显示低音冲击波
 * @param coverColor 波形颜色是否跟随封面
 * @param isActive 是否刷新动画,页面不显示、App不在前台时停止
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayingWaveView(logo: String?, isPlaying: Boolean, style: PlayingWaveStyle, shockwave: Boolean, coverColor: Boolean, isActive: Boolean, modifier: Modifier = Modifier) {

    /** 动画的状态,每一帧更新,不需要刷新视图 */
    val simulation = remember { WaveSimulation() }

    /** 封面的主色,跟随封面颜色时才取 */
    var tint by remember { mutableStateOf<WaveColor?>(null) }
    LaunchedEffect(coverColor, logo) {
        tint = if (coverColor) CoverColor.dominant(logo) else null
    }

    //每一帧前进一次
    var frameNanos by remember { mutableLongStateOf(0L) }
    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val currentStyle by rememberUpdatedState(style)
    val currentShockwave by rememberUpdatedState(shockwave)
    LaunchedEffect(isActive) {
        while (isActive) {
            withFrameNanos {
                simulation.advance(it / 1e9, currentIsPlaying, currentStyle, currentShockwave)
                frameNanos = it
            }
        }
    }
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        val logoSize = side * style.logoRatio.toFloat()

        //封面外面的光晕,静态的,不用每一帧画
        val glowSize = logoSize + side * 0.2f
        val glowColor = tint?.lightened(0.3)?.color(0.45) ?: Color(110 / 255f, 120 / 255f, 1f, 0.45f)
        Box(
            Modifier
                .size(glowSize)
                .background(
                    Brush.radialGradient(
                        (logoSize * 0.4f / (glowSize / 2)) to glowColor,
                        1f to Color.Transparent,
                    ),
                    CircleShape,
                ),
        )
        Box(Modifier.size(side), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(side).graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
                val time = frameNanos / 1e9
                val palette = tint?.let { WavePalette.cover(it) } ?: WavePalette.standard(sin(time * 0.25) * 0.5 + 0.5)
                scale(this.density, Offset.Zero) {
                    simulation.draw(this, side.value.toDouble(), style, palette)
                }
            }
            MusicImage(
                logo,
                Modifier
                    .size(logoSize)
                    .graphicsLayer {
                        @Suppress("UNUSED_EXPRESSION")
                        frameNanos
                        rotationZ = Math.toDegrees(simulation.rotation).toFloat()
                    }
                    .clip(CircleShape)
                    .border(side / 96, Color.White.copy(alpha = 0.08f), CircleShape),
            )
        }
    }
}
