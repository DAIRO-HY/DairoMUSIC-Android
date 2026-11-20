package cn.dairo.music.page.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import cn.dairo.music.api.model.MusicModel
import cn.dairo.music.components.AppRoot
import cn.dairo.music.components.MiniPlayerBar
import cn.dairo.music.components.MusicItemView
import cn.dairo.music.components.NavigationPage
import cn.dairo.music.components.liquidGlassBackground
import cn.dairo.music.components.rememberSyncedTextFieldState
import cn.dairo.music.constants.FavoriteId
import cn.dairo.music.extension.isSameMusic
import cn.dairo.music.extension.primaryText
import cn.dairo.music.extension.secondaryText
import cn.dairo.music.page.musicinfo.MusicInfoSheet
import cn.dairo.music.player.PlayerManager
import cn.dairo.music.util.rememberStateObject
import cn.dairo.ui.setting.settingColors

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun SearchPage() {
    val vm = rememberStateObject { SearchViewModel() }

    /** 要显示详情的歌曲 */
    var infoMusic by remember { mutableStateOf<MusicModel?>(null) }

    /** 输入框焦点 */
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        if (!vm.isSearched) {
            runCatching { focusRequester.requestFocus() }
        }
    }
    val onMusicClick = { list: List<MusicModel>, i: Int ->
        focusManager.clearFocus()
        vm.play(list, i)

        //点击歌曲之后打开播放页面
        AppRoot.showPlaying()
    }
    NavigationPage(title = "搜索") {
        SearchBar(vm, focusRequester)
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            if (vm.isSearched) {
                resultView(vm, onMusicClick) { infoMusic = it }
            } else {
                historyView(vm) {
                    focusManager.clearFocus()
                    vm.onHistoryClick(it)
                }
            }
        }
        MiniPlayerBar(Modifier.navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 4.dp))
    }
    infoMusic?.let {
        MusicInfoSheet(it, favoriteId = null) { infoMusic = null }
    }
}

/** 搜索框 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun SearchBar(vm: SearchViewModel, focusRequester: FocusRequester) {
    val textStyle = LocalTextStyle.current.copy(color = Color.primaryText)
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .height(40.dp)
            .liquidGlassBackground(RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = Color.secondaryText, modifier = Modifier.size(20.dp))
        BasicTextField(
            state = rememberSyncedTextFieldState(vm.key, vm::onKeyChange),
            lineLimits = TextFieldLineLimits.SingleLine,
            textStyle = textStyle,
            cursorBrush = SolidColor(settingColors.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            onKeyboardAction = { vm.search() },
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp).focusRequester(focusRequester),
            decorator = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (vm.key.isEmpty()) {
                        Text("搜索歌曲、歌手", style = textStyle.copy(color = Color.secondaryText))
                    }
                    inner()
                }
            },
        )
        if (vm.key.isNotEmpty()) {
            Icon(
                Icons.Default.Cancel,
                contentDescription = "清除",
                tint = Color.secondaryText,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable {
                        vm.onKeyChange("")
                        runCatching { focusRequester.requestFocus() }
                    },
            )
        }
    }
}

/** 搜索历史 */
@androidx.annotation.OptIn(UnstableApi::class)
private fun LazyListScope.historyView(vm: SearchViewModel, onClick: (String) -> Unit) {
    if (vm.history.isEmpty()) {
        return
    }
    item {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("搜索历史", style = MaterialTheme.typography.bodyMedium, color = Color.secondaryText, modifier = Modifier.weight(1f))
            Text(
                "清空",
                style = MaterialTheme.typography.bodyMedium,
                color = settingColors.accent,
                modifier = Modifier.clickable(onClick = vm::onClearHistoryClick).padding(4.dp),
            )
        }
    }
    itemsIndexed(vm.history) { _, key ->
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onClick(key) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Default.History, contentDescription = null, tint = settingColors.accent, modifier = Modifier.size(20.dp))
            Text(key, color = Color.primaryText)
        }
        HorizontalDivider(Modifier.padding(start = 48.dp))
    }
}

/** 搜索结果 */
@androidx.annotation.OptIn(UnstableApi::class)
private fun LazyListScope.resultView(vm: SearchViewModel, onMusicClick: (List<MusicModel>, Int) -> Unit, onMoreClick: (MusicModel) -> Unit) {
    item {
        SectionHeader("曲库")
    }
    if (vm.musicList.isEmpty()) {
        item {
            Text("曲库里没有找到", color = Color.secondaryText, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        }
    }
    musicRows(vm.musicList, onMusicClick, onMoreClick)

    //服务器没有开启采集时返回空列表,有结果时才显示采集平台,没有开启采集的服务器上看不到采集相关的文字
    if (vm.isCollectSearching) {
        item {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("正在搜索更多结果", color = Color.secondaryText, modifier = Modifier.padding(start = 12.dp))
            }
        }
    } else if (vm.collectList.isNotEmpty()) {
        item {
            SectionHeader("采集平台")
        }
        musicRows(vm.collectList, onMusicClick, onMoreClick)
        item {
            Text(
                "采集平台的歌曲加入歌单之后会自动保存到曲库",
                style = MaterialTheme.typography.bodySmall,
                color = Color.secondaryText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

/** 分组标题 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.secondaryText,
        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 4.dp),
    )
}

/** 歌曲列表 */
@androidx.annotation.OptIn(UnstableApi::class)
private fun LazyListScope.musicRows(list: List<MusicModel>, onMusicClick: (List<MusicModel>, Int) -> Unit, onMoreClick: (MusicModel) -> Unit) {
    itemsIndexed(list) { i, music ->
        MusicItemView(
            music,
            isPlaying = isPlaying(music),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onMusicClick(list, i) }
                .padding(horizontal = 16.dp),
        ) {
            onMoreClick(music)
        }
        HorizontalDivider(Modifier.padding(start = 74.dp))
    }
}

/** 是否正在播放的歌曲 */
@androidx.annotation.OptIn(UnstableApi::class)
private fun isPlaying(music: MusicModel): Boolean {
    if (PlayerManager.favoriteId != FavoriteId.SEARCH) {
        return false
    }
    val playing = PlayerManager.music ?: return false
    return playing.isSameMusic(music)
}
