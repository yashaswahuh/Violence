package com.music.bitchord.data.share

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SongLinksTest {

    private val yt = "dQw4w9WgXcQ"
    private val sp = "4uLU6hMCjMI75M1A2tKUQC"

    private fun cp(code: Int): String =
        if (code < 0x10000) {
            code.toChar().toString()
        } else {
            val v = code - 0x10000
            "" + (0xD800 + (v shr 10)).toChar() + (0xDC00 + (v and 0x3FF)).toChar()
        }

    private fun ok(text: String): SongRef =
        assertIs<SongLinks.Result.Ok>(SongLinks.parse(text), "expected a song for: $text").ref

    private fun err(text: String?): SongLinks.Error =
        assertIs<SongLinks.Result.Rejected>(SongLinks.parse(text), "expected a rejection for: $text").error

    private fun uri(
        source: String = "yt",
        id: String = yt,
        query: String = "v=1&t=Song&a=Artist",
        prefix: String = SongLinks.CUSTOM_PREFIX,
    ) = "$prefix$source/$id?$query"

    // --------------------------------------------------------------- building --

    @Test
    fun buildsTheDocumentedShape() {
        val ref = SongLinks.youtube(yt, "Never Gonna Give You Up", "Rick Astley")!!
        assertEquals("violence://song/yt/$yt?v=1&t=Never%20Gonna%20Give%20You%20Up&a=Rick%20Astley", SongLinks.toUri(ref))
        assertEquals("${SongLinks.WEB_PREFIX}yt/$yt?v=1&t=Never%20Gonna%20Give%20You%20Up&a=Rick%20Astley", SongLinks.toWebUrl(ref))
        assertEquals(ref, ok(SongLinks.toUri(ref)))
        assertEquals(ref, ok(SongLinks.toWebUrl(ref)))
    }

    @Test
    fun hintsAreOptionalForYouTube() {
        val bare = SongLinks.youtube(yt)!!
        assertEquals("violence://song/yt/$yt?v=1", SongLinks.toUri(bare))
        assertEquals(bare, ok(SongLinks.toUri(bare)))
        assertEquals(SongRef(SongSource.YOUTUBE, yt, "T", null), ok(uri(query = "v=1&t=T")))
    }

    @Test
    fun aBadVideoIdIsNotBuilt() {
        assertNull(SongLinks.youtube("short"))
        assertNull(SongLinks.youtube("dQw4w9WgXc!"))
        assertNull(SongLinks.youtube(yt + "x"))
    }

    @Test
    fun searchLinksNeedATitle() {
        assertNull(SongLinks.search(null))
        assertNull(SongLinks.search("   "))
        val ref = SongLinks.search("Blinding Lights", "The Weeknd")!!
        assertEquals("violence://song/q/search?v=1&t=Blinding%20Lights&a=The%20Weeknd", SongLinks.toUri(ref))
        assertEquals(ref, ok(SongLinks.toUri(ref)))
        assertEquals("Blinding Lights The Weeknd", ref.searchWords())
    }

    @Test
    fun theViolenceFormAlwaysFitsTwoHundredBytes() {
        val long = SongLinks.youtube(yt, "t".repeat(300), "a".repeat(300))!!
        val link = SongLinks.toUri(long)
        assertTrue(link.encodeToByteArray().size <= SongLinks.TARGET_BYTES, link)
        val back = ok(link)
        assertEquals(yt, back.id)
        assertTrue(back.title!!.isNotEmpty())
    }

    @Test
    fun cjkAndEmojiHintsAreShortenedOnCharacterBoundaries() {
        val cjk = cp(0x5C71).repeat(100)
        val ref = SongLinks.youtube(yt, cjk, cp(0x1F600).repeat(60))!!
        val link = SongLinks.toUri(ref)
        assertTrue(link.encodeToByteArray().size <= SongLinks.TARGET_BYTES, "size ${link.encodeToByteArray().size}")
        val back = ok(link)
        assertTrue((back.title ?: "").all { it == 0x5C71.toChar() }, "title was cut mid-character or changed")
    }

    @Test
    fun hintsWithControlOrInvisibleCharactersAreCleanedWhenBuilding() {
        val ref = SongLinks.youtube(yt, "Hel" + 0x07.toChar() + "lo" + 0x200B.toChar() + " " + 0x202E.toChar() + "World", "A\nB")!!
        assertEquals("Hello World", ref.title)
        assertEquals("A B", ref.artist)
        assertEquals(ref, ok(SongLinks.toUri(ref)))
    }

    @Test
    fun aHintThatIsAllInvalidIsDropped() {
        val ref = SongLinks.youtube(yt, "" + 0x00.toChar() + 0x01.toChar(), "ok")!!
        assertNull(ref.title)
        assertEquals("ok", ref.artist)
    }

    @Test
    fun unicodeRoundTrips() {
        for (title in listOf("Zo" + cp(0xEB), "Σόφια", "山田太郎", "محمد", "DJ " + cp(0x1F3A7), "Don't Stop & Go = #1 + 2")) {
            val ref = SongLinks.youtube(yt, title, "x")!!
            assertEquals(title, ok(SongLinks.toUri(ref)).title)
        }
    }

    // ---------------------------------------------------------------- parsing --

    @Test
    fun parsesBothForms() {
        val expected = SongRef(SongSource.YOUTUBE, yt, "Song", "Artist")
        assertEquals(expected, ok(uri()))
        assertEquals(expected, ok(uri(prefix = SongLinks.WEB_PREFIX)))
        assertEquals(expected, ok(uri(prefix = SongLinks.WEB_PREFIX.uppercase().replace(yt.uppercase(), yt))))
        assertEquals(expected, ok("  \n" + uri() + " \t"))
        assertEquals(expected, ok(uri() + "#frag"))
    }

    @Test
    fun spotifyLinksNeedAnIdAndATitle() {
        assertEquals(SongRef(SongSource.SPOTIFY, sp, "Song", "Artist"), ok(uri("sp", sp)))
        assertEquals(SongLinks.Error.MISSING_FIELD, err(uri("sp", sp, "v=1")))
        assertEquals(SongLinks.Error.BAD_ID, err(uri("sp", sp.dropLast(1))))
        assertEquals(SongLinks.Error.BAD_ID, err(uri("sp", sp.dropLast(1) + "-")))
        assertEquals(SongLinks.Error.BAD_ID, err(uri("sp", sp + "A")))
    }

    @Test
    fun youtubeIdsAreExactlyElevenCharacters() {
        for (bad in listOf("", "abc", yt.dropLast(1), yt + "A", "dQw4w9WgXc!", "dQw4w9 WgXc", "dQw4w9WgXc.")) {
            assertEquals(SongLinks.Error.BAD_ID, err(uri(id = bad)), "id: $bad")
        }
        assertEquals("a_-B9zZ0xY1", ok(uri(id = "a_-B9zZ0xY1")).id)
    }

    @Test
    fun searchLinkShapeIsFixed() {
        assertEquals(SongLinks.Error.BAD_ID, err(uri("q", "other", "v=1&t=T")))
        assertEquals(SongLinks.Error.MISSING_FIELD, err(uri("q", "search", "v=1")))
        assertEquals(SongRef(SongSource.SEARCH, "search", "T", null), ok(uri("q", "search", "v=1&t=T")))
    }

    @Test
    fun unknownSourceAndWrongShapes() {
        assertEquals(SongLinks.Error.BAD_SOURCE, err(uri("spotify", sp)))
        assertEquals(SongLinks.Error.BAD_SOURCE, err(uri("YT", yt)))
        assertEquals(SongLinks.Error.NOT_A_SONG_LINK, err("violence://song/yt/$yt/extra?v=1"))
        assertEquals(SongLinks.Error.NOT_A_SONG_LINK, err("violence://song/yt?v=1"))
        assertEquals(SongLinks.Error.NOT_A_SONG_LINK, err("violence://song/yt/$yt/?v=1"))
        assertEquals(SongLinks.Error.NOT_A_SONG_LINK, err("violence://songs/yt/$yt?v=1"))
        assertEquals(SongLinks.Error.NOT_A_SONG_LINK, err("http://yashaswahuh.is-a.dev/Violence/song/yt/$yt?v=1"))
        assertEquals(SongLinks.Error.NOT_A_SONG_LINK, err("https://evil.example/Violence/song/yt/$yt?v=1"))
        assertEquals(SongLinks.Error.NOT_A_SONG_LINK, err(null))
        assertEquals(SongLinks.Error.NOT_A_SONG_LINK, err(""))
    }

    @Test
    fun versionIsRequiredAndMustBeOne() {
        assertEquals(SongLinks.Error.WRONG_VERSION, err(uri(query = "v=2&t=T")))
        assertEquals(SongLinks.Error.WRONG_VERSION, err(uri(query = "v=&t=T")))
        assertEquals(SongLinks.Error.WRONG_VERSION, err(uri(query = "v=1.0")))
        assertEquals(SongLinks.Error.MISSING_FIELD, err(uri(query = "t=T&a=A")))
        assertEquals(SongLinks.Error.MISSING_FIELD, err("violence://song/yt/$yt"))
    }

    @Test
    fun duplicateAndUnknownFieldsAreRefused() {
        assertEquals(SongLinks.Error.DUPLICATE_FIELD, err(uri(query = "v=1&t=A&t=B")))
        assertEquals(SongLinks.Error.DUPLICATE_FIELD, err(uri(query = "v=1&v=1")))
        assertEquals(SongLinks.Error.DUPLICATE_FIELD, err(uri(query = "v=1&a=A&a=B")))
        assertEquals(SongLinks.Error.UNKNOWN_FIELD, err(uri(query = "v=1&t=A&x=1")))
        assertEquals(SongLinks.Error.UNKNOWN_FIELD, err(uri(query = "v=1&url=http%3A%2F%2Fevil")))
        assertEquals(SongLinks.Error.UNKNOWN_FIELD, err(uri(query = "v=1&T=A")))
    }

    @Test
    fun hintLengthLimitsAreExactAndNeverTruncate() {
        assertEquals("a".repeat(120), ok(uri(query = "v=1&t=" + "a".repeat(120))).title)
        assertEquals(SongLinks.Error.BAD_TITLE, err(uri(query = "v=1&t=" + "a".repeat(121))))
        assertEquals("a".repeat(80), ok(uri(query = "v=1&a=" + "a".repeat(80))).artist)
        assertEquals(SongLinks.Error.BAD_ARTIST, err(uri(query = "v=1&a=" + "a".repeat(81))))
        assertEquals(SongLinks.Error.TOO_LONG, err(uri(query = "v=1&t=" + "a".repeat(600))))
    }

    @Test
    fun emptyOrBlankHints() {
        assertEquals(SongLinks.Error.BAD_TITLE, err(uri(query = "v=1&t=")))
        assertEquals(SongLinks.Error.BAD_TITLE, err(uri(query = "v=1&t=%20%20")))
        assertEquals(SongLinks.Error.BAD_ARTIST, err(uri(query = "v=1&a=+++")))
    }

    @Test
    fun maliciousHintsAreRejectedOrHarmlessText() {
        for (escaped in listOf("%00", "%07", "%0A", "%0D", "%1B", "%7F", "%C2%85")) {
            assertEquals(SongLinks.Error.BAD_TITLE, err(uri(query = "v=1&t=Hi${escaped}there")), "escape $escaped")
        }
        for (escaped in listOf("%E2%80%AE", "%E2%80%8B", "%E2%81%A6", "%EF%BB%BF", "%E2%80%A8", "%F3%A0%80%81", "%EE%80%80")) {
            assertEquals(SongLinks.Error.BAD_ARTIST, err(uri(query = "v=1&a=Hi${escaped}there")), "escape $escaped")
        }
        assertEquals(SongLinks.Error.BAD_ENCODING, err(uri(query = "v=1&t=%ZZ")))
        assertEquals(SongLinks.Error.BAD_ENCODING, err(uri(query = "v=1&t=%C3")))
        assertEquals(SongLinks.Error.BAD_ENCODING, err(uri(query = "v=1&t=%ED%A0%80")))
        // markup and URLs in a title are just text: the receiver only ever shows it as plain text
        for (text in listOf("<script>alert(1)</script>", "violence://party/AB12CD", "https://evil.example")) {
            val ref = SongLinks.youtube(yt, text, "x")!!
            assertEquals(text, ok(SongLinks.toUri(ref)).title)
        }
    }

    @Test
    fun theTitleCanNeverChangeWhatPlays() {
        val a = ok(uri(query = "v=1&t=First"))
        val b = ok(uri(query = "v=1&t=Totally%20Different"))
        assertEquals(a.id, b.id)
        assertEquals(a.key, b.key)
    }

    @Test
    fun keysIdentifyTheSongNotTheHints() {
        assertEquals(SongRef(SongSource.YOUTUBE, yt, "A", "B").key, SongRef(SongSource.YOUTUBE, yt, null, null).key)
        assertFalse(SongRef(SongSource.YOUTUBE, yt).key == SongRef(SongSource.YOUTUBE, "aaaaaaaaaaa").key)
        assertEquals(SongRef(SongSource.SEARCH, "search", "Hello", "Adele").key, SongRef(SongSource.SEARCH, "search", "Hello", "Adele").key)
        assertFalse(SongRef(SongSource.SEARCH, "search", "Hello", "Adele").key == SongRef(SongSource.SEARCH, "search", "Hello", "Other").key)
    }

    @Test
    fun noLinkEverCarriesAnythingButTheSong() {
        val link = SongLinks.toUri(SongLinks.youtube(yt, "Title", "Artist")!!)
        for (forbidden in listOf("token", "cookie", "email", "account", "device")) {
            assertFalse(link.contains(forbidden, ignoreCase = true))
        }
        assertNotNull(link)
    }
}
