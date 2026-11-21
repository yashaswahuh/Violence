package com.music.violence.data.share

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** Which links from other music services Violence reads, and as what. */
class StreamingLinksTest {

    private val spotifyId = "4cOdK2wGLETKBW3PvgPWqT"

    // ---- Apple Music ------------------------------------------------------------------------

    @Test fun appleSongPage() {
        val link = StreamingLinks.parse("https://music.apple.com/us/song/harder-better-faster-stronger/697195787")
        val track = assertIs<StreamingLink.Track>(link)
        assertEquals(StreamingService.APPLE_MUSIC, track.service)
        assertEquals("697195787", track.id)
        assertEquals("us", track.storefront)
    }

    @Test fun appleSongSharedFromAnAlbumPageNamesTheSongInI() {
        val link = StreamingLinks.parse("https://music.apple.com/in/album/discovery/697194953?i=697195787&uo=4")
        val track = assertIs<StreamingLink.Track>(link)
        assertEquals("697195787", track.id)
        assertEquals("in", track.storefront)
    }

    @Test fun appleAlbumWithoutASongIsAnAlbum() {
        val album = assertIs<StreamingLink.Album>(StreamingLinks.parse("https://music.apple.com/gb/album/discovery/697194953"))
        assertEquals("697194953", album.id)
    }

    @Test fun applePlaylistAndArtist() {
        assertIs<StreamingLink.Playlist>(StreamingLinks.parse("https://music.apple.com/us/playlist/todays-hits/pl.f4d106fed2bd41149aaacabb233eb5eb"))
        assertIs<StreamingLink.Artist>(StreamingLinks.parse("https://music.apple.com/us/artist/daft-punk/5468295"))
    }

    @Test fun appleLinkWithoutACountryStillReads() {
        val track = assertIs<StreamingLink.Track>(StreamingLinks.parse("https://music.apple.com/song/x/697195787"))
        assertNull(track.storefront)
    }

    @Test fun anAppleLinkToTheHomePageIsNothing() {
        assertNull(StreamingLinks.parse("https://music.apple.com/us/browse"))
        assertNull(StreamingLinks.parse("https://music.apple.com/"))
    }

    // ---- Spotify ----------------------------------------------------------------------------

    @Test fun spotifyTrackWithTrackingParameters() {
        val track = assertIs<StreamingLink.Track>(StreamingLinks.parse("https://open.spotify.com/track/$spotifyId?si=abc123def&utm_source=copy-link"))
        assertEquals(StreamingService.SPOTIFY, track.service)
        assertEquals(spotifyId, track.id)
    }

    @Test fun spotifyTrackWithAnIntlPrefix() {
        val track = assertIs<StreamingLink.Track>(StreamingLinks.parse("https://open.spotify.com/intl-de/track/$spotifyId"))
        assertEquals(spotifyId, track.id)
    }

    @Test fun spotifyEmbedAndUriForms() {
        assertIs<StreamingLink.Track>(StreamingLinks.parse("https://open.spotify.com/embed/track/$spotifyId"))
        assertEquals(spotifyId, assertIs<StreamingLink.Track>(StreamingLinks.parse("spotify:track:$spotifyId")).id)
    }

    @Test fun spotifyAlbumPlaylistArtist() {
        assertIs<StreamingLink.Album>(StreamingLinks.parse("https://open.spotify.com/album/$spotifyId"))
        assertIs<StreamingLink.Playlist>(StreamingLinks.parse("https://open.spotify.com/playlist/$spotifyId"))
        assertIs<StreamingLink.Artist>(StreamingLinks.parse("https://open.spotify.com/artist/$spotifyId"))
        assertIs<StreamingLink.Playlist>(StreamingLinks.parse("https://open.spotify.com/user/someone/playlist/$spotifyId"))
    }

    @Test fun spotifyShortLinksAreFollowedNotParsed() {
        val short = assertIs<StreamingLink.Short>(StreamingLinks.parse("https://spotify.link/AbCdEf123"))
        assertEquals(StreamingService.SPOTIFY, short.service)
        assertIs<StreamingLink.Short>(StreamingLinks.parse("https://spotify.app.link/xyz"))
    }

    @Test fun aSpotifyIdThatIsNotAnIdIsRefused() {
        assertNull(StreamingLinks.parse("https://open.spotify.com/track/short"))
        assertNull(StreamingLinks.parse("https://open.spotify.com/track/$spotifyId/extra".replace(spotifyId, "not-a-valid-spotify-id!!")))
        assertNull(StreamingLinks.parse("spotify:track:short"))
    }

