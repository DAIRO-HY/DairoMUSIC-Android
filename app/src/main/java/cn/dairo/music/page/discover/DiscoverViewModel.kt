package cn.dairo.music.page.discover

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import cn.dairo.music.api.HomeApi
import cn.dairo.music.api.MusicApi
import cn.dairo.music.api.model.SwiperModel
import cn.dairo.music.api.model.UsefulModel
import cn.dairo.music.components.AppRoot
import cn.dairo.music.constants.FavoriteId
import cn.dairo.music.player.PlayerManager
import cn.dairo.music.store.FavoriteStore
import cn.dairo.music.store.MusicStore
import cn.dairo.music.store.SettingStore
import cn.dairo.music.util.ObservableObject
import cn.dairo.music.util.ServerConfig
import cn.dairo.ui.util.LocalObjectUtil

@UnstableApi
class DiscoverViewModel : ObservableObject() {

    companion object {

        /** 轮播的本地缓存名 */
        private const val KEY_SWIPER = "discover_swiper"

        /** 推荐歌单的本地缓存名 */
        private const val KEY_USEFUL = "discover_useful"
    }

    /** 轮播 */
    var swiperList by mutableStateOf(emptyList<SwiperModel>())

    /** 推荐歌单分组 */
    var usefulList by mutableStateOf(emptyList<UsefulModel>())

    /** 是否正在下拉刷新 */
    var isRefreshing by mutableStateOf(false)

    init {

        //先显示本地缓存,再从服务器更新
        this.swiperList = SettingStore.accountObjectName(KEY_SWIPER)?.let { LocalObjectUtil.read<List<SwiperModel>>(it) } ?: emptyList()
        this.usefulList = SettingStore.accountObjectName(KEY_USEFUL)?.let { LocalObjectUtil.read<List<UsefulModel>>(it) } ?: emptyList()
        this.loadData()

        //进入首页时在后台刷新我的歌单,其他页面用到时已经是最新的
        FavoriteStore.loadInBackground()
        MusicStore.loadInBackground()
    }

    /** 从服务器加载数据 */
    fun loadData() {

        //登录、切换账号之后进入首页时刷新服务器配置
        ServerConfig.load()
        HomeApi.swiper().hide().send {
            this.swiperList = it
            SettingStore.accountObjectName(KEY_SWIPER)?.let { name -> LocalObjectUtil.write(it, name) }
        }
        HomeApi.useful().hide().finish {
            this.isRefreshing = false
        }.send {
            this.usefulList = it
            SettingStore.accountObjectName(KEY_USEFUL)?.let { name -> LocalObjectUtil.write(it, name) }
        }
    }

    /** 下拉刷新 */
    fun refresh() {
        this.isRefreshing = true
        this.loadData()
    }

    /** 轮播点击事件,推荐歌单在页面里跳转,这里处理其他类型 */
    fun onSwiperClick(item: SwiperModel, context: Context) {
        val data = item.data
        if (data.isNullOrEmpty()) {
            return
        }
        when (item.type) {
            2 -> {//推荐单曲
                val id = data.toLongOrNull() ?: return
                MusicApi.detail(id).send {
                    PlayerManager.prepareAndPlay(listOf(it), favoriteId = FavoriteId.NONE)
                    AppRoot.showPlaying()
                }
            }

            3 -> {//拨打电话
                runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$data".toUri())) }
            }

            4 -> {//跳转网页
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, data.toUri())) }
            }
        }
    }
}
