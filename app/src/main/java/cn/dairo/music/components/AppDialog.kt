package cn.dairo.music.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.dairo.music.extension.primaryText
import cn.dairo.music.extension.secondaryText
import cn.dairo.ui.setting.settingColors

/**
 * 弹框按钮的类型,对应iOS的ButtonRole
 */
enum class ButtonRole {

    /** 普通按钮 */
    DEFAULT,

    /** 危险操作,红色 */
    DESTRUCTIVE,

    /** 取消 */
    CANCEL,
}

/**
 * 弹框按钮,点击之后先关闭弹框再执行action
 */
class AlertButton(val text: String, val role: ButtonRole = ButtonRole.DEFAULT, val action: () -> Unit = {})

/** 按钮文字的颜色 */
@Composable
private fun buttonColor(role: ButtonRole): Color {
    return if (role == ButtonRole.DESTRUCTIVE) Color(0xFFFF3B30) else settingColors.accent
}

/**
 * 确认框,对应iOS的alert
 */
@Composable
fun AppAlert(
    isPresented: Boolean,
    title: String,
    onDismiss: () -> Unit,
    message: String? = null,
    buttons: List<AlertButton>,
) {
    if (!isPresented) {
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = message?.let { { Text(it) } },
        confirmButton = {
            Row {
                buttons.filter { it.role != ButtonRole.CANCEL }.forEach { button ->
                    TextButton(onClick = {
                        onDismiss()
                        button.action()
                    }) {
                        Text(button.text, color = buttonColor(button.role))
                    }
                }
            }
        },
        dismissButton = buttons.firstOrNull { it.role == ButtonRole.CANCEL }?.let { button ->
            {
                TextButton(onClick = {
                    onDismiss()
                    button.action()
                }) {
                    Text(button.text, color = settingColors.secondary)
                }
            }
        },
    )
}

/**
 * 带输入框的确认框,对应iOS里放了TextField的alert
 */
@Composable
fun TextFieldAlert(
    isPresented: Boolean,
    title: String,
    text: String,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    message: String? = null,
    placeholder: String = "",
    isPassword: Boolean = false,
    buttons: List<AlertButton>,
) {
    if (!isPresented) {
        return
    }
    val focusRequester = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                if (message != null) {
                    Text(message, modifier = Modifier.padding(bottom = 12.dp))
                }
                val state = rememberSyncedTextFieldState(text, onTextChange)
                if (isPassword) {
                    OutlinedSecureTextField(
                        state = state,
                        placeholder = { Text(placeholder) },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    )
                } else {
                    OutlinedTextField(
                        state = state,
                        placeholder = { Text(placeholder) },
                        lineLimits = TextFieldLineLimits.SingleLine,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    )
                }
            }
        },
        confirmButton = {
            Row {
                buttons.filter { it.role != ButtonRole.CANCEL }.forEach { button ->
                    TextButton(onClick = {
                        onDismiss()
                        button.action()
                    }) {
                        Text(button.text, color = buttonColor(button.role))
                    }
                }
            }
        },
        dismissButton = buttons.firstOrNull { it.role == ButtonRole.CANCEL }?.let { button ->
            {
                TextButton(onClick = {
                    onDismiss()
                    button.action()
                }) {
                    Text(button.text, color = settingColors.secondary)
                }
            }
        },
    )
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }
}

/**
 * 从底部弹出的选项列表,对应iOS的confirmationDialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSheet(
    isPresented: Boolean,
    title: String?,
    onDismiss: () -> Unit,
    message: String? = null,
    buttons: List<AlertButton>,
) {
    if (!isPresented) {
        return
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberFullSheetState()) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 8.dp)) {
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.secondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (message != null) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.secondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            buttons.forEach { button ->
                HorizontalDivider()
                Text(
                    button.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (button.role == ButtonRole.CANCEL) Color.primaryText else buttonColor(button.role),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            button.action()
                        }
                        .padding(vertical = 16.dp),
                )
            }
        }
    }
}

/**
 * 从底部弹出的页面,对应iOS的sheet
 * @param heightFraction 页面高度占屏幕的比例
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(onDismiss: () -> Unit, heightFraction: Float = 0.92f, content: @Composable BoxScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberFullSheetState(),
        dragHandle = null,
    ) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(heightFraction), contentAlignment = Alignment.TopCenter, content = content)
    }
}

/**
 * 弹出时直接全部展开、不停在一半高度的弹出框状态
 * 第二个参数是允许的状态,只允许隐藏和全部展开
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberFullSheetState() = rememberBottomSheetState(SheetValue.Hidden, setOf(SheetValue.Hidden, SheetValue.Expanded))
