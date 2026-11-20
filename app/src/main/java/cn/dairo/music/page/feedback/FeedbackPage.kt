package cn.dairo.music.page.feedback

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import cn.dairo.music.api.CommonApi
import cn.dairo.music.components.rememberSyncedTextFieldState
import cn.dairo.ui.root.Toast
import cn.dairo.ui.setting.LocalSettingNavigator
import cn.dairo.ui.setting.SettingButtonSingle
import cn.dairo.ui.setting.SettingCustomView
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.setting.settingColors

/**
 * 意见反馈
 */
@Composable
fun FeedbackPage() {
    var content by remember { mutableStateOf("") }
    val navigator = LocalSettingNavigator.current
    SettingStack(embedInNavigationStack = false) {
        SettingPage(title = "意见反馈") {
            SettingGroup(header = "请描述你遇到的问题或建议") {
                SettingCustomView {
                    BasicTextField(
                        state = rememberSyncedTextFieldState(content) { content = it },
                        textStyle = LocalTextStyle.current.copy(color = settingColors.primary),
                        cursorBrush = SolidColor(settingColors.accent),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp).padding(16.dp),
                    )
                }
            }
            SettingGroup {
                SettingButtonSingle("提交") {
                    val text = content.trim()
                    if (text.isEmpty()) {
                        Toast.show("请输入反馈内容")
                        return@SettingButtonSingle
                    }
                    CommonApi.feedback(text).send { msg ->
                        Toast.show(msg.ifEmpty { "感谢你的反馈" })
                        navigator?.pop()
                    }
                }
            }
        }
    }
}
