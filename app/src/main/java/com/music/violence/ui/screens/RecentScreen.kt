package com.music.violence.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.violence.R
import com.music.violence.share.IncomingLinkHub
import com.music.violence.share.RecentShares
import com.music.violence.ui.icons.SfIcons

/**
 * The last things sent or received by tap, QR or link. On this device only; tapping one opens it
 * again the way a pasted link would, and asks before doing anything.
 */
@Composable
fun RecentScreen(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    val items by RecentShares.items.collectAsStateWithLifecycle()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SettingsPageTitle(stringResource(R.string.recent_title))
        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.recent_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 28.dp),
            )
        } else {
            SettingsGroup(topSpacing = 0.dp, footer = stringResource(R.string.recent_footer)) {
                items.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                    val direction = stringResource(if (item.sent) R.string.recent_sent else R.string.recent_received)
                    SettingsRow(
                        icon = if (item.sent) SfIcons.Share else SfIcons.Add,
                        title = item.title,
                        subtitle = listOfNotNull(direction, item.subtitle).joinToString("  ·  "),
                        onClick = { IncomingLinkHub.reopen(item.uri) },
                    )
                }
            }
            TextButton(onClick = RecentShares::clear, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.recent_clear))
            }
        }
    }
}
