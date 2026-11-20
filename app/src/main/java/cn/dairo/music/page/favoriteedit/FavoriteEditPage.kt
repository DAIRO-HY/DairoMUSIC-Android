package cn.dairo.music.page.favoriteedit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.dairo.music.components.MusicImage
import cn.dairo.music.extension.secondaryText
import cn.dairo.music.util.rememberStateObject
import cn.dairo.ui.image.rememberSystemImagePicker
import cn.dairo.ui.setting.SettingButtonSingle
import cn.dairo.ui.setting.SettingCustomView
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.setting.SettingTextField

@Composable
fun FavoriteEditPage(favoriteId: Long) {
    val vm = rememberStateObject { FavoriteEditViewModel(favoriteId) }

    /** 系统图片选择器,不需要相册权限 */
    val pickImage = rememberSystemImagePicker { bitmap ->
        if (bitmap == null) {
            return@rememberSystemImagePicker
        }
        vm.onLogoPick(bitmap)
    }
    SettingStack(embedInNavigationStack = false) {
        SettingPage(title = "编辑歌单") {
            SettingCustomView {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = pickImage).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MusicImage(vm.logo, Modifier.size(100.dp).clip(RoundedCornerShape(10.dp)), placeholder = Icons.AutoMirrored.Filled.QueueMusic)
                        Text("更换封面", style = MaterialTheme.typography.bodySmall, color = Color.secondaryText)
                    }
                }
            }
            SettingGroup {
                SettingTextField(title = "歌单名", text = vm.name, onTextChange = { vm.name = it }, placeholder = "请输入歌单名")
                SettingTextField(title = "描述", text = vm.description, onTextChange = { vm.description = it }, placeholder = "可以不填")
            }
            SettingGroup {
                SettingButtonSingle("保存", onClick = vm::onSaveClick)
            }
        }
    }
}
