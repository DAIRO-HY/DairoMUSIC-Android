package cn.dairo.music.page.playing.wave

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * 波形用的颜色,方便调亮、生成不同透明度的颜色
 * @param red 0到1
 */
data class WaveColor(val red: Double, val green: Double, val blue: Double) {

    /** 指定透明度的颜色 */
    fun color(opacity: Double): Color {
        return Color(this.red.toFloat().coerceIn(0f, 1f), this.green.toFloat().coerceIn(0f, 1f), this.blue.toFloat().coerceIn(0f, 1f), opacity.toFloat().coerceIn(0f, 1f))
    }

    /** 往白色混合,amount为0时不变,为1时是白色 */
    fun lightened(amount: Double): WaveColor {
        return WaveColor(this.red + (1 - this.red) * amount, this.green + (1 - this.green) * amount, this.blue + (1 - this.blue) * amount)
    }
}

/**
 * 波形的调色板:从圆环往外的内圈、中间、外圈三个颜色
 */
class WavePalette(val inner: WaveColor, val middle: WaveColor, val outer: WaveColor) {

    /** 第几个颜色(0内圈 1中间 2外圈) */
    fun color(index: Int): WaveColor {
        return if (index == 0) this.inner else if (index == 1) this.middle else this.outer
    }

    companion object {

        /**
         * 默认的青蓝、蓝紫、紫粉,中间和外圈的颜色随时间慢慢变化
         * @param hueShift 颜色变化的进度(0到1)
         */
        fun standard(hueShift: Double): WavePalette {
            return WavePalette(
                WaveColor(90 / 255.0, 200 / 255.0, 1.0),
                WaveColor((120 + hueShift * 40) / 255, 120 / 255.0, 1.0),
                WaveColor(1.0, (170 + hueShift * 40) / 255, 1.0),
            )
        }

        /** 跟随封面:从封面的主色调出三个深浅不同的颜色 */
        fun cover(base: WaveColor): WavePalette {
            return WavePalette(base.lightened(0.55), base.lightened(0.3), base.lightened(0.05))
        }
    }
}

/**
 * 环形波形的尺寸和画法:圆环半径、波形最大高度、点的位置、渐变色
 * 所有尺寸的单位都是dp,和iOS的点一样
 * @param side 画布边长
 * @param count 一圈的点数
 * @param palette 调色板
 */
class WaveGeometry(val side: Double, val count: Int, val palette: WavePalette) {

    companion object {

        /** 封面直径占画布边长的比例 */
        const val LOGO_RATIO = 0.525

        /** 圆环比封面大出来的半径(占画布边长的比例),波形从圆环开始往外 */
        private const val RING_GAP = 0.0075

        /** 波形最大高度占画布边长的比例 */
        private const val LENGTH_RATIO = 0.15

        /** 经过每两点中点的二次贝塞尔曲线,得到平滑的闭合波形 */
        fun closedCurve(points: List<Offset>): Path {
            val n = points.size
            val path = Path()
            if (n <= 2) {
                return path
            }
            fun mid(a: Offset, b: Offset) = Offset((a.x + b.x) / 2, (a.y + b.y) / 2)
            val start = mid(points[n - 1], points[0])
            path.moveTo(start.x, start.y)
            for (i in 0 until n) {
                val to = mid(points[i], points[(i + 1) % n])
                path.quadraticTo(points[i].x, points[i].y, to.x, to.y)
            }
            path.close()
            return path
        }

        /** 圆 */
        fun circle(center: Double, radius: Double): Path {
            return Path().apply {
                addOval(Rect((center - radius).toFloat(), (center - radius).toFloat(), (center + radius).toFloat(), (center + radius).toFloat()))
            }
        }
    }

    val center: Double
        get() = this.side / 2

    /** 圆环半径 */
    val r0: Double
        get() = this.side * (LOGO_RATIO / 2 + RING_GAP)

    /** 波形的最大高度 */
    val length: Double
        get() = this.side * LENGTH_RATIO

    /** 圆环 */
    val innerRing: Path
        get() = circle(this.center, this.r0)

    /** 第i个点的角度,从12点方向开始顺时针 */
    fun angle(i: Int): Double {
        return i.toDouble() / this.count * 2 * PI - PI / 2
    }

    /** 第i个点在半径radius上的位置 */
    fun point(i: Int, radius: Double, rotation: Double = 0.0): Offset {
        val angle = this.angle(i) + rotation
        return Offset((this.center + cos(angle) * radius).toFloat(), (this.center + sin(angle) * radius).toFloat())
    }

