package com.music.bitchord

import com.music.bitchord.data.listentogether.JamInviteLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * What the join-by-code field does with whatever lands in it: a typed code, a pasted invite link,
 * or a whole pasted share message. The field used to filter a pasted link down to its first six
 * letters ("HTTPSY").
 */
class JamInviteCodeExtractionTest {

    @Test
    fun `a bare code in any case`() {
        assertEquals("ABC123", JamInviteLink.extractCode("ABC123"))
        assertEquals("ABC123", JamInviteLink.extractCode("abc123"))
        assertEquals("ABC123", JamInviteLink.extractCode("  abc123 "))
    }

    @Test
    fun `a code typed with spaces or hyphens`() {
        assertEquals("ABC123", JamInviteLink.extractCode("ABC 123"))
        assertEquals("ABC123", JamInviteLink.extractCode("abc-123"))
    }

    @Test
    fun `a pasted web link`() {
        assertEquals("K7M2PQ", JamInviteLink.extractCode("https://yashaswahuh.is-a.dev/Violence/party/K7M2PQ"))
        assertEquals("K7M2PQ", JamInviteLink.extractCode("https://yashaswahuh.is-a.dev/Violence/party/k7m2pq?server=https%3A%2F%2Fjam.example.com"))
        assertEquals("K7M2PQ", JamInviteLink.extractCode("https://yashaswahuh.is-a.dev/Violence/invite/K7M2PQ"))
    }

    @Test
    fun `a pasted app link`() {
        assertEquals("K7M2PQ", JamInviteLink.extractCode("violence://party/K7M2PQ"))
        assertEquals("K7M2PQ", JamInviteLink.extractCode("violence://party/K7M2PQ?server=https%3A%2F%2Fjam.example.com"))
    }

    @Test
    fun `a whole share message that holds a link`() {
        val message = "https://yashaswahuh.is-a.dev/Violence/party/K7M2PQ\n\nListen with me on Violence - party code K7M2PQ"
        assertEquals("K7M2PQ", JamInviteLink.extractCode(message))
    }

    @Test
    fun `a share message with only the code in it`() {
        assertEquals("K7M2PQ", JamInviteLink.extractCode("Listen with me on Violence - party code K7M2PQ"))
    }

    @Test
    fun `ordinary words are not mistaken for a code`() {
        // "invite" and "listen" contain I and L, which codes never use.
        assertNull(JamInviteLink.extractCode("please invite me to listen"))
        assertNull(JamInviteLink.extractCode("hello there"))
    }

    @Test
    fun `two candidates are ambiguous so nothing is guessed`() {
        assertNull(JamInviteLink.extractCode("try K7M2PQ or X9Y8Z7 maybe"))
    }

    @Test
    fun `empty and partial input yield nothing`() {
        assertNull(JamInviteLink.extractCode(null))
        assertNull(JamInviteLink.extractCode(""))
        assertNull(JamInviteLink.extractCode("   "))
        assertNull(JamInviteLink.extractCode("AB12"))
        assertNull(JamInviteLink.extractCode("ABC1234"))
    }
}
