package cn.dairo.music.page.mine

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.dairo.music.page.playing.wave.PlayingWaveStyle
import cn.dairo.music.store.SettingStore
import cn.dairo.ui.setting.SettingCustomView
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.setting.SettingToggle
import cn.dairo.ui.setting.settingColors
import cn.dairo.ui.setting.settingRowClickable

/**
 * 播放页波形的设置:选择波形样式,低音冲击波和波形颜色跟随封面的开关
 */
@Composable
fun PlayingWaveSettingPage() {
    SettingStack(embedInNavigationStack = false) {
        SettingPage(title = "播放页波形") {
            SettingGroup {
                for (style in PlayingWaveStyle.entries) {
                    SettingCustomView {
                        StyleRow(style)
                    }
                }
            }

            //关闭波形时这两项没有作用,不显示
            if (SettingStore.playingWaveStyle != PlayingWaveStyle.OFF.value) {
                SettingGroup(footer = "低音冲击波:鼓点时从封面边缘往外扩散一圈光环;跟随封面:波形的颜色从封面里取,每首歌都不一样") {
                    SettingToggle(title = "低音冲击波", isOn = SettingStore.playingWaveShockwave, onIsOnChange = { SettingStore.playingWaveShockwave = it })
                    SettingToggle(title = "波形颜色跟随封面", isOn = SettingStore.playingWaveCoverColor, onIsOnChange = { SettingStore.playingWaveCoverColor = it })
                }
            }
        }
    }
}

/** 一个样式选项,选中的打勾 */
@Composable
private fun StyleRow(style: PlayingWaveStyle) {
    val colors = settingColors
    Row(
        Modifier
            .fillMaxWidth()
            .settingRowClickable { SettingStore.playingWaveStyle = style.value }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(style.label, color = colors.primary, modifier = Modifier.weight(1f))
        if (SettingStore.playingWaveStyle == style.value) {
            Icon(Icons.Default.Check, contentDescription = "已选择", tint = colors.accent)
        }
    }
}
