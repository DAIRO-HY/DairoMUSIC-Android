package cn.dairo.music.util

import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import cn.dairo.ui.DairoUI
import cn.dairo.ui.root.Toast
import java.io.File

/**
 * 系统分享面板:分享链接,通过其他App发送下载的文件
 */
object ShareUtil {

    /**
     * 发送下载的文件
     * 缓存里的文件名不是歌名,先复制到缓存目录,改成"歌名 - 歌手.扩展名"再发送,只保留这次要发送的文件
     * @param path 下载的文件路径
     * @param name 发送的文件名,不带扩展名
     * @param ext 扩展名,为空时用下载文件的扩展名
     */
    fun sendFile(path: String, name: String, ext: String?) {
        val context = DairoUI.context
        val source = File(path)
        val extension = ext?.takeIf { it.isNotEmpty() } ?: source.extension
        val fileName = name.replace("/", "_")
        val folder = File(context.cacheDir, "send_file")
        val target = File(folder, if (extension.isEmpty()) fileName else "$fileName.$extension")
        try {
            folder.deleteRecursively()
            folder.mkdirs()
            source.copyTo(target)
        } catch (e: Exception) {
            Toast.show("读取文件失败")
            return
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".file_provider", target)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "*/*"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        present(intent)
    }

    /** 分享链接 */
    fun shareUrl(url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        present(intent)
    }

    /** 弹出系统的分享面板 */
    private fun present(intent: Intent) {
        val chooser = Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        DairoUI.context.startActivity(chooser)
    }
}
