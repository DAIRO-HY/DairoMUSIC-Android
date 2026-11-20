package cn.dairo.music.api

import cn.dairo.music.api.http.ApiHttp
import cn.dairo.music.api.model.SwiperModel
import cn.dairo.music.api.model.UsefulModel

object HomeApi {

    /** 首页轮播 */
    fun swiper(): ApiHttp<List<SwiperModel>> {
        return ApiHttp(ApiConst.HOME_SWIPER, "GET")
    }

    /** 推荐歌单分组 */
    fun useful(): ApiHttp<List<UsefulModel>> {
        return ApiHttp(ApiConst.HOME_USEFUL, "GET")
    }
}
