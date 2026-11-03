package com.music.bitchord.ui.brutal.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.music.bitchord.R

/**
 * Which type treatment a language gets (spec 4.3). None of the bundled fonts covers Cyrillic,
 * Vietnamese, Hebrew, Devanagari, Thai or CJK completely, so those languages use the system font
 * with the same weights and **no** letter-spacing, no synthetic italic and no uppercase: a half
 * mixed line, or tracking that breaks Indic and Thai shaping, looks worse than a plain system face.
 */
enum class ScriptProfile {
    /** The designed look: Archivo Black, Archivo Narrow, Figtree. Latin and Latin-extended. */
    LATIN,

    /** The system sans for display and body. */
    SYSTEM_SANS,
    ;

    /** Uppercase is part of the design only where the script has case and the fonts cover it. */
    val usesUppercase: Boolean get() = this == LATIN

    companion object {
        /** Languages the bundled Latin fonts cover completely. Anything else: [SYSTEM_SANS]. */
        private val LATIN_LANGUAGES = setOf(
            "en", "es", "fr", "de", "it", "pt", "pl", "tr", "id", "ms", "nl", "sv", "da", "no", "nb",
            "fi", "cs", "sk", "ro", "hu", "ca", "hr", "sl", "fil", "tl",
        )

        /** [language] is an ISO 639 code (`Locale.language`); case and a region part are ignored. */
        fun forLanguage(language: String?): ScriptProfile {
            val code = language?.lowercase()?.substringBefore('-')?.substringBefore('_')
            return if (code != null && code in LATIN_LANGUAGES) LATIN else SYSTEM_SANS
        }
    }
}

/**
 * Archivo Black (display), Archivo Narrow (stamps), Figtree (body and UI), IBM Plex Mono (rare mono),
 * all bundled under the SIL Open Font License 1.1 (see NOTICE; no network fonts). Variable weights
 * need Android 8 (API 26), which is the app's minSdk.
 *
 * Archivo Black and Plex Mono are static files with one weight each. Every weight the app can ask
 * for is mapped onto that one file, so Compose never fakes a bold on top of a face that is already
 * heavy.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
object BrutalFonts {
    private val ALL_WEIGHTS = listOf(300, 400, 500, 600, 700, 800, 900)

    val Display: FontFamily = FontFamily(ALL_WEIGHTS.map { Font(R.font.brutal_display, FontWeight(it)) })

    val Mono: FontFamily = FontFamily(ALL_WEIGHTS.map { Font(R.font.brutal_mono, FontWeight(it)) })

    /** Figtree, variable, weight 300 to 900. */
    val Body: FontFamily = FontFamily(
        ALL_WEIGHTS.map {
            Font(R.font.brutal_body, FontWeight(it), variationSettings = FontVariation.Settings(FontVariation.weight(it)))
        },
    )

    /** Archivo Narrow, variable, weight 400 to 700. */
    val Stamp: FontFamily = FontFamily(
        listOf(400, 500, 600, 700).map {
            Font(R.font.brutal_stamp, FontWeight(it), variationSettings = FontVariation.Settings(FontVariation.weight(it)))
        },
    )
}

val LocalBrutalType = staticCompositionLocalOf { BrutalTypeSet(ScriptProfile.LATIN) }

/**
 * The type scale of the brutal look: the sizes of spec section 1.4 (mockup pixels x 0.75, raised to
 * 11 sp as the smallest). [forProfile] gives the same set in either treatment.
 */
@Immutable
class BrutalTypeSet(val profile: ScriptProfile) {
    private val latin = profile == ScriptProfile.LATIN
    private val display: FontFamily = if (latin) BrutalFonts.Display else FontFamily.Default
    private val body: FontFamily = if (latin) BrutalFonts.Body else FontFamily.Default
    private val stampFamily: FontFamily = if (latin) BrutalFonts.Stamp else FontFamily.Default

    /** Tracking only where the script is Latin; zero elsewhere. */
    private fun tracking(em: Float) = if (latin) em.em else 0.em

