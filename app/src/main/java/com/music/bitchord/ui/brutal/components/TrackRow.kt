package com.music.bitchord.ui.brutal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.ui.brutal.theme.BrutalDimens
import com.music.bitchord.ui.brutal.theme.BrutalShapes
import com.music.bitchord.ui.brutal.theme.LocalBrutalPalette
import com.music.bitchord.ui.brutal.theme.LocalBrutalType

private const val THUMB_PX = 240

/**
 * One track in a list, in the mockup's layout: the title in white, the artist in the accent, the
 * duration chip under them, and a rounded-square thumbnail at the end.
 *
 * - **Never a fixed height**: the row is at least 72 dp and grows with the font; the title takes up to
 *   two lines.
 * - **Tap** opens ([onClick]); **long-press** ([onLongClick]) is the menu hook. A long-press must
 *   always have a visible equivalent, so [onOverflow] draws a 48 dp "more options" button at the end;
 *   TalkBack also gets the menu as a custom action on the row.
 * - The overflow button is a **sibling** of the clickable row content, not a child, so it stays its
 *   own control (`clickable` merges its children into a single node).
 * - The row is read as one phrase: "Too Young, Post Malone, 3 minutes 45 seconds".
 * - Thumbnail from [artworkUrl] through Coil and the app's image loader, or from the [artwork] slot.
 */
@Composable
fun TrackRow(
    title: String,
    artist: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    durationMs: Long? = null,
    artworkUrl: String? = null,
    artwork: (@Composable (Modifier) -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onOverflow: (() -> Unit)? = null,
) {
    val palette = LocalBrutalPalette.current
    val type = LocalBrutalType.current
    val spokenDuration = if (durationMs != null) spokenDuration(durationMs) else null
    val spoken = if (spokenDuration != null) {
        stringResource(R.string.brutal_track_row_description, title, artist, spokenDuration)
    } else {
        "$title, $artist"
    }
    val openLabel = stringResource(R.string.brutal_open_item, title)
    val moreLabel = stringResource(R.string.brutal_more_options_for, title)
    val menu = onOverflow ?: onLongClick
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .brutalClickable(
                    onClick = onClick,
                    onClickLabel = openLabel,
                    role = Role.Button,
                    shape = BrutalShapes.ListArtwork,
                    onLongClick = onLongClick ?: onOverflow,
                    onLongClickLabel = moreLabel,
                )
                // Clears the chip and the texts inside: the row is read as the one phrase, not twice.
                .clearAndSetSemantics {
                    contentDescription = spoken
                    if (menu != null) {
                        customActions = listOf(CustomAccessibilityAction(moreLabel) { menu(); true })
                    }
                }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = type.listTitle.copy(color = palette.white),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist,
                    style = type.listArtist.copy(color = palette.accentText),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (durationMs != null) {
                    DurationChip(durationMs, Modifier.padding(top = 8.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .size(BrutalDimens.ListArtwork)
                    .clip(BrutalShapes.ListArtwork)
                    .background(palette.glassBody),
            ) {
                if (artwork != null) {
                    artwork(Modifier.size(BrutalDimens.ListArtwork))
                } else {
                    AsyncImage(
                        model = artworkUrl.artworkAt(THUMB_PX),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(BrutalDimens.ListArtwork),
                    )
                }
            }
        }
        if (onOverflow != null) {
            OverflowButton(onClick = onOverflow, description = moreLabel)
        }
    }
}

/** Three dots in the accent on a 48 dp touch target; the visible equivalent of a long-press. */
@Composable
fun OverflowButton(onClick: () -> Unit, description: String, modifier: Modifier = Modifier) {
    val dots = LocalBrutalPalette.current.accentText
    Box(
        modifier
            .size(BrutalDimens.MinTouch)
            .brutalClickable(onClick = onClick, role = Role.Button, shape = CircleShape)
            .semantics { contentDescription = description }
            .drawBehind {
                val r = 2.5.dp.toPx()
                val gap = 7.dp.toPx()
                val cx = size.width / 2f
                val cy = size.height / 2f
                for (i in -1..1) drawCircle(dots, r, Offset(cx, cy + i * gap))
            },
    )
}
