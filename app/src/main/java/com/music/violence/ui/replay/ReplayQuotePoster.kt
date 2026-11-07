package com.music.violence.ui.replay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.music.violence.R
import com.music.violence.data.stats.ReplaySummary
import com.music.violence.ui.player.drawCoverPrint
import com.music.violence.ui.player.quotePalette
import com.music.violence.ui.player.quoteTypeface
import com.music.violence.ui.player.withAlpha
import com.music.violence.ui.player.wrapLyricsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * The whole Replay in the Quote design (Test 1.0): a pull-quote in the listener's own voice, their
 * listening personality written out as a name and a line about them, three big numbers, and the
 * most-played song held up as a framed print beside its title and play count, with the next two
 * songs under it, each with its own cover. Set in a serif on a tone taken from the top song's cover.
 *
 * The quote is built from what the listener actually did ("I played X 41 times and I'd do it
 * again"), so no two Replays read the same. Drawn on a software canvas at 1080 x 1920, like
 * [renderReplayPoster], so it shares and saves the same way.
 */
internal suspend fun renderReplayQuotePoster(
    context: Context,
    summary: ReplaySummary,
    holder: String,
    loader: suspend (String) -> Bitmap? = { url -> loadBitmap(context, url) },
): Bitmap = coroutineScope {
    val songs = summary.songRows(LIST_ROWS)
    val covers = songs.mapNotNull { it.artworkUrl }.distinct()
        .associateWith { url -> async(Dispatchers.IO) { loader(url) } }
        .mapValues { it.value.await() }

    withContext(Dispatchers.Default) {
        val top = songs.firstOrNull()
        val cover = top?.artworkUrl?.let { covers[it] }
        val colors = quotePalette(cover)
        val serif = quoteTypeface(context, R.font.vs_bodoni_moda_italic, Typeface.SERIF)
        val serifUpright = quoteTypeface(context, R.font.vs_bodoni_moda, Typeface.SERIF)
        val sans = quoteTypeface(context, R.font.vs_inter, Typeface.SANS_SERIF)

        val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(colors.ground)
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                0f, 0f, 1100f,
                withAlpha(colors.accent, 0x4A), withAlpha(colors.accent, 0x00),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), glow)

        // ── Top line ────────────────────────────────────────────────────────
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sans
            textSize = 30f
            letterSpacing = 0.2f
            color = withAlpha(Color.WHITE, 0xB8)
        }
        canvas.drawText("${summary.label.uppercase()} · REPLAY", MARGIN, 118f, small)
        val brand = Paint(small).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("VIOLENCE", W - MARGIN, 118f, brand)
        drawLogo(canvas, context, W - MARGIN - brand.measureText("VIOLENCE") - 20f, 118f)

        // ── The quote, centred in the room it has ───────────────────────────
        val who = holder.trim().ifBlank { "me" }
        val line = if (top != null && top.plays >= 2) {
            "I played ${top.title} ${top.plays} times and I’d do it again."
        } else {
            "${grouped(summary.minutes)} minutes of music, and not one wasted."
        }
        val ladder = floatArrayOf(96f, 88f, 80f, 72f, 64f, 58f, 52f)
        var size = ladder.last()
        var lines = emptyList<String>()
        for (candidate in ladder) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = serif; textSize = candidate }
            val wrapped = wrapLyricsText(line, paint, CONTENT_W)
            if (wrapped.size * candidate * LEAD <= QUOTE_ROOM) { size = candidate; lines = wrapped; break }
            lines = wrapped
        }
        val quote = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = serif
            textSize = size
            color = withAlpha(Color.WHITE, 0xF2)
        }
        val textH = lines.size * size * LEAD
        val block = MARK_ABOVE + textH + 60f + 60f
        val regionTop = 210f
        val regionBottom = 830f
        val blockTop = regionTop + ((regionBottom - regionTop) - block).coerceAtLeast(0f) / 2f
        val textTop = blockTop + MARK_ABOVE

        val mark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = serifUpright
            textSize = 300f
            color = colors.accent
        }
        canvas.drawText("“", MARGIN - 8f, textTop + 50f, mark)
        var y = textTop
        for (row in lines) {
            canvas.drawText(row, MARGIN, y - quote.ascent(), quote)
            y += size * LEAD
        }
        val ruleY = y + 30f
        canvas.drawRoundRect(RectF(MARGIN, ruleY, MARGIN + 160f, ruleY + 7f), 4f, 4f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.accent })
        val by = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sans
            textSize = 30f
            letterSpacing = 0.2f
            color = withAlpha(Color.WHITE, 0xB8)
        }
        canvas.drawText(ellipsised("— ${who.uppercase()}", by, CONTENT_W), MARGIN, ruleY + 66f, by)

        // ── Your listening personality ──────────────────────────────────────
        // A name, a line about the person it describes, and the evidence for it.
        val me = listeningPersona(context, summary)
        val pillText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sans
            textSize = 28f
            letterSpacing = 0.16f
            isFakeBoldText = true
            color = colors.accent
        }
        val label = me.title.uppercase()
        val pillW = pillText.measureText(label) + 64f
        canvas.drawRoundRect(
            RectF(MARGIN, PERSONA_TOP, MARGIN + pillW, PERSONA_TOP + 66f), 33f, 33f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = withAlpha(colors.accent, 0xCC)
            },
        )
        canvas.drawText(label, MARGIN + 32f, PERSONA_TOP + 43f, pillText)
        val tagline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = serif
            textSize = 40f
            color = withAlpha(Color.WHITE, 0xE6)
        }
        var taglineY = PERSONA_TOP + 66f + 56f
        wrapLyricsText(me.tagline, tagline, CONTENT_W).take(2).forEach { row ->
            canvas.drawText(row, MARGIN, taglineY, tagline)
            taglineY += 50f
        }
        val evidence = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sans
            textSize = 26f
            color = withAlpha(Color.WHITE, 0x99)
        }
        canvas.drawText(ellipsised(me.evidence, evidence, CONTENT_W), MARGIN, taglineY + 6f, evidence)

        // ── Three numbers ───────────────────────────────────────────────────
        val numbers = listOf(
            grouped(summary.minutes) to "MINUTES",
            grouped(summary.distinctSongs.toLong()) to "SONGS",
            grouped(summary.distinctArtists.toLong()) to "ARTISTS",
        )
        val column = CONTENT_W / 3f
        val big = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = serifUpright
            color = Color.WHITE
        }
        val cap = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sans
            textSize = 24f
            letterSpacing = 0.22f
            color = withAlpha(Color.WHITE, 0x99)
        }
        val divider = Paint().apply { color = withAlpha(Color.WHITE, 0x26); strokeWidth = 2f }
        numbers.forEachIndexed { index, (value, name) ->
            val x = MARGIN + index * column
            // The largest size at which the number still fits its column.
            big.textSize = 84f
            while (big.measureText(value) > column - 36f && big.textSize > 40f) big.textSize -= 4f
            canvas.drawText(value, x + if (index == 0) 0f else 28f, STATS_TOP + 84f, big)
            canvas.drawText(name, x + if (index == 0) 0f else 28f, STATS_TOP + 128f, cap)
            if (index > 0) canvas.drawLine(x, STATS_TOP + 12f, x, STATS_TOP + 134f, divider)
        }

        // ── Most played ─────────────────────────────────────────────────────
        // The top song, held up: its cover as a framed print, its name beside it, and how many
        // times it was played as the biggest number in the block. The name takes at most two
        // lines and the play count is sized to the room left under the artist, so the two can
        // never run into each other however long the title is.
        if (top != null) {
            drawCoverPrint(canvas, cover, colors, MARGIN, FEATURE_TOP, PRINT)
            val textX = MARGIN + PRINT + 44f
            val room = W - MARGIN - textX
            val kicker = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = sans
                textSize = 24f
                letterSpacing = 0.24f
                isFakeBoldText = true
                color = colors.accent
            }
            canvas.drawText("MOST PLAYED", textX, FEATURE_TOP + 28f, kicker)
            val name = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = serif
                textSize = 52f
                color = Color.WHITE
            }
            val titleRows = wrapLyricsText(top.title, name, room).let { rows ->
                if (rows.size <= 2) rows else listOf(rows[0], ellipsised(rows.drop(1).joinToString(" "), name, room))
            }
            var nameY = FEATURE_TOP + 92f
            titleRows.forEach { row ->
                canvas.drawText(row, textX, nameY, name)
                nameY += 58f
            }
            val artistY = nameY - 58f + 44f
            val artist = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = sans
                textSize = 26f
                color = withAlpha(Color.WHITE, 0xA6)
            }
            canvas.drawText(ellipsised(top.subtitle.orEmpty(), artist, room), textX, artistY, artist)

            val count = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = serifUpright
                color = Color.WHITE
            }
            val countY = FEATURE_TOP + PRINT - 6f
            // Tall enough to be the star, short enough to clear the artist line above it.
            var countSize = 96f
            while (countSize > 52f && countY - countSize * 0.72f < artistY + 28f) countSize -= 4f
            count.textSize = countSize
            val countText = "${top.plays}"
            canvas.drawText(countText, textX, countY, count)
            val unit = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = sans
                textSize = 24f
                letterSpacing = 0.24f
                color = withAlpha(Color.WHITE, 0x99)
            }
            canvas.drawText(if (top.plays == 1) "PLAY" else "PLAYS", textX + count.measureText(countText) + 18f, countY, unit)
        }

        // ── The next two, each with its cover ───────────────────────────────
        val rule = Paint().apply { color = withAlpha(Color.WHITE, 0x26); strokeWidth = 2f }
        canvas.drawLine(MARGIN, RUNNERS_TOP - 26f, W - MARGIN, RUNNERS_TOP - 26f, rule)
        val rank = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = serif; textSize = 38f; color = colors.accent }
        val rowTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = serif; textSize = 40f; color = Color.WHITE }
        val rowArtist = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sans
            textSize = 24f
            color = withAlpha(Color.WHITE, 0x99)
        }
        val plays = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sans
            textSize = 24f
            letterSpacing = 0.12f
            color = withAlpha(Color.WHITE, 0xB8)
            textAlign = Paint.Align.RIGHT
        }
        songs.drop(1).take(2).forEachIndexed { i, row ->
            val rowY = RUNNERS_TOP + i * RUNNER_H
            canvas.drawText("%02d".format(i + 2), MARGIN, rowY + 46f, rank)
            drawCoverPrint(canvas, row.artworkUrl?.let { covers[it] }, colors, MARGIN + 64f, rowY, THUMB, radius = 16f, shadow = false)
            val textX = MARGIN + 64f + THUMB + 26f
            val room = W - MARGIN - 190f - textX
            canvas.drawText(ellipsised(row.title, rowTitle, room), textX, rowY + 34f, rowTitle)
            canvas.drawText(ellipsised(row.subtitle.orEmpty(), rowArtist, room), textX, rowY + 66f, rowArtist)
            canvas.drawText(if (row.plays == 1) "1 PLAY" else "${row.plays} PLAYS", W - MARGIN, rowY + 44f, plays)
        }
        bitmap
    }
}

