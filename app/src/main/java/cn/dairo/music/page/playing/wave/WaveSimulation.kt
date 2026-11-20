package cn.dairo.music.page.playing.wave

import androidx.media3.common.util.UnstableApi
import cn.dairo.music.player.AudioSpectrum
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * 环形波形的物理效果:上升直接追随频谱,下降按重力加速,各样式的粒子、残影、冲击波
 */
@UnstableApi
class WaveSimulation {

    /** 一个方向上的一根水柱(左右对称,只算一半) */
    class Bar {

        /** 当前高度(0到1) */
        var height = MIN_HEIGHT

        /** 下落速度 */
        var velocity = 0.0

        /** 顶端水滴的高度 */
        var cap = MIN_HEIGHT

        /** 顶端水滴的下落速度 */
        var capVelocity = 0.0

        /** 顶端水滴还要停顿的时间(秒) */
        var hold = 0.0
    }

    /**
     * 甩出去的水珠(水柱、圆润)
     * @param angle 角度(弧度)
     * @param radius 离圆环的高度(和水柱高度同一个单位)
     * @param radialVelocity 往外飞的速度,受重力影响会变成负数落回来
     * @param angularVelocity 角度方向的速度
     * @param life 剩余时间(秒)
     * @param scale 大小的倍数
     */
    class Drop(var angle: Double, var radius: Double, var radialVelocity: Double, val angularVelocity: Double, var life: Double, val scale: Double)

    /**
     * 往外喷的光点(粒子)
     * @param angle 角度(弧度)
     * @param speed 往外飞的速度,越来越慢
     * @param scale 大小的倍数
     * @param colorIndex 用调色板里的哪个颜色(0内圈 1中间 2外圈)
     */
    class Particle(val angle: Double, var speed: Double, val scale: Double, val colorIndex: Int) {

        /** 离圆环的距离(和波形高度同一个单位) */
        var radius = 0.0

        /** 剩余亮度(1到0) */
        var life = 1.0
    }

    /** 低音冲击波的一圈光环 */
    class Shockwave {

        /** 扩散的距离(和波形高度同一个单位) */
        var radius = 0.0

        /** 透明度,扩散时变淡 */
        var alpha = 0.9
    }

    companion object {

        /** 水柱的最低高度,暂停时落到这个高度 */
        const val MIN_HEIGHT = 0.04

        /** 顶部低音放大的倍数 */
        private const val LOW_BOOST = 1.0

        /** 高音压低的比例,最底部的高音只剩(1 - HIGH_CUT),越大高低音的反差越大 */
        private const val HIGH_CUT = 0.5

        /** 水柱下落的重力加速度,越大落得越快 */
        private const val GRAVITY = 3.2

        /** 顶端水滴下落的重力加速度 */
        private const val CAP_GRAVITY = 2.2

        /** 顶端水滴停顿的时间(秒) */
        private const val CAP_HOLD = 0.14

        /** 同时最多的水珠数 */
        private const val MAX_DROPS = 60

        /** 同时最多的光点数 */
        private const val MAX_PARTICLES = 420

        /** 示波器保留的残影数 */
        const val SCOPE_TRAILS = 5

        /** 封面转一圈的时间(秒) */
        private const val ROTATION_SECONDS = 30.0

        /** 极光三条光带各自的音量比例(低音、中音、高音) */
        private val AURORA_GAINS = doubleArrayOf(1.0, 0.85, 0.75)

        /** 极光三条光带的转动速度(弧度/秒),相邻两条反方向转 */
        private val AURORA_SPEEDS = doubleArrayOf(0.05, -0.085, 0.12)

        /** 水柱的目标高度:从频谱里取对应的频段,顶部是低音,往下是高音;没有频谱(暂停)时落到最低 */
        private fun target(k: Int, half: Int, bands: FloatArray): Double {
            if (bands.isEmpty()) {
                return MIN_HEIGHT
            }
            val f = k.toDouble() / half
            val value = bands[min(bands.size - 1, (f * (bands.size - 1)).toInt())].toDouble()

            //顶部的低音放大(最多LOW_BOOST倍,往下很快减弱),高音的柱子低一些
            val lowBoost = 1 + (LOW_BOOST - 1) * (1 - f).pow(3)
            return min(1.0, max(MIN_HEIGHT, value * (1 - HIGH_CUT * f) * lowBoost))
        }

        /**
         * 一根水柱:比目标低时快速冲高,比目标高时受重力加速落下;顶端水滴停一下再落
         * @return 落得快的高柱子返回下落之前的高度,可以甩出水珠
         */
        private fun step(bar: Bar, target: Double, dt: Double): Double? {
            var dropHeight: Double? = null
            if (target > bar.height) {
                bar.height += (target - bar.height) * min(1.0, dt * 28)
                bar.velocity = 0.0
            } else {
                val previous = bar.height
                bar.velocity += GRAVITY * dt
                bar.height = max(target, bar.height - bar.velocity * dt)
                if (bar.velocity > 1.1 && previous > 0.5 && Random.nextDouble() < 0.18) {
                    dropHeight = previous
                }
            }
            if (bar.height >= bar.cap) {
                bar.cap = bar.height
                bar.capVelocity = 0.0
                bar.hold = CAP_HOLD
            } else if (bar.hold > 0) {
                bar.hold -= dt
            } else {
                bar.capVelocity += CAP_GRAVITY * dt
                bar.cap = max(bar.height, bar.cap - bar.capVelocity * dt)
            }
            return dropHeight
        }
    }

