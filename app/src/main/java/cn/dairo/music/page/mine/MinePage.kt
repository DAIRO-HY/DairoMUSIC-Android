package cn.dairo.music.page.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.dairo.music.components.AlertButton
import cn.dairo.music.components.AppAlert
import cn.dairo.music.components.ButtonRole
import cn.dairo.music.components.HomeTabView
import cn.dairo.music.components.OnPageAppear
import cn.dairo.music.components.TabPage
import cn.dairo.music.components.UserAvatar
import cn.dairo.music.extension.groupedBackground
import cn.dairo.music.extension.primaryText
import cn.dairo.music.extension.secondaryGroupedBackground
import cn.dairo.music.extension.secondaryText
import cn.dairo.music.extension.statusText
import cn.dairo.music.extension.theme
import cn.dairo.music.page.about.AboutPage
import cn.dairo.music.page.accountlist.AccountListPage
import cn.dairo.music.page.cache.CachePage
import cn.dairo.music.page.feedback.FeedbackPage
import cn.dairo.music.page.modifypwd.ModifyPwdPage
import cn.dairo.music.page.playing.wave.PlayingWaveStyle
import cn.dairo.music.page.profile.ProfilePage
import cn.dairo.music.page.vip.VipRedeemPage
import cn.dairo.music.player.SleepTimer
import cn.dairo.music.store.SettingStore
import cn.dairo.music.util.ServerConfig
import cn.dairo.music.util.rememberStateObject
import cn.dairo.ui.setting.LocalSettingNavigator
import cn.dairo.ui.setting.SettingButtonSingle
import cn.dairo.ui.setting.SettingCustomView
import cn.dairo.ui.setting.SettingGroup
import cn.dairo.ui.setting.SettingIcon
import cn.dairo.ui.setting.SettingNavigationLink
import cn.dairo.ui.setting.SettingPage
import cn.dairo.ui.setting.SettingPicker
import cn.dairo.ui.setting.SettingPickerData
import cn.dairo.ui.setting.SettingStack
import cn.dairo.ui.setting.SettingToggle
import cn.dairo.ui.setting.settingRowClickable
import cn.dairo.ui.theme.ThemeMode
import cn.dairo.ui.theme.rememberThemeMode
import kotlinx.coroutines.delay

/** 主题选择数据 */
private val THEME_DATA = listOf(
    SettingPickerData("跟随系统", ThemeMode.SYSTEM.value),
    SettingPickerData("明亮模式", ThemeMode.LIGHT.value),
    SettingPickerData("黑暗模式", ThemeMode.DARK.value),
)

/** 设置项的图标 */
private fun icon(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) = SettingIcon.System(icon, backgroundColor = color)

