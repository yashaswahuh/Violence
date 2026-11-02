package com.music.bitchord.ui.v2.chrome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.music.bitchord.R
import com.music.bitchord.ui.components.GlassBottomSheet
import com.music.bitchord.ui.navigation.AppNavigator
import com.music.bitchord.ui.navigation.AppRoute
import com.music.bitchord.ui.v2.a11y.v2Interactive
import com.music.bitchord.ui.v2.theme.LocalV2Colors
import com.music.bitchord.ui.v2.theme.V2Dimens
import com.music.bitchord.ui.v2.theme.V2Type

/** Whether the Hub sheet is up. Opened by the account avatar when the New UI is on. */
object V2HubState {
    var open by mutableStateOf(false)
}

/** One destination in the Hub: what it is called, its glyph, and the route it opens. */
data class HubEntry(val labelRes: Int, val icon: ImageVector, val route: AppRoute)

/** Every destination that is not a tab. The reachability test walks this list. */
val V2HubGroups: List<Pair<Int, List<HubEntry>>> = listOf(
    R.string.v2_hub_people to listOf(
        HubEntry(R.string.profile_title, Icons.Rounded.Person, AppRoute.Profile),
        HubEntry(R.string.friends_title, Icons.Rounded.Group, AppRoute.Friends),
        HubEntry(R.string.listen_together, Icons.Rounded.Groups, AppRoute.ListenTogether),
    ),
    R.string.v2_hub_share to listOf(
        HubEntry(R.string.receive_title, Icons.Rounded.Nfc, AppRoute.Receive),
        HubEntry(R.string.recent_title, Icons.Rounded.Share, AppRoute.Recent),
    ),
    R.string.v2_hub_music to listOf(
        HubEntry(R.string.downloads, Icons.Rounded.Download, AppRoute.Downloads),
        HubEntry(R.string.history, Icons.Rounded.History, AppRoute.History),
        HubEntry(R.string.replay, Icons.Rounded.Replay, AppRoute.Replay),
        HubEntry(R.string.spotify, Icons.Rounded.CloudDownload, AppRoute.Spotify),
        HubEntry(R.string.sources, Icons.Rounded.CloudDownload, AppRoute.Sources),
        HubEntry(R.string.equalizer, Icons.Rounded.Tune, AppRoute.Equalizer),
    ),
    R.string.v2_hub_you to listOf(
        HubEntry(R.string.account_scrobbling, Icons.Rounded.ManageAccounts, AppRoute.AccountScrobbling),
        HubEntry(R.string.settings, Icons.Rounded.Settings, AppRoute.Settings),
    ),
)

/**
 * The Hub: one glass sheet listing every destination that is not a tab, so nothing depends on
 * reaching a corner one-handed (the sheet rises from the bottom). Rows are 48 dp or taller,
 * labelled, grouped under headings. The same [AppNavigator] opens the current pages.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun V2Hub(navigator: AppNavigator, onDismiss: () -> Unit) {
    val colors = LocalV2Colors.current
    GlassBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = V2Dimens.ScreenEdge)
                .padding(bottom = V2Dimens.Gap5),
        ) {
            Text(
                text = stringResource(R.string.v2_hub_title),
                style = V2Type.title,
                color = colors.ink,
                modifier = Modifier.padding(vertical = V2Dimens.Gap3).semantics { heading() },
            )
            V2HubGroups.forEach { (headingRes, entries) ->
                Text(
                    text = stringResource(headingRes),
                    style = V2Type.label,
                    color = colors.voltageText,
                    modifier = Modifier.padding(top = V2Dimens.Gap3, bottom = V2Dimens.Gap1).semantics { heading() },
                )
                entries.forEach { entry ->
                    val label = stringResource(entry.labelRes)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .v2Interactive(
                                label = label,
                                role = Role.Button,
                                onClick = {
                                    onDismiss()
                                    navigator.push(entry.route)
                                },
                            )
                            .padding(vertical = V2Dimens.Gap2),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        Icon(entry.icon, contentDescription = null, tint = colors.ink2, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(V2Dimens.Gap4))
                        Text(label, style = V2Type.body, color = colors.ink)
                    }
                }
            }
            Spacer(Modifier.height(V2Dimens.Gap2))
        }
    }
}
