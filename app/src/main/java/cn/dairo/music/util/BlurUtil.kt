package cn.dairo.music.util

import android.graphics.Bitmap
import androidx.core.graphics.scale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 图片模糊,Android 12以下的系统不支持Modifier.blur时使用
 * 先把图片缩得很小再做几遍均值模糊,放大显示之后就是很柔和的模糊效果,速度也很快
 */
object BlurUtil {

    /**
     * 生成模糊图片
     * @param width 缩小之后的宽度,越小越模糊
     * @param radius 均值模糊的半径
     */
    fun blur(source: Bitmap, width: Int = 48, radius: Int = 3): Bitmap {
        val height = max(1, (source.height.toFloat() / source.width * width).roundToInt())
        val small = source.copy(Bitmap.Config.ARGB_8888, false).scale(width, height)
        val pixels = IntArray(width * height)
        small.getPixels(pixels, 0, width, 0, 0, width, height)

        //横竖各做三遍均值模糊,接近高斯模糊
        repeat(3) {
            boxBlur(pixels, width, height, radius, horizontal = true)
            boxBlur(pixels, width, height, radius, horizontal = false)
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    /** 一个方向的均值模糊 */
    private fun boxBlur(pixels: IntArray, width: Int, height: Int, radius: Int, horizontal: Boolean) {
        val lines = if (horizontal) height else width
        val length = if (horizontal) width else height
        val line = IntArray(length)
        for (l in 0 until lines) {
            for (i in 0 until length) {
                line[i] = pixels[if (horizontal) l * width + i else i * width + l]
            }
            for (i in 0 until length) {
                var a = 0
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                for (k in i - radius..i + radius) {
                    val p = line[k.coerceIn(0, length - 1)]
                    a += p ushr 24
                    r += p shr 16 and 0xFF
                    g += p shr 8 and 0xFF
                    b += p and 0xFF
                    count++
                }
                pixels[if (horizontal) l * width + i else i * width + l] =
                    (a / count shl 24) or (r / count shl 16) or (g / count shl 8) or (b / count)
            }
        }
    }
}