    /**
     * 从内半径到外半径的渐变色,颜色从调色板的内圈到外圈
     * @param inner 内圈的透明度
     * @param outer 外圈的透明度
     */
    fun gradient(inner: Double, outer: Double, from: Double? = null, to: Double? = null): Brush {
        return radialGradient(
            listOf(
                0.0 to this.palette.inner.color(inner),
                0.48 to this.palette.middle.color((inner + outer) / 2),
                1.0 to this.palette.outer.color(outer),
            ),
            this.center, from ?: this.r0, to ?: (this.r0 + this.length),
        )
    }

    /**
     * 有起始半径的径向渐变(Compose的径向渐变从圆心开始,这里把颜色位置换算一下)
     */
    fun radialGradient(stops: List<Pair<Double, Color>>, center: Double, startRadius: Double, endRadius: Double): Brush {
        val end = max(endRadius, 0.001)
        val start = min(max(startRadius, 0.0), end)
        val mapped = stops.map { ((start + it.first * (end - start)) / end).toFloat() to it.second }.toTypedArray()
        return Brush.radialGradient(*mapped, center = Offset(center.toFloat(), center.toFloat()), radius = end.toFloat())
    }
}

/**
 * 画这一帧,坐标的单位是dp
 */
fun WaveSimulation.draw(scope: DrawScope, side: Double, style: PlayingWaveStyle, palette: WavePalette) {
    if (style == PlayingWaveStyle.OFF) {
        return
    }
    val g = WaveGeometry(side, style.pointCount, palette)
    this.drawShockwaves(scope, g, style)
    when (style) {
        PlayingWaveStyle.BARS -> this.drawBars(scope, g)
        PlayingWaveStyle.SMOOTH -> this.drawSmooth(scope, g)
        PlayingWaveStyle.AURORA -> this.drawAurora(scope, g)
        PlayingWaveStyle.PARTICLES -> this.drawParticles(scope, g)
        PlayingWaveStyle.MIRROR -> this.drawMirror(scope, g)
        PlayingWaveStyle.SCOPE -> this.drawScope(scope, g)
        PlayingWaveStyle.OFF -> {}
    }
}

/** 叠加混合,对应iOS的plusLighter */
private val PLUS = BlendMode.Plus

/** 第i个点的水柱高度 */
private fun WaveSimulation.height(i: Int, count: Int): Double {
    return this.bars[this.barIndex(i, count, this.bars.size)].height
}

/** 和相邻的点平滑(首尾相连) */
private fun smooth(values: List<Double>, times: Int = 1): List<Double> {
    var result = values
    repeat(times) {
        val n = result.size
        val current = result
        result = current.indices.map { (current[(it - 1 + n) % n] + 2 * current[it] + current[(it + 1) % n]) / 4 }
    }
    return result
}

/** 画线 */
private fun DrawScope.strokePath(path: Path, brush: Brush, width: Double, cap: StrokeCap = StrokeCap.Butt, alpha: Float = 1f) {
    this.drawPath(path, brush, alpha = alpha, style = Stroke(width.toFloat(), cap = cap, join = StrokeJoin.Round), blendMode = PLUS)
}

/** 画线 */
private fun DrawScope.strokePath(path: Path, color: Color, width: Double, cap: StrokeCap = StrokeCap.Butt) {
    this.drawPath(path, color, style = Stroke(width.toFloat(), cap = cap, join = StrokeJoin.Round), blendMode = PLUS)
}

/** 低音冲击波:从封面边缘往外扩散的光环,越远越细越淡 */
private fun WaveSimulation.drawShockwaves(scope: DrawScope, g: WaveGeometry, style: PlayingWaveStyle) {
    val start = if (style == PlayingWaveStyle.MIRROR) g.side * 0.2 else g.r0
    for (wave in this.shockwaves) {
        val radius = start + wave.radius * g.length * 1.3
        scope.strokePath(
            WaveGeometry.circle(g.center, radius), g.palette.inner.lightened(0.3).color(wave.alpha * 0.55),
            g.side * 0.012 * max(0.2, 1 - wave.radius * 0.5),
        )
    }
}