    /** Sticker rows 1 and 2. Capped to a font scale of 1.3 where it is drawn (`withDisplayFontScale`). */
    val sticker = TextStyle(fontFamily = display, fontWeight = FontWeight.W900, fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = tracking(-0.01f))
    val stickerNumber = TextStyle(fontFamily = body, fontWeight = FontWeight.W700, fontSize = 18.sp, lineHeight = 22.sp)
    val stickerMeta = TextStyle(fontFamily = body, fontWeight = FontWeight.W500, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = tracking(0.04f))

    /** The statement line ("BRUTALISM is all about"). */
    val statement = TextStyle(fontFamily = body, fontWeight = FontWeight.W400, fontSize = 20.sp, lineHeight = 28.sp)
    val listTitle = TextStyle(fontFamily = body, fontWeight = FontWeight.W500, fontSize = 20.sp, lineHeight = 26.sp)
    val listArtist = TextStyle(fontFamily = body, fontWeight = FontWeight.W500, fontSize = 13.sp, lineHeight = 18.sp)
    val chip = TextStyle(fontFamily = body, fontWeight = FontWeight.W700, fontSize = 12.sp, lineHeight = 16.sp, fontFeatureSettings = "tnum")
    val caption = TextStyle(fontFamily = body, fontWeight = FontWeight.W400, fontSize = 12.sp, lineHeight = 16.sp)
    val barTitle = TextStyle(fontFamily = body, fontWeight = FontWeight.W500, fontSize = 20.sp, lineHeight = 26.sp)
    val barArtist = TextStyle(fontFamily = body, fontWeight = FontWeight.W400, fontSize = 14.sp, lineHeight = 18.sp)
    val railTab = TextStyle(fontFamily = body, fontWeight = FontWeight.W700, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = tracking(0.04f))
    val verticalTitle = TextStyle(fontFamily = body, fontWeight = FontWeight.W500, fontSize = 12.sp, lineHeight = 16.sp)

    /** Condensed date stamps, accent coloured where drawn. 11 sp is the smallest text in the app. */
    val stamp = TextStyle(fontFamily = stampFamily, fontWeight = FontWeight.W600, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = tracking(0.06f))

    val mono = TextStyle(fontFamily = if (latin) BrutalFonts.Mono else FontFamily.Default, fontWeight = FontWeight.W600, fontSize = 14.sp, lineHeight = 18.sp, fontFeatureSettings = "tnum")

    /**
     * The wordmark and the tag are brand marks, set in Latin in every language, with their tracking
     * (spec 4.3 rule 3 and 4): a brand name is not translated and must not change shape.
     */
    val wordmark = TextStyle(fontFamily = BrutalFonts.Body, fontWeight = FontWeight.W600, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 1.3.em)
    val tag = TextStyle(fontFamily = BrutalFonts.Body, fontWeight = FontWeight.W700, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.25.em)

    /**
     * The scale handed to Material, so every existing page wears the new type through the theme.
     * The sizes of the existing scale are kept (34/30/22/20/16/14/12/11), not the 48 sp sticker size:
     * pages use `displayLarge` for titles, and 48 sp would grow to 96 sp at a font scale of 2.
     */
    val material: Typography = Typography(
        displayLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.W900, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = tracking(-0.01f)),
        displayMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.W900, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = tracking(-0.01f)),
        displaySmall = TextStyle(fontFamily = display, fontWeight = FontWeight.W900, fontSize = 26.sp, lineHeight = 32.sp),
        headlineLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.W900, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = tracking(-0.01f)),
        headlineMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.W800, fontSize = 22.sp, lineHeight = 28.sp),
        headlineSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.W700, fontSize = 20.sp, lineHeight = 26.sp),
        titleLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.W700, fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.W600, fontSize = 16.sp, lineHeight = 22.sp),
        titleSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.W600, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.W400, fontSize = 16.sp, lineHeight = 22.sp),
        bodyMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.W400, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = body, fontWeight = FontWeight.W400, fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.W700, fontSize = 14.sp, lineHeight = 18.sp),
        labelMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.W700, fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = stamp,
    )
}
