package cn.dairo.music.page.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import cn.dairo.music.api.model.FavoriteModel
import cn.dairo.music.api.model.SwiperModel
import cn.dairo.music.api.model.UsefulModel
import cn.dairo.music.components.HomeTabView
import cn.dairo.music.components.MusicImage
import cn.dairo.music.components.TabPage
import cn.dairo.music.components.liquidGlassBackground
import cn.dairo.music.extension.listBackground
import cn.dairo.music.extension.primaryText
import cn.dairo.music.extension.secondaryText
import cn.dairo.music.page.musiclist.MusicListPage
import cn.dairo.music.page.search.SearchPage
import cn.dairo.music.util.rememberStateObject
import cn.dairo.ui.setting.LocalSettingNavigator
import cn.dairo.ui.setting.SettingNavigator
import cn.dairo.ui.setting.SettingStack
import kotlinx.coroutines.delay

/** 表格显示时歌单的最小宽度,手机上每行3个,平板上根据屏幕宽度自动增加 */
private val GRID_ITEM_MIN_WIDTH = 100.dp

/** 轮播的最大宽度,避免平板上轮播图太高 */
private val SWIPER_MAX_WIDTH = 800.dp

/** 轮播自动切换的间隔(毫秒) */
private const val SWIPER_INTERVAL = 6000L

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun DiscoverPage() {
    val vm = rememberStateObject { DiscoverViewModel() }
    SettingStack {
        val navigator = LocalSettingNavigator.current
        Column(Modifier.fillMaxSize().background(Color.listBackground)) {
            PullToRefreshBox(
                isRefreshing = vm.isRefreshing,
                onRefresh = vm::refresh,
                modifier = Modifier.weight(1f).fillMaxWidth().statusBarsPadding(),
            ) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                    item {
                        SearchBar { navigator?.push { SearchPage() } }
                    }
                    if (vm.swiperList.isNotEmpty()) {
                        item {
                            SwiperView(vm, navigator)
                        }
                    }
                    items(vm.usefulList) { group ->
                        UsefulGroupView(group, navigator)
                    }
                }
            }
            HomeTabView(TabPage.DISCOVER_PAGE)
        }
    }
}

/** 搜索按钮 */
@Composable
private fun SearchBar(onClick: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 16.dp)
            .fillMaxWidth()
            .height(40.dp)
            .liquidGlassBackground(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = Color.secondaryText, modifier = Modifier.size(20.dp))
        Text("搜索歌曲、歌手", color = Color.secondaryText, modifier = Modifier.padding(start = 8.dp))
    }
}

/** 轮播 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun SwiperView(vm: DiscoverViewModel, navigator: SettingNavigator?) {
    val context = LocalContext.current
    val list = vm.swiperList
    val pagerState = rememberPagerState { list.size }

    //轮播自动切换
    LaunchedEffect(list.size) {
        while (list.size > 1) {
            delay(SWIPER_INTERVAL)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % list.size)
        }
    }
    Box(Modifier.fillMaxWidth().padding(bottom = 16.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.widthIn(max = SWIPER_MAX_WIDTH).fillMaxWidth().aspectRatio(2.5f)) {
            HorizontalPager(pagerState, Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp), pageSpacing = 16.dp) { i ->
                val item = list[i]
                MusicImage(
                    item.image,
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSwiperClick(item, vm, navigator, context) },
                    placeholder = Icons.Default.Photo,
                )
            }

            //页面指示
            if (list.size > 1) {
                Row(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    list.indices.forEach {
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = if (it == pagerState.currentPage) 1f else 0.4f)),
                        )
                    }
                }
            }
        }
    }
}

/** 轮播的点击事件,推荐歌单跳转到歌单页面,其他类型交给ViewModel处理 */
@androidx.annotation.OptIn(UnstableApi::class)
private fun onSwiperClick(item: SwiperModel, vm: DiscoverViewModel, navigator: SettingNavigator?, context: android.content.Context) {
    val id = item.data?.toLongOrNull()
    if (item.type == 1 && id != null) {
        navigator?.push { MusicListPage(favoriteId = id, name = item.name) }
        return
    }
    vm.onSwiperClick(item, context)
}

/** 推荐歌单分组 */
@Composable
private fun UsefulGroupView(group: UsefulModel, navigator: SettingNavigator?) {
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        val name = group.name
        if (!name.isNullOrEmpty()) {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.primaryText,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp),
            )
        }
        if (group.type == 2) {//横向滚动
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(group.favorites) {
                    FavoriteCard(it, Modifier.width(100.dp), navigator)
                }
            }
        } else {//表格显示
            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                val spacing = 12.dp
                val columns = maxOf(1, ((maxWidth + spacing) / (GRID_ITEM_MIN_WIDTH + spacing)).toInt())
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    group.favorites.chunked(columns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            row.forEach {
                                FavoriteCard(it, Modifier.weight(1f), navigator)
                            }
                            repeat(columns - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 推荐歌单 */
@Composable
private fun FavoriteCard(favorite: FavoriteModel, modifier: Modifier, navigator: SettingNavigator?) {
    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { navigator?.push { MusicListPage(favoriteId = favorite.id, name = favorite.name) } },
    ) {
        MusicImage(
            favorite.logo,
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(8.dp)),
            placeholder = Icons.AutoMirrored.Filled.QueueMusic,
        )
        Text(
            favorite.name,
            style = MaterialTheme.typography.bodySmall,
            color = Color.primaryText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