/**
 * Who this listener is, in a name, a line about them and the evidence for it. The name is picked
 * from how they actually listened, the first of these that fits:
 *
 *  - lots of different artists: the Explorer
 *  - one artist is most of what they play: the Loyalist
 *  - one song played over and over: the Repeater
 *  - hundreds of songs, hardly any played twice: the Sampler
 *  - few songs, played again and again: the Deep Diver
 *  - hardly started: the Newcomer
 *  - then by the hour they listen most: Night Owl, Early Bird, Golden Hour, Daytimer
 *  - and the Daydreamer for everyone else.
 */
internal class Persona(val title: String, val tagline: String, val evidence: String)

internal fun listeningPersona(context: Context, s: ReplaySummary): Persona {
    val topSong = s.songRows(1).firstOrNull()
    val topArtist = s.artistRows(1).firstOrNull()
    val plays = s.totalPlays.coerceAtLeast(1)
    val peak = s.peakHour
    val peakLine = peak?.let { "Peak hour ${formatHour(context, it)}" } ?: "Always something playing"
    val perSong = plays.toFloat() / s.distinctSongs.coerceAtLeast(1)
    return when {
        s.distinctArtists >= 60 -> Persona(
            "The Explorer",
            "A new artist every week, a new corner of the map. You’re never done searching.",
            "${grouped(s.distinctArtists.toLong())} different artists, and counting",
        )
        topArtist != null && topArtist.plays * 100 / plays >= 30 -> Persona(
            "The Loyalist",
            "You found your sound and you stayed. Ride or die.",
            "${topArtist.title} was ${topArtist.plays * 100 / plays}% of everything you played",
        )
        topSong != null && topSong.plays >= 10 && topSong.plays * 100 / plays >= 8 -> Persona(
            "The Repeater",
            "When a song gets you, it really gets you. Again. And again.",
            "${topSong.plays} plays of one song. No notes.",
        )
        s.distinctSongs >= 40 && perSong < 1.6f -> Persona(
            "The Sampler",
            "You taste-test everything. One play and on to the next, always hunting for the one that sticks.",
            "${grouped(s.distinctSongs.toLong())} different songs, hardly any twice",
        )
        s.distinctSongs >= 15 && perSong >= 3.5f -> Persona(
            "The Deep Diver",
            "You don’t skim, you dive. Every song gets your full attention until you know it by heart.",
            "${"%.1f".format(perSong)} plays per song on average",
        )
        s.minutes < 90 && s.totalPlays < 60 -> Persona(
            "The Newcomer",
            "Just getting started. The best chapters of your soundtrack are still ahead.",
            "${grouped(s.minutes)} minutes so far",
        )
        peak != null && (peak >= 22 || peak <= 4) -> Persona(
            "The Night Owl",
            "The world goes quiet and your playlist wakes up. Your best listening happens after dark.",
            peakLine,
        )
        peak != null && peak in 5..9 -> Persona(
            "The Early Bird",
            "Headphones in before the sun is up. You start the day with a soundtrack.",
            peakLine,
        )
        peak != null && peak in 17..21 -> Persona(
            "The Golden Hour Listener",
            "Work’s done, the light is low, the volume goes up. Evenings belong to your music.",
            peakLine,
        )
        peak != null && peak in 10..16 -> Persona(
            "The Daytimer",
            "Music is how you get through the day: focus, flow and the odd dance break.",
            peakLine,
        )
        else -> Persona(
            "The Daydreamer",
            "You drift through the day with a soundtrack in your head.",
            peakLine,
        )
    }
}

/** The name and the evidence, for the Replay's personality card. */
internal fun persona(context: Context, s: ReplaySummary): Pair<String, String> =
    listeningPersona(context, s).let { it.title to it.evidence }

private const val W = 1080
private const val H = 1920
private const val MARGIN = 84f
private const val CONTENT_W = W - MARGIN * 2
private const val LEAD = 1.2f

/** The most height the quote's own lines may take. */
private const val QUOTE_ROOM = 330f

/** Room the open quotation mark takes above the first line. */
private const val MARK_ABOVE = 150f

private const val PERSONA_TOP = 850f
private const val STATS_TOP = 1115f

/** The most-played block: the cover's size, and where the block starts. */
private const val PRINT = 280f
private const val FEATURE_TOP = 1290f

/** The two quieter songs under it, each with a small cover. */
private const val RUNNERS_TOP = 1638f
private const val RUNNER_H = 100f
private const val THUMB = 80f
private const val LIST_ROWS = 3