    var bars = emptyList<Bar>()
        private set
    var auroraLayers = emptyList<List<Bar>>()
        private set
    val auroraAngles = doubleArrayOf(0.0, 0.0, 0.0)
    val drops = ArrayList<Drop>()
    val particles = ArrayList<Particle>()
    val shockwaves = ArrayList<Shockwave>()

    /** 示波器当前和之前几帧的波形(-1到1),第一个是当前的 */
    val scopeHistory = ArrayList<List<Double>>()

    /** 示波器的振幅比例,暂停时慢慢变成0(波形变回一个圆) */
    var scopeLevel = 0.0
        private set

    /** 最近的最大振幅,用来把不同响度的歌都放大到差不多的幅度 */
    private var scopePeak = 0.1

    /** 低音的平均强度和上一次冲击波的时间,判断鼓点 */
    private var bassAverage = 0.0
    private var lastShockTime = 0.0
    private var time = 0.0

    /** 上一帧的时间(秒) */
    private var lastTime: Double? = null

    /** 封面转动的角度(弧度) */
    var rotation = 0.0
        private set

    /** 前进到这一帧:按频谱更新各样式的状态,播放时封面转动 */
    fun advance(now: Double, isPlaying: Boolean, style: PlayingWaveStyle, shockwave: Boolean) {
        val dt = min(0.05, max(0.0, now - (this.lastTime ?: now)))
        this.lastTime = now
        if (dt == 0.0) {
            return
        }
        this.time += dt
        if (isPlaying) {
            this.rotation += dt * 2 * PI / ROTATION_SECONDS
        }
        if (style == PlayingWaveStyle.OFF) {
            this.bars = emptyList()
            this.auroraLayers = emptyList()
            this.drops.clear()
            this.particles.clear()
            this.shockwaves.clear()
            this.scopeHistory.clear()
            return
        }
        val bands = if (isPlaying) AudioSpectrum.latestBands else FloatArray(0)
        val half = style.pointCount / 2
        when (style) {
            PlayingWaveStyle.BARS, PlayingWaveStyle.SMOOTH, PlayingWaveStyle.MIRROR, PlayingWaveStyle.PARTICLES -> {
                this.stepBars(bands, half, dt, withDrops = style == PlayingWaveStyle.BARS || style == PlayingWaveStyle.SMOOTH)
                if (style == PlayingWaveStyle.PARTICLES) {
                    this.stepParticles(isPlaying, dt)
                }
            }

            PlayingWaveStyle.AURORA -> this.stepAurora(bands, half, dt, isPlaying)
            PlayingWaveStyle.SCOPE -> this.stepScope(if (isPlaying) AudioSpectrum.latestWave else FloatArray(0), isPlaying, dt)
            PlayingWaveStyle.OFF -> {}
        }
        this.stepDrops(dt)
        this.stepShockwaves(bands, shockwave && isPlaying, dt)
    }

    /** 水柱(水柱、圆润、镜像、粒子共用):顶部是低音,往下是高音 */
    private fun stepBars(bands: FloatArray, half: Int, dt: Double, withDrops: Boolean) {
        if (this.bars.size != half + 1) {
            this.bars = List(half + 1) { Bar() }
            this.drops.clear()
        }
        for (k in 0..half) {
            val height = step(this.bars[k], target(k, half, bands), dt)
            if (height != null && withDrops) {
                this.spawnDrop(k, half, height)
            }
        }
    }

    /** 极光:三条光带分别取低音、中音、高音三段频谱 */
    private fun stepAurora(bands: FloatArray, half: Int, dt: Double, isPlaying: Boolean) {
        if (this.auroraLayers.size != 3 || this.auroraLayers[0].size != half + 1) {
            this.auroraLayers = List(3) { List(half + 1) { Bar() } }
        }
        val third = AudioSpectrum.BAND_COUNT / 3
        for (layer in 0 until 3) {
            if (isPlaying) {
                this.auroraAngles[layer] += AURORA_SPEEDS[layer] * dt
            }
            for (k in 0..half) {
                var target = 0.05
                if (bands.isNotEmpty()) {
                    val index = min(bands.size - 1, layer * third + (k.toDouble() / half * (third - 1)).toInt())
                    target = max(0.05, bands[index] * AURORA_GAINS[layer])
                }
                step(this.auroraLayers[layer][k], target, dt)
            }
        }
    }

