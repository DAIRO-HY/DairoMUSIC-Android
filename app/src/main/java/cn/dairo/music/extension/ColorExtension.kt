package cn.dairo.music.extension

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** 主题色,深色模式下也要看得清,不随系统变化 */
val Color.Companion.theme: Color
    get() = Color(0xFF029D94)

/** "我喜欢"的颜色 */
val Color.Companion.like: Color
    get() = Color(0xFFF24559)

/** 当前是否深色主题 */
val isDarkTheme: Boolean
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** 普通列表的背景色(对应iOS的systemBackground):浅色白色,深色黑色 */
val Color.Companion.listBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDarkTheme) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceContainer

/** 分组背景色(对应iOS的systemGroupedBackground) */
val Color.Companion.groupedBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

/** 分组里卡片的背景色(对应iOS的secondarySystemGroupedBackground) */
val Color.Companion.secondaryGroupedBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceContainer

/** 次要背景色(对应iOS的secondarySystemBackground),占位图等使用 */
val Color.Companion.secondaryBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)

/** 主要文字颜色(对应iOS的primary) */
val Color.Companion.primaryText: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurface

/** 次要文字颜色(对应iOS的secondary) */
val Color.Companion.secondaryText: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

/** 更淡的文字颜色(对应iOS的tertiaryLabel) */
val Color.Companion.tertiaryText: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

/** 分隔线颜色(对应iOS的separator) */
val Color.Companion.separator: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outlineVariant
