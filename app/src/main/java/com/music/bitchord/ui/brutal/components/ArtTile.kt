package com.music.bitchord.ui.brutal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.ui.brutal.glass.GlassKind
import com.music.bitchord.ui.brutal.glass.GlassLevel
import com.music.bitchord.ui.brutal.glass.LocalGlassLevel
import com.music.bitchord.ui.brutal.glass.brutalGlass
import com.music.bitchord.ui.brutal.theme.BrutalDimens
import com.music.bitchord.ui.brutal.theme.BrutalShapes
import com.music.bitchord.ui.brutal.theme.LocalBrutalPalette
import com.music.bitchord.ui.brutal.theme.LocalBrutalType
import com.music.bitchord.ui.components.ContinuousCornerShape
import java.time.LocalDate

/** Artwork is requested at this size: the largest a tile shows on a phone, so the loader's cache is shared. */
private const val ART_PX = 480

/**
 * The round play button on a tile: a solid accent disc with an on-accent triangle (optionally with an
 * accent glow).
 *
 * **The touch target is 48 dp even though the disc is drawn at [visualSize]** (28 dp by default):
 * the disc sits in a 48 dp square that is the clickable area. [description] is spoken ("Play Weekend").
 */
@Composable
fun PlayButton(
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    visualSize: Dp = BrutalDimens.PlayDisc,
    glow: Boolean = LocalBrutalPalette.current.glow,
) {
    val palette = LocalBrutalPalette.current
    val level = LocalGlassLevel.current
    val fill = palette.accentFill
    val triangle = palette.onAccent
    val haloColor = palette.accentUi
    val gain = palette.accent.glowGain
    Box(
        modifier
            .size(maxOf(BrutalDimens.MinTouch, visualSize))
            .brutalClickable(onClick = onClick, role = Role.Button, shape = CircleShape)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(visualSize)
                .drawWithCache {
                    val r = size.minDimension / 2f
                    val tri = Path().apply {
                        moveTo(size.width * 0.40f, size.height * 0.28f)
                        lineTo(size.width * 0.73f, size.height * 0.50f)
                        lineTo(size.width * 0.40f, size.height * 0.72f)
                        close()
                    }
                    val halo = if (glow && level != GlassLevel.SOLID) {
                        Brush.radialGradient(
                            listOf(haloColor.copy(alpha = (0.40f * gain).coerceAtMost(0.60f)), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f),
                            radius = r * 2.1f,
                        )
                    } else {
                        null
                    }
                    onDrawBehind {
                        halo?.let { drawCircle(it, radius = r * 2.1f) }
                        drawCircle(fill, radius = r)
                        drawPath(tri, triangle)
                    }
                },
        )
    }
}

/**
 * Artwork in a glass frame: the mockup's tile. The frame is the glass kit's [GlassKind.Tile] (a frosted
 * ring and a dark inner line, **never a live backdrop**, so a tile is safe in any scrolling list).
 *
 * - [heightToWidth]: the tile's aspect, clamped to 1.0 to 1.8 (the mockup's squares to tall portraits).
 * - Artwork comes from [artworkUrl] through Coil and the app's existing image loader and cache, or
 *   from [artwork] (a slot, for previews and tests, so nothing touches the network).
 * - [onPlay] adds the round play button at the bottom end. It is a **sibling** of the clickable tile
 *   (`clickable` merges its children into one node, which would swallow the button), so TalkBack
 *   reads the tile first and then its play button.
 * - The tile is a button with the label "Open <title>"; [onLongClick] is the long-press hook (give
 *   the same action a visible control elsewhere).
 */
