package com.music.violence.data.share

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * What each service really answers, captured from its public endpoint, and what the parsers make of it.
 * The Spotify page is the real embed page's data block with the session and styling dropped.
 */
class StreamingTracksTest {

    private val itunesSong = """{"resultCount":1,"results":[{"wrapperType":"track","kind":"song","artistId":5468295,"collectionId":697194953,"trackId":697195787,"artistName":"Daft Punk","collectionName":"Discovery","trackName":"Harder Better Faster Stronger","collectionCensoredName":"Discovery","trackCensoredName":"Harder Better Faster Stronger","artistViewUrl":"https://music.apple.com/us/artist/daft-punk/5468295?uo=4","collectionViewUrl":"https://music.apple.com/us/album/harder-better-faster-stronger/697194953?i=697195787&uo=4","trackViewUrl":"https://music.apple.com/us/album/harder-better-faster-stronger/697194953?i=697195787&uo=4","previewUrl":"https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/8d/a4/4e/8da44e8f-9705-6182-686c-332714c54671/mzaf_17406318046701183138.plus.aac.p.m4a","artworkUrl30":"https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/fd/4a/77/fd4a77db-0ebc-d043-41a2-f32fa1bb0fb4/dj.qrikkdwj.jpg/30x30bb.jpg","artworkUrl60":"https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/fd/4a/77/fd4a77db-0ebc-d043-41a2-f32fa1bb0fb4/dj.qrikkdwj.jpg/60x60bb.jpg","artworkUrl100":"https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/fd/4a/77/fd4a77db-0ebc-d043-41a2-f32fa1bb0fb4/dj.qrikkdwj.jpg/100x100bb.jpg","collectionPrice":9.99,"trackPrice":1.29,"releaseDate":"2001-02-26T08:00:00Z","collectionExplicitness":"notExplicit","trackExplicitness":"notExplicit","discCount":1,"discNumber":1,"trackCount":14,"trackNumber":4,"trackTimeMillis":224693,"country":"USA","currency":"USD","primaryGenreName":"Dance","isStreamable":true}]}"""
    private val itunesAlbum = """{"resultCount":1,"results":[{"wrapperType":"collection","collectionType":"Album","artistId":909253,"collectionId":1440857781,"amgArtistId":468749,"artistName":"Jack Johnson","collectionName":"In Between Dreams (Bonus Track Version)","collectionCensoredName":"In Between Dreams (Bonus Track Version)","artistViewUrl":"https://music.apple.com/us/artist/jack-johnson/909253?uo=4","collectionViewUrl":"https://music.apple.com/us/album/in-between-dreams-bonus-track-version/1440857781?uo=4","artworkUrl60":"https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/44/06/fd/4406fdc0-aab5-e300-82ba-3e5fe81a68a7/00602537868858.rgb.jpg/60x60bb.jpg","artworkUrl100":"https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/44/06/fd/4406fdc0-aab5-e300-82ba-3e5fe81a68a7/00602537868858.rgb.jpg/100x100bb.jpg","collectionPrice":9.99,"collectionExplicitness":"notExplicit","trackCount":16,"copyright":"℗ 2013 Jack Johnson","country":"USA","currency":"USD","releaseDate":"2005-03-01T08:00:00Z","primaryGenreName":"Rock"}]}"""
    private val deezerTrack = """{"id":3135556,"readable":false,"title":"Harder, Better, Faster, Stronger","title_short":"Harder, Better, Faster, Stronger","title_version":"","isrc":"GBDUW0000059","link":"https://www.deezer.com/track/3135556","share":"https://www.deezer.com/track/3135556?utm_source=deezer&utm_content=track-3135556&utm_term=0_1795242943&utm_medium=web","duration":226,"track_position":4,"disk_number":1,"rank":810329,"release_date":"2001-03-12","explicit_lyrics":false,"explicit_content_lyrics":0,"explicit_content_cover":0,"preview":"","bpm":0,"gain":-12.4,"available_countries":["AE","AF","AG","AI","AL","AM","AO","AQ","AR","AS","AT","AU","AZ","BA","BB","BD","BE","BF","BG","BH","BI","BJ","BN","BO","BQ","BR","BT","BV","BW","BY","CA","CC","CD","CF","CG","CH","CI","CK","CL","CM","CN","CO","CR","CV","CW","CX","CY","CZ","DE","DJ","DK","DM","DO","DZ","EC","EE","EG","EH","ER","ES","ET","FI","FJ","FK","FM","FR","GA","GB","GD","GE","GH","GM","GN","GQ","GR","GS","GT","GU","GW","HK","HM","HN","HR","HU","ID","IE","IL","IN","IO","IQ","IS","IT","JM","JO","JP","KE","KG","KH","KI","KM","KN","KR","KW","KY","KZ","LA","LB","LC","LK","LR","LS","LT","LU","LV","LY","MA","MD","ME","MG","MH","MK","ML","MM","MN","MP","MR","MS","MT","MU","MV","MW","MX","MY","MZ","NA","NE","NF","NG","NI","NL","NO","NP","NR","NU","NZ","OM","PA","PE","PG","PH","PK","PL","PN","PS","PT","PW","PY","QA","RO","RS","RU","RW","SA","SB","SC","SD","SE","SG","SI","SJ","SK","SL","SN","SO","SS","ST","SV","SX","SZ","TC","TD","TG","TH","TJ","TK","TL","TM","TN","TO","TR","TV","TW","TZ","UA","UG","US","UY","UZ","VC","VE","VG","VI","VN","VU","WS","YE","ZA","ZM","ZW"],"contributors":[{"id":27,"name":"Daft Punk","link":"https://www.deezer.com/artist/27","share":"https://www.deezer.com/artist/27?utm_source=deezer&utm_content=artist-27&utm_term=0_1795242943&utm_medium=web","picture":"https://api.deezer.com/artist/27/image","picture_small":"https://cdn-images.dzcdn.net/images/artist/638e69b9caaf9f9f3f8826febea7b543/56x56-000000-80-0-0.jpg","picture_medium":"https://cdn-images.dzcdn.net/images/artist/638e69b9caaf9f9f3f8826febea7b543/250x250-000000-80-0-0.jpg","picture_big":"https://cdn-images.dzcdn.net/images/artist/638e69b9caaf9f9f3f8826febea7b543/500x500-000000-80-0-0.jpg","picture_xl":"https://cdn-images.dzcdn.net/images/artist/638e69b9caaf9f9f3f8826febea7b543/1000x1000-000000-80-0-0.jpg","radio":true,"tracklist":"https://api.deezer.com/artist/27/top?limit=50","type":"artist","role":"Main"}],"md5_image":"5718f7c81c27e0b2417e2a4c45224f8a","track_token":"AAAAAWsBO79rAlT_vUU0rCKgjz46QmsXGB16ncvQeu2ardpqjAc2xY1tq7-OSqtdSuP6svDWbNqHmm7KVGRkrK58rvflolZ_WvqQaNBbZ61y_WGnmJjY1OK0PSP9aduNg6dRofZFH-nSiD87bu32Nwy0umhlirPHrvDIOj5xbzHucd06BHml48WCp70fn3cR71Glar8uzWklRA2Bf5uYhqxLlmtvU8MT4J7pxKD4mOSLnf_FIpfOfcMRO3rO422V9Z-YYqJJ0PUO6aJpvBdpkpQpaWkF8b-4QuqkQU3KU2qCt6rKE9D8Y09iiSnrcOn3-1k2mFDZWre0cSr1dFEIhDtVP3DZEOozhHgMyGBSOqT9xFJSWmOPMVvdJkZn4PWHVIhELjaLb4dlGnWumMA","artist":{"id":27,"name":"Daft Punk","link":"https://www.deezer.com/artist/27","share":"https://www.deezer.com/artist/27?utm_source=deezer&utm_content=artist-27&utm_term=0_1795242943&utm_medium=web","picture":"https://api.deezer.com/artist/27/image","picture_small":"https://cdn-images.dzcdn.net/images/artist/638e69b9caaf9f9f3f8826febea7b543/56x56-000000-80-0-0.jpg","picture_medium":"https://cdn-images.dzcdn.net/images/artist/638e69b9caaf9f9f3f8826febea7b543/250x250-000000-80-0-0.jpg","picture_big":"https://cdn-images.dzcdn.net/images/artist/638e69b9caaf9f9f3f8826febea7b543/500x500-000000-80-0-0.jpg","picture_xl":"https://cdn-images.dzcdn.net/images/artist/638e69b9caaf9f9f3f8826febea7b543/1000x1000-000000-80-0-0.jpg","radio":true,"tracklist":"https://api.deezer.com/artist/27/top?limit=50","type":"artist"},"album":{"id":302127,"title":"Discovery","link":"https://www.deezer.com/album/302127","cover":"https://api.deezer.com/album/302127/image","cover_small":"https://cdn-images.dzcdn.net/images/cover/5718f7c81c27e0b2417e2a4c45224f8a/56x56-000000-80-0-0.jpg","cover_medium":"https://cdn-images.dzcdn.net/images/cover/5718f7c81c27e0b2417e2a4c45224f8a/250x250-000000-80-0-0.jpg","cover_big":"https://cdn-images.dzcdn.net/images/cover/5718f7c81c27e0b2417e2a4c45224f8a/500x500-000000-80-0-0.jpg","cover_xl":"https://cdn-images.dzcdn.net/images/cover/5718f7c81c27e0b2417e2a4c45224f8a/1000x1000-000000-80-0-0.jpg","md5_image":"5718f7c81c27e0b2417e2a4c45224f8a","release_date":"2001-03-07","tracklist":"https://api.deezer.com/album/302127/tracks","type":"album"},"type":"track"}"""
    private val soundCloud = """{"version":1.0,"type":"rich","provider_name":"SoundCloud","provider_url":"https://soundcloud.com","height":400,"width":"100%","title":"Flickermood by Forss","description":"From the Soulhack album,&nbsp;recently featured in this ad <a href=\"https://www.dswshoes.com/tv_commercial.jsp?m=october2007\">https://www.dswshoes.com/tv_commercial.jsp?m=october2007</a> ","thumbnail_url":"https://i1.sndcdn.com/artworks-000067273316-smsiqx-t500x500.jpg","html":"<iframe width=\"100%\" height=\"400\" scrolling=\"no\" frameborder=\"no\" allow=\"autoplay; encrypted-media\" src=\"https://w.soundcloud.com/player/?visual=true&url=https%3A%2F%2Fapi.soundcloud.com%2Ftracks%2F293&show_artwork=true\"></iframe>","author_name":"Forss","author_url":"https://soundcloud.com/forss"}"""
    private val spotifyEmbed = """<html><head></head><body><script id="__NEXT_DATA__" type="application/json">{"props":{"pageProps":{"state":{"data":{"entity":{"type":"track","name":"Never Gonna Give You Up","uri":"spotify:track:4cOdK2wGLETKBW3PvgPWqT","id":"4cOdK2wGLETKBW3PvgPWqT","title":"Never Gonna Give You Up","artists":[{"name":"Rick Astley","uri":"spotify:artist:0gxyHStUsqpMadRV0Di1Qt"}],"duration":213573,"releaseDate":{"isoString":"1987-11-12T00:00:00Z"}}}}}}}</script></body></html>"""

