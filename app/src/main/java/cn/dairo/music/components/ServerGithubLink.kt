package cn.dairo.music.components

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import cn.dairo.music.constants.Const
import cn.dairo.music.extension.secondaryText
import cn.dairo.ui.setting.settingColors

/**
 * 服务器端的GitHub项目地址,点击用浏览器打开,固定显示在页面最底部
 * 键盘弹出时隐藏,不然会被键盘顶上去盖住页面上的按钮
 * @param title 地址上面的说明
 */
@Composable
fun ServerGithubLink(title: String, modifier: Modifier = Modifier) {
    if (WindowInsets.isImeVisible) {
        return
    }
    val context = LocalContext.current
    Column(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodySmall, color = Color.secondaryText, textAlign = TextAlign.Center)
        Row(
            Modifier.clickable {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Const.SERVER_GITHUB_URL.toUri())) }
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Link, contentDescription = null, tint = settingColors.accent, modifier = Modifier.size(16.dp))
            Text(
                Const.SERVER_GITHUB_URL.replace("https://", ""),
                style = MaterialTheme.typography.bodySmall,
                color = settingColors.accent,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
