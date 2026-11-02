package com.music.bitchord.ui.brutal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.music.bitchord.R
import com.music.bitchord.ui.brutal.theme.BrutalAngles
import com.music.bitchord.ui.brutal.theme.BrutalDimens
import com.music.bitchord.ui.brutal.theme.BrutalShapes
import com.music.bitchord.ui.brutal.theme.LocalBrutalPalette
import com.music.bitchord.ui.brutal.theme.LocalBrutalType

/**
 * A loading placeholder in the brutal language: the glass body colour with a **static** diagonal
 * hatch and a hairline. It does **not** shimmer: nothing in this look animates while idle (spec
 * 7.5), and a hatch reads as "unfinished" without moving. Hidden from TalkBack on its own; put a
 * group of them in [BrutalLoadingGroup] to announce "Loading" once.
 */
@Composable
fun BrutalSkeleton(modifier: Modifier = Modifier, shape: Shape = BrutalShapes.Tile) {
    val palette = LocalBrutalPalette.current
    Box(
        modifier
            .clip(shape)
            .background(palette.glassBody)
            .drawWithCache {
                val hatch = Path()
                val step = 14.dp.toPx()
                var x = -size.height
                while (x < size.width) {
                    hatch.moveTo(x, size.height)
                    hatch.lineTo(x + size.height, 0f)
                    x += step
                }
                val stroke = Stroke(width = 1.dp.toPx())
                onDrawBehind { drawPath(hatch, Color.White.copy(alpha = 0.05f), style = stroke) }
            }
            .border(1.dp, palette.hairline, shape)
            .clearAndSetSemantics { },
    )
}

/** A tile-shaped skeleton at the aspect the real tile will have. */
@Composable
fun BrutalSkeletonTile(modifier: Modifier = Modifier, heightToWidth: Float = 1f) {
    BrutalSkeleton(modifier.aspectRatio(1f / clampTileAspect(heightToWidth)), BrutalShapes.Tile)
}

/** A row-shaped skeleton: two text bars, a chip and a thumbnail, as [TrackRow] lays them out. */
@Composable
fun BrutalSkeletonRow(modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BrutalSkeleton(Modifier.fillMaxWidth(0.7f).height(20.dp), BrutalShapes.Stadium)
            BrutalSkeleton(Modifier.fillMaxWidth(0.4f).height(14.dp), BrutalShapes.Stadium)
            BrutalSkeleton(Modifier.width(63.dp).height(BrutalDimens.ChipMinHeight), BrutalShapes.Stadium)
        }
        Spacer(Modifier.width(12.dp))
        BrutalSkeleton(Modifier.size(BrutalDimens.ListArtwork), BrutalShapes.ListArtwork)
    }
}

/**
 * Groups skeletons so TalkBack hears **"Loading"** once instead of nothing or a stack of blanks.
 * Polite live region: it announces when it appears, without cutting off what is being read.
 */
@Composable
fun BrutalLoadingGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val description = stringResource(R.string.brutal_loading)
    Column(
        modifier.semantics {
            contentDescription = description
            liveRegion = LiveRegionMode.Polite
        },
    ) { content() }
}

/**
 * Nothing to show yet. A sticker with the title, an optional line of explanation and an optional
 * accent button; the same plate as everywhere else, so empty reads as part of the page, not an
 * afterthought. Never clips: everything wraps and grows.
 */
@Composable
fun BrutalEmptyState(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.brutal_empty_title_default),
    body: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val palette = LocalBrutalPalette.current
    val type = LocalBrutalType.current
    Column(
        modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StickerLabel(lines = listOf(title), angle = BrutalAngles.STICKER)
        if (body != null) {
            Text(
                text = body,
                style = type.statement.copy(color = palette.textMuted),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
        if (actionLabel != null && onAction != null) {
            BrutalButton(text = actionLabel, onClick = onAction, modifier = Modifier.padding(top = 20.dp))
        }
    }
}

/**
 * Something went wrong. The same sticker with an ERROR line, the message, and a retry button.
 * Announced assertively when it appears (an assertive live region): a failure should interrupt.
 */
@Composable
fun BrutalErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.brutal_error_title_default),
    retryLabel: String = stringResource(R.string.brutal_retry),
) {
    val palette = LocalBrutalPalette.current
    val type = LocalBrutalType.current
    Column(
        modifier
            .fillMaxWidth()
            .padding(24.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StickerLabel(
            lines = listOf(title),
            metaLabel = stringResource(R.string.brutal_error_tag),
            angle = BrutalAngles.STICKER,
        )
        Text(
            text = message,
            style = type.statement.copy(color = palette.white),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp),
        )
        BrutalButton(text = retryLabel, onClick = onRetry, modifier = Modifier.padding(top = 20.dp))
    }
}