@Composable
fun ArtTile(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    artworkUrl: String? = null,
    artwork: (@Composable (Modifier) -> Unit)? = null,
    heightToWidth: Float = 1f,
    glow: Boolean = LocalBrutalPalette.current.glow,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    onPlay: (() -> Unit)? = null,
) {
    val palette = LocalBrutalPalette.current
    val aspect = clampTileAspect(heightToWidth)
    val spoken = listOfNotNull(title, subtitle).joinToString(", ")
    val openLabel = stringResource(R.string.brutal_open_item, title)
    val playLabel = stringResource(R.string.brutal_play_item, title)
    Box(modifier.aspectRatio(1f / aspect)) {
        val tap = if (onClick != null) {
            Modifier.brutalClickable(
                onClick = onClick,
                onClickLabel = openLabel,
                role = Role.Button,
                shape = BrutalShapes.Tile,
                onLongClick = onLongClick,
                onLongClickLabel = onLongClickLabel,
            )
        } else {
            Modifier
        }
        Box(
            Modifier
                .fillMaxSize()
                .brutalGlass(BrutalShapes.Tile, GlassKind.Tile, glow)
                .then(tap)
                .clearAndSetSemantics { contentDescription = spoken },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(BrutalDimens.TileFrame)
                    .clip(INNER_ART_SHAPE)
                    .background(palette.glassBody),
            ) {
                if (artwork != null) {
                    artwork(Modifier.fillMaxSize())
                } else {
                    AsyncImage(
                        model = artworkUrl.artworkAt(ART_PX),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        if (onPlay != null) {
            PlayButton(
                onClick = onPlay,
                description = playLabel,
                glow = glow,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 2.dp, bottom = 2.dp),
            )
        }
    }
}

/** The artwork's own corner: the tile's outer radius less its frame, so the two curves stay concentric. */
private val INNER_ART_SHAPE = ContinuousCornerShape(percent = 16)

/**
 * A tile with the vertical lane of the mockup beside it: the date stamp above the title, both turned
 * bottom to top along the tile's start edge, bottom-aligned to it. The lane is as tall as the tile
 * (the title ellipsises there) and as wide as the font is tall, so at 200 percent it widens and the
 * tile gives up that width: nothing clips.
 *
 * The lane **repeats** what the tile says for sighted users, so it is hidden from TalkBack: pass the
 * date to the tile as its `subtitle` (use [spokenDate]) and TalkBack hears it once.
 *
 * It is a custom `Layout` (the tile is measured first at the width that is left, then the lane at the
 * tile's height) so no intrinsic measurements are needed.
 */
@Composable
fun LabeledArtTile(
    label: String,
    modifier: Modifier = Modifier,
    date: LocalDate? = null,
    tile: @Composable (Modifier) -> Unit,
) {
    // The lane is as wide as its tallest line of text. Known from the type styles, so no child is
    // measured twice and no intrinsic query goes through the turned text (whose intrinsics are the
    // unturned ones).
    val type = LocalBrutalType.current
    val laneWidthPx = with(LocalDensity.current) {
        maxOf(type.verticalTitle.lineHeight.toPx(), 14.sp.toPx()).toInt() + 2
    }
    Layout(
        modifier = modifier,
        content = {
            Column(
                Modifier.clearAndSetSemantics { },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (date != null) DateStamp(date)
                VerticalLabel(label, modifier = Modifier.weight(1f, fill = false))
            }
            tile(Modifier)
        },
    ) { measurables, constraints ->
        val gap = BrutalDimens.LabelLaneGap.roundToPx()
        val laneMeasurable = measurables[0]
        val tileMeasurable = measurables[1]
        val laneWidth = laneWidthPx
        val bounded = constraints.hasBoundedWidth
        val tileWidth = (constraints.maxWidth - laneWidth - gap).coerceAtLeast(0)
        val tilePlaceable = tileMeasurable.measure(
            if (bounded) {
                Constraints(minWidth = tileWidth, maxWidth = tileWidth, minHeight = 0, maxHeight = Constraints.Infinity)
            } else {
                Constraints()
            },
        )
        val lane = laneMeasurable.measure(
            Constraints(minWidth = 0, maxWidth = laneWidth, minHeight = 0, maxHeight = tilePlaceable.height),
        )
        val width = laneWidth + gap + tilePlaceable.width
        layout(width, tilePlaceable.height) {
            tilePlaceable.place(laneWidth + gap, 0)
            lane.place((laneWidth - lane.width) / 2, tilePlaceable.height - lane.height)
        }
    }
}
