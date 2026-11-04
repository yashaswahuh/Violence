package com.music.violence.data.share

import com.music.violence.data.profile.ProfileCard
import com.music.violence.data.profile.ProfileCardCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IncomingLinksTest {

    private val yt = "dQw4w9WgXcQ"
    private val card = ProfileCard("123e4567-e89b-42d3-a456-426614174000", "Alex", "guitar", "0a84ff")

    private fun invalid(text: String?, bases: List<String> = emptyList()): InvalidReason =
        assertIs<IncomingLink.Invalid>(IncomingLinks.parse(text, bases), "expected invalid for: $text").reason

    // ---------------------------------------------------------------- songs --

    @Test
    fun readsASong() {
        val link = IncomingLinks.parse("violence://song/yt/$yt?v=1&t=Song&a=Artist")
        assertEquals(IncomingLink.Song(SongRef(SongSource.YOUTUBE, yt, "Song", "Artist")), link)
        assertEquals("song:yt:$yt", link.key)
        val web = IncomingLinks.parse("https://yashaswahuh.is-a.dev/Violence/song/yt/$yt?v=1")
        assertEquals(IncomingLink.Song(SongRef(SongSource.YOUTUBE, yt)), web)
    }

    @Test
    fun songProblemsAreReportedByCategory() {
        assertEquals(InvalidReason.UNSUPPORTED_VERSION, invalid("violence://song/yt/$yt?v=2"))
        assertEquals(InvalidReason.MALFORMED, invalid("violence://song/yt/short?v=1"))
        assertEquals(InvalidReason.MALFORMED, invalid("violence://song/yt/$yt?v=1&t=%00"))
        assertEquals(InvalidReason.MALFORMED, invalid("violence://song/yt/$yt?v=1&x=1"))
    }

    // ------------------------------------------------------------- profiles --

    @Test
    fun readsAProfileCardInBothForms() {
        assertEquals(IncomingLink.Profile(card), IncomingLinks.parse(ProfileCardCodec.toUri(card)))
        assertEquals(IncomingLink.Profile(card), IncomingLinks.parse(ProfileCardCodec.toWebUrl(card)))
        assertEquals("profile:${card.id}", IncomingLinks.parse(ProfileCardCodec.toUri(card)).key)
    }

    @Test
    fun badProfileCards() {
        assertEquals(InvalidReason.UNSUPPORTED_VERSION, invalid("violence://profile?v=2&id=${card.id}&n=A&a=guitar&c=0a84ff"))
        assertEquals(InvalidReason.MALFORMED, invalid("violence://profile?v=1&id=nope&n=A&a=guitar&c=0a84ff"))
        assertEquals(InvalidReason.MALFORMED, invalid("violence://profile?v=1&id=${card.id}&n=A&a=unicorn&c=0a84ff"))
        assertEquals(InvalidReason.MALFORMED, invalid("violence://profile?v=1&id=${card.id}&n=A%00&a=guitar&c=0a84ff"))
    }

    // ---------------------------------------------------------------- parties --

    @Test
    fun readsAPartyInvite() {
        assertEquals(IncomingLink.Party(PartyInvite("AB12CD")), IncomingLinks.parse("violence://party/AB12CD"))
        assertEquals(IncomingLink.Party(PartyInvite("AB12CD")), IncomingLinks.parse("violence://party/ab12cd"))
        assertEquals(IncomingLink.Party(PartyInvite("AB12CD")), IncomingLinks.parse("bitchord://party/AB12CD"))
        assertEquals(IncomingLink.Party(PartyInvite("AB12CD")), IncomingLinks.parse("violence://party?code=ab12cd"))
        assertEquals(IncomingLink.Party(PartyInvite("AB12CD")), IncomingLinks.parse("https://yashaswahuh.is-a.dev/Violence/party/AB12CD"))
        assertEquals(IncomingLink.Party(PartyInvite("AB12CD")), IncomingLinks.parse("https://yashaswahuh.is-a.dev/Violence/invite/AB12CD/"))
        assertEquals("party:AB12CD", IncomingLinks.parse("violence://party/AB12CD").key)
    }

    @Test
    fun partyCodesAreNormalisedLikeTheServerDoes() {
        // I and L read as 1, O as 0; anything outside the alphabet or the wrong length is refused.
        assertEquals("110012", (IncomingLinks.parse("violence://party/ILO012") as IncomingLink.Party).invite.code)
        assertEquals(InvalidReason.MALFORMED, invalid("violence://party/ABC12"))
        assertEquals(InvalidReason.MALFORMED, invalid("violence://party/ABC1234"))
        assertEquals(InvalidReason.MALFORMED, invalid("violence://party/AB!2CD"))
        assertEquals(InvalidReason.MALFORMED, invalid("violence://party/"))
        assertEquals(InvalidReason.MALFORMED, invalid("violence://party/AB12CD/extra"))
        assertNull(PartyLinks.normalizeCode("AB12C"))
    }

    @Test
    fun partyServerIsHttpsOnlyAndOtherwiseIgnored() {
        fun server(q: String) = (IncomingLinks.parse("violence://party/AB12CD$q") as IncomingLink.Party).invite.serverUrl
        assertEquals("https://jam.example.com", server("?server=https%3A%2F%2Fjam.example.com"))
        assertEquals("https://jam.example.com:8443", server("?server=https%3A%2F%2Fjam.example.com%3A8443%2F"))
        assertNull(server("?server=http%3A%2F%2Fjam.example.com"))
        assertNull(server("?server=https%3A%2F%2Fuser%3Apass%40jam.example.com"))
        assertNull(server("?server=https%3A%2F%2Fjam.example.com%2Fpath"))
        assertNull(server("?server=javascript%3Aalert(1)"))
        assertNull(server("?server=https%3A%2F%2F"))
        assertNull(server("?server=" + "a".repeat(300)))
    }

    @Test
    fun extraWebBasesOnlyCarryPartyCodes() {
        val bases = listOf("https://friends.example.org/jam/")
        assertEquals(IncomingLink.Party(PartyInvite("AB12CD")), IncomingLinks.parse("https://friends.example.org/jam/party/AB12CD", bases))
        assertEquals(InvalidReason.NOT_A_VIOLENCE_LINK, invalid("https://friends.example.org/jam/party/AB12CD"))
        assertEquals(InvalidReason.NOT_A_VIOLENCE_LINK, invalid("https://friends.example.org/jam/song/yt/$yt?v=1", bases))
        assertEquals(InvalidReason.NOT_A_VIOLENCE_LINK, invalid("http://friends.example.org/jam/party/AB12CD", bases))
    }

    // ---------------------------------------------------------- hostile input --

    @Test
    fun otherThingsAreNotViolenceLinks() {
        for (text in listOf(
            "hello", "https://example.org", "http://yashaswahuh.is-a.dev/Violence/party/AB12CD",
            "javascript:alert(1)", "intent://x#Intent;scheme=violence;end", "file:///etc/passwd",
            "violence://evil@song/yt/$yt?v=1", "violence:song/yt/$yt?v=1", "tel:123", "content://x/y",
            "https://yashaswahuh.is-a.dev.evil.example/Violence/song/yt/$yt?v=1",
            "https://yashaswahuh.is-a.dev/violence/party/AB12CD".replace("violence", "other"),
        )) {
            assertEquals(InvalidReason.NOT_A_VIOLENCE_LINK, invalid(text), text)
        }
    }

    @Test
    fun emptyAndOversizedInput() {
        assertEquals(InvalidReason.EMPTY, invalid(null))
        assertEquals(InvalidReason.EMPTY, invalid("   "))
        assertEquals(InvalidReason.TOO_LONG, invalid("violence://song/yt/$yt?v=1&t=" + "a".repeat(600)))
        assertEquals(InvalidReason.TOO_LONG, invalid("x".repeat(513)))
    }

    @Test
    fun invalidResultsNeverEchoTheInput() {
        val secret = "hunter2-should-not-appear"
        val result = IncomingLinks.parse("violence://song/yt/$yt?v=1&t=$secret%00")
        assertFalse(result.toString().contains(secret))
        assertNull(result.key)
    }

    // ------------------------------------------------------------------ paste --

    @Test
    fun extractsALinkFromAChatMessage() {
        val message = "Check this out!\nviolence://song/yt/$yt?v=1&t=Song, it is great."
        assertTrue(IncomingLinks.extract(message) is IncomingLink.Song)
        assertTrue(IncomingLinks.extract("(<violence://party/AB12CD>)") is IncomingLink.Party)
        assertTrue(IncomingLinks.extract("https://example.org/a violence://party/AB12CD") is IncomingLink.Party)
    }

    @Test
    fun extractKeepsTheMostUsefulReason() {
        assertEquals(InvalidReason.UNSUPPORTED_VERSION, (IncomingLinks.extract("see https://example.org and violence://song/yt/$yt?v=9") as IncomingLink.Invalid).reason)
        assertEquals(InvalidReason.NOT_A_VIOLENCE_LINK, (IncomingLinks.extract("just words") as IncomingLink.Invalid).reason)
        assertEquals(InvalidReason.EMPTY, (IncomingLinks.extract("  ") as IncomingLink.Invalid).reason)
    }
}
