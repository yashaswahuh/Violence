package com.music.violence.ui.preview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.music.violence.data.model.Song
import com.music.violence.data.stats.RankedEntry
import com.music.violence.data.stats.RankedSong
import com.music.violence.data.stats.ReplayPeriod
import com.music.violence.data.stats.ReplaySummary
import com.music.violence.ui.player.LyricsShareLine
import com.music.violence.ui.player.LyricsShareRequest
import com.music.violence.ui.player.renderLyricsQuoteCard
import com.music.violence.ui.replay.renderReplayQuotePoster
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Draws the share cards to PNG files under `app/build/card-previews/` so a design can be looked at
 * without a phone. These are JVM renders with a made-up cover, not device screenshots.
 * Run with: powershell -File scripts/preview-cards.ps1
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = android.app.Application::class)
class CardPreviewTest {
    private val out = File("build/card-previews").apply { mkdirs() }

    /** Rendering is slow, so it only runs when asked: set VIOLENCE_PREVIEWS=1. */
    @Before
    fun onlyWhenAsked() {
        assumeTrue(System.getenv("VIOLENCE_PREVIEWS") == "1")
    }

    private fun save(name: String, bitmap: Bitmap) {
        File(out, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** A made-up album cover: a sunset over water. */
    private fun cover(top: Int, bottom: Int, sun: Int): Bitmap {
        val b = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.drawRect(0f, 0f, 640f, 640f, Paint().apply { shader = LinearGradient(0f, 0f, 0f, 640f, top, bottom, Shader.TileMode.CLAMP) })
        c.drawCircle(320f, 300f, 130f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = sun })
        for (i in 0 until 9) {
            c.drawRect(0f, 380f + i * 30f, 640f, 390f + i * 30f, Paint().apply { color = Color.argb(70, 0, 0, 0) })
        }
        return b
    }

    private val warm = cover(0xFF3A1C71.toInt(), 0xFFFF7A3D.toInt(), 0xFFFFD36E.toInt())
    private val cool = cover(0xFF021B3A.toInt(), 0xFF1FA2FF.toInt(), 0xFF9DF4FF.toInt())

    private fun summary(): ReplaySummary {
        fun song(i: Int, title: String, artist: String) =
            Song(videoId = "v$i", title = title, artist = artist, thumbnailUrl = "warm")
        val hours = List(24) { h ->
            when (h) { in 21..23 -> 900L + h * 40; 0, 1 -> 700L; in 18..20 -> 420L; in 8..10 -> 260L; else -> 60L + h * 7 }
        }
        return ReplaySummary(
            period = ReplayPeriod.THIS_YEAR,
            label = "2026",
            totalMs = 3_412L * 60_000,
            totalPlays = 1_480,
            songs = listOf(
                RankedSong(song(1, "Midnight Static", "The Afterglow"), 9_000_000, 41),
                RankedSong(song(2, "Glass Harbour", "Nova Lane"), 7_000_000, 33),
                RankedSong(song(3, "Slow Burn Skyline", "Kite & Compass"), 5_000_000, 24),
                RankedSong(song(4, "Paper Moons", "Hollow Season"), 4_000_000, 19),
                RankedSong(song(5, "Ten Past Midnight", "Aurelia"), 3_000_000, 15),
            ),
            artists = listOf(
                RankedEntry("The Afterglow", null, "warm", null, 20_000_000, 210),
                RankedEntry("Nova Lane", null, "warm", null, 14_000_000, 150),
                RankedEntry("Kite & Compass", null, "warm", null, 9_000_000, 98),
            ),
            albums = listOf(RankedEntry("Static Hearts", "The Afterglow", "warm", null, 12_000_000, 120)),
            genres = listOf(RankedEntry("Synthpop", null, null, null, 18_000_000, 300)),
            hourOfDay = hours,
            busiestDay = "2026-05-02",
            busiestDayMs = 9_000_000,
            distinctSongs = 412,
            distinctArtists = 96,
            distinctAlbums = 140,
            since = "2026-01",
        )
    }

    @Test
    fun replay() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        save("replay-warm", runBlocking { renderReplayQuotePoster(context, summary(), "Namra", loader = { warm }) })
        save("replay-cool", runBlocking { renderReplayQuotePoster(context, summary(), "Namra", loader = { cool }) })
    }

    @Test
    fun lyrics() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val request = LyricsShareRequest(
            song = Song(videoId = "x", title = "Midnight Static", artist = "The Afterglow", thumbnailUrl = null),
            artworkUrl = "warm",
            lines = listOf(
                LyricsShareLine("We were neon in the rain", null),
                LyricsShareLine("Every light a little loud", null),
                LyricsShareLine("Running through the static", null),
            ),
        )
        save("lyrics-warm", runBlocking { renderLyricsQuoteCard(context, request, story = false, loader = { warm }) })
        save("lyrics-warm-story", runBlocking { renderLyricsQuoteCard(context, request, story = true, loader = { warm }) })
    }
}
