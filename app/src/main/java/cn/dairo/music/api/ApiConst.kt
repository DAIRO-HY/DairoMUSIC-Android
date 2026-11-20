package cn.dairo.music.api

/**
 * Api地址
 */
object ApiConst {

    const val COMMON_CONFIG = "/api/common/config"
    const val APP_VERSION_CHECK = "/api/app_version/check"
    const val EMAIL_SEND_CODE = "/api/email/send_code"

    const val USER_TEMP_LOGIN = "/api/user/temp_login"
    const val USER_LOGIN = "/api/user/login"
    const val USER_REGISTER = "/api/user/register"
    const val USER_RESET_PWD = "/api/user/reset_pwd"
    const val USER_BIND_EMAIL = "/api/user/bind_email"
    const val USER_INFO = "/api/user/info"
    const val USER_EDIT = "/api/user/edit"
    const val USER_AVATAR = "/api/user/avatar"
    const val USER_MODIFY_PWD = "/api/user/modify_pwd"
    const val USER_UNREGISTER = "/api/user/unregister"
    const val USER_LOGOUT = "/api/user/logout"

    const val VIP_REDEEM = "/api/vip/redeem"

    const val MUSIC_SEARCH = "/api/music/search"
    const val MUSIC_SEARCH_COLLECT = "/api/music/search_collect"
    const val MUSIC_DETAIL = "/api/music/detail"
    const val MUSIC_LRC = "/api/music/lrc"
    const val MUSIC_VIDEO = "/api/music/video"
    const val MUSIC_PLAY = "/api/music/play"
    const val MUSIC_COLLECT = "/api/music/collect"
    const val MUSIC_CORRECT = "/api/music/correct"
    const val MUSIC_AUDIT = "/api/music/audit"
    const val MUSIC_COMPLAINT = "/api/music/complaint"
    const val MUSIC_COMPLAINT_SAMPLES = "/api/music/complaint_samples"

    const val FAVORITE_LIST = "/api/favorite/list"
    const val FAVORITE_DETAIL = "/api/favorite/detail"
    const val FAVORITE_ADD = "/api/favorite/add"
    const val FAVORITE_EDIT = "/api/favorite/edit"
    const val FAVORITE_LOGO = "/api/favorite/logo"
    const val FAVORITE_DELETE = "/api/favorite/delete"
    const val FAVORITE_SORT = "/api/favorite/sort"
    const val FAVORITE_MUSICS = "/api/favorite/musics"
    const val FAVORITE_ALL_MUSICS = "/api/favorite/all_musics"
    const val FAVORITE_ADD_MUSIC = "/api/favorite/add_music"
    const val FAVORITE_REMOVE_MUSIC = "/api/favorite/remove_music"

    const val HOME_SWIPER = "/api/home/swiper"
    const val HOME_USEFUL = "/api/home/useful"

    const val FEEDBACK_ADD = "/api/feedback/add"

    /** 歌曲分享页面 */
    const val SHARE_MUSIC = "/share/music.html"
}
