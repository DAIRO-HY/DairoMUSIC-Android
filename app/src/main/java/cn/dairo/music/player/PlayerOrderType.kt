package cn.dairo.music.player

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

/**
 * 播放顺序
 */
@Serializable
enum class PlayerOrderType {

    /** 列表循环 */
    LIST_LOOP,

    /** 随机播放 */
    RANDOM_LOOP,

    /** 单曲循环 */
    ONE_LOOP;

    /** 图标 */
    val icon: ImageVector
        get() = when (this) {
            LIST_LOOP -> Icons.Default.Repeat
            RANDOM_LOOP -> Icons.Default.Shuffle
            ONE_LOOP -> Icons.Default.RepeatOne
        }

    /** 说明 */
    val label: String
        get() = when (this) {
            LIST_LOOP -> "列表循环"
            RANDOM_LOOP -> "随机播放"
            ONE_LOOP -> "单曲循环"
        }

    /** 下一种播放顺序 */
    val next: PlayerOrderType
        get() = when (this) {
            LIST_LOOP -> RANDOM_LOOP
            RANDOM_LOOP -> ONE_LOOP
            ONE_LOOP -> LIST_LOOP
        }
}
