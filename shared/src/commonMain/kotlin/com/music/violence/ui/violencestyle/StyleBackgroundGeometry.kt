package com.music.violence.ui.violencestyle

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure geometry of the live background (STYLE_SPEC.md section 2): where each glow blob sits, how big it
 * is, how bright, and how it drifts. No Compose, no allocation per call (the caller passes the output
 * array), so it is unit-tested on the JVM and cheap to run in a draw lambda.
 *
 * Arcs was dropped by the owner; the three styles are Aurora, Corner glow and Haze (Off draws nothing).
 */
object BackgroundGeometry {

    /** Floats per blob in the output array: centre x, centre y, radius, alpha. */
    const val STRIDE = 4
    const val MAX_BLOBS = 3

    /** Radial falloff stops, shared by the draw code and the contrast guard (they must agree). */
    val FALLOFF_STOPS = floatArrayOf(0f, 0.35f, 0.7f, 1f)
    val FALLOFF_ALPHAS = floatArrayOf(1f, 0.55f, 0.18f, 0f)

    /** Where a blob sits (fractions of the layer), how big (x layer width), how bright, and its drift periods. */
    private class Spec(
        val x: Float, val y: Float, val radius: Float, val peak: Float,
        /** Drift periods in seconds at Normal motion; Subtle stretches them. */
        val periodX: Float, val periodY: Float,
        val phaseX: Float, val phaseY: Float,
    )

    /** Dark vignette: from [startFraction] of the height the layer fades to the base colour. */
    class Vignette(val startFraction: Float, val alpha: Float)

    private val AURORA = arrayOf(
        Spec(0.08f, 0.08f, 0.85f, 1.00f, 31f, 37f, 0.0f, 1.1f),
        Spec(1.00f, 0.46f, 0.75f, 0.80f, 27f, 40f, 2.0f, 0.4f),
        Spec(0.10f, 0.70f, 0.60f, 0.25f, 38f, 33f, 4.0f, 3.1f),
    )
    private val CORNER = arrayOf(
        Spec(0.05f, 0.02f, 0.95f, 1.00f, 33f, 29f, 0.0f, 0.7f),
        Spec(0.25f, 0.12f, 1.30f, 0.35f, 39f, 36f, 2.6f, 1.9f),
    )
    private val HAZE = arrayOf(
        Spec(0.50f, 0.13f, 1.00f, 0.90f, 31f, 37f, 0.0f, 2.2f),
        Spec(0.50f, 0.58f, 1.20f, 0.55f, 35f, 29f, 3.3f, 0.9f),
    )
    private val NONE = emptyArray<Spec>()

    private fun specs(style: StyleBackground): Array<Spec> = when (style) {
        StyleBackground.AURORA -> AURORA
        StyleBackground.CORNER_GLOW -> CORNER
        StyleBackground.HAZE -> HAZE
        StyleBackground.OFF -> NONE
    }

    fun blobCount(style: StyleBackground): Int = specs(style).size

    fun vignette(style: StyleBackground): Vignette? = when (style) {
        StyleBackground.AURORA -> Vignette(0.60f, 0.85f)
        StyleBackground.HAZE -> Vignette(0.70f, 0.90f)
        else -> null
    }

    /** Drift amplitude as a fraction of the layer width (STYLE_SPEC.md: Subtle 0.025, Normal 0.06). */
    fun amplitude(motion: StyleMotion): Float = when (motion) {
        StyleMotion.OFF -> 0f
        StyleMotion.SUBTLE -> 0.07f
        StyleMotion.NORMAL -> 0.15f
    }

    /** High glow saturates the centre, so a bigger drift keeps the movement readable (Low and Medium x1, High x1.6). */
    fun driftBoost(intensity: StyleIntensity): Float = if (intensity == StyleIntensity.HIGH) 1.6f else 1f

    /** Brightness breath depth: the alpha dips by up to this fraction and returns to the guarded maximum. */
    private fun pulseDepth(motion: StyleMotion): Float = when (motion) {
        StyleMotion.OFF -> 0f
        StyleMotion.SUBTLE -> 0.12f
        StyleMotion.NORMAL -> 0.22f
    }

    /** Subtle stretches the periods x0.6 (about 16 to 24 s per loop); Subtle x1.0 (27 to 40 s). Raised after the S2 device review: the first values were too slow to see. */
    fun periodScale(motion: StyleMotion): Float = if (motion == StyleMotion.SUBTLE) 1f else 0.6f

