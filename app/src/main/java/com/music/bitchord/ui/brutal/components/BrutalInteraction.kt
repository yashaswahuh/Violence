package com.music.bitchord.ui.brutal.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.music.bitchord.ui.brutal.theme.BrutalDimens
import com.music.bitchord.ui.brutal.theme.LocalBrutalPalette

/**
 * Press and focus behaviour every brutal control shares:
 * - tap and optional long-press (a long-press always has a visible equivalent elsewhere in the
 *   component: callers also pass an overflow action);
 * - a visible **focus ring in the accent** (2 dp, spec 7.5; drawn in the accent-text colour, the lightened
 *   variant, so it keeps 3:1 even on the brightest glass), so keyboard and switch users can see where they are;
 * - a plain pressed state (a little less opaque) instead of a ripple;
 * - the role, click label and long-click label as given, so TalkBack announces what a tap does.
 *
 * Put it **after** `brutalGlass` so the glass clips first and the ring is drawn inside the shape.
 * Note: `clickable` merges the semantics of everything inside it into one node. A separate control
 * (a play button on a tile, an overflow button on a row) must therefore be a *sibling* of the
 * clickable region, never a child.
 */
@Composable
fun Modifier.brutalClickable(
    onClick: () -> Unit,
    onClickLabel: String? = null,
    role: Role? = Role.Button,
    shape: Shape = RectangleShape,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    enabled: Boolean = true,
    /** Pass one to observe or drive press and focus from outside; by default the modifier keeps its own. */
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
): Modifier {
    val source = interactionSource
    val focused by source.collectIsFocusedAsState()
    val pressed by source.collectIsPressedAsState()
    val ring = LocalBrutalPalette.current.accentText
    return this
        .graphicsLayer { alpha = if (pressed) 0.82f else 1f }
        .then(if (focused) Modifier.border(BrutalDimens.FocusRing, ring, shape) else Modifier)
        .combinedClickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onClick = onClick,
        )
}

/**
 * [brutalClickable] for a tab or a choice: the same focus ring and pressed state, with the selected
 * state and a role (a tab by default) that TalkBack announces ("selected, tab").
 */
@Composable
fun Modifier.brutalSelectable(
    selected: Boolean,
    onClick: () -> Unit,
    role: Role? = Role.Tab,
    shape: Shape = RectangleShape,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
): Modifier {
    val source = interactionSource
    val focused by source.collectIsFocusedAsState()
    val pressed by source.collectIsPressedAsState()
    val ring = LocalBrutalPalette.current.accentText
    return this
        .graphicsLayer { alpha = if (pressed) 0.82f else 1f }
        .then(if (focused) Modifier.border(BrutalDimens.FocusRing, ring, shape) else Modifier)
        .selectable(
            selected = selected,
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}

/** A square of at least [BrutalDimens.MinTouch] (48 dp): the touch target for a control drawn smaller. */
fun Modifier.minTouchTarget(visual: Dp = 0.dp): Modifier =
    size(maxOf(BrutalDimens.MinTouch, visual))
