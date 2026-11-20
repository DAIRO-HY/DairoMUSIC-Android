package cn.dairo.music.page.playing.lrc

/**
 * 一行歌词
 * @param time 开始时间(秒)
 * @param text 歌词
 */
data class LrcLine(val time: Double, val text: String)

/**
 * 歌词解析,支持LRC格式和采集平台的WEBVTT字幕格式
 */
object LrcParser {

    /** 解析歌词,按时间排序 */
    fun parse(lrc: String): List<LrcLine> {
        val lines = lrc.replace("\r\n", "\n").split("\n")
        if (lines.any { it.contains("-->") }) {
            return parseVtt(lines)
        }
        return parseLrc(lines)
    }

    /** LRC格式,一行可能有多个时间标签,例如:[00:12.34][01:20.00]歌词 */
    private fun parseLrc(lines: List<String>): List<LrcLine> {
        val result = ArrayList<LrcLine>()
        for (line in lines) {
            var rest = line
            val times = ArrayList<Double>()
            while (rest.startsWith("[")) {
                val end = rest.indexOf(']')
                if (end < 0) {
                    break
                }

                //[ti:歌名]这类信息标签,不显示
                val time = toSeconds(rest.substring(1, end)) ?: break
                times.add(time)
                rest = rest.substring(end + 1)
            }
            val text = rest.trim()
            for (time in times) {
                result.add(LrcLine(time, text))
            }
        }
        return result.sortedBy { it.time }
    }

    /**
     * WEBVTT格式,时间行之后到空行之前都是这一段的字幕
     * 00:00:01.000 --> 00:00:04.000
     */
    private fun parseVtt(lines: List<String>): List<LrcLine> {
        val result = ArrayList<LrcLine>()
        var time: Double? = null
        val texts = ArrayList<String>()
        val flush = {
            time?.let { result.add(LrcLine(it, texts.joinToString("\n"))) }
            time = null
            texts.clear()
        }
        for (line in lines) {
            if (line.contains("-->")) {
                flush()
                time = toSeconds(line.split("-->")[0].trim())
                continue
            }
            val text = line.trim()
            if (text.isEmpty()) {
                flush()
                continue
            }
            if (time != null) {
                texts.add(text)
            }
        }
        flush()
        return result.sortedBy { it.time }
    }

    /** 时间转换成秒,支持 mm:ss.xx 和 hh:mm:ss.xxx */
    private fun toSeconds(value: String): Double? {
        val parts = value.replace(",", ".").split(":")
        if (parts.size < 2 || parts.size > 3) {
            return null
        }
        var seconds = 0.0
        for (part in parts) {
            val number = part.trim().toDoubleOrNull() ?: return null
            seconds = seconds * 60 + number
        }
        return seconds
    }
}
