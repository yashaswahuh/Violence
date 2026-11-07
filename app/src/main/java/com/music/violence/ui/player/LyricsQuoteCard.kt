package com.music.violence.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.music.violence.R
import com.music.violence.ui.replay.drawLogo
import com.music.violence.ui.replay.ellipsised
import com.music.violence.ui.replay.loadBitmap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lyrics card, "Quote" design (Test 1.0).
 *
 * Deliberately not the blurred-cover card the other share sheet draws, and not a
 * flat colour block with a small cover on top either. This one reads as a page
 * from a record sleeve's liner notes: the verse is set as a pull-quote in a
 * serif italic under a large open quotation mark, on a deep tone taken from the
 * cover itself, with the song credited underneath in small spaced capitals. The
 * cover turns up once, as a vinyl record sliding in from the bottom corner,
 * grooves and all.
 *
 * Drawn on a software [Canvas] for the same reasons as [renderLyricsShareCard],
 * and grown to its words the same way: as tall as the passage needs, or the full
 * 1080x1920 frame when [story] is set.
 */
internal suspend fun renderLyricsQuoteCard(
    context: Context,
    card: LyricsShareRequest,
    story: Boolean = false,
    loader: suspend (String) -> Bitmap? = { url -> loadBitmap(context, url) },
): Bitmap = withContext(Dispatchers.Default) {
    val cover = card.artworkUrl?.let { loader(it) }
    val colors = quotePalette(cover)
    val serif = quoteTypeface(context, R.font.vs_bodoni_moda_italic, Typeface.SERIF)
    val serifUpright = quoteTypeface(context, R.font.vs_bodoni_moda, Typeface.SERIF)
    val sans = quoteTypeface(context, R.font.vs_inter, Typeface.SANS_SERIF)

    // ── Fit the words ───────────────────────────────────────────────────────
    val frameRoom = MAX_H - HEAD - FOOT
    val plan = QUOTE_LADDER.asList().firstNotNullOfOrNull { size ->
        planQuote(card.lines, serif, sans, size).takeIf { it.height <= frameRoom }
    } ?: planQuote(card.lines, serif, sans, QUOTE_LADDER.last())
    val spare = if (story) (frameRoom - plan.height).coerceAtLeast(0f) else 0f
    val top = HEAD + spare / 2
    val cardH = (top + plan.height + spare / 2 + FOOT).roundToInt()

    val bitmap = Bitmap.createBitmap(W, cardH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // ── Ground ──────────────────────────────────────────────────────────────
    canvas.drawColor(colors.ground)
    // A soft pool of the cover's accent in the top-left corner, behind the mark.
    val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            0f, 0f, 980f,
            withAlpha(colors.accent, 0x4A), withAlpha(colors.accent, 0x00),
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRect(0f, 0f, W.toFloat(), cardH.toFloat(), glow)

    // ── Mark, top right ─────────────────────────────────────────────────────
    val brand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = sans
        textSize = 34f
        letterSpacing = 0.18f
        color = withAlpha(Color.WHITE, 0xB8)
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText(BRAND, W - MARGIN, 118f, brand)
    drawLogo(canvas, context, W - MARGIN - brand.measureText(BRAND) - 20f, 118f)

    // ── The open quotation mark ─────────────────────────────────────────────
    val mark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = serifUpright
        textSize = 340f
        color = colors.accent
    }
    canvas.drawText("“", MARGIN - 8f, top + 40f, mark)

    // ── The verse ───────────────────────────────────────────────────────────
    var y = top
    for (row in plan.rows) {
        y += row.gapBefore
        canvas.drawText(row.text, MARGIN, y - row.paint.ascent(), row.paint)
        y += row.height
    }

    // ── Rule and credit ─────────────────────────────────────────────────────
    val ruleY = top + plan.height + 70f
    val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors.accent }
    canvas.drawRoundRect(RectF(MARGIN, ruleY, MARGIN + 160f, ruleY + 7f), 4f, 4f, rule)

    val creditWidth = CREDIT_W
    val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = serif
        textSize = 54f
        color = Color.WHITE
    }
    var creditY = ruleY + 84f
    wrapLyricsText(card.song.title, title, creditWidth).take(2).forEachIndexed { index, line ->
        canvas.drawText(line, MARGIN, creditY, title)
        creditY += if (index == 0) 60f else 0f
    }
    val artist = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = sans
        textSize = 28f
        letterSpacing = 0.22f
        color = withAlpha(Color.WHITE, 0xA6)
    }
    canvas.drawText(
        ellipsised(card.song.artist.uppercase(), artist, creditWidth),
        MARGIN,
        creditY + 52f,
        artist,
    )

    // ── The cover ───────────────────────────────────────────────────────────
    if (cover != null) drawCoverPrint(canvas, cover, colors, W - MARGIN - PRINT, ruleY - 4f, PRINT)

    bitmap
}