    @Test fun appleSongIsReadFromTheLookup() {
        val track = assertNotNull(StreamingTracks.parseItunesTrack(itunesSong))
        assertEquals("Harder Better Faster Stronger", track.title)
        assertEquals("Daft Punk", track.artist)
        assertEquals("Discovery", track.album)
        assertEquals(224693L, track.durationMs)
    }

    @Test fun appleAlbumLookupIsNotASong() {
        assertNull(StreamingTracks.parseItunesTrack(itunesAlbum))
        val album = assertNotNull(StreamingTracks.parseItunesAlbum(itunesAlbum))
        assertEquals("Jack Johnson", album.artist)
        assertEquals("In Between Dreams (Bonus Track Version)", album.title)
    }

    @Test fun anEmptyLookupHasNoSong() {
        assertNull(StreamingTracks.parseItunesTrack("""{"resultCount":0,"results":[]}"""))
        assertEquals(0, StreamingTracks.resultCount("""{"resultCount":0,"results":[]}"""))
        assertEquals(1, StreamingTracks.resultCount(itunesSong))
        assertNull(StreamingTracks.parseItunesTrack("not json"))
    }

    @Test fun deezerTrackIsReadFromTheApi() {
        val track = assertNotNull(StreamingTracks.parseDeezerTrack(deezerTrack))
        assertEquals("Harder, Better, Faster, Stronger", track.title)
        assertEquals("Daft Punk", track.artist)
        assertEquals("Discovery", track.album)
        assertEquals(226_000L, track.durationMs)
    }

