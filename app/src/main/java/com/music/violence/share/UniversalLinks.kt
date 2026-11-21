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
import com.music.violence.data.YtMusicRepository
import com.music.violence.data.model.SearchFilter
import com.music.violence.data.model.SearchResult
import com.music.violence.data.sources.TrackMatcher

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

    /**
     * Each address Violence declares for music links, with a link that stands for it. The sample is what Android is
     * asked about: "who would open this?" tells which app owns the address.
     */
    internal val SAMPLES: Map<String, String> = linkedMapOf(
        "music.apple.com" to "https://music.apple.com/us/song/harder-better-faster-stronger/697195787",
        "open.spotify.com" to "https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT",
        "spotify.link" to "https://spotify.link/AbCdEf123",
        "www.deezer.com" to "https://www.deezer.com/track/3135556",
        "deezer.com" to "https://deezer.com/track/3135556",
        "link.deezer.com" to "https://link.deezer.com/s/abc",
        "deezer.page.link" to "https://deezer.page.link/abc",
        // The YouTube ones Violence has always declared, which the same rule applies to.
        "music.youtube.com" to "https://music.youtube.com/watch?v=dQw4w9WgXcQ",
        "youtu.be" to "https://youtu.be/dQw4w9WgXcQ",
    )

    /** The addresses, for counting how many the person has approved. */
    val HOSTS: List<String> get() = SAMPLES.keys.toList()

    /**
     * An app that Android opens an address in instead of Violence. Android lets one app own a web address: the app
     * that verified it (Apple Music, Spotify and YouTube Music have). The owner keeps it, and the address cannot be
     * handed to Violence from Violence's own page (its box is grey there, "Opens in Apple Music"), until the owner
     * lets go: its own "Open supported links" setting off.
     */
    data class Blocker(val packageName: String, val label: String, val hosts: List<String>)

    /** The apps that would open these addresses in place of Violence right now, one entry each. */
    fun blockers(context: Context): List<Blocker> {
        val pm = context.packageManager
        fun ownerOf(url: String): String? = runCatching {
            val view = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
            pm.resolveActivity(view, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        }.getOrNull()
        // A browser, or the chooser itself, means no app owns the address: nothing is in the way.
        val browser = ownerOf("https://example.com/")
        val byPackage = LinkedHashMap<String, MutableList<String>>()
        for ((host, sample) in SAMPLES) {
            val owner = ownerOf(sample) ?: continue
            if (owner == context.packageName || owner == "android" || owner == browser) continue
            byPackage.getOrPut(owner) { mutableListOf() } += host
        }
        return byPackage.map { (pkg, hosts) ->
            val label = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
            Blocker(pkg, label, hosts)
        }
    }

    /** That app's own "Open by default" page, where "Open supported links" is turned off; null before Android 12. */
    fun linkSettingsOf(context: Context, packageName: String): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

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

    /**
     * The song among [candidates] that is [title] by [artist], or null when none is confidently it: the same song
     * at about the same length, by the same artist, not a different version of it. Violence's own matcher
     * does the judging.
     */
    internal fun pickSong(candidates: List<Song>, title: String, artist: String, album: String?, durationMs: Long?): Song? =
        TrackMatcher.best(
            candidates,
            TrackMatcher.Target(
                title = title,
                artist = artist,
                durationSec = durationMs?.takeIf { it > 0 }?.div(1000)?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt(),
                album = album,
            ),
        )

    /** Works out what [link] points at and finds it on YouTube Music. Network, so off the main thread. */
    suspend fun resolve(link: StreamingLink): Found {
        val target = if (link is StreamingLink.Short) StreamingTracks.follow(link) ?: return Found.Nothing else link
        return when (target) {
            is StreamingLink.Track -> {
                val info = StreamingTracks.track(target) ?: return Found.Nothing
                val query = "${info.title} ${info.artist}".trim()
                val candidates = YtMusicRepository.search(query, SearchFilter.SONGS).getOrNull()
                    ?.filterIsInstance<SearchResult.Track>()?.map { it.song }.orEmpty()
                // Only a confident match is played. The search's first hit is often a remix, a live take or a
                // cover, and playing that when the link named the original is worse than showing the results.
                val song: Song? = pickSong(candidates, info.title, info.artist, info.album, info.durationMs)
                song?.let { Found.Song(it) } ?: Found.Search(query)
            }
            is StreamingLink.Album -> StreamingTracks.album(target)
                ?.let { Found.Search("${it.title} ${it.artist}".trim()) } ?: Found.Nothing
            is StreamingLink.Playlist, is StreamingLink.Artist -> Found.Unsupported
            is StreamingLink.Short -> Found.Nothing
        }
    }
}
