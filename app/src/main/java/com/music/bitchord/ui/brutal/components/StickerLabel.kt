package com.music.bitchord.ui.brutal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.bitchord.ui.brutal.theme.BrutalAngles
import com.music.bitchord.ui.brutal.theme.BrutalDimens
import com.music.bitchord.ui.brutal.theme.LocalBrutalPalette
import com.music.bitchord.ui.brutal.theme.LocalBrutalType
import com.music.bitchord.ui.brutal.theme.withDisplayFontScale
import java.util.Locale

/**
 * The yellow sticker of the mockup (ABOUT / PRIVACY / VERSION 0.4): a solid accent plate with square
 * corners, tilted about -12 degrees, with 2 dp black rules between its rows.
 *
 * - [lines]: the heavy display rows. The first is the heaviest (Archivo Black), the following rows are
 *   the same size in a lighter weight (the mockup's PRIVACY). Latin text is set in italic caps.
 * - [metaLabel] and [metaValue]: the last row ("VERSION" and "0.4"); the value is in the mono face.
 * - The tilt is a **static** `graphicsLayer`. It takes part in layout (`rotatedLayout`), so the parent
 *   gets the size of the tilted plate and nothing is clipped.
 * - **At 200 percent font** the plate grows: lines wrap and the plate gets taller. The display rows are
 *   capped at a font scale of 1.3 (spec 5.2); the meta row scales fully.
 * - **One accessibility node**: a heading read as one phrase in horizontal reading order
 *   ("About, Privacy, Version 0.4"); the rows and the tilt are invisible to TalkBack. [description]
 *   overrides the default phrase.
 */
@Composable
fun StickerLabel(
    lines: List<String>,
    modifier: Modifier = Modifier,
    metaLabel: String? = null,
    metaValue: String? = null,
    angle: Float = BrutalAngles.STICKER,
    description: String? = null,
) {
    val palette = LocalBrutalPalette.current
    val type = LocalBrutalType.current
    val uppercase = type.profile.usesUppercase
    val ink = palette.onAccent
    val spoken = description ?: buildList {
        addAll(lines)
        val meta = listOfNotNull(metaLabel, metaValue).joinToString(" ")
        if (meta.isNotEmpty()) add(meta)
    }.joinToString(", ")
    val rule = BrutalDimens.StickerRule

    Column(
        modifier
            .rotatedLayout(angle)
            .clearAndSetSemantics {
                heading()
                contentDescription = spoken
            }
            .background(palette.accentFill),
    ) {
        // Display rows are drawn at a capped font scale; the rest of the page keeps scaling.
        CompositionLocalProvider(LocalDensity provides LocalDensity.current.withDisplayFontScale()) {
            lines.forEachIndexed { index, line ->
                Text(
                    text = if (uppercase) line.uppercase(Locale.getDefault()) else line,
                    style = (if (index == 0) type.sticker else type.stickerLight).copy(color = ink),
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            val y = size.height - rule.toPx() / 2f
                            drawLine(ink, Offset(0f, y), Offset(size.width, y), strokeWidth = rule.toPx())
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        if (metaLabel != null || metaValue != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                if (metaLabel != null) {
                    Text(
                        text = if (uppercase) metaLabel.uppercase(Locale.getDefault()) else metaLabel,
                        style = type.stickerMeta.copy(color = ink),
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (metaValue != null) {
                    Text(
                        text = metaValue,
                        style = type.mono.copy(color = ink),
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}