// ── Layout ─────────────────────────────────────────────────────────────────

private class QuoteRow(val text: String, val paint: Paint, val height: Float, val gapBefore: Float)

private class QuotePlan(val rows: List<QuoteRow>) {
    val height: Float get() = rows.sumOf { (it.gapBefore + it.height).toDouble() }.toFloat()
}

private fun planQuote(lines: List<LyricsShareLine>, serif: Typeface, sans: Typeface, size: Float): QuotePlan {
    val primary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = serif
        textSize = size
        color = withAlpha(Color.WHITE, 0xF2)
    }
    val subSize = (size * 0.46f).coerceAtLeast(26f)
    val secondary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = sans
        textSize = subSize
        color = withAlpha(Color.WHITE, 0x9E)
    }
    val rows = mutableListOf<QuoteRow>()
    lines.forEachIndexed { index, line ->
        if (line.isGap) {
            // An omission, as a lone ellipsis the way a printed quotation marks one.
            val dots = Paint(primary).apply { color = withAlpha(Color.WHITE, 0x66) }
            rows += QuoteRow("…", dots, size * 1.0f, 0f)
            return@forEachIndexed
        }
        var first = true
        fun add(text: String, paint: Paint, height: Float) {
            val gap = if (rows.isEmpty()) 0f else if (first && index > 0) size * 0.5f else 0f
            rows += QuoteRow(text, paint, height, gap)
            first = false
        }
        wrapLyricsText(line.text, primary, CONTENT_W).forEach { add(it, primary, size * 1.22f) }
        val sub = line.subText
        if (!sub.isNullOrBlank() && sub != line.text) {
            wrapLyricsText(sub, secondary, CONTENT_W).forEach { add(it, secondary, subSize * 1.35f) }
        }
    }
    return QuotePlan(rows)
}

// ── The cover, as a print ──────────────────────────────────────────────────

/**
 * The cover as a clean framed print: rounded corners, a soft shadow lifting it off the page, a
 * hairline edge and a faint light from the top left. No gimmick, just the picture, the way a
 * record sleeve would be pinned to a wall. With no cover, a quiet tile in the card's own colours.
 */
