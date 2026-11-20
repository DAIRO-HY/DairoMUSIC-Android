package cn.dairo.music.page.mine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.dairo.music.components.rememberFullSheetState
import cn.dairo.music.extension.primaryText
import cn.dairo.music.extension.secondaryText
import cn.dairo.music.player.SleepTimer
import cn.dairo.ui.setting.settingColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 自定义定时停止播放的时间,选择时和分,比现在早时为明天的这个时间
 * @param onFinish 选择结束,确定时返回选择的时和分,取消时返回null
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimeCustomPage(onFinish: (time: Pair<Int, Int>?) -> Unit) {

    //默认30分钟之后
    val initial = remember { Calendar.getInstance().apply { add(Calendar.MINUTE, 30) } }
    val state = rememberTimePickerState(initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE), is24Hour = true)
    ModalBottomSheet(onDismissRequest = { onFinish(null) }, sheetState = rememberFullSheetState()) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                TextButton(onClick = { onFinish(null) }, modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("取消", color = settingColors.accent)
                }
                Text("自定义停止时间", style = MaterialTheme.typography.titleMedium, color = Color.primaryText, modifier = Modifier.align(Alignment.Center))
                TextButton(onClick = { onFinish(Pair(state.hour, state.minute)) }, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Text("确定", color = settingColors.accent)
                }
            }
            Column(
                Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TimePicker(state)
                Text(tip(state.hour, state.minute), style = MaterialTheme.typography.bodyMedium, color = Color.secondaryText)
            }
        }
    }
}

/** 停止时间的说明,明天的时间加上"明天" */
private fun tip(hour: Int, minute: Int): String {
    val endDate = SleepTimer.nextDate(hour, minute)
    val minutes = ((endDate.time - System.currentTimeMillis()) / 60_000 + 1).toInt()
    val duration = if (minutes >= 60) "${minutes / 60}小时${minutes % 60}分钟" else "${minutes}分钟"
    return SleepTimeCustomPage.format(endDate) + "停止播放,大约" + duration + "之后"
}

object SleepTimeCustomPage {

    /** 格式化停止时间,不是今天时加上"明天" */
    fun format(date: Date): String {
        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply { time = date }
        val isToday = today.get(Calendar.YEAR) == target.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
        return (if (isToday) "" else "明天") + SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }
}
