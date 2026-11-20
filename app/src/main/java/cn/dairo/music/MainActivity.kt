package cn.dairo.music

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import cn.dairo.music.components.AppRootView
import cn.dairo.music.player.PlaybackService
import cn.dairo.music.util.OrientationUtil
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class MainActivity : ComponentActivity() {

    /** 连接后台播放服务,连接之后播放时才能显示通知栏和锁屏的播放控制 */
    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        OrientationUtil.attach(this)
        setContent {
            AppRootView()
        }
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        this.controllerFuture = MediaController.Builder(this, token).buildAsync()
    }

    override fun onStop() {
        this.controllerFuture?.let { MediaController.releaseFuture(it) }
        this.controllerFuture = null
        super.onStop()
    }
}