internal fun drawCoverPrint(
    canvas: Canvas,
    cover: Bitmap?,
    colors: QuoteColors,
    x: Float,
    y: Float,
    size: Float,
    radius: Float = 26f,
    /** Off for a small thumbnail, which a heavy shadow would swamp. */
    shadow: Boolean = true,
) {
    val rect = RectF(x, y, x + size, y + size)
    // The shadow, offset down so the print sits above the page.
    if (shadow) {
        canvas.drawRoundRect(
            RectF(x + 10f, y + 26f, x + size - 10f, y + size + 26f), radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = withAlpha(Color.BLACK, 0x80)
                maskFilter = BlurMaskFilter(36f, BlurMaskFilter.Blur.NORMAL)
            },
        )
    }
    val frame = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }
    canvas.save()
    canvas.clipPath(frame)
    if (cover != null) {
        // Centre-cropped square, so a wide thumbnail is not squashed.
        val side = min(cover.width, cover.height)
        val src = Rect((cover.width - side) / 2, (cover.height - side) / 2, (cover.width + side) / 2, (cover.height + side) / 2)
        canvas.drawBitmap(cover, src, rect, Paint(Paint.FILTER_BITMAP_FLAG))
    } else {
        canvas.drawRect(
            rect,
            Paint().apply {
                shader = LinearGradient(x, y, x + size, y + size, colors.accent, colors.ground, Shader.TileMode.CLAMP)
            },
        )
    }
    // Light from the top left, fading out across the picture.
    canvas.drawRect(
        rect,
        Paint().apply {
            shader = LinearGradient(x, y, x + size * 0.9f, y + size * 0.9f, withAlpha(Color.WHITE, 0x26), 0x00FFFFFF, Shader.TileMode.CLAMP)
        },
    )
    canvas.restore()
    canvas.drawRoundRect(
        rect, radius, radius,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = withAlpha(Color.WHITE, 0x2E)
        },
    )
}

// ── Colour ─────────────────────────────────────────────────────────────────

internal class QuoteColors(val ground: Int, val accent: Int)

/**
 * A deep ground and a bright accent, both from the cover.
 *
 * The cover is sampled on a coarse grid: the ground is its average pulled down to
 * a dark, slightly muted tone, and the accent is its most vivid sample lifted to
 * a light tone, so white type sits on the one and the quotation mark shows on it.
 * With no cover the card falls back to a neutral dark and a warm white.
 */
internal fun quotePalette(cover: Bitmap?): QuoteColors {
    if (cover == null || cover.width == 0 || cover.height == 0) {
        return QuoteColors(ground = 0xFF121214.toInt(), accent = 0xFFE8D9B5.toInt())
    }
    val step = max(1, min(cover.width, cover.height) / 24)
    var r = 0L; var g = 0L; var b = 0L; var n = 0
    var best = 0f
    var vivid = Color.WHITE
    val hsv = FloatArray(3)
    var y = 0
    while (y < cover.height) {
        var x = 0
        while (x < cover.width) {
            val p = cover.getPixel(x, y)
            r += Color.red(p); g += Color.green(p); b += Color.blue(p); n++
            Color.colorToHSV(p, hsv)
            val score = hsv[1] * hsv[2]
            if (score > best) { best = score; vivid = p }
            x += step
        }
        y += step
    }
    val avg = Color.rgb((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
    Color.colorToHSV(avg, hsv)
    val ground = Color.HSVToColor(floatArrayOf(hsv[0], (hsv[1] * 0.85f).coerceIn(0.15f, 0.7f), 0.20f))
    Color.colorToHSV(vivid, hsv)
    val accent = Color.HSVToColor(floatArrayOf(hsv[0], hsv[1].coerceIn(0.25f, 0.85f), 0.95f))
    return QuoteColors(ground, accent)
}

internal fun withAlpha(color: Int, alpha: Int): Int = (alpha shl 24) or (color and 0x00FFFFFF)

internal fun quoteTypeface(context: Context, id: Int, fallback: Typeface): Typeface =
    runCatching { ResourcesCompat.getFont(context, id) }.getOrNull() ?: fallback

// ── Geometry ───────────────────────────────────────────────────────────────

private const val W = 1080
private const val MAX_H = 1920f
private const val MARGIN = 84f
private const val CONTENT_W = W - MARGIN * 2

/** Room above the verse, for the quotation mark and the brand. */
private const val HEAD = 300f

/** Room below the verse: the rule, the credit and the cover beside it. */
private const val FOOT = 460f

/** The credit stays left of the cover. */
private const val CREDIT_W = 560f

/** The cover's size beside the credit. */
private const val PRINT = 300f

private const val BRAND = "VIOLENCE"

private val QUOTE_LADDER = floatArrayOf(84f, 76f, 68f, 62f, 56f, 50f, 45f, 40f, 36f)
