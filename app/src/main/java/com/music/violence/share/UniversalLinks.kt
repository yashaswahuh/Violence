package com.music.violence.share

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.music.violence.data.model.Song
import com.music.violence.data.share.StreamingLink
import com.music.violence.data.share.StreamingTracks
import com.music.violence.data.spotify.SpotifyImporter
import com.music.violence.data.spotify.SpotifyTrack

/**
 * Universal links (experimental): a song link from Apple Music, Spotify or Deezer, tapped anywhere on the
 * phone, opens in Violence and plays that song.
 *
 * Two halves. [resolve] is the part that works on any link that reaches the app, from a tap or from Share >
 * Violence: it asks the service what the song is called, finds the same one on YouTube Music and hands
 * it back. The switch ([isEnabled] and [setEnabled]) decides whether Violence is *offered* those links when
 * they are tapped: the web-address filters live on an activity alias that is switched off until the
 * person turns this on, so Violence does not appear in the "Open with" list for them before that.
 *
 * Android 12 and up only hand a web address to an app the person has approved for it, because Violence
 * cannot verify the music services' domains: [approval] reads how many are approved, and
 * [openApprovalSettings] opens the system page where they are ticked.
 */
object UniversalLinks {

    /** The alias in the manifest that carries the music services' web-address filters. */
    private const val ALIAS = "MusicLinksAlias"

    /** The addresses the alias declares, for counting how many the person has approved. */
    val HOSTS: List<String> = listOf(
        "music.apple.com",
        "open.spotify.com",
        "spotify.link",
        "www.deezer.com",
        "deezer.com",
        "link.deezer.com",
        "deezer.page.link",
    )

    private fun component(context: Context) = ComponentName(context.packageName, "com.music.violence.$ALIAS")

    fun isEnabled(context: Context): Boolean =
        context.packageManager.getComponentEnabledSetting(component(context)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    fun setEnabled(context: Context, on: Boolean) {
        context.packageManager.setComponentEnabledSetting(
            component(context),
            if (on) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }

    /** How many of [HOSTS] the system lets Violence open: (approved, of how many), or null before Android 12. */
    fun approval(context: Context): Pair<Int, Int>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return runCatching {
            val manager = context.getSystemService(android.content.pm.verify.domain.DomainVerificationManager::class.java)
            val state = manager?.getDomainVerificationUserState(context.packageName) ?: return null
            val ours = state.hostToStateMap.filterKeys { it in HOSTS }
            val approved = ours.count {
                it.value == android.content.pm.verify.domain.DomainVerificationUserState.DOMAIN_STATE_SELECTED ||
                    it.value == android.content.pm.verify.domain.DomainVerificationUserState.DOMAIN_STATE_VERIFIED
            }
            approved to ours.size
        }.getOrNull()
    }

    /** The system page where the person ticks which web addresses open in this app, or null before Android 12. */
    fun approvalSettingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** What a link came to. */
    sealed interface Found {
        /** The same song on YouTube Music: play it. */
        data class Song(val song: com.music.violence.data.model.Song) : Found

        /** Not matched as a song, or an album: show these results for the person to pick from. */
        data class Search(val query: String) : Found

        /** A kind of link this does not open yet (a playlist, an artist). */
        data object Unsupported : Found

        /** The service did not say what it is: gone, private, or no network. */
        data object Nothing : Found
    }

    /** Works out what [link] points at and finds it on YouTube Music. Network, so off the main thread. */
    suspend fun resolve(link: StreamingLink): Found {
        val target = if (link is StreamingLink.Short) StreamingTracks.follow(link) ?: return Found.Nothing else link
        return when (target) {
            is StreamingLink.Track -> {
                val info = StreamingTracks.track(target) ?: return Found.Nothing
                val song: Song? = SpotifyImporter.matchTrack(
                    SpotifyTrack(
                        id = target.id,
                        title = info.title,
                        artist = info.artist,
                        album = info.album,
                        durationMs = (info.durationMs ?: 0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                        imageUrl = null,
                    ),
                )
                song?.let { Found.Song(it) } ?: Found.Search("${info.title} ${info.artist}".trim())
            }
            is StreamingLink.Album -> StreamingTracks.album(target)
                ?.let { Found.Search("${it.title} ${it.artist}".trim()) } ?: Found.Nothing
            is StreamingLink.Playlist, is StreamingLink.Artist -> Found.Unsupported
            is StreamingLink.Short -> Found.Nothing
        }
    }
}