    /** Intensity also scales blob radii: Low x0.8, Medium x1.0, High x1.15. */
    fun radiusScale(intensity: StyleIntensity): Float = when (intensity) {
        StyleIntensity.LOW -> 0.8f
        StyleIntensity.MEDIUM -> 1.0f
        StyleIntensity.HIGH -> 1.15f
    }

    /** Radius breathing, as a fraction of the radius (Normal 3 percent, Subtle 1.5 percent). */
    private fun breath(motion: StyleMotion): Float = when (motion) {
        StyleMotion.OFF -> 0f
        StyleMotion.SUBTLE -> 0.03f
        StyleMotion.NORMAL -> 0.06f
    }

    /**
     * Writes the blobs for time [tSeconds] into [out] (STRIDE floats each: cx, cy, radius, alpha) and
     * returns how many. [scales] (from [GlowGuard]) scales each blob's alpha; null means 1.
     * [dx] and [dy] shift every centre (used by the guard to test the extremes of the drift).
     */
    fun fill(
        style: StyleBackground,
        width: Float,
        height: Float,
        tSeconds: Double,
        motion: StyleMotion,
        intensity: StyleIntensity,
        scales: FloatArray?,
        out: FloatArray,
        dx: Float = 0f,
        dy: Float = 0f,
    ): Int {
        val list = specs(style)
        val amp = amplitude(motion) * width * driftBoost(intensity)
        val depth = pulseDepth(motion)
        val pScale = periodScale(motion)
        val rScale = radiusScale(intensity)
        val breath = breath(motion)
        for (i in list.indices) {
            val s = list[i]
            val ax = if (amp == 0f) 0f else {
                (sin(TWO_PI * (tSeconds / (s.periodX * pScale)) + s.phaseX) * amp).toFloat()
            }
            val ay = if (amp == 0f) 0f else {
                (cos(TWO_PI * (tSeconds / (s.periodY * pScale)) + s.phaseY) * amp).toFloat()
            }
            val br = if (breath == 0f) 1f else {
                1f + (sin(TWO_PI * (tSeconds / (s.periodX * pScale * 1.3)) + s.phaseY) * breath).toFloat()
            }
            val o = i * STRIDE
            out[o] = s.x * width + ax + dx
            out[o + 1] = s.y * height + ay + dy
            out[o + 2] = s.radius * width * rScale * br
            // 1 at the brightest, 1 - depth at the dimmest: never above the alpha the contrast guard allowed.
            val pulse = if (depth == 0f) 1f else 1f - depth * (0.5f - 0.5f * sin(TWO_PI * (tSeconds / (s.periodY * pScale * 0.8)) + s.phaseX).toFloat())
            out[o + 3] = s.peak * intensity.peakAlpha * (scales?.get(i) ?: 1f) * pulse
        }
        return list.size
    }

    /** Alpha multiplier at normalised distance [d] (0 at the centre, 1 at the radius), from the stops. */
    fun falloff(d: Float): Float {
        if (d <= 0f) return 1f
        if (d >= 1f) return 0f
        for (i in 1 until FALLOFF_STOPS.size) {
            if (d <= FALLOFF_STOPS[i]) {
                val t = (d - FALLOFF_STOPS[i - 1]) / (FALLOFF_STOPS[i] - FALLOFF_STOPS[i - 1])
                return FALLOFF_ALPHAS[i - 1] + (FALLOFF_ALPHAS[i] - FALLOFF_ALPHAS[i - 1]) * t
            }
        }
        return 0f
    }

    private const val TWO_PI = 2.0 * PI

