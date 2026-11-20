package cn.dairo.music.page.musiccomplaint

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.dairo.music.api.MusicApi
import cn.dairo.music.extension.theme
import cn.dairo.ui.root.Toast
import cn.dairo.ui.setting.LocalSettingNavigator
import cn.dairo.ui.setting.SettingButtonSingle
import cn.dairo.ui.setting.SettingCustomView
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.setting.settingColors
import cn.dairo.ui.setting.settingRowClickable

/**
 * 歌曲投诉,从服务器提供的理由里选择
 */
@Composable
fun MusicComplaintPage(musicId: Long) {

    /** 投诉理由的选项 */
    var samples by remember { mutableStateOf(emptyList<String>()) }

    /** 选中的理由 */
    var selected by remember { mutableStateOf<String?>(null) }

    val navigator = LocalSettingNavigator.current
    LaunchedEffect(Unit) {
        if (samples.isEmpty()) {
            MusicApi.complaintSamples().send {
                samples = it
            }
        }
    }
    SettingStack(embedInNavigationStack = false) {
        SettingPage(title = "投诉") {
            SettingGroup(header = "请选择投诉理由") {
                samples.forEach { sample ->
                    SettingCustomView {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .settingRowClickable { selected = sample }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(sample, color = settingColors.primary, modifier = Modifier.weight(1f))
                            if (selected == sample) {
                                Icon(Icons.Default.Check, contentDescription = "已选择", tint = Color.theme)
                            }
                        }
                    }
                }
            }
            SettingGroup {
                SettingButtonSingle("提交", enabled = selected != null) {
                    val content = selected ?: return@SettingButtonSingle
                    MusicApi.complaint(musicId, content).send {
                        Toast.show("投诉已提交,感谢你的反馈")
                        navigator?.pop()
                    }
                }
            }
        }
    }
}
