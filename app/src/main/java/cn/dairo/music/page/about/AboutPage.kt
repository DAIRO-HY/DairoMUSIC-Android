package cn.dairo.music.page.about

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import cn.dairo.music.R
import cn.dairo.music.components.AlertButton
import cn.dairo.music.components.AppAlert
import cn.dairo.music.components.ButtonRole
import cn.dairo.music.components.ServerGithubLink
import cn.dairo.music.constants.Const
import cn.dairo.music.extension.primaryText
import cn.dairo.music.extension.secondaryText
import cn.dairo.music.util.AppRelease
import cn.dairo.music.util.AppUpdateUtil
import cn.dairo.ui.root.Loading
import cn.dairo.ui.root.Toast
import cn.dairo.ui.setting.SettingButton
import cn.dairo.ui.setting.SettingCustomView
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingNavigationLink
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.setting.settingColors
import cn.dairo.ui.util.fileSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

/** 免责声明 */
private const val DISCLAIMER = "Dairo音乐是一个音乐播放客户端，本身不提供任何音乐、视频内容。App里播放和下载的内容全部来自你自己填写的服务器，由服务器的管理者负责。请只播放和下载你有权使用的内容，尊重音乐版权，不要用于商业用途。"

/**
 * 关于,版本信息、检查更新、免责声明、隐私政策
 */
@Composable
fun AboutPage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    /** 检查到的新版本 */
    var newRelease by remember { mutableStateOf<AppRelease?>(null) }

    /** 正在下载的新版本和下载进度 */
    var downloading by remember { mutableStateOf<AppRelease?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    /** 下载完成、等待用户允许安装应用的文件 */
    var pendingInstallFile by remember { mutableStateOf<File?>(null) }

    //从系统设置的“安装未知应用”页面回来之后继续安装
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val file = pendingInstallFile ?: return@rememberLauncherForActivityResult
        pendingInstallFile = null
        if (AppUpdateUtil.canInstall(context)) {
            AppUpdateUtil.install(context, file)
        } else {
            Toast.show("没有允许安装应用,无法更新")
        }
    }

    /** 下载完成之后安装,没有安装权限时先打开系统设置 */
    fun install(file: File) {
        if (AppUpdateUtil.canInstall(context)) {
            AppUpdateUtil.install(context, file)
            return
        }
        Toast.show("请允许安装应用,然后返回继续更新")
        pendingInstallFile = file
        permissionLauncher.launch(AppUpdateUtil.installPermissionIntent(context))
    }

    /** 下载新版本 */
    fun download(release: AppRelease) {
        downloading = release
        downloadProgress = 0f
        downloadJob = scope.launch {
            try {
                val file = AppUpdateUtil.download(release) { progress ->
                    scope.launch { downloadProgress = progress }
                }
                downloading = null
                install(file)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                downloading = null
                Toast.show("下载失败:${e.message}")
            }
        }
    }

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

                //Android版不上架应用商店,从GitHub的Release检查更新
                SettingGroup {
                    SettingButton("检查更新") {
                        scope.launch {
                            Loading.show()
                            try {
                                val release = AppUpdateUtil.check()
                                if (release == null) {
                                    Toast.show("已经是最新版本")
                                } else {
                                    newRelease = release
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Throwable) {
                                Toast.show("检查更新失败:${e.message}")
                            } finally {
                                Loading.hide()
                            }
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
    val release = newRelease
    AppAlert(
        isPresented = release != null,
        title = "发现新版本 ${release?.versionName ?: ""}",
        message = release?.notes?.ifEmpty { null },
        onDismiss = { newRelease = null },
        buttons = listOf(
            AlertButton("下载并安装") { release?.let(::download) },
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
    downloading?.let {
        DownloadProgressDialog(it, downloadProgress) {
            downloadJob?.cancel()
            downloading = null
        }
    }
}

/** 下载新版本的进度,只能点取消关闭 */
@Composable
private fun DownloadProgressDialog(release: AppRelease, progress: Float, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text("正在下载 ${release.versionName}", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = settingColors.accent)
                Text("${(progress * 100).toInt()}%  ${release.apkSize.fileSize}", style = MaterialTheme.typography.bodySmall, color = Color.secondaryText)
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("取消", color = settingColors.secondary)
            }
        },
    )
}
