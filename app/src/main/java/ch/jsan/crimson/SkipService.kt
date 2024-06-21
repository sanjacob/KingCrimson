package ch.jsan.crimson

import android.app.Notification
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import android.media.session.MediaSession as MediaSession1

private const val FORWARD_GAP = 100
private const val TAG = "SkipService"

/**
 * Skip YouTube web ads by listening to media notifications
 *
 * YouTube ads play out as normal videos, which means they have media controls and
 * can be fast-forwarded/skipped even manually, this service automates the process
 *
 * The ad detection is based on two heuristics:
 * 1. Video title and channel
 * 2. Video duration
 */
class SkipService : NotificationListenerService() {
    // Common short ad durations
    private val blacklist = listOf<Long>(6041, 14021, 20021, 30061)

    override fun onListenerConnected() {}
    override fun onListenerDisconnected() {}
    override fun onNotificationRemoved(sbn: StatusBarNotification) {}

    private fun isYouTube(extras: Bundle) : Boolean {
        return extras.getString("android.subText") == "m.youtube.com"
    }

    /**
     * Attempt to match the video duration to some common ad durations
     */
    private fun isAdDuration(duration: Long) : Boolean {
        return blacklist.contains(duration)
    }

    /**
     * Attempt to match the video title or channel to those used for ads
     */
    private fun isAdMetadata(metadata: MediaMetadata): Boolean {
        val text = metadata.artist.toString().lowercase()
        val title = metadata.title.toString().lowercase()

        return title == "video ad" ||
                text.startsWith("video ad upload channel for") ||
                text.startsWith("youtube ads")
    }

    private fun isAd(controller: MediaController): Boolean {
        return isAdDuration(controller.duration) || isAdMetadata(controller.mediaMetadata)
    }

    /**
     * Listen to incoming notifications, obtain their media controller and skip if needed
     */
    @OptIn(UnstableApi::class) override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn != null && isYouTube(sbn.notification.extras))
            findController(sbn.notification) { controller ->
                Log.d(TAG, "Checking video: " + controller.duration)

                if (isAd(controller)) {
                    // Avoid getting stuck in a loop
                    if (controller.currentPosition < (controller.duration - FORWARD_GAP)) {
                        Log.d(TAG, "FAST FORWARDING")

                        // fast-forward
                        controller.seekTo(controller.duration - FORWARD_GAP)
                    }
                }
            }
    }

    /**
     * Extract token from notification and attempt connection to media controller
     */
    @OptIn(UnstableApi::class) private fun findController(notification: Notification, onSuccess: (MediaController) -> Unit) {
        val mediaSessionToken = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notification.extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession1.Token::class.java)
        } else {
            @Suppress("DEPRECATION")
            notification.extras.getParcelable(Notification.EXTRA_MEDIA_SESSION)
        }

        if (mediaSessionToken == null) return

        val tokenFuture = SessionToken.createSessionToken(this, mediaSessionToken)

        Futures.addCallback(tokenFuture, object : FutureCallback<SessionToken> {
            override fun onSuccess(token: SessionToken) {
                val controllerFuture = MediaController.Builder(baseContext, token).buildAsync()

                Futures.addCallback(controllerFuture, object: FutureCallback<MediaController> {
                    override fun onSuccess(controller: MediaController) {
                        onSuccess(controller)
                    }

                    override fun onFailure(t: Throwable) {
                        Log.w(TAG, "Could not connect to the media3 session")
                    }
                },
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                        mainExecutor
                    } else {
                        ContextCompat.getMainExecutor(baseContext)
                    }
                )
            }

            override fun onFailure(t: Throwable) {
                Log.w(TAG, "Could not create media3 session token")
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