package cn.dairo.music.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import cn.dairo.music.MainActivity

/**
 * 后台播放服务:通知栏、锁屏、耳机按键的播放控制,对应iOS的后台音频和MPRemoteCommandCenter
 * 播放器由PlayerManager创建和管理,这里只把它交给MediaSession
 */
@UnstableApi
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        //点击通知栏打开App
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        this.session = MediaSession.Builder(this, PlayerManager.sessionPlayer)
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return this.session
    }

    /** 从最近任务里划掉App时,没有在播放就停止服务 */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = PlayerManager.player
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            this.stopSelf()
        }
    }

    override fun onDestroy() {

        //播放器整个App共用,这里不释放
        this.session?.release()
        this.session = null
        super.onDestroy()
    }
}
