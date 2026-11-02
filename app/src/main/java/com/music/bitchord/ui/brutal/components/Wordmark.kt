package com.music.bitchord.ui.brutal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.R
import com.music.bitchord.ui.brutal.glass.GlassKind
import com.music.bitchord.ui.brutal.glass.GlassLevel
import com.music.bitchord.ui.brutal.glass.LocalGlassLevel
import com.music.bitchord.ui.brutal.glass.brutalGlass
import com.music.bitchord.ui.brutal.theme.BrutalAngles
import com.music.bitchord.ui.brutal.theme.BrutalShapes
import com.music.bitchord.ui.brutal.theme.LocalBrutalPalette
import com.music.bitchord.ui.brutal.theme.LocalBrutalType
import com.music.bitchord.ui.brutal.theme.withDisplayFontScale
import java.util.Locale

/**
 * The glass level for a surface that is *not* allowed to sample the page live: FULL becomes SIMPLIFIED,
 * the others stay. Rail items, round buttons over the solid page and the masthead use it: they sit over
 * flat black, so a live backdrop would sample nothing and cost a capture (spec 6.2).
 */
@Composable
fun nonLiveGlassLevel(): GlassLevel {
    val level = LocalGlassLevel.current
    return if (level == GlassLevel.FULL) GlassLevel.SIMPLIFIED else level
}

/**
 * "V I O L E N C E": wide-tracked caps (letter-spacing 1.3 em, measured off the mockup), white, one
 * line, with the small tilted EXPERIMENTAL tag under it while [showTag] (it follows
 * [BrutalFlags.EXPERIMENTAL]; the tag text is a string resource).
 *
 * - The word is a **brand name**: always set in the Latin face with its tracking, in every language.
 * - It is a logo, so it is exempt from font scaling (a 1.3 em tracked word would not fit a phone at
 *   130 percent); the tag scales with the font, capped at 1.3.
 * - The tracking after the last letter is cancelled with start padding so the word is optically centred.
 * - One accessibility node, a heading: "Violence, experimental".
 */
@Composable
fun Wordmark(
    modifier: Modifier = Modifier,
    showTag: Boolean = BrutalFlags.EXPERIMENTAL,
    color: Color = Color.White,
) {
    val type = LocalBrutalType.current
    val tag = stringResource(R.string.brutal_tag_experimental)
    val spoken = if (showTag) stringResource(R.string.brutal_wordmark_description, tag) else "Violence"
    val unscaled = LocalDensity.current.withDisplayFontScale(1f)
    Column(
        modifier.clearAndSetSemantics {
            heading()
            contentDescription = spoken
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompositionLocalProvider(LocalDensity provides unscaled) {
            // The word's trailing letter-space (1.3 em) is added back as padding at the start.
            val trailing = with(unscaled) { (type.wordmark.fontSize.value * 1.3f).sp.toDp() }
            Text(
                text = "VIOLENCE",
                style = type.wordmark.copy(color = color),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(start = trailing),
            )
        }
        if (showTag) {
            Spacer(Modifier.height(10.dp))
            ExperimentalTag()
        }
    }
}

/** The accent tag, about 4 degrees off the plate it sits on. Not a button. */
@Composable
fun ExperimentalTag(modifier: Modifier = Modifier) {
    val palette = LocalBrutalPalette.current
    val type = LocalBrutalType.current
    val text = stringResource(R.string.brutal_tag_experimental)
    CompositionLocalProvider(LocalDensity provides LocalDensity.current.withDisplayFontScale()) {
        Text(
            text = text.uppercase(Locale.getDefault()),
            style = type.tag.copy(color = palette.onAccent),
            modifier = modifier
                .rotatedLayout(BrutalAngles.TAG)
                .background(palette.accentFill)
                .padding(start = 10.dp, end = 8.dp, top = 3.dp, bottom = 3.dp),
        )
    }
}

/**
 * The wordmark on its glass tile: the rounded masthead tilted about -8.5 degrees (the tilt takes part
 * in layout, so nothing is clipped). Drawn **without** a live backdrop: it sits over the flat header.
 * The tile has no fixed height; it grows with the tag and the font.
 */
@Composable
fun WordmarkMasthead(
    modifier: Modifier = Modifier,
    showTag: Boolean = BrutalFlags.EXPERIMENTAL,
    glow: Boolean = LocalBrutalPalette.current.glow,
) {
    Box(modifier.rotatedLayout(BrutalAngles.MASTHEAD)) {
        Box(
            Modifier
                .widthIn(min = 280.dp)
                .brutalGlass(BrutalShapes.Masthead, GlassKind.Floating, glow, textScrim = false, level = nonLiveGlassLevel())
                .padding(horizontal = 28.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Wordmark(showTag = showTag)
        }
    }
}
