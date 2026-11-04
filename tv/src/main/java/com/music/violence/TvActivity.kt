package com.music.violence

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.music.violence.data.listentogether.JamInviteLink
import com.music.violence.share.IncomingLinkHub
import com.music.violence.playback.MusicLink
import com.music.violence.playback.PlayerDeepLink
import com.music.violence.playback.rememberMediaController
import com.music.violence.playback.rememberPlayerState
import com.music.violence.ui.MainViewModel
import com.music.violence.ui.theme.ViolenceTheme
import com.music.violence.ui.tv.TvApp
import com.music.violence.ui.tv.display.TvRefreshRateController
import com.music.violence.ui.tv.theme.ViolenceTvTheme

/**
 * Dedicated TV Activity for Android TV, Google TV, and Fire TV devices.
 * Launched via `android.intent.category.LEANBACK_LAUNCHER`.
 */
class TvActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Attach TV Refresh Rate and Display Controller
        TvRefreshRateController.attach(this)

        // An invite that *started* the app (a cold launch from violence://party/<CODE>). The phone does
        // this in its onCreate; here only onNewIntent did, so a link opened on a TV that was not
        // already running was dropped.
        IncomingLinkHub.consume(intent)

        // Make window full edge-to-edge for 1080p/4K television screens
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        setContent {
            ViolenceTvTheme {
                // The phone's Material theme too, so the components shared with
                // it (lyrics, mood sleeves) are set in SF Pro on its type scale.
                ViolenceTheme(darkTheme = true) {
                    val mediaController = rememberMediaController()
                    val playerState = rememberPlayerState(mediaController)

                    TvApp(
                        viewModel = viewModel,
                        mediaController = mediaController,
                        playerState = playerState,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeLinks(intent)
    }

    private fun consumeLinks(intent: Intent) {
        PlayerDeepLink.consume(intent)
        IncomingLinkHub.consume(intent)
        MusicLink.consume(intent)
    }

    override fun onDestroy() {
        // Closing the app (back out of Home, or the task being swiped away)
        // pauses the music rather than leaving it playing behind the launcher.
        if (isFinishing) {
            runCatching {
                val future = androidx.media3.session.MediaController.Builder(
                    applicationContext,
                    androidx.media3.session.SessionToken(
                        applicationContext,
                        android.content.ComponentName(
                            applicationContext,
                            com.music.violence.playback.PlaybackService::class.java,
                        ),
                    ),
                ).buildAsync()
                future.addListener({
                    runCatching { future.get().run { pause(); release() } }
                }, androidx.core.content.ContextCompat.getMainExecutor(applicationContext))
            }
        }
        super.onDestroy()
        TvRefreshRateController.detach()
    }
}
