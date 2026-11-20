package cn.dairo.music.components

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow

/**
 * 输入框的状态,和外部的text双向同步,对外保持text/onTextChange的写法(和DairoUI的SettingTextField一样)
 * 外部修改text时写回输入框(例如格式化卡号),用户输入时回调onTextChange
 */
@Composable
fun rememberSyncedTextFieldState(text: String, onTextChange: (String) -> Unit): TextFieldState {
    val state = rememberTextFieldState(text)
    val currentOnTextChange by rememberUpdatedState(onTextChange)

    //最后一次回调给外部的内容;外部text等于它时说明只是输入的回传,不能写回输入框
    //否则快速输入时,回传得慢的旧内容会覆盖掉刚输入的字
    val lastSent = remember { mutableStateOf(text) }

    //外部text变化(例如格式化卡号、清空) → 写回输入框
    LaunchedEffect(text) {
        if (text != lastSent.value && state.text.toString() != text) {
            state.setTextAndPlaceCursorAtEnd(text)
        }
        lastSent.value = text
    }

    //用户输入 → 回调给外部
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect {
            if (it != lastSent.value) {
                lastSent.value = it
                currentOnTextChange(it)
            }
        }
    }
    return state
}
