package cn.dairo.music.util

import cn.dairo.music.api.MusicApi
import cn.dairo.music.api.model.MusicModel
import cn.dairo.music.store.SettingStore
import cn.dairo.ui.util.LocalObjectUtil
import kotlinx.coroutines.MainScope

/**
 * 播放次数:歌曲自然播放到结尾时先记录到本地的待提交记录,再提交到服务器,在线和离线播放用同一套逻辑
 * 待提交记录按账号保存到磁盘,退出App不会丢;提交成功之后删除已经提交的部分,失败时保留
 * 下一次播放完成、网络恢复、App启动、切换账号时再提交
 * 只在主线程调用
 */
object PlayCountUtil {

    /** 待提交记录的本地缓存名,内容是歌曲ID,播放了几次就有几个 */
    private const val KEY_PENDING = "play_count_pending"

    /** 一次最多提交的记录数 */
    private const val MAX_BATCH = 500

    /** 正在提交的记录文件名,同一个账号的记录不会同时提交两次 */
    private val submitting = HashSet<String>()

    /** 是否已经开始监听网络恢复 */
    private var isStarted = false

    /** App启动时调用:开始监听网络恢复,提交上次没有提交成功的记录 */
    fun start() {
        if (!isStarted) {
            isStarted = true
            NotificationCenter.observe(MainScope(), NetworkMonitor.AVAILABLE) {
                submit()
            }
        }
        submit()
    }

    /** 歌曲播放完成,记录一次并提交;采集平台的搜索结果(还没有加入曲库)不计 */
    fun add(music: MusicModel) {
        if (music.id <= 0) {
            return
        }
        val name = SettingStore.accountObjectName(KEY_PENDING) ?: return
        LocalObjectUtil.write(read(name) + music.id, name)
        submit()
    }

    /** 提交当前账号的待提交记录,超过500条时分批提交 */
    fun submit() {
        val name = SettingStore.accountObjectName(KEY_PENDING) ?: return
        if (submitting.contains(name)) {
            return
        }
        val batch = read(name).take(MAX_BATCH)
        if (batch.isEmpty()) {
            return
        }
        submitting.add(name)
        MusicApi.play(batch.joinToString(",")).hide().fail {
            submitting.remove(name)
        }.error {
            submitting.remove(name)
        }.send {
            submitting.remove(name)

            //提交过程中新增的记录追加在后面,只删除已经提交的部分
            LocalObjectUtil.write(read(name).drop(batch.size), name)

            //还有没提交的继续提交下一批
            submit()
        }
    }

    /** 读取待提交记录 */
    private fun read(name: String): List<Long> {
        return LocalObjectUtil.read<List<Long>>(name) ?: emptyList()
    }
}