    // ---- Deezer and SoundCloud --------------------------------------------------------------

    @Test fun deezerTrackWithAndWithoutALanguage() {
        assertEquals("3135556", assertIs<StreamingLink.Track>(StreamingLinks.parse("https://www.deezer.com/track/3135556")).id)
        assertEquals("3135556", assertIs<StreamingLink.Track>(StreamingLinks.parse("https://www.deezer.com/en/track/3135556?utm_source=x")).id)
        assertIs<StreamingLink.Album>(StreamingLinks.parse("https://www.deezer.com/fr/album/302127"))
        assertIs<StreamingLink.Short>(StreamingLinks.parse("https://link.deezer.com/s/abc"))
        assertNull(StreamingLinks.parse("https://www.deezer.com/track/notanumber"))
    }

    @Test fun soundCloudSongPlaylistAndArtist() {
        val track = assertIs<StreamingLink.Track>(StreamingLinks.parse("https://soundcloud.com/forss/flickermood"))
        assertEquals("/forss/flickermood", track.id)
        assertIs<StreamingLink.Playlist>(StreamingLinks.parse("https://soundcloud.com/forss/sets/soulhack"))
        assertIs<StreamingLink.Artist>(StreamingLinks.parse("https://soundcloud.com/forss"))
        assertIs<StreamingLink.Short>(StreamingLinks.parse("https://on.soundcloud.com/abc123"))
    }

    @Test fun soundCloudPagesThatAreNotSongsAreNothing() {
        assertNull(StreamingLinks.parse("https://soundcloud.com/discover"))
        assertNull(StreamingLinks.parse("https://soundcloud.com/forss/tracks"))
        assertNull(StreamingLinks.parse("https://soundcloud.com/forss/likes"))
    }

    // ---- what is not claimed ----------------------------------------------------------------

    @Test fun otherSitesAndOtherSchemesAreNotClaimed() {
        assertNull(StreamingLinks.parse("https://example.com/track/3135556"))
        assertNull(StreamingLinks.parse("https://music.apple.com.evil.example/us/song/x/1"))
        assertNull(StreamingLinks.parse("https://evil.example/https://open.spotify.com/track/$spotifyId"))
        assertNull(StreamingLinks.parse("ftp://music.apple.com/us/song/x/1"))
        assertNull(StreamingLinks.parse("https://user:pass@open.spotify.com/track/$spotifyId"))
        assertNull(StreamingLinks.parse(""))
        assertNull(StreamingLinks.parse(null))
        assertNull(StreamingLinks.parse("x".repeat(3000)))
    }

    @Test fun youtubeIsNotThisParsersJob() {
        assertNull(StreamingLinks.parse("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    // ---- inside a shared message ------------------------------------------------------------

    @Test fun extractFindsTheLinkInAShareMessage() {
        val message = "Listen to Never Gonna Give You Up on Spotify. https://open.spotify.com/track/$spotifyId?si=zzz."
        assertEquals(spotifyId, assertIs<StreamingLink.Track>(StreamingLinks.extract(message)).id)
        val apple = "Harder Better Faster Stronger by Daft Punk\nhttps://music.apple.com/us/album/discovery/697194953?i=697195787"
        assertEquals("697195787", assertIs<StreamingLink.Track>(StreamingLinks.extract(apple)).id)
    }

    @Test fun extractSkipsOtherLinksAndFindsTheMusicOne() {
        val message = "see https://example.com/page and https://www.deezer.com/track/3135556 ok"
        assertEquals("3135556", assertIs<StreamingLink.Track>(StreamingLinks.extract(message)).id)
        assertNull(StreamingLinks.extract("no link here"))
        assertNull(StreamingLinks.extract(null))
    }

    @Test fun extractReadsASpotifyUriInText() {
        assertEquals(spotifyId, assertIs<StreamingLink.Track>(StreamingLinks.extract("play spotify:track:$spotifyId now")).id)
    }

    @Test fun percentEncodedPathsAreDecodedForTheirIds() {
        val track = assertIs<StreamingLink.Track>(StreamingLinks.parse("https://soundcloud.com/for%73s/flicker%2Dmood"))
        assertEquals("/forss/flicker-mood", track.id)
    }
}
