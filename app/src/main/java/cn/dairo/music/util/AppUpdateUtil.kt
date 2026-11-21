package cn.dairo.music.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import cn.dairo.music.constants.Const
import cn.dairo.ui.DairoUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * GitHub上发布的版本
 * @param versionName 版本号,就是Release的标签名
 * @param notes 更新说明
 * @param apkUrl APK的下载地址
 * @param apkSize APK的文件大小
 * @param sha256 APK的SHA-256,GitHub没有提供时为null
 */
class AppRelease(
    val versionName: String,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long,
    val sha256: String?,
)

/**
 * 从GitHub的Release检查更新、下载和安装新版本
 * 只在关于页面手动检查,Android版不上架应用商店
 */
object AppUpdateUtil {

    /** 最新正式版的接口,不包含草稿和预发布版本 */
    private val LATEST_URL = "https://api.github.com/repos/${Const.APP_GITHUB_REPO}/releases/latest"

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 检查新版本
     * @return 有新版本时返回新版本,已经是最新版本时返回null
     */
    suspend fun check(): AppRelease? = withContext(Dispatchers.IO) {
        val connection = URL(LATEST_URL).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            val code = connection.responseCode
            if (code == 404) {//还没有发布过任何版本
                return@withContext null
            }
            if (code != 200) {
                error("GitHub返回错误:$code")
            }
            val text = connection.inputStream.use { it.readBytes().decodeToString() }
            val release = json.parseToJsonElement(text).jsonObject
            val versionName = release.string("tag_name") ?: error("没有获取到版本号")
            if (compareVersion(versionName, Const.versionName) <= 0) {
                return@withContext null
            }

            //Jenkins打包上传的文件名是DairoMUSIC-{版本号}.apk
            val asset = release["assets"]?.jsonArray?.map { it.jsonObject }?.firstOrNull { it.string("name")?.endsWith(".apk") == true }
                ?: error("新版本${versionName}里没有找到APK文件")
            AppRelease(
                versionName = versionName,
                notes = release.string("body").orEmpty().trim(),
                apkUrl = asset.string("browser_download_url") ?: error("没有获取到下载地址"),
                apkSize = asset["size"]?.jsonPrimitive?.longOrNull ?: 0,
                sha256 = asset.string("digest")?.takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:"),
            )
        } finally {
            connection.disconnect()
        }
    }

    /**
     * 下载APK到缓存目录,下载完成后检查大小和SHA-256
     * 页面关闭或取消时协程被取消,下载随之停止
     * @param onProgress 下载进度,0到1
     */
    suspend fun download(release: AppRelease, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val folder = File(DairoUI.context.cacheDir, "update")

        //只保留这一次下载的文件,之前下载的旧版本没用了
        folder.deleteRecursively()
        folder.mkdirs()
        val file = File(folder, "DairoMUSIC-${release.versionName}.apk")
        val connection = URL(release.apkUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            if (connection.responseCode != 200) {
                error("下载失败:${connection.responseCode}")
            }
            val total = if (release.apkSize > 0) release.apkSize else connection.contentLengthLong
            val digest = MessageDigest.getInstance("SHA-256")
            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0L
                    while (true) {
                        ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) {
                            break
                        }
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        downloaded += count
                        if (total > 0) {
                            onProgress((downloaded.toFloat() / total).coerceAtMost(1f))
                        }
                    }
                }
            }
            if (release.apkSize > 0 && file.length() != release.apkSize) {
                error("下载的文件不完整")
            }
            val sha256 = digest.digest().joinToString("") { "%02x".format(it) }
            if (release.sha256 != null && !release.sha256.equals(sha256, ignoreCase = true)) {
                error("下载的文件校验失败")
            }
            file
        } catch (e: Throwable) {
            file.delete()
            throw e
        } finally {
            connection.disconnect()
        }
    }

    /** 是否已经允许本App安装应用,Android 8开始需要用户在系统设置里允许 */
    fun canInstall(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()
    }

    /** 打开系统设置里本App的“安装未知应用”页面 */
    fun installPermissionIntent(context: Context): Intent {
        return Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())
    }

    /** 打开系统的安装界面,由用户确认安装 */
    fun install(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.file_provider", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 比较两个版本号,按数字逐段比较,比如2.0.10比2.0.9新
     * @return 大于0表示a比b新
     */
    fun compareVersion(a: String, b: String): Int {
        val partsA = a.trimStart('v', 'V').split(".").map { it.toIntOrNull() ?: 0 }
        val partsB = b.trimStart('v', 'V').split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(partsA.size, partsB.size)) {
            val diff = partsA.getOrElse(i) { 0 } - partsB.getOrElse(i) { 0 }
            if (diff != 0) {
                return diff
            }
        }
        return 0
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
}
