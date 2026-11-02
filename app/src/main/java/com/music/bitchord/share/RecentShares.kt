package com.music.bitchord.share

import android.content.Context
import android.content.SharedPreferences
import com.music.bitchord.data.profile.ProfileCardCodec
import com.music.bitchord.data.share.IncomingLink
import com.music.bitchord.data.share.IncomingLinks
import com.music.bitchord.data.share.PlaylistLinks
import com.music.bitchord.data.share.SongLinks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** One thing sent or received by tap, QR or link. */
data class RecentShare(
    val sent: Boolean,
    /** `song`, `playlist` or `profile`. */
    val kind: String,
    val title: String,
    val subtitle: String?,
    val at: Long,
    /** The `violence://` link, so the entry can be opened again the way a pasted link is. */
    val uri: String,
)

/**
 * The last few things shared or received, kept on this device only (never uploaded) so a card
 * dismissed by accident is not lost. A Jam invite is not kept: its code is gone with the party.
 * Cleared from the Recent page.
 */
object RecentShares {

    private const val PREFS = "violence_recent_shares"
    private const val KEY = "items"
    private const val MAX = 20

    private var prefs: SharedPreferences? = null
    private val _items = MutableStateFlow<List<RecentShare>>(emptyList())
    val items: StateFlow<List<RecentShare>> = _items.asStateFlow()

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _items.value = runCatching {
            val array = JSONArray(p.getString(KEY, "[]"))
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                RecentShare(
                    sent = o.getBoolean("sent"),
                    kind = o.getString("kind"),
                    title = o.getString("title"),
                    subtitle = o.optString("sub").takeIf { it.isNotEmpty() },
                    at = o.getLong("at"),
                    uri = o.getString("uri"),
                )
            }
        }.getOrDefault(emptyList())
    }

    /** Remembers [link] as received. Jam invites and invalid links are ignored. */
    fun received(link: IncomingLink, now: Long = System.currentTimeMillis()) = record(link, sent = false, now = now)

    /** Remembers the item behind [uri] (a link this phone put on the air) as sent. */
    fun sent(uri: String, now: Long = System.currentTimeMillis()) = record(IncomingLinks.parse(uri), sent = true, now = now)

    private fun record(link: IncomingLink, sent: Boolean, now: Long) {
        val entry = when (link) {
            is IncomingLink.Song -> RecentShare(
                sent, "song", link.ref.title ?: return, link.ref.artist, now, SongLinks.toUri(link.ref.copy(from = null)),
            )
            is IncomingLink.Playlist -> RecentShare(
                sent, "playlist", link.ref.title, null, now, PlaylistLinks.toUri(link.ref.copy(from = null)),
            )
            is IncomingLink.Profile -> RecentShare(
                sent, "profile", link.card.displayName, null, now, ProfileCardCodec.toUri(link.card),
            )
            else -> return
        }
        // The same item moves to the top instead of repeating.
        val next = (listOf(entry) + _items.value.filterNot { it.uri == entry.uri && it.sent == entry.sent }).take(MAX)
        _items.value = next
        persist(next)
    }

    fun clear() {
        _items.value = emptyList()
        persist(emptyList())
    }

    private fun persist(list: List<RecentShare>) {
        val target = prefs ?: return
        val array = JSONArray()
        list.forEach {
            array.put(
                JSONObject()
                    .put("sent", it.sent).put("kind", it.kind).put("title", it.title)
                    .put("sub", it.subtitle ?: "").put("at", it.at).put("uri", it.uri),
            )
        }
        target.edit().putString(KEY, array.toString()).apply()
    }
}
