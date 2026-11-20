package cn.dairo.music.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue

/**
 * 播放进度,变化很频繁,和播放器的其他状态分开,避免歌曲列表跟着刷新
 */
object PlayerProgress {

    /** 当前播放位置(秒) */
    var currentTime by mutableDoubleStateOf(0.0)

    /** 歌曲总时长(秒),还没加载出来时为0 */
    var duration by mutableDoubleStateOf(0.0)

    /** 是否正在拖动进度条,拖动时不更新播放位置 */
    var isDragging = false
}
