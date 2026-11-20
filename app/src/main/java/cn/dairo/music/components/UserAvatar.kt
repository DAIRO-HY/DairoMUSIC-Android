package cn.dairo.music.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import cn.dairo.music.R

/**
 * 用户头像,没有头像时显示App图标
 */
@Composable
fun UserAvatar(logo: String?, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape)) {
        if (!logo.isNullOrEmpty()) {
            MusicImage(logo, Modifier.fillMaxSize(), placeholder = Icons.Default.Person)
        } else {
            Image(painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.fillMaxSize())
        }
    }
}
