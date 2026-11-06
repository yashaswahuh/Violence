package com.music.violence.data.spotify

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppleMusicImporterTest {

    @Test
    fun acceptsAppleMusicPlaylistLinks() {
        assertEquals(
            "https://music.apple.com/us/playlist/todays-hits/pl.f4d106fed2bd41149aaacabb233eb5eb",
            AppleMusicImporter.extractPlaylistUrl(
                "https://music.apple.com/us/playlist/todays-hits/pl.f4d106fed2bd41149aaacabb233eb5eb?l=en",
            ),
        )
        assertEquals(
            "https://music.apple.com/in/playlist/mine/pl.u-abc123",
            AppleMusicImporter.extractPlaylistUrl("  https://music.apple.com/in/playlist/mine/pl.u-abc123  "),
        )
    }

    @Test
    fun rejectsOtherLinks() {
        assertNull(AppleMusicImporter.extractPlaylistUrl(""))
        assertNull(AppleMusicImporter.extractPlaylistUrl("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M"))
        assertNull(AppleMusicImporter.extractPlaylistUrl("https://music.apple.com/us/album/x/123"))
        assertNull(AppleMusicImporter.extractPlaylistUrl("https://evil.example/music.apple.com/us/playlist/x/pl.abc"))
    }

    @Test
    fun readsNameAndSongsFromThePage() {
        val html = """
            <html><head>
            <script id=schema:music-playlist type="application/ld+json">{"@type":"MusicPlaylist","name":"Road Trip"}</script>
            </head><body>
            <script type="application/json" id="serialized-server-data">
            {"data":[{"data":{"sections":[
              {"id":"header","items":[{"title":"Road Trip","subtitle":"Playlist"}]},
              {"id":"tracks","items":[
                {"title":"First","artistName":"A","contentDescriptor":{"kind":"song"}},
                {"title":"Second","artistName":"B & C","contentDescriptor":{"kind":"song"}}
              ]}
            ]}}]}
            </script></body></html>
        """.trimIndent()
        val (name, tracks) = AppleMusicImporter.parsePlaylistPage(html)
        assertEquals("Road Trip", name)
        assertEquals(
            listOf(SpotifyImportTrack("First", "A"), SpotifyImportTrack("Second", "B & C")),
            tracks,
        )
    }
}
