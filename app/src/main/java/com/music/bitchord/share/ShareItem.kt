package com.music.bitchord.share

import com.music.bitchord.data.listentogether.JamInviteLink
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.profile.ProfileCard
import com.music.bitchord.data.profile.ProfileCardCodec
import com.music.bitchord.data.share.SongLinks
import com.music.bitchord.data.share.SongRef

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
        fun of(song: Song): ShareItem? {
            val ref = SongLinks.youtube(song.videoId, song.title, song.artist)
                ?: SongLinks.search(song.title, song.artist)
                ?: return null
            return SongItem(ref)
        }
    }
}
