package cn.dairo.music.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 简单的HTTP请求,查询采集进度等不需要登录票据、直接返回文字的地址使用
 */
object HttpUtil {

    /**
     * GET请求,返回内容的文字,网络错误时返回null
     */
    suspend fun text(url: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                val stream = if (connection.responseCode >= 400) connection.errorStream else connection.inputStream
                stream?.use { String(it.readBytes()) } ?: ""
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }
}
