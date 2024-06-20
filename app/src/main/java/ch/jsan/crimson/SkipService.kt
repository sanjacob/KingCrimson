package ch.jsan.crimson

import android.app.Notification
import android.os.Build
import android.os.Bundle

import android.media.session.MediaSession as MediaSession1
import androidx.media3.session.SessionToken
import androidx.media3.session.MediaController

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

private const val MIN_POSITION = 1000;
private const val FORWARD_GAP = 200;

class SkipService : NotificationListenerService() {
    private val blacklist = listOf<Long>(6041, 14021, 20021, 30061);

    override fun onListenerConnected() {}
    override fun onListenerDisconnected() {}
    override fun onNotificationRemoved(sbn: StatusBarNotification) {}

    @OptIn(UnstableApi::class) override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || !isYouTube(sbn.notification.extras)) return
        skip(sbn.notification.extras)
    }

    fun isAdDuration(n: Long) : Boolean {
        return blacklist.contains(n)
    }

    private fun isYouTube(extras: Bundle) : Boolean {
        val subText = extras.getString("android.subText")
        return subText == "m.youtube.com"
    }

    private fun isAdMetadata(extras: Bundle): Boolean {
        val title = extras.getString("android.title", "").lowercase()
        val text = extras.getString("android.text", "").lowercase()

        return title == "video ad" ||
                text.startsWith("video ad upload channel for") ||
                text.startsWith("youtube ads")
    }

    @OptIn(UnstableApi::class) private fun skip(extras: Bundle) {
        val mediaSessionToken = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession1.Token::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(Notification.EXTRA_MEDIA_SESSION)
        }

        if (mediaSessionToken == null) return

        val tokenFuture: ListenableFuture<SessionToken> = SessionToken.createSessionToken(this, mediaSessionToken)
        Futures.addCallback(tokenFuture, object : FutureCallback<SessionToken> {
            override fun onSuccess(token: SessionToken) {
                val controllerFuture = MediaController.Builder(baseContext, token).buildAsync()
                controllerFuture.addListener({
                    val controller = controllerFuture.get()

                    println(controller.duration)
                    if (controller.currentPosition < MIN_POSITION) {
                        if (isAdDuration(controller.duration) || isAdMetadata(extras)) {
                            println("FAST FORWARDING")
                            // fast-forward
                            controller.seekTo(controller.duration - FORWARD_GAP)
                        }
                    }

                }, ContextCompat.getMainExecutor(baseContext))
            }

            override fun onFailure(t: Throwable) {
                println("Could not resolve media3 session token")
            }
        },
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                mainExecutor
            } else {
                ContextCompat.getMainExecutor(this)
            }
        )
    }
}

