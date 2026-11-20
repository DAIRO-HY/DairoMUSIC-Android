package cn.dairo.music.api.http

import cn.dairo.music.store.SettingStore
import cn.dairo.ui.root.Loading
import cn.dairo.ui.root.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.UUID

/**
 * api请求基类
 */
interface ApiHttpBase {

    /** 取消请求 */
    fun cancel()
}

/**
 * 创建API请求,返回值按类型转换:String、Boolean、EmptyModel直接转换,其他类型按JSON解析
 */
inline fun <reified T> ApiHttp(url: String, method: String, parameter: Map<String, Any?> = emptyMap()): ApiHttp<T> {
    return ApiHttp(ApiHttp.converter<T>(serializer<T>()), url, method, parameter)
}

/**
 * API请求
 */
class ApiHttp<T>(

    /** 把返回内容转换成指定的类型,转换失败时返回null */
    private val converter: (String) -> Result<T>,
    private val url: String,

    /** 请求方式 */
    private val method: String,

    /** API参数 */
    private val parameter: Map<String, Any?>,
) : ApiHttpBase {

    companion object {

        /** 请求超时时间(毫秒) */
        private const val TIMEOUT = 15_000

        /** 所有请求共用的协程,回调在主线程 */
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        @PublishedApi
        internal val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }

        /**
         * 生成返回值的转换函数
         */
        @PublishedApi
        internal fun <T> converter(serializer: KSerializer<T>): (String) -> Result<T> {
            val descriptorName = serializer.descriptor.serialName
            return { text ->
                runCatching {
                    @Suppress("UNCHECKED_CAST")
                    when (descriptorName) {
                        "kotlin.String" -> text as T
                        "kotlin.Boolean" -> (text.lowercase() == "true") as T
                        EmptyModel.serializer().descriptor.serialName -> EmptyModel as T
                        else -> json.decodeFromString(serializer, text.ifEmpty { "null" })
                    }
                }
            }
        }
    }

    /** 请求任务 */
    private var job: Job? = null

    /** 当前的连接 */
    @Volatile
    private var connection: HttpURLConnection? = null

    /** 是否已经取消 */
    @Volatile
    private var isCancelled = false

    /** 是否显示加载中的遮罩层 */
    private var isShowWaiting = true

    /** 是否携带登录票据 */
    private var isAuth = true

    /** 指定服务器地址,为null时使用当前登录的服务器 */
    private var domain: String? = null

    /** 要上传的文件 */
    private var uploadFile: ApiUploadFile? = null

    /** 失败时消息回调 */
    private var failFunc: ((failModel: ApiFailModel) -> Unit)? = null

    /** 出错时消息回调 */
    private var errorFunc: ((msg: String) -> Unit)? = null

    /** 最终回调 */
    private var finishFunc: (() -> Unit)? = null

    /**
     * 请求失败时的回调
     */
    fun fail(block: (failModel: ApiFailModel) -> Unit): ApiHttp<T> {
        this.failFunc = block
        return this
    }

    /**
     * 请求异常时的回调
     */
    fun error(block: (msg: String) -> Unit): ApiHttp<T> {
        this.errorFunc = block
        return this
    }

    /**
     * 最终回调
     */
    fun finish(block: () -> Unit): ApiHttp<T> {
        this.finishFunc = block
        return this
    }

    /**
     * 不显示等待框
     */
    fun hide(): ApiHttp<T> {
        this.isShowWaiting = false
        return this
    }

    /**
     * 不携带登录票据,用于登录等不需要登录的接口
     */
    fun noAuth(): ApiHttp<T> {
        this.isAuth = false
        return this
    }

    /**
     * 请求指定的服务器,用于登录、注册等还没有切换服务器的情况,同时不携带登录票据
     */
    fun domain(domain: String): ApiHttp<T> {
        this.domain = domain
        this.isAuth = false
        return this
    }

    /**
     * 以multipart方式上传文件
     */
    fun file(name: String, fileName: String, mimeType: String, data: ByteArray): ApiHttp<T> {
        this.uploadFile = ApiUploadFile(name, fileName, mimeType, data)
        return this
    }

    /**
     * 调用失败函数
     */
    private fun callFail(failModel: ApiFailModel) {
        val failFunc = this.failFunc
        if (failFunc != null) {
            failFunc(failModel)
        } else if (this.isShowWaiting) {//有遮照层
            Toast.show(failModel.msg ?: "处理失败")
        }
    }

    /**
     * 调用出错函数
     */
    private fun callError(msg: String) {
        val errorFunc = this.errorFunc
        if (errorFunc != null) {
            errorFunc(msg)
        } else if (this.isShowWaiting) {//有遮照层
            Toast.show(msg)
        }
    }

    /**
     * 发起请求
     * @param successFunc 成功的回调,在主线程
     */
    fun send(successFunc: (data: T) -> Unit): ApiHttp<T> {
        val parameterStr = this.parameterStr
        var urlStr = (this.domain ?: SettingStore.domainNotNull) + this.url
        if (this.method == "GET" && parameterStr.isNotEmpty()) {
            urlStr += "?$parameterStr"
        }
        val token = if (this.isAuth) SettingStore.token else null

        //如果需要显示等待框
        if (this.isShowWaiting) {
            Loading.show()
        }
        this.job = scope.launch {
            val response = withContext(Dispatchers.IO) {
                request(urlStr, parameterStr, token)
            }
            if (isShowWaiting) {
                Loading.hide()
            }
            if (isCancelled) {//请求被取消
                return@launch
            }
            try {
                when (response) {
                    is ApiResponse.Error -> callError(response.msg)
                    is ApiResponse.Success -> onResponse(response.code, response.body, successFunc)
                }
            } finally {
                finishFunc?.invoke()
            }
        }
        return this
    }

    /**
     * 发起请求,在后台线程
     */
    private fun request(urlStr: String, parameterStr: String, token: String?): ApiResponse {
        val connection = try {
            URL(urlStr).openConnection() as HttpURLConnection
        } catch (e: Exception) {
            return ApiResponse.Error("服务器地址不正确:$urlStr")
        }
        this.connection = connection
        if (this.isCancelled) {
            return ApiResponse.Error("已取消")
        }
        return try {
            connection.requestMethod = this.method//请求方式
            connection.connectTimeout = TIMEOUT
            connection.readTimeout = TIMEOUT
            connection.useCaches = false
            if (token != null) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            val uploadFile = this.uploadFile
            val body: ByteArray? = if (uploadFile != null) {
                val boundary = UUID.randomUUID().toString()
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                uploadFile.multipartBody(boundary, this.parameter)
            } else if (this.method != "GET" && parameterStr.isNotEmpty()) {
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                parameterStr.toByteArray()
            } else {
                null
            }
            if (body != null) {
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            }
            val code = connection.responseCode
            val stream = if (code >= 400) connection.errorStream else connection.inputStream

            //数据量可能很大,读取不能放在主线程
            val bytes = stream?.use { input ->
                val output = ByteArrayOutputStream()
                input.copyTo(output)
                output.toByteArray()
            } ?: ByteArray(0)
            ApiResponse.Success(code, String(bytes))
        } catch (e: SocketTimeoutException) {
            ApiResponse.Error("请求超时")
        } catch (e: UnknownHostException) {
            ApiResponse.Error("无法连接到服务器")
        } catch (e: IOException) {
            ApiResponse.Error(e.message ?: "网络连接失败")
        } catch (e: Exception) {
            ApiResponse.Error(e.message ?: "请求失败")
        } finally {
            connection.disconnect()
        }
    }

    /**
     * 将参数拼接成字符串,数组参数使用重复的key
     */
    private val parameterStr: String
        get() {
            val items = ArrayList<String>()
            for ((key, value) in this.parameter) {
                val values: List<Any?> = if (value is Collection<*>) value.toList() else listOf(value)
                for (item in values) {
                    if (item == null) {
                        continue
                    }
                    items.add(encode(key) + "=" + encode(item.toString()))
                }
            }
            return items.joinToString("&")
        }

    /**
     * 对参数进行URL编码
     */
    private fun encode(value: String): String {
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    }

    /**
     * 处理服务器返回结果
     * 200:成功 401:登录失效 其他:失败,返回内容是错误信息
     */
    private fun onResponse(code: Int, body: String, successFunc: (data: T) -> Unit) {
        if (code == 200) {
            val result = this.converter(body)
            if (result.isFailure) {
                this.callError("服务器返回数据格式不正确")
                return
            }
            successFunc(result.getOrThrow())
            return
        }
        if (code == 401 && this.isAuth && SettingStore.isLogin) {
            Toast.show("登录已失效,请重新登录")
            SettingStore.toLoginPage()
            return
        }
        this.callFail(this.makeFailModel(code, body))
    }

    /**
     * 生成失败信息,表单验证失败时优先显示字段的错误信息
     * 需要登录的接口没有权限时返回403或404,这两种情况的返回内容不适合直接显示给用户
     */
    private fun makeFailModel(code: Int, body: String): ApiFailModel {
        if (this.isAuth && code == 403) {
            return ApiFailModel(code, "没有操作权限")
        }
        if (this.isAuth && code == 404) {
            return ApiFailModel(code, "内容不存在或没有访问权限")
        }
        val formFail = runCatching { json.decodeFromString(ApiFormFailModel.serializer(), body) }.getOrNull()
        if (formFail != null) {
            val msg = formFail.data?.values?.firstOrNull { it.isNotEmpty() }?.first() ?: formFail.msg
            return ApiFailModel(code, msg)
        }
        return ApiFailModel(code, body.ifEmpty { null })
    }

    /**
     * 取消请求
     */
    override fun cancel() {
        this.isCancelled = true
        val connection = this.connection
        if (connection != null) {

            //断开连接不能在主线程
            Thread { connection.disconnect() }.start()
        }
    }
}