    @Test fun aDeezerErrorHasNoSong() {
        assertNull(StreamingTracks.parseDeezerTrack("""{"error":{"type":"DataException","message":"no data","code":800}}"""))
    }

    @Test fun soundCloudSplitsTheTitleFromItsArtist() {
        val track = assertNotNull(StreamingTracks.parseSoundCloudOEmbed(soundCloud))
        assertEquals("Flickermood", track.title)
        assertEquals("Forss", track.artist)
    }

    @Test fun soundCloudTitleWithoutTheArtistSuffixIsKept() {
        val track = assertNotNull(StreamingTracks.parseSoundCloudOEmbed("""{"title":"Just a title","author_name":"Someone"}"""))
        assertEquals("Just a title", track.title)
        assertEquals("Someone", track.artist)
    }

    @Test fun spotifyEmbedGivesTheNameTheArtistsAndTheLength() {
        val track = assertNotNull(StreamingTracks.parseSpotifyEmbed(spotifyEmbed))
        assertEquals("Never Gonna Give You Up", track.title)
        assertEquals("Rick Astley", track.artist)
        assertEquals(213573L, track.durationMs)
    }

    @Test fun severalSpotifyArtistsAreJoined() {
        val html = """<script id="__NEXT_DATA__" type="application/json">{"props":{"pageProps":{"state":{"data":{"entity":{"name":"Song","artists":[{"name":"A"},{"name":"B"}],"duration":1000}}}}}}</script>"""
        assertEquals("A, B", assertNotNull(StreamingTracks.parseSpotifyEmbed(html)).artist)
    }

    @Test fun aPageWithoutTheDataBlockHasNoSong() {
        assertNull(StreamingTracks.parseSpotifyEmbed("<html>nothing</html>"))
    }

    @Test fun applePageTitleReadsSongAndArtist() {
        val html = """<meta property="og:title" content="Harder Better Faster Stronger by Daft Punk on Apple Music">"""
        val track = assertNotNull(StreamingTracks.parseApplePageTitle(html))
        assertEquals("Harder Better Faster Stronger", track.title)
        assertEquals("Daft Punk", track.artist)
    }
}
