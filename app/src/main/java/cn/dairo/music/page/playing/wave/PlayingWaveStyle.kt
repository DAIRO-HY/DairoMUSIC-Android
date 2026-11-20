package cn.dairo.music.page.playing.wave

import androidx.media3.common.util.UnstableApi
import cn.dairo.music.player.AudioSpectrum

/**
 * 播放页环形波形的样式,在"我的"页面设置;声明的顺序就是设置里的顺序,值保存在本地,不能改
 */
@UnstableApi
enum class PlayingWaveStyle(val value: Int) {

    /** 一根根细水柱,顶端有水滴 */
    BARS(0),

    /** 两层平滑曲线,外面有一条慢一拍落下的水面线 */
    SMOOTH(1),

    /** 低音、中音、高音三条半透明光带,各自转动、起伏 */
    AURORA(3),

    /** 圆环每个方向往外喷光点,喷多快多远跟着频段强度 */
    PARTICLES(4),

    /** 外圈柱子往外冲,内圈柱子同时往封面方向压进去 */
    MIRROR(5),

    /** 声音的原始波形绕成一圈,后面拖着残影 */
    SCOPE(6),

    /** 不显示波形,只有转动的圆形封面,不计算频谱 */
    OFF(2);

    /** 设置里显示的名称 */
    val label: String
        get() = when (this) {
            BARS -> "水柱"
            SMOOTH -> "圆润"
            AURORA -> "极光"
            PARTICLES -> "粒子"
            MIRROR -> "镜像"
            SCOPE -> "示波器"
            OFF -> "关闭"
        }

    /** 一圈的点数,左右对称,只算一半;关闭时为0 */
    val pointCount: Int
        get() = when (this) {
            BARS, PARTICLES -> 120
            SMOOTH -> 72
            AURORA -> 96
            MIRROR -> 90
            SCOPE -> AudioSpectrum.WAVE_COUNT * 2
            OFF -> 0
        }

    /** 封面直径占画布边长的比例,镜像样式里面还有一圈往里压的柱子,封面小一些 */
    val logoRatio: Double
        get() = if (this == MIRROR) 0.4 else WaveGeometry.LOGO_RATIO

    companion object {

        /** 按保存的值获取样式 */
        fun of(value: Int): PlayingWaveStyle {
            return entries.firstOrNull { it.value == value } ?: BARS
        }
    }
}