    /**
     * The colour at ([x], [y]): the base, the blobs drawn over it in order (source-over, as the canvas
     * does), then the vignette. Mirrors the draw code, and is what the contrast guard samples.
     */
    fun compositeAt(
        x: Float, y: Float, width: Float, height: Float,
        blobs: FloatArray, blobCount: Int,
        glowArgb: Int, baseArgb: Int, vignette: Vignette?,
    ): Int {
        var r = AccentMath.red(baseArgb).toFloat()
        var g = AccentMath.green(baseArgb).toFloat()
        var b = AccentMath.blue(baseArgb).toFloat()
        val gr = AccentMath.red(glowArgb).toFloat()
        val gg = AccentMath.green(glowArgb).toFloat()
        val gb = AccentMath.blue(glowArgb).toFloat()
        for (i in 0 until blobCount) {
            val o = i * STRIDE
            val dxp = x - blobs[o]
            val dyp = y - blobs[o + 1]
            val d = sqrt(dxp * dxp + dyp * dyp) / blobs[o + 2]
            val a = blobs[o + 3] * falloff(d)
            r += (gr - r) * a; g += (gg - g) * a; b += (gb - b) * a
        }
        if (vignette != null) {
            val start = vignette.startFraction * height
            if (y > start) {
                val av = vignette.alpha * ((y - start) / (height - start)).coerceIn(0f, 1f)
                val br = AccentMath.red(baseArgb).toFloat()
                val bg = AccentMath.green(baseArgb).toFloat()
                val bb = AccentMath.blue(baseArgb).toFloat()
                r += (br - r) * av; g += (bg - g) * av; b += (bb - b) * av
            }
        }
        return AccentMath.argb(255, r.toInt().coerceIn(0, 255), g.toInt().coerceIn(0, 255), b.toInt().coerceIn(0, 255))
    }

    /** The light version's base: the light page colour tinted 5 percent toward the glow colour. */
    fun baseColorLight(glowArgb: Int): Int {
        fun mix(page: Int, glow: Int) = (page + (glow - page) * 0.05f).toInt().coerceIn(0, 255)
        val page = AccentMath.LIGHT_BACKGROUND
        return AccentMath.argb(
            255,
            mix(AccentMath.red(page), AccentMath.red(glowArgb)),
            mix(AccentMath.green(page), AccentMath.green(glowArgb)),
            mix(AccentMath.blue(page), AccentMath.blue(glowArgb)),
        )
    }

    /** Black tinted 4 percent toward the glow colour (STYLE_SPEC.md section 2). */
    fun baseColor(glowArgb: Int): Int = AccentMath.argb(
        255,
        (AccentMath.red(glowArgb) * 0.04f).toInt(),
        (AccentMath.green(glowArgb) * 0.04f).toInt(),
        (AccentMath.blue(glowArgb) * 0.04f).toInt(),
    )
}

/**
 * Keeps the glow from reducing text contrast (STYLE_SPEC.md section 3, task S2-5).
 *
 * The rules, evaluated on a 12 x 24 grid over a phone-shaped layer, at the base pose and at the four
 * extremes of the drift:
 *  - upper zone (the top 30 percent: headings, icons): white at least 3:1 (large text);
 *  - lower zone: the app's secondary text colour (`#8E8E93`) at least 4.5:1, which also covers white.
 * A blob that causes a violation is dimmed (x0.9 per step, down to 5 percent) until every sample
 * passes, so the effective intensity is never higher than the chosen one.
 */
object GlowGuard {
    const val UPPER_ZONE = 0.30f
    const val LARGE_TEXT_CONTRAST = 3.0
    const val BODY_TEXT_CONTRAST = 4.5
    const val SECONDARY_TEXT: Int = 0xFF8E8E93.toInt()

    private const val GRID_X = 12
    private const val GRID_Y = 24
    private const val REF_W = 390f
    private const val REF_H = 844f
    private const val MIN_SCALE = 0.05f
    private const val STEP = 0.9f

    /** Highest background luminance that still gives [contrast] for text of luminance [textLum]. */
    fun maxBackgroundLuminance(textLum: Double, contrast: Double): Double = (textLum + 0.05) / contrast - 0.05

