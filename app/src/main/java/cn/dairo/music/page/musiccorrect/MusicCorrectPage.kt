package cn.dairo.music.page.musiccorrect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cn.dairo.music.api.MusicApi
import cn.dairo.music.api.model.MusicModel
import cn.dairo.music.store.MusicStore
import cn.dairo.music.store.SettingStore
import cn.dairo.ui.root.Toast
import cn.dairo.ui.setting.LocalSettingNavigator
import cn.dairo.ui.setting.SettingButtonSingle
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.setting.SettingTextField

/**
 * 歌曲纠错,信赖用户的纠错直接生效
 */
@Composable
fun MusicCorrectPage(music: MusicModel) {
    val musicId = music.id
    var name by remember { mutableStateOf(music.name ?: "") }
    var artist by remember { mutableStateOf(music.artist ?: "") }
    var album by remember { mutableStateOf(music.album ?: "") }
    val navigator = LocalSettingNavigator.current
    SettingStack(embedInNavigationStack = false) {
        SettingPage(title = "纠错") {
            SettingGroup(footer = "审核通过之后生效") {
                SettingTextField(title = "歌名", text = name, onTextChange = { name = it }, placeholder = "请输入歌名")
                SettingTextField(title = "歌手", text = artist, onTextChange = { artist = it }, placeholder = "请输入歌手")
                SettingTextField(title = "专辑", text = album, onTextChange = { album = it }, placeholder = "请输入专辑")
            }
            SettingGroup {
                SettingButtonSingle("提交") {
                    if (name.trim().isEmpty()) {
                        Toast.show("请输入歌名")
                        return@SettingButtonSingle
                    }
                    MusicApi.correct(musicId, name.trim(), artist.trim(), album.trim()).send {
                        Toast.show("提交成功")
                        if (SettingStore.user?.isFriend == true) {
                            MusicStore.loadInBackground()
                        }
                        navigator?.pop()
                    }
                }
            }
        }
    }
}
