package com.music.bitchord.ui.v2.chrome

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.music.bitchord.R
import com.music.bitchord.ui.navigation.AppNavigator
import com.music.bitchord.ui.navigation.AppRoute
import com.music.bitchord.ui.v2.a11y.v2Interactive
import com.music.bitchord.ui.v2.glass.v2Solid
import com.music.bitchord.ui.v2.theme.LocalV2Colors
import com.music.bitchord.ui.v2.theme.V2Dimens
import com.music.bitchord.ui.v2.theme.V2Shapes
import com.music.bitchord.ui.v2.theme.V2Type

/**
 * The placeholder Home of the new UI (UI-1): a title, a short note, and a tile for every
 * destination that is not a tab (the "hub"), each opening the *current* page through the same
 * [AppNavigator], plus a way into the classic Home. Solid surfaces only: pages carry no glass.
 * Replaced by the real new Home in a later phase.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun V2HomePlaceholder(
    navigator: AppNavigator,
    topPadding: Dp,
    onOpenClassic: () -> Unit = { V2HomeState.classic = true },
    modifier: Modifier = Modifier,
) {
    val colors = LocalV2Colors.current
    val hub = listOf(
        stringResource(R.string.profile_title) to AppRoute.Profile,
        stringResource(R.string.friends_title) to AppRoute.Friends,
        stringResource(R.string.listen_together) to AppRoute.ListenTogether,
        stringResource(R.string.receive_title) to AppRoute.Receive,
        stringResource(R.string.downloads) to AppRoute.Downloads,
        stringResource(R.string.recent_title) to AppRoute.Recent,
        stringResource(R.string.history) to AppRoute.History,
        stringResource(R.string.replay) to AppRoute.Replay,
        stringResource(R.string.settings) to AppRoute.Settings,
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = topPadding, start = V2Dimens.ScreenEdge, end = V2Dimens.ScreenEdge, bottom = 180.dp),
    ) {
        Text(
            text = stringResource(R.string.home),
            style = V2Type.display,
            color = colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(V2Dimens.Gap3))
        Box(
            Modifier
                .fillMaxWidth()
                .v2Solid(V2Shapes.Card)
                .padding(V2Dimens.Gap4),
        ) {
            Column {
                Text(stringResource(R.string.v2_home_preview_title), style = V2Type.title, color = colors.voltageText)
                Spacer(Modifier.height(V2Dimens.Gap2))
                Text(stringResource(R.string.v2_home_preview_body), style = V2Type.body, color = colors.ink2)
            }
        }
        Spacer(Modifier.height(V2Dimens.Gap5))
        Text(
            text = stringResource(R.string.v2_home_everything_else),
            style = V2Type.heading,
            color = colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(V2Dimens.Gap3))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(V2Dimens.Gap2),
            verticalArrangement = Arrangement.spacedBy(V2Dimens.Gap2),
        ) {
            hub.forEach { (label, route) ->
                Box(
                    modifier = Modifier
                        .v2Solid(V2Shapes.Chip)
                        .v2Interactive(label = label, role = Role.Button, onClick = { navigator.push(route) })
                        .padding(horizontal = V2Dimens.Gap4, vertical = V2Dimens.Gap3),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = V2Type.label, color = colors.ink)
                }
            }
        }
        Spacer(Modifier.height(V2Dimens.Gap5))
        val classic = stringResource(R.string.v2_open_classic_home)
        Box(
            modifier = Modifier
                .v2Solid(V2Shapes.Chip, colors.voltage)
                .v2Interactive(label = classic, role = Role.Button, onClick = onOpenClassic)
                .padding(horizontal = V2Dimens.Gap5, vertical = V2Dimens.Gap3),
            contentAlignment = Alignment.Center,
        ) {
            Text(classic, style = V2Type.label, color = androidx.compose.ui.graphics.Color.White)
        }
    }
}