    /** Returns one alpha scale (0.05..1) per blob. All ones when the glow is already safe. */
    fun scales(
        style: StyleBackground,
        glowArgb: Int,
        intensity: StyleIntensity,
        motion: StyleMotion,
        secondaryText: Int = SECONDARY_TEXT,
    ): FloatArray {
        val n = BackgroundGeometry.blobCount(style)
        val scales = FloatArray(max(n, 1)) { 1f }
        if (n == 0) return scales
        val base = BackgroundGeometry.baseColor(glowArgb)
        val vignette = BackgroundGeometry.vignette(style)
        val whiteLum = AccentMath.relativeLuminance(AccentMath.WHITE)
        val upperMax = maxBackgroundLuminance(whiteLum, LARGE_TEXT_CONTRAST)
        val lowerMax = maxBackgroundLuminance(AccentMath.relativeLuminance(secondaryText), BODY_TEXT_CONTRAST)
        val amp = BackgroundGeometry.amplitude(motion) * BackgroundGeometry.driftBoost(intensity) * REF_W
        val shifts = floatArrayOf(0f, 0f, amp, 0f, -amp, 0f, 0f, amp, 0f, -amp)
        val buf = FloatArray(BackgroundGeometry.MAX_BLOBS * BackgroundGeometry.STRIDE)

        repeat(200) {
            var worstExcess = 0.0
            var worstBlob = -1
            for (c in 0 until shifts.size / 2) {
                BackgroundGeometry.fill(style, REF_W, REF_H, 0.0, StyleMotion.OFF, intensity, scales, buf, shifts[c * 2], shifts[c * 2 + 1])
                for (gy in 0 until GRID_Y) {
                    val y = (gy + 0.5f) / GRID_Y * REF_H
                    val limit = if (y < UPPER_ZONE * REF_H) upperMax else lowerMax
                    for (gx in 0 until GRID_X) {
                        val x = (gx + 0.5f) / GRID_X * REF_W
                        val colour = BackgroundGeometry.compositeAt(x, y, REF_W, REF_H, buf, n, glowArgb, base, vignette)
                        val excess = AccentMath.relativeLuminance(colour) - limit
                        if (excess > worstExcess) {
                            worstExcess = excess
                            worstBlob = strongestBlob(x, y, buf, n)
                        }
                    }
                }
            }
            if (worstBlob < 0) return scales
            if (scales[worstBlob] <= MIN_SCALE) return scales
            scales[worstBlob] = max(MIN_SCALE, scales[worstBlob] * STEP)
        }
        return scales
    }

    private fun strongestBlob(x: Float, y: Float, blobs: FloatArray, n: Int): Int {
        var best = 0
        var bestA = -1f
        for (i in 0 until n) {
            val o = i * BackgroundGeometry.STRIDE
            val d = sqrt((x - blobs[o]) * (x - blobs[o]) + (y - blobs[o + 1]) * (y - blobs[o + 1])) / blobs[o + 2]
            val a = blobs[o + 3] * BackgroundGeometry.falloff(d)
            if (a > bestA) { bestA = a; best = i }
        }
        return best
    }

    /** Worst (smallest) text contrast over the grid for the given scales; used by the tests. */
    fun worstContrast(
        style: StyleBackground,
        glowArgb: Int,
        intensity: StyleIntensity,
        scales: FloatArray,
        textArgb: Int,
        zoneUpper: Boolean,
    ): Double {
        val n = BackgroundGeometry.blobCount(style)
        val base = BackgroundGeometry.baseColor(glowArgb)
        val vignette = BackgroundGeometry.vignette(style)
        val buf = FloatArray(BackgroundGeometry.MAX_BLOBS * BackgroundGeometry.STRIDE)
        BackgroundGeometry.fill(style, REF_W, REF_H, 0.0, StyleMotion.OFF, intensity, scales, buf)
        var worst = Double.MAX_VALUE
        for (gy in 0 until GRID_Y) {
            val y = (gy + 0.5f) / GRID_Y * REF_H
            if ((y < UPPER_ZONE * REF_H) != zoneUpper) continue
            for (gx in 0 until GRID_X) {
                val x = (gx + 0.5f) / GRID_X * REF_W
                val colour = BackgroundGeometry.compositeAt(x, y, REF_W, REF_H, buf, n, glowArgb, base, vignette)
                worst = minOf(worst, AccentMath.contrastRatio(textArgb, colour))
            }
        }
        return worst
    }
}

/** Throttle levels for the background clock when the device cannot hold the target rate. */
object StyleFrameThrottle {
    /** Update intervals: 30 fps, 20 fps, 10 fps, then static (0). */
    private val LEVELS_NANOS = longArrayOf(33_333_333L, 50_000_000L, 100_000_000L, 0L)

    const val STATIC_LEVEL = 3
    const val SAMPLE_COUNT = 30

    /** A level is dropped when the real tick interval averaged this much over the intended one. */
    const val OVERRUN_FACTOR = 1.4

    fun intervalNanos(level: Int): Long = LEVELS_NANOS[level.coerceIn(0, STATIC_LEVEL)]

    /** The next level given the average real interval between ticks at [level]; never goes back up. */
    fun nextLevel(level: Int, averageTickNanos: Long): Int {
        if (level >= STATIC_LEVEL) return STATIC_LEVEL
        val intended = intervalNanos(level)
        return if (averageTickNanos > intended * OVERRUN_FACTOR) level + 1 else level
    }
}

