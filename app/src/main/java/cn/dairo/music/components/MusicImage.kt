package cn.dairo.music.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import cn.dairo.music.util.FileSdkUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 通过SDK加载的图片,尺寸由调用方通过modifier指定
 * 已经缓存好的直接读文件,没有缓存时通过SDK的本机服务下载,页面关闭时连接断开,SDK会自动取消下载
 * @param placeholder 没有图片或者加载失败时显示的图标
 */
@Composable
fun MusicImage(
    url: String?,
    modifier: Modifier = Modifier,
    placeholder: ImageVector = Icons.Default.MusicNote,
    colorFilter: ColorFilter? = null,
) {
    var image by remember(url) { mutableStateOf(url?.let { MusicImageCache.get(it) }) }
    LaunchedEffect(url) {
        if (url.isNullOrEmpty() || image != null) {
            return@LaunchedEffect
        }
        image = MusicImageCache.load(url)
    }
    Box(modifier.clipToBounds()) {
        val current = image
        if (current != null) {
            Image(current, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, colorFilter = colorFilter)
        } else {
            ImagePlaceholder(placeholder, Modifier.fillMaxSize())
        }
    }
}

/**
 * 已解码图片的内存缓存,避免列表滑动时重新解码
 */
object MusicImageCache {

    /** 解码之后图片最长边的最大像素数,避免大图占用太多内存 */
    private const val MAX_SIZE = 1024

    private val memoryCache = object : LruCache<String, ImageBitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    /** 读取内存里的图片 */
    fun get(url: String): ImageBitmap? = this.memoryCache.get(url)

    /** 加载图片,优先读内存缓存 */
    suspend fun load(url: String): ImageBitmap? {
        this.memoryCache.get(url)?.let { return it }
        val data = FileSdkUtil.imageData(url) ?: return null
        val image = withContext(Dispatchers.Default) { decode(data) } ?: return null
        this.memoryCache.put(url, image)
        return image
    }

    /** 解码图片,超过最大尺寸时缩小 */
    fun decodeBitmap(data: ByteArray, maxSize: Int = MAX_SIZE): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= maxSize) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeByteArray(data, 0, data.size, options)
    }

    private fun decode(data: ByteArray): ImageBitmap? {
        val bitmap = decodeBitmap(data) ?: return null
        bitmap.prepareToDraw()
        return bitmap.asImageBitmap()
    }
}
