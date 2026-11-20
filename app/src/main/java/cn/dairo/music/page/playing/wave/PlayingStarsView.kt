package cn.dairo.music.page.playing.wave

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * 播放页背景:大小不一的十字发光星星慢慢往上飘,忽明忽暗;大的星星飘得快、显得近,小的慢、显得远
 * @param isActive 是否刷新动画,页面不显示、App不在前台时停止
 */
@Composable
fun PlayingStarsView(isActive: Boolean, modifier: Modifier = Modifier) {
    val field = remember { StarField() }
    var frameNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isActive) {
        while (isActive) {
            withFrameNanos { frameNanos = it }
        }
    }
    Canvas(modifier) {
        val time = frameNanos / 1e9
        scale(this.density, Offset.Zero) {
            field.draw(this, Size(size.width / density, size.height / density), time)
        }
    }
}

/**
 * 星星的位置和动画,坐标的单位是dp
 */
private class StarField {

    /**
     * @param depth 远近(0到1),越大越近:越大、越亮、飘得越快
     * @param speed 往上飘的速度(dp/秒)
     * @param phase 闪烁的相位
     * @param twinkle 闪烁的速度
     * @param sway 左右摆动的相位
     * @param isWarm 是否暖色(少数星星偏金色)
     */
    class Star(var x: Double, var y: Double, val depth: Double, val size: Double, val speed: Double, val phase: Double, val twinkle: Double, val sway: Double, val isWarm: Boolean)

    companion object {

        /** 星星数量 */
        private const val COUNT = 34

        /** 冷色的星星图片,只生成一次 */
        private val coolSprite by lazy { sprite(Color(200 / 255f, 215 / 255f, 1f), Color(190 / 255f, 205 / 255f, 1f)) }

        /** 暖色的星星图片 */
        private val warmSprite by lazy { sprite(Color(1f, 220 / 255f, 170 / 255f), Color(1f, 205 / 255f, 150 / 255f)) }

        /** 随机生成一颗星星,小星多,大星少 */
        private fun random(size: Size, y: Double): Star {
            val depth = Random.nextDouble().pow(2.2)
            return Star(
                x = Random.nextDouble() * maxOf(1.0, size.width.toDouble()), y = y, depth = depth, size = 9 + depth * 32, speed = 6 + depth * 22,
                phase = Random.nextDouble() * 2 * PI, twinkle = 0.6 + Random.nextDouble() * 1.6,
                sway = Random.nextDouble() * 2 * PI, isWarm = Random.nextDouble() < 0.18,
            )
        }

        /** 生成星星图片:中间一团光,横竖两道细长的光芒;128dp的图按2倍生成 */
        private fun sprite(core: Color, ray: Color): ImageBitmap {
            val pixel = 256
            val image = ImageBitmap(pixel, pixel)
            CanvasDrawScope().draw(Density(2f), LayoutDirection.Ltr, Canvas(image), Size(pixel.toFloat(), pixel.toFloat())) {
                scale(2f, Offset.Zero) {
                    val c = 64f
                    drawCircle(
                        Brush.radialGradient(
                            0f to Color.White, 0.15f to core.copy(alpha = 0.7f), 1f to Color(140 / 255f, 150 / 255f, 1f, 0f),
                            center = Offset(c, c), radius = 30f,
                        ),
                        radius = 30f, center = Offset(c, c),
                    )
                    val horizontal = Path().apply {
                        moveTo(0f, c)
                        quadraticTo(c, c - 3, 128f, c)
                        quadraticTo(c, c + 3, 0f, c)
                    }
                    drawPath(horizontal, Brush.horizontalGradient(0f to ray.copy(alpha = 0f), 0.5f to Color.White, 1f to ray.copy(alpha = 0f), startX = 0f, endX = 128f))
                    val vertical = Path().apply {
                        moveTo(c, 0f)
                        quadraticTo(c + 3, c, c, 128f)
                        quadraticTo(c - 3, c, c, 0f)
                    }
                    drawPath(vertical, Brush.verticalGradient(0f to ray.copy(alpha = 0f), 0.5f to Color.White, 1f to ray.copy(alpha = 0f), startY = 0f, endY = 128f))
                }
            }
            return image
        }
    }

    private var stars = emptyList<Star>()
    private var canvasSize = Size.Zero
    private var lastTime: Double? = null

    /** 画这一帧:往上飘,飘出顶部之后从底部重新出现 */
    fun draw(scope: androidx.compose.ui.graphics.drawscope.DrawScope, size: Size, time: Double) {
        if (size != this.canvasSize) {
            this.canvasSize = size
            this.stars = List(COUNT) { random(size, Random.nextDouble() * size.height) }
        }
        val dt = minOf(0.05, maxOf(0.0, time - (this.lastTime ?: time)))
        this.lastTime = time
        val cool = coolSprite
        val warm = warmSprite
        for (i in this.stars.indices) {
            val star = this.stars[i]
            star.y -= star.speed * dt
            if (star.y < -star.size) {
                this.stars = this.stars.toMutableList().also { it[i] = random(size, size.height + star.size) }
                continue
            }
            val x = star.x + sin(time * 0.4 + star.sway) * 3
            val alpha = (0.45 + 0.55 * (0.5 + 0.5 * sin(time * star.twinkle + star.phase))) * (0.5 + 0.5 * star.depth)
            val half = star.size / 2
            scope.drawImage(
                if (star.isWarm) warm else cool,
                dstOffset = IntOffset((x - half).roundToInt(), (star.y - half).roundToInt()),
                dstSize = IntSize(star.size.roundToInt(), star.size.roundToInt()),
                alpha = alpha.toFloat().coerceIn(0f, 1f),
                blendMode = BlendMode.Plus,
            )
        }
    }
}
