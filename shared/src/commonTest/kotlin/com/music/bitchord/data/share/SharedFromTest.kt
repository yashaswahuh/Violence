package com.music.bitchord.data.share

import com.music.bitchord.data.profile.ProfileCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedFromTest {

    private val card = ProfileCard("123e4567-e89b-42d3-a456-426614174000", "Alex", "headphones", "ff8800")
    private val yt = "dQw4w9WgXcQ"

    @Test
    fun songLinkCarriesTheSenderAndFits() {
        val ref = SongLinks.youtube(yt, "Never Gonna Give You Up", "Rick Astley")!!.copy(from = card)
        val uri = SongLinks.toUri(ref)
        assertTrue(uri.encodeToByteArray().size <= SongLinks.TARGET_BYTES)
        val back = assertIs<SongLinks.Result.Ok>(SongLinks.parse(uri)).ref
        assertEquals(card, back.from)
        assertEquals(yt, back.id)
    }

    @Test
    fun theSenderIsDroppedBeforeTheLinkOverflows() {
        val long = "x".repeat(100)
        val ref = SongLinks.search(long, long)!!.copy(from = card)
        val uri = SongLinks.toUri(ref, 120)
        assertTrue(uri.encodeToByteArray().size <= 120)
    }

    @Test
    fun aHalfSenderOrBadSenderIsRejectedInASongLink() {
        val base = "violence://song/yt/$yt?v=1"
        assertEquals(
            SongLinks.Error.BAD_FROM,
            assertIs<SongLinks.Result.Rejected>(SongLinks.parse("$base&f=123e4567-e89b-42d3-a456-426614174000&fn=Alex")).error,
        )
        assertEquals(
            SongLinks.Error.BAD_FROM,
            assertIs<SongLinks.Result.Rejected>(SongLinks.parse("$base&f=nope&fn=Alex&fa=headphones&fc=ff8800")).error,
        )
    }

    @Test
    fun aJamInviteIgnoresADamagedSender() {
        val good = PartyLinks.parse("violence://party/ABC234?" + SharedFrom.query(card).removePrefix("&"))
        assertEquals(card, good?.from)
        val bad = PartyLinks.parse("violence://party/ABC234?f=nope&fn=Alex&fa=headphones&fc=ff8800")
        assertEquals("ABC234", bad?.code)
        assertNull(bad?.from)
    }
}
