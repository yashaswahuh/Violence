package com.music.bitchord.ui.brutal.components

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** The new UI is still experimental, so the wordmark carries its tag (spec section 1.4). One switch, one place. */
object BrutalFlags {
    const val EXPERIMENTAL = true
}

/**
 * The text helpers the components share. All pure, so they are unit tested without a device.
 */

/** Whole hours, minutes and seconds of [durationMs]; negative or huge input is clamped, never thrown at. */
data class DurationParts(val hours: Int, val minutes: Int, val seconds: Int)

fun durationParts(durationMs: Long): DurationParts {
    val total = (durationMs.coerceAtLeast(0L) / 1000L)
    return DurationParts(
        hours = (total / 3600L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        minutes = ((total % 3600L) / 60L).toInt(),
        seconds = (total % 60L).toInt(),
    )
}

/** `3:45`, or `1:02:03` from an hour up. The digits of the mockup's chip ("03:45") are padded to two. */
fun formatDuration(durationMs: Long): String {
    val p = durationParts(durationMs)
    return if (p.hours > 0) {
        "%d:%02d:%02d".format(Locale.ROOT, p.hours, p.minutes, p.seconds)
    } else {
        "%02d:%02d".format(Locale.ROOT, p.minutes, p.seconds)
    }
}

/** `dd mm yy`: the mockup's date stamp (`28 11 22`). */
fun formatDateStamp(date: LocalDate): String =
    "%02d %02d %02d".format(Locale.ROOT, date.dayOfMonth, date.monthValue, date.year % 100)

/** The date as a person would hear it ("28 November 2022"), in the device language. */
fun spokenDate(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(date)

/** The tile aspect (height over width) the masonry allows: the mockup's tiles run from square to 1.78 tall (spec 5.1). */
const val MIN_TILE_ASPECT = 1.0f
const val MAX_TILE_ASPECT = 1.8f

fun clampTileAspect(heightToWidth: Float): Float =
    if (heightToWidth.isNaN()) MIN_TILE_ASPECT else heightToWidth.coerceIn(MIN_TILE_ASPECT, MAX_TILE_ASPECT)

/**
 * The small, deterministic offset that breaks the grid (spec: "controlled overlap"). It depends only
 * on the item's key, so an item keeps its place across recompositions, scrolls and reorders, and it
 * is bounded: at most [maxDp] vertically and half of that sideways.
 */
data class Jitter(val xDp: Float, val yDp: Float)

fun masonryJitter(keyHash: Int, maxDp: Float): Jitter {
    // Spread the hash first (a plain index hashes to small neighbours that would all lean the same way).
    var h = keyHash * -0x61c88647
    h = h xor (h ushr 15)
    val fx = ((h and 0xFF) / 255f) * 2f - 1f
    val fy = (((h ushr 8) and 0xFF) / 255f) * 2f - 1f
    return Jitter(xDp = fx * maxDp * 0.5f, yDp = fy * maxDp)
}
