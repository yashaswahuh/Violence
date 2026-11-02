package com.music.bitchord.ui.brutal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.bitchord.ui.brutal.glass.GlassKind
import com.music.bitchord.ui.brutal.glass.brutalGlass
import com.music.bitchord.ui.brutal.glass.brutalHalo
import com.music.bitchord.ui.brutal.theme.BrutalDimens
import com.music.bitchord.ui.brutal.theme.BrutalShapes
import com.music.bitchord.ui.brutal.theme.LocalBrutalPalette
import com.music.bitchord.ui.brutal.theme.LocalBrutalType
import java.util.Locale

/**
 * A vertical glass pill of the right-hand rail (the mockup's JAZZ, R&B, METAL ...): the label runs
 * bottom to top inside it. **Selected** = a solid accent plate with the on-accent label (and the
 * accent under-glow with Glow); **not selected** = a glass bubble with the label in the accent.
 *
 * - 48 dp wide (the touch target), at least 48 dp tall and **as tall as its label needs** plus
 *   16 dp at each end, so a long word or a large font makes it taller; never a fixed height.
 * - Glass is drawn without a live backdrop (the rail sits over the flat page, spec 6.2).
 * - Semantics: a tab, with its selected state, and its position ([index] of [count]) so TalkBack can
 *   say "tab 2 of 4". The label is the text itself.
 */
@Composable
fun GenreRailTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
    count: Int = 1,
    glow: Boolean = LocalBrutalPalette.current.glow,
) {
    val palette = LocalBrutalPalette.current
    val type = LocalBrutalType.current
    val shape = BrutalShapes.Stadium
    val surface = if (selected) {
        Modifier
            .brutalHalo(shape, glow)
            .background(palette.accentFill, shape)
    } else {
        Modifier.brutalGlass(shape, GlassKind.Floating, glow, textScrim = false, level = nonLiveGlassLevel())
    }
    Box(
        modifier
            .width(BrutalDimens.RailItemWidth)
            .heightIn(min = BrutalDimens.MinTouch)
            .then(surface)
            .brutalSelectable(selected = selected, onClick = onClick, role = Role.Tab, shape = shape)
            .semantics { collectionItemInfo = CollectionItemInfo(index, 1, 0, 1) },
        contentAlignment = Alignment.Center,
    ) {
        VerticalLabel(
            text = if (type.profile.usesUppercase) text.uppercase(Locale.getDefault()) else text,
            style = type.railTab,
            color = if (selected) palette.onAccent else palette.accentText,
            modifier = Modifier.padding(vertical = BrutalDimens.RailTabEndPadding),
        )
    }
}

/**
 * A round (or, with [tall], a stadium) glass button: the collapse chevron and the search button of the
 * rail. 48 dp wide, 48 dp tall (72 dp for the tall one); icon 24 dp in the accent. Glass without a
 * live backdrop. [description] is spoken; [state] (optional) is the state it reports ("collapsed").
 */
@Composable
fun RoundGlassButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tall: Boolean = false,
    state: String? = null,
    glow: Boolean = LocalBrutalPalette.current.glow,
) {
    val palette = LocalBrutalPalette.current
    val shape = BrutalShapes.Stadium
    Box(
        modifier
            .size(width = BrutalDimens.RailItemWidth, height = if (tall) BrutalDimens.SearchButtonHeight else BrutalDimens.MinTouch)
            .brutalGlass(shape, GlassKind.Floating, glow, textScrim = false, level = nonLiveGlassLevel())
            .brutalClickable(onClick = onClick, role = Role.Button, shape = shape)
            .semantics {
                contentDescription = description
                if (state != null) stateDescription = state
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(24.dp))
    }
}

/**
 * A solid accent button (empty and error states, dialogs): stadium plate, on-accent label, 48 dp at
 * least and as tall as its text needs, the accent under-glow with Glow.
 */
@Composable
fun BrutalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glow: Boolean = LocalBrutalPalette.current.glow,
) {
    val palette = LocalBrutalPalette.current
    val type = LocalBrutalType.current
    val shape = BrutalShapes.Stadium
    Box(
        modifier
            .heightIn(min = BrutalDimens.MinTouch)
            .brutalHalo(shape, glow)
            .background(palette.accentFill, shape)
            .brutalClickable(onClick = onClick, role = Role.Button, shape = shape)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = type.railTab.copy(color = palette.onAccent, fontSize = type.chip.fontSize),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
