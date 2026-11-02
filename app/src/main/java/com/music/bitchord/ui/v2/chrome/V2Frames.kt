package com.music.bitchord.ui.v2.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.music.bitchord.R
import com.music.bitchord.ui.navigation.AppNavigator
import com.music.bitchord.ui.navigation.AppRoute
import com.music.bitchord.ui.v2.a11y.v2Interactive
import com.music.bitchord.ui.v2.rememberNewUi
import com.music.bitchord.ui.v2.theme.LocalV2Colors
import com.music.bitchord.ui.v2.theme.V2Dimens
import com.music.bitchord.ui.v2.theme.V2Type

/** Which page a shortcut strip belongs to. */
enum class V2Shortcuts { HOME, LIBRARY }

/** The chips a strip shows, as (label, route) with null meaning "open the Hub". Pure, for tests. */
fun v2ShortcutRoutes(kind: V2Shortcuts): List<Pair<Int, AppRoute?>> = when (kind) {
    V2Shortcuts.HOME -> listOf(
        R.string.downloads to AppRoute.Downloads,
        R.string.listen_together to AppRoute.ListenTogether,
        R.string.friends_title to AppRoute.Friends,
        R.string.receive_title to AppRoute.Receive,
        R.string.recent_title to AppRoute.Recent,
        R.string.replay to AppRoute.Replay,
        R.string.v2_hub_all to null,
    )
    V2Shortcuts.LIBRARY -> listOf(
        R.string.downloads to AppRoute.Downloads,
        R.string.spotify to AppRoute.Spotify,
        R.string.sources to AppRoute.Sources,
        R.string.history to AppRoute.History,
        R.string.v2_hub_all to null,
    )
}

/** [PaddingValues] with [extra] added to the top (the page padding here has only top and bottom). */
fun PaddingValues.plusTop(extra: Dp): PaddingValues =
    PaddingValues(top = calculateTopPadding() + extra, bottom = calculateBottomPadding())

/**
 * Hosts a current page (Home, Library) inside the new shell: the page is drawn exactly as it is,
 * with a strip of shortcut chips pinned above it (solid, no glass: pages carry none). [content]
 * receives the strip's height to add to the page's top padding so nothing hides under it. With the
 * New UI off this is the page and nothing else ([content] gets 0).
 *
 * The same composition shape either way, so switching the variant does not reset the page.
 */
@Composable
fun V2ShortcutFrame(
    kind: V2Shortcuts,
    navigator: AppNavigator,
    topPadding: Dp,
    content: @Composable (extraTop: Dp) -> Unit,
) {
    val enabled = rememberNewUi()
    var stripHeightPx by remember { mutableStateOf(0) }
    val extra = if (enabled) with(LocalDensity.current) { stripHeightPx.toDp() } else 0.dp
    Box(Modifier.fillMaxSize()) {
        content(extra)
        if (enabled) {
            val colors = LocalV2Colors.current
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = topPadding)
                    .fillMaxWidth()
                    .onSizeChanged { stripHeightPx = it.height }
                    .background(colors.background)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = V2Dimens.ScreenEdge, vertical = V2Dimens.Gap2),
                horizontalArrangement = Arrangement.spacedBy(V2Dimens.Gap2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                v2ShortcutRoutes(kind).forEach { (labelRes, route) ->
                    val label = stringResource(labelRes)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colors.surface, RoundedCornerShape(50))
                            .v2Interactive(
                                label = label,
                                role = Role.Button,
                                onClick = { if (route != null) navigator.push(route) else V2HubState.open = true },
                            )
                            .padding(horizontal = V2Dimens.Gap4, vertical = V2Dimens.Gap2),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, style = V2Type.label, color = colors.ink)
                    }
                }
            }
        }
    }
}
