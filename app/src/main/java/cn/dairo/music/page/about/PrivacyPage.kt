package cn.dairo.music.page.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.dairo.music.components.NavigationPage
import cn.dairo.music.extension.primaryText
import cn.dairo.music.extension.secondaryText

/** 更新日期 */
private const val UPDATE_DATE = "2026年11月11日"

/** 每一节的标题和内容,和iOS版、项目根目录的PRIVACY.md一致,修改时一起改 */
private val SECTIONS = listOf(
    "说明" to "Dairo音乐（以下简称\"本App\"）是一个音乐播放客户端，本身不提供任何音乐、视频内容，也没有开发者运营的服务器。使用本App需要填写服务器地址，你的账号和数据只保存在你填写的服务器上，由该服务器的管理者负责保管。",
    "我们会收集哪些信息" to """以下信息只发送到你填写的服务器，只用于本App的功能：
· 账号信息：邮箱、密码（只用于登录验证）、昵称、头像。
· 设备标识：经过处理（MD5）的设备标识，用于免注册使用时识别设备，不能用来追踪你。
· 你创建的内容：歌单、歌单封面、喜欢的歌曲，以及你提交的意见反馈、歌曲纠错和投诉。""",
    "保存在手机上的信息" to "App的设置、登录状态、最近播放记录，以及缓存和下载的歌曲、视频、图片只保存在你的手机上，不会上传。卸载App时会一起删除，也可以在\"缓存管理\"里删除。",
    "权限" to """· 相册：选择头像和歌单封面（使用系统的图片选择器，不需要相册权限）；把下载的视频保存到相册（Android 9及以下需要存储权限）。
· 网络：连接你填写的服务器，包括部署在局域网里的服务器。
· 通知：后台播放时在通知栏显示播放控制。
以上权限只在你使用对应功能时才会用到，可以随时在系统设置里关闭。""",
    "我们不会做的事" to """· 不追踪你，不收集位置、通讯录等与功能无关的信息。
· 不包含广告、统计分析等第三方SDK。
· 不会把你的信息出售或提供给第三方。""",
    "注销账号" to "你可以在\"我的 → 个人资料 → 注销账号\"里注销账号，注销之后服务器会删除你的账号和歌单。",
    "联系我们" to "对本隐私政策有疑问时，可以通过App里的\"意见反馈\"联系我们。",
)

/**
 * 隐私政策
 */
@Composable
fun PrivacyPage() {
    NavigationPage(title = "隐私政策") {
        Box(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 700.dp).fillMaxWidth().padding(16.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text("更新日期：$UPDATE_DATE", style = MaterialTheme.typography.bodySmall, color = Color.secondaryText)
                SECTIONS.forEach { (title, content) ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.primaryText)
                        Text(content, style = MaterialTheme.typography.bodyMedium, color = Color.secondaryText)
                    }
                }
            }
        }
    }
}