/** 水柱:光晕、水柱、内圈光环、顶端水滴、水珠 */
private fun WaveSimulation.drawBars(scope: DrawScope, g: WaveGeometry) {
    if (this.bars.isEmpty()) {
        return
    }
    val barWidth = 2 * PI * g.r0 / g.count * 0.42
    val path = Path()
    for (i in 0 until g.count) {
        val from = g.point(i, g.r0 + 2)
        val to = g.point(i, g.r0 + 2 + this.height(i, g.count) * g.length)
        path.moveTo(from.x, from.y)
        path.lineTo(to.x, to.y)
    }
    scope.strokePath(path, g.gradient(0.16, 0.16), barWidth * 3.2, StrokeCap.Round)
    scope.strokePath(path, g.gradient(0.85, 0.85), barWidth, StrokeCap.Round)
    scope.strokePath(g.innerRing, g.palette.middle.lightened(0.5).color(0.25), 1.0)
    val caps = Path()
    for (i in 0 until g.count) {
        val bar = this.bars[this.barIndex(i, g.count, this.bars.size)]
        if (bar.cap - bar.height < 0.015 && bar.height < 0.08) {
            continue
        }
        val center = g.point(i, g.r0 + 2 + bar.cap * g.length + barWidth * 1.4)
        val r = (barWidth * 0.55).toFloat()
        caps.addOval(Rect(center.x - r, center.y - r, center.x + r, center.y + r))
    }
    scope.drawPath(caps, g.palette.inner.lightened(0.8).color(0.95), blendMode = PLUS)
    this.drawDrops(scope, g, 0.0, barWidth * 2.2)
}

/** 圆润:两层平滑波形、轮廓光晕和亮边、慢一拍落下的水面线、内圈光环、水珠 */
private fun WaveSimulation.drawSmooth(scope: DrawScope, g: WaveGeometry) {
    if (this.bars.isEmpty()) {
        return
    }
    val lineWidth = g.side * 0.006
    val heights = smooth((0 until g.count).map { this.height(it, g.count) })
    val caps = smooth((0 until g.count).map { this.bars[this.barIndex(it, g.count, this.bars.size)].cap })
    val curve = { values: List<Double>, offset: Double ->
        WaveGeometry.closedCurve(values.indices.map { g.point(it, g.r0 + 2 + values[it] * g.length + offset) })
    }
    val outer = curve(heights, 0.0)
    val inner = curve(heights.map { it * 0.55 }, 0.0)
    val surface = curve(caps, lineWidth * 2)

    //填充时挖掉圆环里面的部分
    val outerFill = Path().apply {
        addPath(outer)
        addPath(g.innerRing)
        fillType = PathFillType.EvenOdd
    }
    scope.drawPath(outerFill, g.gradient(0.42, 0.12), blendMode = PLUS)
    val innerFill = Path().apply {
        addPath(inner)
        addPath(g.innerRing)
        fillType = PathFillType.EvenOdd
    }
    scope.drawPath(innerFill, g.gradient(0.3, 0.12), blendMode = PLUS)
    scope.strokePath(outer, g.gradient(0.18, 0.18), lineWidth * 4)
    scope.strokePath(outer, g.gradient(0.95, 0.9), lineWidth * 0.9)
    scope.strokePath(surface, g.palette.inner.lightened(0.8).color(0.4), lineWidth * 0.6)
    scope.strokePath(g.innerRing, g.palette.middle.lightened(0.5).color(0.3), 1.0)
    this.drawDrops(scope, g, lineWidth * 2, lineWidth * 2.6)
}

/** 甩出去的水珠,快消失时变淡 */
private fun WaveSimulation.drawDrops(scope: DrawScope, g: WaveGeometry, offset: Double, size: Double) {
    for (drop in this.drops) {
        val r = g.r0 + 2 + drop.radius * g.length + offset
        val cx = g.center + cos(drop.angle) * r
        val cy = g.center + sin(drop.angle) * r
        val radius = size * drop.scale
        if (radius <= 0) {
            continue
        }
        val brush = Brush.radialGradient(
            listOf(g.palette.inner.lightened(0.85).color(min(1.0, drop.life * 1.5)), g.palette.middle.color(0.0)),
            center = Offset(cx.toFloat(), cy.toFloat()),
            radius = radius.toFloat(),
        )
        scope.drawCircle(brush, radius.toFloat(), Offset(cx.toFloat(), cy.toFloat()), blendMode = PLUS)
    }
}

/** 极光:三条平滑光带(宽的光晕、中等的光带、细的亮线),低音在里、高音在外,各自转动 */
private fun WaveSimulation.drawAurora(scope: DrawScope, g: WaveGeometry) {
    if (this.auroraLayers.size != 3) {
        return
    }
    for (layer in 0 until 3) {
        val bars = this.auroraLayers[layer]
        val heights = smooth((0 until g.count).map { bars[this.barIndex(it, g.count, bars.size)].height }, times = 3)
        val base = g.r0 + g.length * (0.1 + layer * 0.15)
        val path = WaveGeometry.closedCurve(heights.indices.map {
            g.point(it, base + heights[it] * g.length * 0.6, this.auroraAngles[layer])
        })
        val color = g.palette.color(layer)
        scope.strokePath(path, color.color(0.14), g.side * 0.05)
        scope.strokePath(path, color.color(0.35), g.side * 0.016)
        scope.strokePath(path, color.lightened(0.4).color(0.9), g.side * 0.004)
    }
    scope.strokePath(g.innerRing, g.palette.middle.lightened(0.5).color(0.3), 1.0)
}

