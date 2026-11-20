package cn.dairo.music.api

import cn.dairo.music.api.http.ApiHttp
import cn.dairo.music.api.http.EmptyModel
import cn.dairo.music.api.model.FavoriteModel
import cn.dairo.music.api.model.MusicModel

object FavoriteApi {

    /** 我的歌单 */
    fun list(): ApiHttp<List<FavoriteModel>> {
        return ApiHttp(ApiConst.FAVORITE_LIST, "GET")
    }

    /** 歌单详情 */
    fun detail(id: Long): ApiHttp<FavoriteModel> {
        return ApiHttp(ApiConst.FAVORITE_DETAIL, "GET", mapOf("id" to id))
    }

    /** 新建歌单,返回歌单ID */
    fun add(name: String): ApiHttp<Long> {
        return ApiHttp(ApiConst.FAVORITE_ADD, "POST", mapOf("name" to name))
    }

    /** 修改歌单 */
    fun edit(id: Long, name: String, description: String): ApiHttp<EmptyModel> {
        return ApiHttp(ApiConst.FAVORITE_EDIT, "POST", mapOf("id" to id, "name" to name, "description" to description))
    }

    /** 上传封面,返回封面地址;不限大小,服务器会居中裁切成正方形并缩小 */
    fun logo(id: Long, jpeg: ByteArray): ApiHttp<String> {
        return ApiHttp<String>(ApiConst.FAVORITE_LOGO, "POST", mapOf("id" to id)).file("file", "logo.jpg", "image/jpeg", jpeg)
    }

    /** 删除歌单 */
    fun delete(id: Long): ApiHttp<EmptyModel> {
        return ApiHttp(ApiConst.FAVORITE_DELETE, "DELETE", mapOf("id" to id))
    }

    /** 歌单排序,ids为排好序的歌单ID */
    fun sort(ids: List<Long>): ApiHttp<EmptyModel> {
        return ApiHttp(ApiConst.FAVORITE_SORT, "POST", mapOf("ids" to ids.joinToString(",")))
    }

    /** 歌单里的歌曲,最近添加的在前面 */
    fun musics(id: Long): ApiHttp<List<MusicModel>> {
        return ApiHttp(ApiConst.FAVORITE_MUSICS, "GET", mapOf("id" to id))
    }

    /** 我所有歌单里的歌曲 */
    fun allMusics(): ApiHttp<List<MusicModel>> {
        return ApiHttp(ApiConst.FAVORITE_ALL_MUSICS, "GET")
    }

    /** 添加歌曲到歌单 */
    fun addMusic(favoriteId: Long, musicId: Long): ApiHttp<EmptyModel> {
        return ApiHttp(ApiConst.FAVORITE_ADD_MUSIC, "POST", mapOf("favoriteId" to favoriteId, "musicId" to musicId))
    }

    /** 从歌单删除歌曲 */
    fun removeMusic(favoriteId: Long, musicId: Long): ApiHttp<EmptyModel> {
        return ApiHttp(ApiConst.FAVORITE_REMOVE_MUSIC, "DELETE", mapOf("favoriteId" to favoriteId, "musicId" to musicId))
    }
}
