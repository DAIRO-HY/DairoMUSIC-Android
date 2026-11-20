package cn.dairo.music.constants

/**
 * 全局通知名,通过NotificationCenter在主线程发送
 */
object EventName {

    /** 我的歌单发生了变化 */
    const val FAVORITE_CHANGE = "FAVORITE_CHANGE"

    /** 我的歌单里的歌曲发生了变化 */
    const val MUSIC_CHANGE = "MUSIC_CHANGE"

    /** 最近播放发生了变化 */
    const val RECENT_CHANGE = "RECENT_CHANGE"

    /** 定时停止播放的时间到了 */
    const val SLEEP_TIMER_END = "SLEEP_TIMER_END"
}
