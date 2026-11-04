package com.music.violence.desktop

import com.music.violence.data.model.Song
import com.music.violence.data.profile.ProfileCard
import com.music.violence.data.share.IncomingLink
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopIncomingLinkHubTest {

    @BeforeTest
    @AfterTest
    fun cleanup() {
        DesktopIncomingLinkHub.dismiss()
        DesktopIncomingLinkHub.clearNotice()
    }

    @Test
    fun `party link is offered and accepted by the hub`() {
        val offered = DesktopIncomingLinkHub.offer("violence://party/AB12CD", DesktopLinkSource.ARGUMENT)
        assertTrue(offered)

        val pending = DesktopIncomingLinkHub.pending.value
        assertNotNull(pending)
        assertTrue(pending is IncomingLink.Party)
        assertEquals("AB12CD", pending.invite.code)

        DesktopIncomingLinkHub.handled()
        assertNull(DesktopIncomingLinkHub.pending.value)
    }

    @Test
    fun `song link is offered and accepted by the hub`() {
        val offered = DesktopIncomingLinkHub.offer("violence://song/yt/dQw4w9WgXcQ?v=1&t=Never%20Gonna%20Give%20You%20Up&a=Rick%20Astley", DesktopLinkSource.PASTE)
        assertTrue(offered)

        val pending = DesktopIncomingLinkHub.pending.value
        assertNotNull(pending)
        assertTrue(pending is IncomingLink.Song)
        assertEquals("dQw4w9WgXcQ", pending.ref.id)
        assertEquals("Never Gonna Give You Up", pending.ref.title)
        assertEquals("Rick Astley", pending.ref.artist)

        DesktopIncomingLinkHub.dismiss()
        assertNull(DesktopIncomingLinkHub.pending.value)
    }

    @Test
    fun `invalid link is rejected and sets last notice`() {
        val offered = DesktopIncomingLinkHub.offer("not-a-link", DesktopLinkSource.PASTE)
        assertFalse(offered)
        assertNull(DesktopIncomingLinkHub.pending.value)
        assertNotNull(DesktopIncomingLinkHub.lastNotice.value)
    }

    @Test
    fun `ShareItem helpers produce correct URIs and links`() {
        val song = Song(
            videoId = "dQw4w9WgXcQ",
            title = "Never Gonna Give You Up",
            artist = "Rick Astley",
            thumbnailUrl = null,
        )
        val songShare = ShareItem.of(song)
        assertNotNull(songShare)
        assertTrue(songShare.uri.startsWith("violence://song/yt/dQw4w9WgXcQ"))
        assertTrue(songShare.link.contains("dQw4w9WgXcQ"))

        val plShare = ShareItem.ofPlaylist("PL1234567890", "My Hits")
        assertNotNull(plShare)
        assertTrue(plShare.uri.startsWith("violence://playlist/yt/PL1234567890"))

        val jamShare = ShareItem.JamItem("XYZ789")
        assertEquals("violence://party/XYZ789", jamShare.uri)
        assertTrue(jamShare.link.contains("XYZ789"))

        val profileCard = ProfileCard(UUID.randomUUID().toString(), "Dave", "headphones", "fa2d48")
        val profileShare = ShareItem.ProfileItem(profileCard)
        assertTrue(profileShare.uri.startsWith("violence://profile?"))
    }
}
