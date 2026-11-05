package com.music.violence.data.listentogether

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JamLanProtocolTest {

    @Test
    fun testReactionEncodingAndDecodingRoundTrip() {
        val original = JamReaction(
            partyHash = JamReactionProtocol.computePartyHash("VIOL-1234"),
            emoji = JamEmoji.FIRE,
            timestampMs = 1730000000000L,
        )

        val encoded = JamReactionProtocol.encode(original)
        assertEquals(JamReactionProtocol.FRAME_LENGTH, encoded.size)
        assertEquals(JamReactionProtocol.MAGIC_BYTE_0, encoded[0])
        assertEquals(JamReactionProtocol.MAGIC_BYTE_1, encoded[1])

        val decoded = JamReactionProtocol.decode(encoded)
        assertNotNull(decoded)
        assertEquals(original.partyHash, decoded.partyHash)
        assertEquals(JamEmoji.FIRE, decoded.emoji)
        assertEquals(original.timestampMs, decoded.timestampMs)
    }

    @Test
    fun testAllEmojisEncodeAndDecode() {
        val hash = JamReactionProtocol.computePartyHash("TEST-CODE")
        for (emoji in JamEmoji.entries) {
            val reaction = JamReaction(hash, emoji, 123456789L)
            val bytes = JamReactionProtocol.encode(reaction)
            val decoded = JamReactionProtocol.decode(bytes)
            assertNotNull(decoded)
            assertEquals(emoji, decoded.emoji)
        }
    }

    @Test
    fun testInvalidReactionFrameRejected() {
        // Less than 15 bytes
        val shortFrame = ByteArray(14)
        assertNull(JamReactionProtocol.decode(shortFrame))

        // Wrong magic
        val wrongMagic = ByteArray(15) { 0 }
        wrongMagic[0] = 0x55 // Not 'V'
        wrongMagic[1] = 0x4C // 'L'
        assertNull(JamReactionProtocol.decode(wrongMagic))

        // Unknown emoji ID
        val validEncoded = JamReactionProtocol.encode(
            JamReaction(123, JamEmoji.HEART, 1000L)
        )
        validEncoded[6] = 99 // Invalid emoji ID
        assertNull(JamReactionProtocol.decode(validEncoded))
    }

    @Test
    fun testPartyHashConsistency() {
        val hash1 = JamReactionProtocol.computePartyHash("party-xyz")
        val hash2 = JamReactionProtocol.computePartyHash("  PARTY-XYZ  ")
        assertEquals(hash1, hash2, "Party hash must be case and whitespace insensitive")
    }

    @Test
    fun testAnnouncementEncodingAndDecoding() {
        val jam = DiscoveredJam(
            partyCode = "PARTY-99",
            hostName = "Yashaswa",
            currentTrack = "Never Gonna Give You Up - Rick Astley",
            port = 42421,
        )

        val encoded = JamDiscoveryProtocol.encodeAnnouncement(jam)
        assertTrue(encoded.size > 2)
        assertEquals(JamDiscoveryProtocol.ANNOUNCEMENT_MAGIC_0, encoded[0])
        assertEquals(JamDiscoveryProtocol.ANNOUNCEMENT_MAGIC_1, encoded[1])

        val decoded = JamDiscoveryProtocol.decodeAnnouncement(encoded, encoded.size, sourceAddress = "192.168.1.50")
        assertNotNull(decoded)
        assertEquals(jam.partyCode, decoded.partyCode)
        assertEquals(jam.hostName, decoded.hostName)
        assertEquals(jam.currentTrack, decoded.currentTrack)
        assertEquals(42421, decoded.port)
        assertEquals("192.168.1.50", decoded.address)
    }

    @Test
    fun testMdnsPacketQueryBuild() {
        val queryBytes = MdnsPacket.buildQuery()
        assertTrue(queryBytes.isNotEmpty())
        // Transaction ID is 0
        assertEquals(0, queryBytes[0])
        assertEquals(0, queryBytes[1])
        // Flags: 0
        assertEquals(0, queryBytes[2])
        assertEquals(0, queryBytes[3])
    }

    @Test
    fun testMdnsResponseBuildAndParse() {
        val jam = DiscoveredJam(
            partyCode = "JAM-42",
            hostName = "Alice",
            currentTrack = "Subwoofer Lullaby - C418",
            port = 42421,
        )

        val responseBytes = MdnsPacket.buildResponse(jam)
        val parsed = MdnsPacket.parseResponse(responseBytes, responseBytes.size, "192.168.1.200")

        assertNotNull(parsed)
        assertEquals("JAM-42", parsed.partyCode)
        assertEquals("Alice", parsed.hostName)
        assertEquals("Subwoofer Lullaby - C418", parsed.currentTrack)
        assertEquals(42421, parsed.port)
        assertEquals("192.168.1.200", parsed.address)
    }

    @Test
    fun testTxtRecordParsing() {
        val txtMap = mapOf(
            JamDiscoveryProtocol.KEY_PARTY_CODE to "ROOM101",
            JamDiscoveryProtocol.KEY_HOST_NAME to "Bob",
            JamDiscoveryProtocol.KEY_CURRENT_TRACK to "Track A - Artist B",
        )
        val baos = java.io.ByteArrayOutputStream()
        for ((k, v) in txtMap) {
            val item = "$k=$v".toByteArray(Charsets.UTF_8)
            baos.write(item.size)
            baos.write(item)
        }
        val rdata = baos.toByteArray()
        val parsed = MdnsPacket.parseTxtRdata(rdata)
        assertEquals("ROOM101", parsed[JamDiscoveryProtocol.KEY_PARTY_CODE])
        assertEquals("Bob", parsed[JamDiscoveryProtocol.KEY_HOST_NAME])
        assertEquals("Track A - Artist B", parsed[JamDiscoveryProtocol.KEY_CURRENT_TRACK])
    }

    @Test
    fun testEmojiFromIdOverloads() {
        assertEquals(JamEmoji.FIRE, JamEmoji.fromId(1))
        assertEquals(JamEmoji.HEART, JamEmoji.fromId(2))
        assertEquals(JamEmoji.SKULL, JamEmoji.fromId(3))
        assertEquals(JamEmoji.MUSIC, JamEmoji.fromId(4))
        assertEquals(JamEmoji.CLAP, JamEmoji.fromId(5))
        assertNull(JamEmoji.fromId(99))
    }

    @Test
    fun testMulticastChannelDeduplication() {
        val channel = JamMulticastChannel()
        channel.setActiveParty("ROOM-ABC")
        val hash = JamReactionProtocol.computePartyHash("ROOM-ABC")
        val now = 1730000000000L

        val r1 = JamReaction(hash, JamEmoji.FIRE, now)
        val r2 = JamReaction(hash, JamEmoji.FIRE, now + 10) // duplicate within 100ms
        val r3 = JamReaction(hash, JamEmoji.FIRE, now + 500) // distinct > 100ms
        val rDifferentEmoji = JamReaction(hash, JamEmoji.HEART, now + 10) // different emoji, not duplicate

        assertTrue(channel.recordReaction(r1))
        kotlin.test.assertFalse(channel.recordReaction(r2), "Duplicate within 100ms must be rejected")
        assertTrue(channel.recordReaction(rDifferentEmoji), "Different emoji must be accepted")
        assertTrue(channel.recordReaction(r3), "Subsequent reaction after 500ms must be accepted")
    }
}
