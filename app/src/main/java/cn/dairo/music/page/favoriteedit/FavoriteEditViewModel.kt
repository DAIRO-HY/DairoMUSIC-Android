package cn.dairo.music.page.favoriteedit

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.music.api.FavoriteApi
import cn.dairo.music.extension.toUploadJpeg
import cn.dairo.music.store.FavoriteStore
import cn.dairo.music.util.ObservableObject
import cn.dairo.ui.root.Toast

class FavoriteEditViewModel(val favoriteId: Long) : ObservableObject() {

    var name by mutableStateOf("")

    var description by mutableStateOf("")

    var logo by mutableStateOf<String?>(null)

    init {
        FavoriteStore.byId(favoriteId)?.let {
            this.name = it.name
            this.description = it.description ?: ""
            this.logo = it.logo
        }
    }

    /** 保存点击事件 */
    fun onSaveClick() {
        val name = this.name.trim()
        if (name.isEmpty()) {
            Toast.show("请输入歌单名")
            return
        }
        FavoriteApi.edit(this.favoriteId, name, this.description.trim()).send {
            Toast.show("保存成功")
            FavoriteStore.loadInBackground()
        }
    }

    /** 选择封面之后上传 */
    fun onLogoPick(image: Bitmap) {
        val jpeg = image.toUploadJpeg()
        if (jpeg == null) {
            Toast.show("读取图片失败")
            return
        }
        FavoriteApi.logo(this.favoriteId, jpeg).send {
            this.logo = it
            FavoriteStore.loadInBackground()
        }
    }
}
