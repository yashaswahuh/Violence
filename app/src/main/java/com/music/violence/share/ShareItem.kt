package com.music.violence.share

import com.music.violence.data.listentogether.JamInviteLink
import com.music.violence.data.model.Song
import com.music.violence.data.profile.ProfileCard
import com.music.violence.data.profile.ProfileCardCodec
import com.music.violence.data.profile.ProfileManager
import com.music.violence.data.share.PlaylistLinks
import com.music.violence.data.share.PlaylistRef
import com.music.violence.data.share.SongLinks
import com.music.violence.data.share.SongRef

/**
 * One thing that can be shared, and the forms its link takes:
 *  - [uri]: the `violence://` form, what goes on an NFC tag (opens the app directly);
 *  - [webUrl]: the https landing page, what a QR code, "copy link" and the system share sheet
 *    carry (works on any phone's camera app and opens the app once App Links are verified).
 * Only the item's own link is ever included: no account, token or device information.
 */
sealed interface ShareItem {
    val uri: String
    val webUrl: String?

    /** What to put in a QR code, copy, or hand to another app. */
    val link: String get() = webUrl ?: uri

    data class SongItem(val ref: SongRef) : ShareItem {
        override val uri: String = SongLinks.toUri(ref)
        override val webUrl: String = SongLinks.toWebUrl(ref)
    }

    /** A playlist: the https landing page (like a song's) for QR and links, `violence://` for a tap. */
    data class PlaylistItem(val ref: PlaylistRef) : ShareItem {
        override val uri: String = PlaylistLinks.toUri(ref)
        override val webUrl: String = PlaylistLinks.toWebUrl(ref)
    }

    data class ProfileItem(val card: ProfileCard) : ShareItem {
        override val uri: String = ProfileCardCodec.toUri(card)
        override val webUrl: String = ProfileCardCodec.toWebUrl(card)
    }

    data class JamItem(val code: String, val customServer: String? = null) : ShareItem {
        override val uri: String = JamInviteLink.schemeUrl(code, customServer)

        /** Null when no web base is configured: then the link shared is the `violence://` one. */
        override val webUrl: String? = JamInviteLink.webLink(code, customServer)
    }

    companion object {
        /**
         * The share item for [song]: its YouTube id when it has a real one, otherwise a
         * search by title and artist, or null if there is nothing to go on.
         */
        /** A YouTube Music playlist by id (`PL...` or `VL...`), or null if [browseId] is not one. */
        fun ofPlaylist(browseId: String, title: String, from: ProfileCard? = ProfileManager.current()?.toCard()): ShareItem? =
            PlaylistLinks.youtube(browseId, title, from)?.let(::PlaylistItem)

        /** A playlist kept on this phone: its first songs that have a real YouTube id. */
        fun ofPlaylist(title: String, songs: List<Song>, from: ProfileCard? = ProfileManager.current()?.toCard()): ShareItem? =
            PlaylistLinks.songs(title, songs.map { it.videoId }, from)?.let(::PlaylistItem)

        fun of(song: Song, from: ProfileCard? = ProfileManager.current()?.toCard()): ShareItem? {
            val ref = SongLinks.youtube(song.videoId, song.title, song.artist)
                ?: SongLinks.search(song.title, song.artist)
                ?: return null
            return SongItem(ref.copy(from = from))
        }
    }
}