    /** 粒子:每个方向按水柱高度的平方随机喷出光点,飞出去之后减速、变淡 */
    private fun stepParticles(isPlaying: Boolean, dt: Double) {
        if (isPlaying) {
            val count = (this.bars.size - 1) * 2
            for (i in 0 until count) {
                if (this.particles.size >= MAX_PARTICLES) {
                    break
                }
                val h = this.bars[min(this.bars.size - 1, if (i <= count / 2) i else count - i)].height
                if (Random.nextDouble() < h * h * 1.4 * dt * 30) {
                    val angle = i.toDouble() / count * 2 * PI - PI / 2 + Random.nextDouble(-0.02, 0.02)
                    this.particles.add(
                        Particle(angle, (0.6 + h * 1.6) * Random.nextDouble(0.6, 1.2), Random.nextDouble(0.5, 1.7), Random.nextInt(0, 3))
                    )
                }
            }
        }
        val drag = 0.18.pow(dt)
        for (particle in this.particles) {
            particle.radius += particle.speed * dt
            particle.speed *= drag
            particle.life -= dt * (0.55 + 0.25 * particle.scale)
        }
        this.particles.removeAll { it.life <= 0 || it.radius > 1.2 }
    }

    /** 示波器:按最近的最大振幅放大,平滑掉太细的抖动,保留几帧作为残影 */
    private fun stepScope(wave: FloatArray, isPlaying: Boolean, dt: Double) {
        this.scopeLevel += ((if (isPlaying) 1.0 else 0.0) - this.scopeLevel) * min(1.0, dt * 4)
        var values: List<Double>
        if (wave.isEmpty()) {
            values = this.scopeHistory.firstOrNull() ?: List(AudioSpectrum.WAVE_COUNT) { 0.0 }
        } else {
            values = wave.map { it.toDouble() }
            val peak = values.maxOf { abs(it) }
            this.scopePeak = max(0.08, max(peak, this.scopePeak * 0.5.pow(dt)))
            values = values.map { it / this.scopePeak }
            repeat(2) {
                val n = values.size
                val current = values
                values = current.indices.map { (current[max(0, it - 1)] + 2 * current[it] + current[min(n - 1, it + 1)]) / 4 }
            }
        }
        this.scopeHistory.add(0, values)
        if (this.scopeHistory.size > SCOPE_TRAILS) {
            this.scopeHistory.removeAt(this.scopeHistory.size - 1)
        }
    }

    /** 甩出去的水珠受重力落回圆环 */
    private fun stepDrops(dt: Double) {
        for (drop in this.drops) {
            drop.radialVelocity -= GRAVITY * 0.9 * dt
            drop.radius += drop.radialVelocity * dt
            drop.angle += drop.angularVelocity * dt
            drop.life -= dt
        }
        this.drops.removeAll { it.radius <= 0 || it.life <= 0 }
    }

    /** 低音冲击波:低音明显比平时强(鼓点)时放出一圈光环,往外扩散并变淡;两圈之间至少隔0.32秒 */
    private fun stepShockwaves(bands: FloatArray, enabled: Boolean, dt: Double) {
        if (enabled && bands.size >= 6) {
            var sum = 0.0
            for (i in 0 until 6) {
                sum += bands[i]
            }
            val bass = sum / 6
            if (bass > 0.42 && bass > this.bassAverage * 1.12 && this.time - this.lastShockTime > 0.32) {
                this.shockwaves.add(Shockwave())
                this.lastShockTime = this.time
            }
            this.bassAverage = this.bassAverage * 0.85 + bass * 0.15
        }
        for (wave in this.shockwaves) {
            wave.radius += dt * 0.9
            wave.alpha -= dt * 0.9
        }
        this.shockwaves.removeAll { it.alpha <= 0 }
    }

    /** 从一根水柱的顶端左右各甩出一颗水珠 */
    private fun spawnDrop(k: Int, half: Int, height: Double) {
        if (this.drops.size > MAX_DROPS) {
            return
        }
        val count = half * 2
        for (side in intArrayOf(1, -1)) {
            if (side < 0 && (k == 0 || k == half)) {
                continue
            }
            val i = if (side > 0) k else count - k
            this.drops.add(
                Drop(
                    angle = i.toDouble() / count * 2 * PI - PI / 2 + Random.nextDouble(-0.015, 0.015),
                    radius = height,
                    radialVelocity = 0.35 + Random.nextDouble(0.0, 0.5),
                    angularVelocity = Random.nextDouble(-0.125, 0.125),
                    life = 1.6,
                    scale = 0.6 + Random.nextDouble(0.0, 0.8),
                )
            )
        }
    }

    /** 第i个点对应的水柱(左右对称) */
    fun barIndex(i: Int, count: Int, barCount: Int): Int {
        val half = count / 2
        return min(barCount - 1, if (i <= half) i else count - i)
    }
}
