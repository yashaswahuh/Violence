package com.music.violence.playback

import android.content.Intent
import android.net.Uri
import com.music.violence.data.share.StreamingLink
import com.music.violence.data.share.StreamingService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A link from another music service reaches Violence as a request, from a tap or from Share. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class MusicLinkStreamingTest {

    private val spotifyId = "4cOdK2wGLETKBW3PvgPWqT"

    @Test fun aTappedSpotifyLinkIsAStreamingRequest() {
        val request = MusicLink.parse(Uri.parse("https://open.spotify.com/track/$spotifyId?si=abc"))
        val link = (request as LinkRequest.Streaming).link as StreamingLink.Track
        assertEquals(StreamingService.SPOTIFY, link.service)
        assertEquals(spotifyId, link.id)
    }

    @Test fun aTappedAppleMusicLinkIsAStreamingRequest() {
        val request = MusicLink.parse(Uri.parse("https://music.apple.com/in/album/discovery/697194953?i=697195787"))
        assertEquals("697195787", ((request as LinkRequest.Streaming).link as StreamingLink.Track).id)
    }

    @Test fun youtubeLinksAreStillTheirOwnRequests() {
        assertTrue(MusicLink.parse(Uri.parse("https://music.youtube.com/watch?v=dQw4w9WgXcQ")) is LinkRequest.Track)
        assertTrue(MusicLink.parse(Uri.parse("https://youtu.be/dQw4w9WgXcQ")) is LinkRequest.Track)
    }

    @Test fun otherAddressesAreNotRequests() {
        assertNull(MusicLink.parse(Uri.parse("https://example.com/track/1")))
    }

    @Test fun aLinkSharedAsTextIsReadWhateverSurroundsIt() {
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "Never Gonna Give You Up\nhttps://open.spotify.com/track/$spotifyId?si=xyz")
        assertTrue(MusicLink.consume(intent))
        val pending = MusicLink.pending.value
        assertTrue(pending is LinkRequest.Streaming)
        MusicLink.handled()
    }

    @Test fun aSharedSpotifyUriWithNoWebAddressIsRead() {
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "spotify:track:$spotifyId")
        assertTrue(MusicLink.consume(intent))
        assertTrue(MusicLink.pending.value is LinkRequest.Streaming)
        MusicLink.handled()
    }

    @Test fun aSharedYouTubeLinkIsStillRead() {
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "Song\nhttps://music.youtube.com/watch?v=dQw4w9WgXcQ")
        assertTrue(MusicLink.consume(intent))
        assertTrue(MusicLink.pending.value is LinkRequest.Track)
        MusicLink.handled()
    }
}
