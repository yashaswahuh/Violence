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
import com.music.violence.ui.player.drawRecord
import com.music.violence.ui.player.quotePalette
import com.music.violence.ui.player.quoteTypeface
import com.music.violence.ui.player.withAlpha
import com.music.violence.ui.player.wrapLyricsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * The whole Replay in the Quote design (Test 1.0): a pull-quote in the listener's own voice, a
 * listening personality, three big numbers and the top songs, on a tone taken from the top song's
 * cover with that cover as a vinyl record in the corner.
 *
 * The quote is built from what the listener actually did ("I played X 41 times and I'd do it
 * again"), so no two Replays read the same. Drawn on a software canvas at 1080 x 1920, like
 * [renderReplayPoster], so it shares and saves the same way.
 */
internal suspend fun renderReplayQuotePoster(
    context: Context,
    summary: ReplaySummary,
    holder: String,
): Bitmap = coroutineScope {
    val songs = summary.songRows(LIST_ROWS)
    val covers = songs.mapNotNull { it.artworkUrl }.distinct()
        .associateWith { url -> async(Dispatchers.IO) { loadBitmap(context, url) } }
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
        val block = MARK_ABOVE + textH + 70f + 70f
        val regionTop = 250f
        val regionBottom = 1040f
        val blockTop = regionTop + ((regionBottom - regionTop) - block).coerceAtLeast(0f) / 2f
        val textTop = blockTop + MARK_ABOVE

        val mark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = serifUpright
            textSize = 320f
            color = colors.accent
        }
        canvas.drawText("“", MARGIN - 8f, textTop + 60f, mark)
        var y = textTop
        for (row in lines) {
            canvas.drawText(row, MARGIN, y - quote.ascent(), quote)
            y += size * LEAD
        }
        val ruleY = y + 36f
        canvas.drawRoundRect(RectF(MARGIN, ruleY, MARGIN + 160f, ruleY + 7f), 4f, 4f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.accent })
        val by = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sans
            textSize = 30f
            letterSpacing = 0.2f
            color = withAlpha(Color.WHITE, 0xB8)
        }
        canvas.drawText(ellipsised("— ${who.uppercase()}", by, CONTENT_W), MARGIN, ruleY + 74f, by)

        // ── Personality ─────────────────────────────────────────────────────
        val (title, detail) = persona(context, summary)
        val pillText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sans
            textSize = 30f
            letterSpacing = 0.16f
            isFakeBoldText = true
            color = colors.accent
        }
        val label = title.uppercase()
        val pillW = pillText.measureText(label) + 72f
        canvas.drawRoundRect(
            RectF(MARGIN, PERSONA_TOP, MARGIN + pillW, PERSONA_TOP + 76f), 38f, 38f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = withAlpha(colors.accent, 0xCC)
            },
        )
        canvas.drawText(label, MARGIN + 36f, PERSONA_TOP + 49f, pillText)
        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sans
            textSize = 28f
            color = withAlpha(Color.WHITE, 0x9E)
        }
        canvas.drawText(ellipsised(detail, detailPaint, CONTENT_W), MARGIN, PERSONA_TOP + 128f, detailPaint)

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
            big.textSize = 92f
            while (big.measureText(value) > column - 36f && big.textSize > 40f) big.textSize -= 4f
            canvas.drawText(value, x + if (index == 0) 0f else 28f, STATS_TOP + 92f, big)
            canvas.drawText(name, x + if (index == 0) 0f else 28f, STATS_TOP + 140f, cap)
            if (index > 0) canvas.drawLine(x, STATS_TOP + 14f, x, STATS_TOP + 146f, divider)
        }

        // ── Top songs ───────────────────────────────────────────────────────
        val rowTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = serif; textSize = 42f; color = Color.WHITE }
        val rowArtist = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sans
            textSize = 26f
            color = withAlpha(Color.WHITE, 0x99)
        }
        songs.take(LIST_ROWS).forEachIndexed { index, row ->
            val rowY = LIST_TOP + index * ROW_H
            drawArtwork(canvas, row.artworkUrl?.let { covers[it] }, row.title, MARGIN, rowY, THUMB, false)
            val textX = MARGIN + THUMB + 26f
            val room = LIST_TEXT_RIGHT - textX
            canvas.drawText(ellipsised(row.title, rowTitle, room), textX, rowY + 42f, rowTitle)
            canvas.drawText(
                ellipsised(row.subtitle.orEmpty(), rowArtist, room),
                textX, rowY + 80f, rowArtist,
            )
        }

        // ── The record ──────────────────────────────────────────────────────
        drawRecord(canvas, cover, colors, cx = W - 90f, cy = H - 90f, radius = 300f)
        bitmap
    }
}

/** A name and one line of evidence for how this person listens, worked out from the Replay. */
internal fun persona(context: Context, s: ReplaySummary): Pair<String, String> {
    val topSong = s.songRows(1).firstOrNull()
    val topArtist = s.artistRows(1).firstOrNull()
    val plays = s.totalPlays.coerceAtLeast(1)
    val peak = s.peakHour
    val peakLine = peak?.let { "Peak hour ${formatHour(context, it)}" } ?: "Always something playing"
    return when {
        s.distinctArtists >= 60 ->
            "The Explorer" to "${grouped(s.distinctArtists.toLong())} different artists, and counting"
        topArtist != null && topArtist.plays * 100 / plays >= 30 ->
            "The Loyalist" to "${topArtist.title} was ${topArtist.plays * 100 / plays}% of everything you played"
        topSong != null && topSong.plays >= 10 && topSong.plays * 100 / plays >= 8 ->
            "The Repeater" to "${topSong.plays} plays of one song. No notes."
        peak != null && (peak >= 22 || peak <= 4) -> "The Night Owl" to peakLine
        peak != null && peak in 5..9 -> "The Early Bird" to peakLine
        else -> "The Daydreamer" to peakLine
    }
}

private const val W = 1080
private const val H = 1920
private const val MARGIN = 84f
private const val CONTENT_W = W - MARGIN * 2
private const val LEAD = 1.2f

/** The most height the quote's own lines may take. */
private const val QUOTE_ROOM = 520f

/** Room the open quotation mark takes above the first line. */
private const val MARK_ABOVE = 164f

private const val PERSONA_TOP = 1080f
private const val STATS_TOP = 1270f
private const val LIST_TOP = 1520f
private const val LIST_ROWS = 3
private const val ROW_H = 96f
private const val THUMB = 80f

/** The song rows stop short of the record in the corner. */
private const val LIST_TEXT_RIGHT = 730f
