package com.music.bitchord.ui.v2.a11y

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.layout.defaultMinSize
import com.music.bitchord.ui.v2.theme.LocalV2Colors
import com.music.bitchord.ui.v2.theme.V2Dimens

/**
 * The accessibility defaults every new interactive component is built from, so a component cannot
 * forget them:
 *
 * - a touch target of at least [V2Dimens.MinTouch] (48 dp) both ways;
 * - a [label] (what TalkBack reads) and a [role];
 * - an optional [stateDescription] and [selected] state;
 * - a visible focus ring ([V2Dimens.FocusRingWidth], the theme's focus colour, 3:1 or better) when
 *   the control has keyboard, switch or D-pad focus.
 *
 * Content inside should be decorative (its own `contentDescription = null`) since the label here
 * names the whole control.
 */
@Composable
fun Modifier.v2Interactive(
    label: String,
    role: Role,
    onClick: () -> Unit,
    enabled: Boolean = true,
    selected: Boolean? = null,
    stateDescription: String? = null,
    shape: CornerBasedShape = RoundedCornerShape(50),
): Modifier {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val ring = LocalV2Colors.current.focusRing
    return this
        .defaultMinSize(minWidth = V2Dimens.MinTouch, minHeight = V2Dimens.MinTouch)
        .then(if (focused) Modifier.border(V2Dimens.FocusRingWidth, ring, shape) else Modifier)
        .semantics(mergeDescendants = true) {
            contentDescription = label
            if (selected != null) this.selected = selected
            if (stateDescription != null) this.stateDescription = stateDescription
        }
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}
