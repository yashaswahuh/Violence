package com.music.bitchord.ui.brutal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.R
import com.music.bitchord.ui.brutal.theme.BrutalDimens
import com.music.bitchord.ui.brutal.theme.BrutalShapes
import com.music.bitchord.ui.brutal.theme.LocalBrutalPalette
import com.music.bitchord.ui.brutal.theme.LocalBrutalType
import java.time.LocalDate

/**
 * Text turned to run bottom to top, **with its layout size swapped** (see [verticalText]): the parent
 * measures a box as wide as one line is tall and as tall as the text is long. One line; it ellipsises
 * at the incoming maximum height, so a long title never outgrows the tile it labels. Its font follows
 * the system font size (no cap): the lane it sits in widens instead of clipping.
 *
 * Accessibility: it is an ordinary text node, read once in normal reading order; the turn does not
 * change that.
 */
@Composable
fun VerticalLabel(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalBrutalType.current.verticalTitle,
    color: Color = Color.White,
) {
    Text(
        text = text,
        style = style.copy(color = color),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.verticalText(),
    )
}

/**
 * The date stamp beside a tile, `dd mm yy` in the accent, in the mono face, turned bottom to top
 * (the mockup's `28 11 22`). The digits are visual; TalkBack hears the date in words in the device
 * language ("28 November 2022").
 */
@Composable
fun DateStamp(
    date: LocalDate,
    modifier: Modifier = Modifier,
    color: Color = LocalBrutalPalette.current.accentText,
) {
    val type = LocalBrutalType.current
    val spoken = spokenDate(date)
    Text(
        text = formatDateStamp(date),
        style = type.mono.copy(fontSize = 11.sp, lineHeight = 14.sp, color = color),
        maxLines = 1,
        softWrap = false,
        // On a short tile at a large font there is not room for all eight characters: shorten with an
        // ellipsis at the lane's height instead of cutting a digit in half.
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .verticalText()
            .clearAndSetSemantics { contentDescription = spoken },
    )
}

/** The duration spoken as words ("3 minutes 45 seconds"), from string resources so it follows the language. */
@Composable
fun spokenDuration(durationMs: Long): String {
    val p = durationParts(durationMs)
    return if (p.hours > 0) {
        stringResource(R.string.brutal_duration_spoken_hms, p.hours, p.minutes, p.seconds)
    } else {
        stringResource(R.string.brutal_duration_spoken_ms, p.minutes, p.seconds)
    }
}

/**
 * The accent pill with a track length (the mockup's `03:45`): solid accent, on-accent text, tabular
 * digits, never shorter than 20 dp and **never a fixed height** (it grows with the font). It is
 * information, not a control, so it has no touch target; its text is read as a spoken duration.
 */
@Composable
fun DurationChip(durationMs: Long, modifier: Modifier = Modifier) {
    val palette = LocalBrutalPalette.current
    val type = LocalBrutalType.current
    val spoken = spokenDuration(durationMs)
    Box(
        modifier
            .heightIn(min = BrutalDimens.ChipMinHeight)
            .background(palette.accentFill, BrutalShapes.Stadium)
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .clearAndSetSemantics { contentDescription = spoken },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = formatDuration(durationMs),
            style = type.chip.copy(color = palette.onAccent),
            maxLines = 1,
            softWrap = false,
        )
    }
}
