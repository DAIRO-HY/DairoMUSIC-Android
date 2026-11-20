package cn.dairo.music.constants

/**
 * 播放列表的来源,大于0时为歌单ID
 */
object FavoriteId {

    /** 没有来源,例如首页推荐的单曲 */
    const val NONE: Long = 0

    /** 来自搜索页面 */
    const val SEARCH: Long = -1

    /** 来自最近播放 */
    const val RECENT: Long = -2
}
