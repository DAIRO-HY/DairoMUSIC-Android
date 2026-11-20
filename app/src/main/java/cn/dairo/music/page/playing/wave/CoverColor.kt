package cn.dairo.music.page.playing.wave

import android.graphics.Bitmap
import androidx.core.graphics.scale
import cn.dairo.music.components.MusicImageCache
import cn.dairo.music.util.FileSdkUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * 从封面取主色,波形颜色跟随封面时用
 */
object CoverColor {

    /** 取过的封面,换歌回来不用再算 */
    private val cache = HashMap<String, WaveColor>()

    /** 缩小到多大再取色 */
    private const val SAMPLE_SIZE = 24

    /** 取最鲜艳、最亮的多少个像素的平均色 */
    private const val TOP_COUNT = 40

    /** 封面的主色:缩小之后取最鲜艳、最亮的那些像素的平均色,再提亮到最亮的分量为1,在深色背景上看得清 */
    suspend fun dominant(url: String?): WaveColor? {
        if (url.isNullOrEmpty()) {
            return null
        }
        cache[url]?.let { return it }
        val data = FileSdkUtil.imageData(url) ?: return null
        val color = withContext(Dispatchers.Default) {
            MusicImageCache.decodeBitmap(data, 256)?.let { compute(it) }
        } ?: return null
        cache[url] = color
        return color
    }

    /** 计算主色 */
    private fun compute(image: Bitmap): WaveColor? {
        val size = SAMPLE_SIZE
        val small = image.scale(size, size)
        val pixels = IntArray(size * size)
        small.getPixels(pixels, 0, size, 0, 0, size, size)
        val candidates = pixels.map { pixel ->
            val r = (pixel shr 16 and 0xFF) / 255.0
            val g = (pixel shr 8 and 0xFF) / 255.0
            val b = (pixel and 0xFF) / 255.0
            val maxValue = max(r, max(g, b))
            val minValue = min(r, min(g, b))
            val saturation = if (maxValue > 0) (maxValue - minValue) / maxValue else 0.0
            doubleArrayOf(saturation * maxValue, r, g, b)
        }
        val top = candidates.sortedByDescending { it[0] }.take(TOP_COUNT)
        if (top.isEmpty()) {
            return null
        }
        val count = top.size.toDouble()
        val r = top.sumOf { it[1] } / count
        val g = top.sumOf { it[2] } / count
        val b = top.sumOf { it[3] } / count
        val brightest = max(max(r, g), max(b, 0.01))
        return WaveColor(r / brightest, g / brightest, b / brightest)
    }
}