/**
 * 请求的结果
 */
internal sealed class ApiResponse {

    /** 服务器有返回 */
    class Success(val code: Int, val body: String) : ApiResponse()

    /** 网络错误 */
    class Error(val msg: String) : ApiResponse()
}

/**
 * 要上传的文件
 */
internal class ApiUploadFile(
    val name: String,
    val fileName: String,
    val mimeType: String,
    val data: ByteArray,
) {

    /**
     * 生成multipart请求内容,普通参数放在文件前面
     */
    fun multipartBody(boundary: String, parameter: Map<String, Any?>): ByteArray {
        val body = ByteArrayOutputStream()
        for ((key, value) in parameter) {
            if (value == null) {
                continue
            }
            body.write("--$boundary\r\n".toByteArray())
            body.write("Content-Disposition: form-data; name=\"$key\"\r\n\r\n".toByteArray())
            body.write("$value\r\n".toByteArray())
        }
        body.write("--$boundary\r\n".toByteArray())
        body.write("Content-Disposition: form-data; name=\"${this.name}\"; filename=\"${this.fileName}\"\r\n".toByteArray())
        body.write("Content-Type: ${this.mimeType}\r\n\r\n".toByteArray())
        body.write(this.data)
        body.write("\r\n--$boundary--\r\n".toByteArray())
        return body.toByteArray()
    }
}

/**
 * 不需要返回结果的Model
 */
@Serializable
object EmptyModel

/**
 * API返回失败Model
 */
data class ApiFailModel(

    /** HTTP状态码 */
    val code: Int,

    /** 错误信息 */
    val msg: String?,
)

/**
 * 表单验证失败Model
 */
@Serializable
private data class ApiFormFailModel(

    /** 错误信息 */
    val msg: String? = null,

    /** 字段名 => 错误信息 */
    val data: Map<String, List<String>>? = null,
)