@Composable
fun MinePage() {
    val vm = rememberStateObject { MineViewModel() }
    val context = LocalContext.current
    val themeMode by rememberThemeMode()

    /** 显示自定义停止时间的选择 */
    var showSleepTimeCustom by remember { mutableStateOf(false) }

    /** 选择自定义之前的定时时长,取消选择时恢复 */
    var sleepMinutesBeforeCustom by remember { mutableIntStateOf(0) }

    /** 选项页面返回之后再弹出时间选择 */
    var pendingSleepTimeCustom by remember { mutableStateOf(false) }
    LaunchedEffect(pendingSleepTimeCustom) {
        if (pendingSleepTimeCustom) {
            delay(500)
            pendingSleepTimeCustom = false
            showSleepTimeCustom = true
        }
    }

    /** 显示退出登录确认框 */
    var showLogoutAlert by remember { mutableStateOf(false) }

    //音质选择数据,非VIP只能听标准音质,其他音质显示VIP标记
    val vipMark = if (ServerConfig.isVipLimited) "(VIP)" else ""
    val bitRateData = listOf(
        SettingPickerData("标准(128K)", SettingStore.BIT_RATE_STANDARD),
        SettingPickerData("超高品质(320K)$vipMark", 320),
        SettingPickerData("无损$vipMark", SettingStore.BIT_RATE_LOSSLESS),
    )

    SettingStack {
        OnPageAppear {
            vm.loadUser()
        }
        Column(Modifier.fillMaxSize().background(Color.groupedBackground)) {

            //底部的Tab栏已经留出了导航栏的高度,页面里不用再留
            Box(Modifier.weight(1f).consumeWindowInsets(WindowInsets.navigationBars)) {

                //页面没有上下边距,账号信息紧贴状态栏下面
                SettingPage(showsTopBar = false, verticalPadding = 0.dp) {
                    SettingCustomView {
                        TopLogoView(vm)
                    }
                    SettingGroup {
                        SettingNavigationLink(
                            "个人资料",
                            tip = if (vm.user?.isTemp == true) "未绑定邮箱" else "昵称、头像",
                            icon = icon(Icons.Default.AccountCircle, Color(0xFF007AFF)),
                        ) {
                            ProfilePage()
                        }

                        //VIP功能没有生效(后台没有开启等)时不显示
                        val vip = ServerConfig.vip
                        if (vip != null && vip.enable) {
                            SettingNavigationLink("VIP会员", tip = vip.statusText, icon = icon(Icons.Default.WorkspacePremium, Color(0xFFFFCC00))) {
                                VipRedeemPage()
                            }
                        }
                        if (vm.user?.isTemp == false) {
                            SettingNavigationLink("修改密码", icon = icon(Icons.Default.LockReset, Color(0xFFFF9500))) {
                                ModifyPwdPage()
                            }
                        }
                    }
                    SettingGroup(footer = sleepTimerFooter()) {
                        SettingPicker(
                            "定时停止播放",
                            data = SleepTimer.OPTIONS,
                            value = SleepTimer.minutes,
                            onValueChange = { SleepTimer.minutes = it },
                            icon = icon(Icons.Default.Bedtime, Color(0xFFAF52DE)),
                            shouldChange = {
                                if (it == SleepTimer.CUSTOM) {
                                    sleepMinutesBeforeCustom = SleepTimer.minutes
                                    pendingSleepTimeCustom = true
                                } else {
                                    SleepTimer.start(it)
                                }
                                true
                            },
                        )
                        if (SleepTimer.endDate != null) {
                            SettingToggle(title = "播完整首歌再停止", isOn = SleepTimer.isWaitSongEnd, onIsOnChange = { SleepTimer.isWaitSongEnd = it })
                        }
                    }
                    SettingGroup {

                        //非VIP只能听标准音质,显示标准;自己设置的音质保留,开通VIP之后恢复显示
                        SettingPicker(
                            "播放音质",
                            data = bitRateData,
                            value = if (ServerConfig.isVipLimited) SettingStore.BIT_RATE_STANDARD else vm.bitRate,
                            onValueChange = {
                                if (!ServerConfig.isVipLimited) {
                                    vm.bitRate = it
                                }
                            },
                            icon = icon(Icons.Default.GraphicEq, Color.theme),
                            shouldChange = vm::onBitRateChange,
                        )
                        SettingNavigationLink(
                            "播放页波形",
                            tip = PlayingWaveStyle.of(SettingStore.playingWaveStyle).label,
                            icon = icon(Icons.Default.Equalizer, Color(0xFFAF52DE)),
                        ) {
                            PlayingWaveSettingPage()
                        }
                        SettingNavigationLink("缓存管理", tip = "歌曲、视频和图片缓存", icon = icon(Icons.Default.Storage, Color(0xFF5856D6))) {
                            CachePage()
                        }
                        SettingPicker(
                            "切换主题",
                            data = THEME_DATA,
                            value = themeMode.value,
                            onValueChange = { ThemeMode.set(context, ThemeMode.of(it)) },
                            icon = icon(Icons.Default.Brush, Color(0xFFFF3B30)),
                        )
                    }
                    SettingGroup {
                        SettingNavigationLink("意见反馈", icon = icon(Icons.Default.EditNote, Color(0xFF34C759))) {
                            FeedbackPage()
                        }
                        SettingNavigationLink("关于", icon = icon(Icons.Default.Info, Color(0xFF8E8E93))) {
                            AboutPage()
                        }
                    }
                    SettingGroup {
                        SettingButtonSingle("退出登录") {
                            showLogoutAlert = true
                        }
                    }

                    //页面没有下边距,最后留出和分组之间一样的距离
                    SettingCustomView {
                        Spacer(Modifier.height(0.dp))
                    }
                }
            }
            HomeTabView(TabPage.MINE_PAGE)
        }
    }
    if (showSleepTimeCustom) {
        SleepTimeCustomPage { time ->
            showSleepTimeCustom = false
            if (time != null) {
                SleepTimer.start(time.first, time.second)
            } else {
                SleepTimer.minutes = sleepMinutesBeforeCustom
            }
        }
    }
    AppAlert(
        isPresented = showLogoutAlert,
        title = "确定退出登录吗？",
        onDismiss = { showLogoutAlert = false },
        buttons = listOf(
            AlertButton("退出", ButtonRole.DESTRUCTIVE, vm::onLogoutClick),
            AlertButton("取消", ButtonRole.CANCEL),
        ),
    )
}

/** 定时停止播放的说明 */
private fun sleepTimerFooter(): String? {
    if (SleepTimer.isWaitingSongEnd) {
        return "定时时间已到,当前歌曲播放结束之后停止"
    }
    val endDate = SleepTimer.endDate ?: return null
    return "将在" + SleepTimeCustomPage.format(endDate) + "停止播放"
}

/** 顶部头像部分,背景往上延伸到状态栏,没有圆角和左右边距 */
@Composable
private fun TopLogoView(vm: MineViewModel) {
    val navigator = LocalSettingNavigator.current
    Box(Modifier.fillMaxWidth().background(Color.secondaryGroupedBackground)) {
        Row(
            Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .settingRowClickable { navigator?.push { AccountListPage() } }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(vm.user?.logo, 50.dp)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        vm.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    val vip = ServerConfig.vip
                    if (vip?.enable == true && vip.isVip) {
                        Tag("VIP", Color(0xFFFF9500), bold = true)
                    }
                    if (vm.user?.isTemp == true && vm.displayName != "临时会员") {
                        Tag("临时会员", Color.secondaryText)
                    }
                }
                Text(vm.account?.domain ?: "", style = MaterialTheme.typography.bodySmall, color = Color.secondaryText, modifier = Modifier.padding(top = 2.dp))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.secondaryText)
        }
    }
}

/** 名字后面的标签 */
@Composable
private fun Tag(text: String, color: Color, bold: Boolean = false) {
    Text(
        text,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        color = color,
        modifier = Modifier
            .padding(start = 4.dp)
            .border(0.5.dp, color, RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp),
    )
}
