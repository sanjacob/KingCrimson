package ch.jsan.crimson

import android.app.Notification
import android.media.MediaMetadata as MediaMetadata1
import android.os.Build
import android.os.Bundle

import android.media.session.MediaSession as MediaSession1
import android.media.session.MediaController as MediaController1
import androidx.media3.session.SessionToken
import androidx.media3.session.MediaController

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat
class SkipService : NotificationListenerService() {
    override fun onListenerConnected() {}
    override fun onListenerDisconnected() {}

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        if (isAd(sbn.notification.extras)) {
            skip(sbn.notification.extras)
        }
    }

    private fun isAd(extras: Bundle): Boolean {
        val title = extras.getString("android.title", "")
        val text = extras.getString("android.text", "")
        val subText = extras.getString("android.subText")

        println(title)
        println(text)

        if (subText != "m.youtube.com") return false
        return text.startsWith("Video ad upload channel for") || text.startsWith("YouTube Ads")
    }

    private fun skip(extras: Bundle) : Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val mediaSessionToken = extras.getParcelable(
                Notification.EXTRA_MEDIA_SESSION,
                SessionToken::class.java) ?: return false

            val controllerFuture = MediaController.Builder(this, mediaSessionToken).buildAsync()

            controllerFuture.addListener({
                val controller = controllerFuture.get()

                if (controller.isPlayingAd) {
                    println("PLAYING AD")
                } else {
                    println("NOT PLAYING AD")
                }

                controller.seekTo(controller.duration - 1)
            }, ContextCompat.getMainExecutor(this))
        } else {
            val mediaSessionToken = extras.getParcelable(
                Notification.EXTRA_MEDIA_SESSION) as MediaSession1.Token? ?: return false

            val controller = MediaController1(this, mediaSessionToken)

            val duration = controller.metadata?.getLong(MediaMetadata1.METADATA_KEY_DURATION) ?: 0
            controller.transportControls.seekTo(duration - 1)
        }

        return true
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}

