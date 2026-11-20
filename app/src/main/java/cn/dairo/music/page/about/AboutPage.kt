package cn.dairo.music.page.about

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import cn.dairo.music.R
import cn.dairo.music.api.CommonApi
import cn.dairo.music.api.model.AppVersionModel
import cn.dairo.music.components.AlertButton
import cn.dairo.music.components.AppAlert
import cn.dairo.music.components.ButtonRole
import cn.dairo.music.components.ServerGithubLink
import cn.dairo.music.constants.Const
import cn.dairo.music.extension.primaryText
import cn.dairo.music.extension.secondaryText
import cn.dairo.ui.root.Toast
import cn.dairo.ui.setting.SettingButton
import cn.dairo.ui.setting.SettingCustomView
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingNavigationLink
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingStack

/** 免责声明 */
private const val DISCLAIMER = "Dairo音乐是一个音乐播放客户端，本身不提供任何音乐、视频内容。App里播放和下载的内容全部来自你自己填写的服务器，由服务器的管理者负责。请只播放和下载你有权使用的内容，尊重音乐版权，不要用于商业用途。"

/**
 * 关于,版本信息、检查更新、免责声明、隐私政策
 */
@Composable
fun AboutPage() {
    val context = LocalContext.current

    /** 检查到的新版本 */
    var newVersion by remember { mutableStateOf<AppVersionModel?>(null) }

    Box(Modifier.fillMaxSize()) {
        SettingStack(embedInNavigationStack = false) {
            SettingPage(title = "关于") {
                SettingCustomView {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Image(painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(14.dp)))
                        Text("Dairo音乐 ${Const.versionName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.primaryText)
                    }
                }

                //Android版不经过应用商店,可以从服务器检查更新
                SettingGroup {
                    SettingButton("检查更新") {
                        CommonApi.checkVersion().send { version ->
                            if (version == null) {
                                Toast.show("已经是最新版本")
                                return@send
                            }
                            newVersion = version
                        }
                    }
                }
                SettingGroup {
                    SettingNavigationLink("隐私政策") {
                        PrivacyPage()
                    }
                }
                SettingGroup(header = "免责声明") {
                    SettingCustomView {
                        Text(
                            DISCLAIMER,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.secondaryText,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        )
                    }
                }
            }
        }

        //固定显示在屏幕最底部
        ServerGithubLink("服务器端项目地址", Modifier.align(Alignment.BottomCenter))
    }
    val version = newVersion
    AppAlert(
        isPresented = version != null,
        title = "发现新版本 ${version?.versionName ?: ""}",
        message = version?.instruction,
        onDismiss = { newVersion = null },
        buttons = listOf(
            AlertButton("去更新") {
                version?.let { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, it.url.toUri())) } }
            },
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
}
