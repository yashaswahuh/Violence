package com.music.bitchord.share

import com.music.bitchord.data.listentogether.JamInviteLink
import com.music.bitchord.data.share.IncomingLink
import com.music.bitchord.data.share.SongSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IncomingLinkHubTest {

    private companion object {
        /** The hub is a process-wide object: every test starts well after the previous one's windows. */
        var base = 1_000_000_000L
    }

    private var now = 0L

    @Before
    fun setUp() {
        base += 10_000_000L
        now = base
        IncomingLinkHub.clock = { now }
        IncomingLinkHub.handled()
        JamInviteLink.handled()
    }

    @After
    fun tearDown() {
        IncomingLinkHub.handled()
        JamInviteLink.handled()
        IncomingLinkHub.clock = System::currentTimeMillis
    }

    private fun song(id: String) = "violence://song/yt/$id?v=1&t=Song"

    @Test
    fun aSongWaitsForTheUserAndIsNeverActedOn() {
        assertTrue(IncomingLinkHub.offer(song("aaaaaaaaaaa"), LinkSource.NFC))
        val pending = IncomingLinkHub.pending.value
        assertTrue(pending is IncomingLink.Song)
        assertEquals(SongSource.YOUTUBE, (pending as IncomingLink.Song).ref.source)
        assertNull("a Jam invite was not triggered", JamInviteLink.pending.value)
    }

    @Test
    fun theSameLinkReadTwiceIsOneConfirmation() {
        assertTrue(IncomingLinkHub.offer(song("bbbbbbbbbbb"), LinkSource.NFC))
        now += 500
        assertFalse(IncomingLinkHub.offer(song("bbbbbbbbbbb"), LinkSource.NFC))
        now += 5_000
        assertFalse("still touching: the window keeps extending", IncomingLinkHub.offer(song("bbbbbbbbbbb"), LinkSource.NFC))
        assertNotNull(IncomingLinkHub.pending.value)
    }

    @Test
    fun theSameSongWithDifferentHintsIsTheSameSong() {
        assertTrue(IncomingLinkHub.offer("violence://song/yt/ccccccccccc?v=1&t=One", LinkSource.QR))
        assertFalse(IncomingLinkHub.offer("violence://song/yt/ccccccccccc?v=1&t=Other&a=X", LinkSource.QR))
    }

    @Test
    fun dismissingLetsTheUserTapAgainAtOnce() {
        assertTrue(IncomingLinkHub.offer(song("ddddddddddd"), LinkSource.NFC))
        IncomingLinkHub.dismiss()
        assertNull(IncomingLinkHub.pending.value)
        now += 300
        assertTrue(IncomingLinkHub.offer(song("ddddddddddd"), LinkSource.NFC))
    }

    @Test
    fun decidingKeepsTheLinkRemembered() {
        assertTrue(IncomingLinkHub.offer(song("eeeeeeeeeee"), LinkSource.NFC))
        IncomingLinkHub.handled()
        now += 300
        assertFalse("a re-read straight after acting is ignored", IncomingLinkHub.offer(song("eeeeeeeeeee"), LinkSource.NFC))
        assertNull(IncomingLinkHub.pending.value)
    }

    @Test
    fun aProfileWaitsForConfirmation() {
        val link = "violence://profile?v=1&id=123e4567-e89b-42d3-a456-426614174000&n=Alex&a=guitar&c=0a84ff"
        assertTrue(IncomingLinkHub.offer(link, LinkSource.QR))
        val pending = IncomingLinkHub.pending.value as IncomingLink.Profile
        assertEquals("Alex", pending.card.displayName)
    }

    @Test
    fun aJamInviteGoesToTheExistingJoinFlowNotTheDialog() {
        assertTrue(IncomingLinkHub.offer("violence://party/AB12CD", LinkSource.NFC))
        assertNull(IncomingLinkHub.pending.value)
        assertEquals("AB12CD", JamInviteLink.pending.value?.code)
        now += 100
        JamInviteLink.handled()
        assertFalse("the same code again is ignored", IncomingLinkHub.offer("violence://party/ab12cd", LinkSource.NFC))
        assertNull(JamInviteLink.pending.value)
    }

    @Test
    fun aCustomServerTravelsWithTheInvite() {
        assertTrue(IncomingLinkHub.offer("violence://party/ZZ99YY?server=https%3A%2F%2Fjam.example.com", LinkSource.QR))
        assertEquals("https://jam.example.com", JamInviteLink.pending.value?.serverUrl)
    }

    @Test
    fun floodsAreCappedAtFiveDifferentLinks() {
        val results = (0 until 8).map { IncomingLinkHub.offer(song("fffffffff0$it"), LinkSource.NFC) }
        assertEquals(5, results.count { it })
        assertEquals(listOf(true, true, true, true, true, false, false, false), results)
    }

    @Test
    fun invalidLinksAreRefusedWithAnoticeAndNothingElse() {
        val notices = mutableListOf<LinkNotice>()
        runBlocking {
            val job = launch(Dispatchers.Unconfined) { IncomingLinkHub.notices.collect { notices.add(it) } }
            assertFalse(IncomingLinkHub.offer("hello", LinkSource.PASTE))
            assertFalse(IncomingLinkHub.offer("violence://song/yt/short?v=1", LinkSource.NFC))
            assertFalse(IncomingLinkHub.offer("violence://song/yt/ggggggggggg?v=9", LinkSource.QR))
            assertFalse(IncomingLinkHub.offer("   ", LinkSource.PASTE))
            assertFalse(IncomingLinkHub.offer("http://yashaswahuh.is-a.dev/Violence/party/AB12CD", LinkSource.NFC))
            job.cancel()
        }
        assertEquals(
            listOf(LinkNotice.NOT_A_VIOLENCE_LINK, LinkNotice.MALFORMED, LinkNotice.UNSUPPORTED_VERSION, LinkNotice.EMPTY, LinkNotice.NOT_A_VIOLENCE_LINK),
            notices,
        )
        assertNull(IncomingLinkHub.pending.value)
        assertNull(JamInviteLink.pending.value)
    }

    @Test
    fun aForeignLinkFromAnotherAppIsNotAnError() {
        val notices = mutableListOf<LinkNotice>()
        runBlocking {
            val job = launch(Dispatchers.Unconfined) { IncomingLinkHub.notices.collect { notices.add(it) } }
            assertFalse(IncomingLinkHub.offer("https://music.youtube.com/watch?v=dQw4w9WgXcQ", LinkSource.INTENT))
            job.cancel()
        }
        assertTrue("a YouTube link tapped elsewhere is handled by another filter, quietly", notices.isEmpty())
    }

    @Test
    fun aPastedMessageCanHoldTheLink() {
        assertTrue(IncomingLinkHub.offer("look at this ${song("hhhhhhhhhhh")} it is great", LinkSource.PASTE, extract = true))
        assertTrue(IncomingLinkHub.pending.value is IncomingLink.Song)
    }
}
