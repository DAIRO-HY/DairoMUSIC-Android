package cn.dairo.music.extension

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

/**
 * 转换成jpg原图,用于上传头像和封面;服务器会裁切、缩小,这里不缩小
 * 选择图片时已经按EXIF的方向转正了
 */
fun Bitmap.toUploadJpeg(): ByteArray? {
    val output = ByteArrayOutputStream()
    if (!this.compress(Bitmap.CompressFormat.JPEG, 90, output)) {
        return null
    }
    return output.toByteArray()
}
