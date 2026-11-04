package com.music.violence.data.share

import com.music.violence.data.profile.ProfileCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PlaylistLinksTest {

    private val card = ProfileCard("123e4567-e89b-42d3-a456-426614174000", "Alex", "headphones", "ff8800")
    private val plId = "PLrAXtmErZgOeiKm4sgNOknGvNjby9efdf"
    private val ids = List(30) { i -> "dQw4w9WgX" + (10 + i) }

    private fun ok(text: String): PlaylistRef =
        assertIs<PlaylistLinks.Result.Ok>(PlaylistLinks.parse(text), "expected a playlist for: $text").ref

    @Test
    fun youtubePlaylistRoundTrips() {
        val ref = PlaylistLinks.youtube("VL$plId", "Road trip", card)!!
        val back = ok(PlaylistLinks.toUri(ref))
        assertEquals(PlaylistRef.Kind.YOUTUBE, back.kind)
        assertEquals(plId, back.id)
        assertEquals("Road trip", back.title)
        assertEquals(card, back.from)
        assertEquals(ref.key, back.key)
    }

    @Test
    fun aLongPhonePlaylistIsCutToFitAndStillParses() {
        val ref = PlaylistLinks.songs("Gym mix", ids, card)!!
        val uri = PlaylistLinks.toUri(ref)
        assertTrue(uri.encodeToByteArray().size <= PlaylistLinks.TARGET_BYTES)
        val back = ok(uri)
        assertEquals(PlaylistRef.Kind.SONGS, back.kind)
        assertTrue(back.videoIds.isNotEmpty() && back.videoIds.size < ids.size)
        assertEquals(ids.take(back.videoIds.size), back.videoIds)
    }

    @Test
    fun badLinksAreRejected() {
        val base = "violence://playlist/songs?v=1&t=Mix"
        listOf(
            "violence://playlist/yt/short?v=1&t=Mix",
            "$base",
            "$base&s=nope",
            "$base&s=dQw4w9WgXcQ&x=1",
            "violence://playlist/yt/$plId?v=1&t=Mix&s=dQw4w9WgXcQ",
            "violence://playlist/yt/$plId?v=1&t=Mix&f=bad&fn=Alex&fa=headphones&fc=ff8800",
        ).forEach { assertIs<PlaylistLinks.Result.Rejected>(PlaylistLinks.parse(it), it) }
        val wrong = assertIs<PlaylistLinks.Result.Rejected>(PlaylistLinks.parse("violence://playlist/yt/$plId?v=2&t=Mix"))
        assertTrue(wrong.wrongVersion)
    }

    @Test
    fun theHubReadsPlaylistLinks() {
        val link = IncomingLinks.parse(PlaylistLinks.toUri(PlaylistLinks.youtube(plId, "Mix")!!))
        assertIs<IncomingLink.Playlist>(link)
    }
}
