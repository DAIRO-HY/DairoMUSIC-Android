package cn.dairo.music.api

import cn.dairo.music.api.http.ApiHttp
import cn.dairo.music.api.http.EmptyModel
import cn.dairo.music.api.model.MusicLrcModel
import cn.dairo.music.api.model.MusicModel
import cn.dairo.music.api.model.VideoModel

object MusicApi {

    /** 从曲库搜索,最多30条 */
    fun search(key: String): ApiHttp<List<MusicModel>> {
        return ApiHttp(ApiConst.MUSIC_SEARCH, "GET", mapOf("key" to key))
    }

    /** 从采集平台搜索,结果的id为0 */
    fun searchCollect(key: String): ApiHttp<List<MusicModel>> {
        return ApiHttp(ApiConst.MUSIC_SEARCH_COLLECT, "GET", mapOf("key" to key))
    }

    /** 歌曲详情 */
    fun detail(id: Long): ApiHttp<MusicModel> {
        return ApiHttp(ApiConst.MUSIC_DETAIL, "GET", mapOf("id" to id))
    }

    /** 歌词列表 */
    fun lrc(id: Long): ApiHttp<List<MusicLrcModel>> {
        return ApiHttp(ApiConst.MUSIC_LRC, "GET", mapOf("id" to id))
    }

    /** 歌曲视频,没有采集或者可以重试的失败时会开始采集,采集和转码过程中每3秒查询一次 */
    fun video(id: Long): ApiHttp<VideoModel> {
        return ApiHttp(ApiConst.MUSIC_VIDEO, "GET", mapOf("id" to id))
    }

    /** 提交播放记录,ids为播放完成的歌曲ID,逗号分隔,播放了几次就出现几次,一次最多500个 */
    fun play(ids: String): ApiHttp<EmptyModel> {
        return ApiHttp(ApiConst.MUSIC_PLAY, "POST", mapOf("ids" to ids))
    }

    /** 采集平台的歌曲加入曲库,返回歌曲ID */
    fun collect(name: String, logo: String?, platform: String, extra: String): ApiHttp<Long> {
        val parameter = mutableMapOf<String, Any?>("name" to name, "platform" to platform, "extra" to extra)
        if (logo != null) {
            parameter["logo"] = logo
        }
        return ApiHttp(ApiConst.MUSIC_COLLECT, "POST", parameter)
    }

    /** 审核歌曲,只有信任会员可以审核,auditState 0:待审核 1:通过 2:拒绝 */
    fun audit(id: Long, auditState: Int): ApiHttp<EmptyModel> {
        return ApiHttp(ApiConst.MUSIC_AUDIT, "POST", mapOf("id" to id, "auditState" to auditState))
    }

    /** 纠错,只提交要修改的字段 */
    fun correct(id: Long, name: String, artist: String, album: String): ApiHttp<EmptyModel> {
        return ApiHttp(ApiConst.MUSIC_CORRECT, "POST", mapOf("id" to id, "name" to name, "artist" to artist, "album" to album))
    }

    /** 投诉 */
    fun complaint(id: Long, content: String): ApiHttp<EmptyModel> {
        return ApiHttp(ApiConst.MUSIC_COMPLAINT, "POST", mapOf("id" to id, "content" to content))
    }

    /** 投诉理由的选项 */
    fun complaintSamples(): ApiHttp<List<String>> {
        return ApiHttp(ApiConst.MUSIC_COMPLAINT_SAMPLES, "GET")
    }
}
