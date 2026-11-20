package cn.dairo.music.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import cn.dairo.music.extension.listBackground
import cn.dairo.ui.setting.LocalSettingNavigator
import cn.dairo.ui.setting.settingColors

/**
 * 带标题栏的页面,对应iOS放在NavigationView里的普通页面(列表等不是设置页样式的页面)
 * 标题栏和DairoUI的SettingPage一样,不是导航栈的根页面时显示返回按钮
 * @param navigationIcon 替换标题栏左边的返回按钮
 * @param actions 标题栏右边的按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationPage(
    title: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.listBackground,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val navigator = LocalSettingNavigator.current
    val canGoBack = rememberCanGoBack()
    val colors = settingColors
    Column(
        modifier
            .fillMaxSize()
            .background(backgroundColor),
    ) {
        CenterAlignedTopAppBar(
            title = { Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                if (navigationIcon != null) {
                    navigationIcon()
                } else if (canGoBack && navigator != null) {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = backgroundColor,
                titleContentColor = colors.primary,
                navigationIconContentColor = colors.accent,
                actionIconContentColor = colors.accent,
            ),
        )
        Column(Modifier.fillMaxWidth().weight(1f), content = content)
    }
}
