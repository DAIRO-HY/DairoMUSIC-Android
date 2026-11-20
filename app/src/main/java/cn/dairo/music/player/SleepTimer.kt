package cn.dairo.music.player

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.music.constants.EventName
import cn.dairo.music.util.NotificationCenter
import cn.dairo.ui.root.Toast
import cn.dairo.ui.setting.SettingPickerData
import java.util.Calendar
import java.util.Date

/**
 * 定时停止播放
 * 到时间之后暂停音乐和视频;开启"播完整首歌再停止"时,等当前歌曲播放结束再停止
 * 后台播放音乐时有前台服务,计时器可以正常触发
 */
object SleepTimer {

    /** 自定义停止时间的选项值 */
    const val CUSTOM = -1

    /** 定时时长的选项(分钟),0表示不开启,-1表示自定义停止时间 */
    val OPTIONS = listOf(
        SettingPickerData("不开启", 0),
        SettingPickerData("15分钟", 15),
        SettingPickerData("30分钟", 30),
        SettingPickerData("45分钟", 45),
        SettingPickerData("60分钟", 60),
        SettingPickerData("90分钟", 90),
        SettingPickerData("自定义", CUSTOM),
    )

    /** 定时时长(分钟),0表示没有开启,-1表示自定义停止时间 */
    var minutes by mutableIntStateOf(0)

    /** 停止播放的时间,没有开启时为null */
    var endDate by mutableStateOf<Date?>(null)
        private set

    /** 是否等当前歌曲播放结束再停止 */
    var isWaitSongEnd by mutableStateOf(false)

    /** 时间已经到了,正在等当前歌曲播放结束 */
    var isWaitingSongEnd by mutableStateOf(false)
        private set

    private val handler = Handler(Looper.getMainLooper())

    /** 计时器 */
    private val timer = Runnable { this.onTimeUp() }

    /**
     * 修改定时时长,重新开始计时
     * @param minutes 定时时长(分钟),0表示取消
     */
    fun start(minutes: Int) {
        if (minutes <= 0) {
            this.schedule(null, 0)
            return
        }
        this.schedule(Date(System.currentTimeMillis() + minutes * 60_000L), minutes)
    }

    /** 自定义停止时间,只用时和分,比现在早时为明天的这个时间 */
    fun start(hour: Int, minute: Int) {
        this.schedule(nextDate(hour, minute), CUSTOM)
    }

    /** 时和分对应的下一个时间,比现在早(或相同)时为明天 */
    fun nextDate(hour: Int, minute: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return calendar.time
    }

    /**
     * 开始计时
     * @param endDate 停止播放的时间,为null时取消定时
     */
    private fun schedule(endDate: Date?, minutes: Int) {
        this.handler.removeCallbacks(this.timer)
        this.minutes = minutes
        this.isWaitingSongEnd = false
        this.endDate = endDate
        endDate ?: return
        this.handler.postDelayed(this.timer, (endDate.time - System.currentTimeMillis()).coerceAtLeast(0))
    }

    /** 取消定时 */
    fun cancel() {
        this.start(0)
    }

    /** 歌曲播放结束时调用,正在等歌曲播放结束时返回true,这时停止播放并结束定时 */
    fun consumeSongEnd(): Boolean {
        if (!this.isWaitingSongEnd) {
            return false
        }
        this.finish()
        return true
    }

    /** 时间到了 */
    private fun onTimeUp() {

        //音乐正在播放并且要等歌曲播放结束
        if (this.isWaitSongEnd && PlayerManager.isPlaying) {
            this.isWaitingSongEnd = true
            return
        }
        PlayerManager.pause()
        this.finish()
    }

    /** 定时结束,通知视频暂停 */
    private fun finish() {
        this.minutes = 0
        this.endDate = null
        this.isWaitingSongEnd = false
        NotificationCenter.post(EventName.SLEEP_TIMER_END)
        Toast.show("定时时间到,已停止播放")
    }
}
