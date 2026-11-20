package cn.dairo.music.player

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 正在播放的歌曲的实时频谱,播放页的环形波形用
 * 在播放器的音频处理链上接一个TeeAudioProcessor取PCM数据,做FFT,按对数频率分成若干频段
 * 只有播放页的封面页显示时才计算(isEnabled),其他时候只是把声音原样传过去
 */
@UnstableApi
object AudioSpectrum : TeeAudioProcessor.AudioBufferSink {

    /** 频段数,从低音到高音按对数频率划分 */
    const val BAND_COUNT = 64

    /** 时域波形的点数,每次FFT的采样平均成这么多个点(示波器样式用) */
    const val WAVE_COUNT = 128

    /** 每次FFT的采样数 */
    private const val FFT_SIZE = 1024

    /** 频段的最低、最高频率(Hz) */
    private const val MIN_FREQ = 40f
    private const val MAX_FREQ = 16000f

    /** 频段强度从分贝换算成0到1:低于这个分贝为0,往上DB_RANGE分贝为1 */
    private const val MIN_DB = -62f
    private const val DB_RANGE = 52f

    /** 高音能量比低音低很多,从低到高最多补偿的分贝,波形左右两侧看起来更均匀 */
    private const val HIGH_BOOST_DB = 8f

    /** 汉宁窗(和iOS的vDSP_HANN_NORM一样归一化),减少频谱泄漏 */
    private val window = FloatArray(FFT_SIZE) { 0.8165f * (1 - cos(2 * PI * it / FFT_SIZE).toFloat()) }

    /** FFT用的正余弦表 */
    private val cosTable = FloatArray(FFT_SIZE / 2) { cos(2 * PI * it / FFT_SIZE).toFloat() }
    private val sinTable = FloatArray(FFT_SIZE / 2) { sin(2 * PI * it / FFT_SIZE).toFloat() }

    /** 累积的采样(单声道),攒够FFT_SIZE个计算一次 */
    private val samples = FloatArray(FFT_SIZE)
    private var sampleCount = 0

    /** FFT的计算缓冲 */
    private val real = FloatArray(FFT_SIZE)
    private val imag = FloatArray(FFT_SIZE)

    /** 当前音频的格式 */
    private var sampleRate = 44100
    private var channelCount = 2
    private var encoding = C.ENCODING_PCM_16BIT

    /** 最新的频段强度,音频线程写,界面线程读 */
    private var bands = FloatArray(BAND_COUNT)

    /** 最新的时域波形(-1到1),音频线程写,界面线程读 */
    private var wave = FloatArray(WAVE_COUNT)

    private val lock = Any()

    /** 是否计算频谱,播放页的封面页显示时打开 */
    private var enabled = false

    /** 是否计算频谱,关闭时频段强度清零 */
    var isEnabled: Boolean
        get() = synchronized(this.lock) { this.enabled }
        set(value) {
            synchronized(this.lock) {
                this.enabled = value
                if (!value) {
                    this.bands = FloatArray(BAND_COUNT)
                    this.wave = FloatArray(WAVE_COUNT)
                }
            }
        }

    /** 最新的频段强度(0到1),从低音到高音 */
    val latestBands: FloatArray
        get() = synchronized(this.lock) { this.bands }

    /** 最新的时域波形(-1到1),一次FFT的采样平均成WAVE_COUNT个点 */
    val latestWave: FloatArray
        get() = synchronized(this.lock) { this.wave }

    /** 音频格式变化,重新开始累积采样 */
    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        this.sampleRate = sampleRateHz
        this.channelCount = max(1, channelCount)
        this.encoding = encoding
        this.sampleCount = 0
    }

    /** 音频线程:取第一个声道的采样,攒够一次FFT的数量就计算频谱;支持16位整数和32位浮点 */
    override fun handleBuffer(buffer: ByteBuffer) {
        if (!this.isEnabled) {
            return
        }
        val bytesPerSample = when (this.encoding) {
            C.ENCODING_PCM_16BIT -> 2
            C.ENCODING_PCM_FLOAT -> 4
            else -> return
        }
        val data = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val frameSize = bytesPerSample * this.channelCount
        var position = data.position()
        val limit = data.limit()
        while (position + frameSize <= limit) {
            val sample = if (bytesPerSample == 2) data.getShort(position) / 32768f else data.getFloat(position)
            this.samples[this.sampleCount] = sample
            this.sampleCount += 1
            if (this.sampleCount == FFT_SIZE) {
                this.sampleCount = 0
                this.computeBands()
            }
            position += frameSize
        }
    }

    /** 计算频谱:加窗、FFT、按对数频率分段取最大值、换算成0到1 */
    private fun computeBands() {
        val n = FFT_SIZE

        //时域波形:每几个采样取平均,去掉太细的抖动
        val step = n / WAVE_COUNT
        val wave = FloatArray(WAVE_COUNT)
        for (i in 0 until WAVE_COUNT) {
            var sum = 0f
            for (j in 0 until step) {
                sum += this.samples[i * step + j]
            }
            wave[i] = sum / step
        }
        for (i in 0 until n) {
            this.real[i] = this.samples[i] * this.window[i]
            this.imag[i] = 0f
        }
        this.fft()

        //和iOS的vDSP_fft_zrip一样,结果是标准DFT的2倍
        val magnitudes = FloatArray(n / 2) { 2 * sqrt(this.real[it] * this.real[it] + this.imag[it] * this.imag[it]) }
        val binWidth = this.sampleRate.toFloat() / n
        val ratio = MAX_FREQ / MIN_FREQ
        val result = FloatArray(BAND_COUNT)
        for (b in 0 until BAND_COUNT) {
            val low = MIN_FREQ * ratio.pow(b.toFloat() / BAND_COUNT)
            val high = MIN_FREQ * ratio.pow((b + 1).toFloat() / BAND_COUNT)
            val from = min(n / 2 - 1, max(1, (low / binWidth).toInt()))
            val to = min(n / 2 - 1, max(from, (high / binWidth).toInt()))
            var peak = 0f
            for (i in from..to) {
                peak = max(peak, magnitudes[i])
            }

            //幅度按FFT大小归一化之后换算成分贝,高音补偿一些
            val db = 20 * log10(max(peak / n, 1e-9f)) + HIGH_BOOST_DB * b / (BAND_COUNT - 1)
            result[b] = min(1f, max(0f, (db - MIN_DB) / DB_RANGE))
        }
        synchronized(this.lock) {
            if (this.enabled) {
                this.bands = result
                this.wave = wave
            }
        }
    }

    /** 原地计算的基2快速傅里叶变换 */
    private fun fft() {
        val n = FFT_SIZE
        val real = this.real
        val imag = this.imag

        //按位反转重新排列
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                var t = real[i]
                real[i] = real[j]
                real[j] = t
                t = imag[i]
                imag[i] = imag[j]
                imag[j] = t
            }
        }
        var size = 2
        while (size <= n) {
            val half = size / 2
            val tableStep = n / size
            var start = 0
            while (start < n) {
                for (k in 0 until half) {
                    val wr = this.cosTable[k * tableStep]
                    val wi = -this.sinTable[k * tableStep]
                    val a = start + k
                    val b = a + half
                    val tr = real[b] * wr - imag[b] * wi
                    val ti = real[b] * wi + imag[b] * wr
                    real[b] = real[a] - tr
                    imag[b] = imag[a] - ti
                    real[a] += tr
                    imag[a] += ti
                }
                start += size
            }
            size *= 2
        }
    }
}