/** 粒子:每颗光点画一圈淡的光晕和一个亮的核心;贴着圆环的亮边随低音变亮 */
private fun WaveSimulation.drawParticles(scope: DrawScope, g: WaveGeometry) {
    val glows = List(3) { Path() }
    val cores = Path()
    for (particle in this.particles) {
        val r = g.r0 + particle.radius * g.length
        val x = (g.center + cos(particle.angle) * r).toFloat()
        val y = (g.center + sin(particle.angle) * r).toFloat()
        val size = (g.side * 0.006 * particle.scale * (0.6 + particle.life * 0.6)).toFloat()
        glows[particle.colorIndex].addOval(Rect(x - size * 2.4f, y - size * 2.4f, x + size * 2.4f, y + size * 2.4f))
        cores.addOval(Rect(x - size * 0.6f, y - size * 0.6f, x + size * 0.6f, y + size * 0.6f))
    }
    for (i in 0 until 3) {
        scope.drawPath(glows[i], g.palette.color(i).color(0.22), blendMode = PLUS)
    }
    scope.drawPath(cores, g.palette.inner.lightened(0.85).color(0.9), blendMode = PLUS)
    val bassCount = max(1, min(8, this.bars.size))
    val bass = this.bars.take(8).sumOf { it.height } / bassCount
    scope.strokePath(WaveGeometry.circle(g.center, g.r0 + 2), g.palette.inner.lightened(0.4).color(0.25 + bass * 0.5), g.side * 0.006)
}

/** 镜像:外圈柱子往外,内圈柱子往封面方向,中间一条亮的圆环 */
private fun WaveSimulation.drawMirror(scope: DrawScope, g: WaveGeometry) {
    if (this.bars.isEmpty()) {
        return
    }
    val middle = g.side * 0.28
    val outerLength = g.side * 0.14
    val innerLength = g.side * 0.07
    val barWidth = 2 * PI * middle / g.count * 0.42
    val outer = Path()
    val inner = Path()
    for (i in 0 until g.count) {
        val h = this.height(i, g.count)
        var from = g.point(i, middle + 3)
        var to = g.point(i, middle + 3 + h * outerLength)
        outer.moveTo(from.x, from.y)
        outer.lineTo(to.x, to.y)
        from = g.point(i, middle - 3)
        to = g.point(i, middle - 3 - h * innerLength)
        inner.moveTo(from.x, from.y)
        inner.lineTo(to.x, to.y)
    }
    val outerShading = g.gradient(0.9, 0.9, middle, middle + outerLength)
    val innerShading = g.radialGradient(
        listOf(0.0 to g.palette.middle.color(0.9), 1.0 to g.palette.inner.color(0.9)),
        g.center, middle - innerLength, middle,
    )
    scope.strokePath(outer, outerShading, barWidth * 3, StrokeCap.Round, alpha = 0.18f)
    scope.strokePath(inner, innerShading, barWidth * 3, StrokeCap.Round, alpha = 0.18f)
    scope.strokePath(outer, outerShading, barWidth, StrokeCap.Round)
    scope.strokePath(inner, innerShading, barWidth, StrokeCap.Round)
    scope.strokePath(WaveGeometry.circle(g.center, middle), g.palette.inner.lightened(0.85).color(0.55), 1.2)
}

/** 示波器:当前波形一条宽的光晕加一条细的亮线,之前几帧画成越来越淡的残影;中间一圈虚线是没有声音时的位置 */
private fun WaveSimulation.drawScope(scope: DrawScope, g: WaveGeometry) {
    val base = g.r0 + g.length * 0.5
    val amplitude = g.length * 0.45 * this.scopeLevel
    for (j in this.scopeHistory.indices.reversed()) {
        val values = this.scopeHistory[j]
        if (values.isEmpty()) {
            continue
        }

        //左右对称,首尾接得上
        val path = WaveGeometry.closedCurve((0 until g.count).map {
            g.point(it, base + values[this.barIndex(it, g.count, values.size)] * amplitude)
        })
        if (j == 0) {
            scope.strokePath(path, g.palette.inner.color(0.18), g.side * 0.03)
            scope.strokePath(path, g.palette.inner.lightened(0.7).color(0.95), g.side * 0.005)
        } else {
            val fade = 1 - j.toDouble() / WaveSimulation.SCOPE_TRAILS
            scope.strokePath(path, g.palette.middle.color(0.28 * fade), g.side * 0.004)
        }
    }
    scope.drawPath(
        WaveGeometry.circle(g.center, base), g.palette.middle.lightened(0.5).color(0.18),
        style = Stroke(1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 6f))), blendMode = PLUS,
    )
}
