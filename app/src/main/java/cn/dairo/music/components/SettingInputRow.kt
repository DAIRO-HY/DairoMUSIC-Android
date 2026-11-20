package cn.dairo.music.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.dairo.ui.setting.LocalSettingEdgePadding
import cn.dairo.ui.setting.settingColors

/**
 * 输入框的一行,样式和DairoUI的SettingTextField一样,右边可以放按钮(例如发送验证码)
 * 用在SettingGroup里的SettingCustomView中
 */
@Composable
fun SettingInputRow(
    title: String,
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    verticalPadding: Int = 14,
    trailing: @Composable () -> Unit = {},
) {
    val colors = settingColors
    val textStyle = LocalTextStyle.current.copy(color = colors.primary, textAlign = TextAlign.End)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = LocalSettingEdgePadding.current),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, color = colors.primary, modifier = Modifier.padding(vertical = verticalPadding.dp))
        val state = rememberSyncedTextFieldState(text, onTextChange)
        BasicTextField(
            state = state,
            lineLimits = TextFieldLineLimits.SingleLine,
            textStyle = textStyle,
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization, autoCorrectEnabled = false),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = verticalPadding.dp),
            decorator = { inner ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (state.text.isEmpty()) {
                        Text(placeholder, style = textStyle.copy(color = colors.secondary), modifier = Modifier.fillMaxWidth())
                    }
                    inner()
                }
            },
        )
        trailing()
    }
}
