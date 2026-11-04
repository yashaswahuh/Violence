package com.music.violence.ui.tv.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.violence.data.listentogether.ListenTogether
import com.music.violence.share.ShareItem
import com.music.violence.ui.tv.components.TvButton
import com.music.violence.ui.tv.components.TvDialog
import com.music.violence.ui.tv.components.TvDialogButton
import com.music.violence.ui.tv.components.tvInitialFocus
import com.music.violence.ui.tv.theme.TvGlass
import com.music.violence.ui.tv.theme.TvType
import kotlinx.coroutines.launch

/** The characters a party code is made of: digits and letters, never I, L or O. */
private const val CODE_KEYS = "0123456789ABCDEFGHJKMNPQRSTUVWXYZ"

/**
 * Listen Together on the TV: start a party, join one with a code, see who is in it, leave.
 *
 * Everything is operable with the remote. A TV has no keyboard worth using, so the code is entered
 * on an on-screen key grid (three rows of eleven: D-pad to a key, OK to enter it). A
 * `violence://party/<CODE>` link opened on the TV arrives as [initialCode] and lands on the same
 * screen with the code filled in, one press from joining.
 *
 * The TV is a listener: every device streams its own audio, and the party only exchanges small
 * sync messages, so the TV follows the party like any other member (the playback service does the
 * following). It needs a signed-in YouTube Music account and a Jam server, exactly as the phone does.
 * UNCLEAR on real hardware: written against the existing TV components and built, but not run on a
 * TV or emulator.
 */
@Composable
fun TvListenTogetherDialog(
    onDismiss: () -> Unit,
    initialCode: String? = null,
) {
    val state by ListenTogether.state.collectAsState()
    val waking by ListenTogether.serverWaking.collectAsState()
    val stalled by ListenTogether.stalledTrack.collectAsState()
    val scope = rememberCoroutineScope()

    var entry by remember { mutableStateOf(initialCode.orEmpty().take(ListenTogether.CODE_LENGTH)) }
    var keypadOpen by remember { mutableStateOf(initialCode != null) }
    var busy by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    val arrivedByLink = initialCode != null
    var showQr by remember { mutableStateOf(false) }

    // A QR code of the invite for a phone to scan (the TV has no NFC and no camera).
    val partyCode = state.code
    if (showQr && partyCode != null) {
        val customServer = ListenTogether.activePartyServerBase()?.takeIf { it != ListenTogether.defaultServer }
        TvShareDialog(item = ShareItem.JamItem(partyCode, customServer), onDismiss = { showQr = false })
        return
    }

    val title = "Listen Together"
    val message = when {
        state.inParty -> "Everyone hears the same music at the same moment."
        else -> null
    }

    TvDialog(onDismissRequest = onDismiss, title = title, message = message, width = 700.dp) {
        when {
            state.inParty -> {
                Text(
                    text = state.code.orEmpty(),
                    style = TvType.LargeTitle,
                    color = TvGlass.TextPrimary,
                    letterSpacing = 6.sp,
                )
                Text(
                    text = "Friends join with this code under Listen Together.",
                    style = TvType.Caption,
                    color = TvGlass.TextTertiary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                )
                MemberColumns(state.members.map { member ->
                    val tags = buildList {
                        if (member.isHost) add("host")
                        if (member.memberId == state.you?.memberId) add("you")
                        if (!member.connected) add("reconnecting")
                    }
                    member.displayName + if (tags.isEmpty()) "" else " (${tags.joinToString(", ")})"
                })
                Text(
                    text = "${state.members.size} of ${state.maxMembers} listening",
                    style = TvType.Caption,
                    color = TvGlass.TextSecondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
                stalled?.let { songTitle ->
                    Text(
                        text = "Can't play $songTitle on this TV. It will rejoin the party on the next song.",
                        style = TvType.Callout,
                        color = TvGlass.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                TvDialogButton(text = "Show QR Code", onClick = { showQr = true })
                TvDialogButton(
                    text = "Leave Party",
                    destructive = true,
                    initialFocus = true,
                    onClick = { scope.launch { ListenTogether.leaveParty() } },
                )
                TvDialogButton(text = "Done", onClick = onDismiss)
            }

            !ListenTogether.hasServer -> {
                Notice("Jam is not set up in this build yet. Ask the owner for a build that has a Jam server.")
                TvDialogButton(text = "OK", initialFocus = true, onClick = onDismiss)
            }

            !ListenTogether.canJoin() -> {
                Notice("Make a Violence profile (Settings > Profile) or sign in to YouTube Music (Settings > Account) first. Listen Together shows your name and picture.")
                TvDialogButton(text = "OK", initialFocus = true, onClick = onDismiss)
            }

            keypadOpen -> {
                Text(
                    text = entry.padEnd(ListenTogether.CODE_LENGTH, '_'),
                    style = TvType.LargeTitle,
                    color = TvGlass.TextPrimary,
                    letterSpacing = 6.sp,
                )
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CODE_KEYS.chunked(11).forEachIndexed { rowIndex, keys ->
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            keys.forEachIndexed { column, key ->
                                TvButton(
                                    text = key.toString(),
                                    modifier = Modifier.tvInitialFocus(!arrivedByLink && rowIndex == 0 && column == 0),
                                    // Never disabled: a disabled button cannot hold D-pad focus, and focus
                                    // would drop out of the grid while a join is in flight.
                                    onClick = {
                                        if (!busy && entry.length < ListenTogether.CODE_LENGTH) entry += key
                                        failure = null
                                    },
                                )
                            }
                        }
                    }
                }
                StatusLines(waking = waking, failure = failure)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvButton(
                        text = "Delete",
                        // Stays focusable when there is nothing to delete (a no-op), so emptying the
                        // code with the remote does not lose focus.
                        onClick = {
                            if (!busy) entry = entry.dropLast(1)
                            failure = null
                        },
                    )
                    TvButton(
                        text = "Clear",
                        onClick = {
                            if (!busy) entry = ""
                            failure = null
                        },
                    )
                    TvButton(
                        text = if (busy) "Joining..." else "Join",
                        isPrimary = true,
                        enabled = entry.length == ListenTogether.CODE_LENGTH,
                        modifier = Modifier.tvInitialFocus(arrivedByLink),
                        onClick = {
                            if (busy) return@TvButton
                            busy = true
                            failure = null
                            scope.launch {
                                failure = ListenTogether.joinParty(entry).exceptionOrNull()?.message
                                busy = false
                            }
                        },
                    )
                    TvButton(text = "Back", onClick = { if (!busy) keypadOpen = false })
                }
            }

            else -> {
                StatusLines(waking = waking, failure = failure)
                TvDialogButton(
                    text = if (busy) "Starting..." else "Start a Party",
                    initialFocus = true,
                    onClick = {
                        if (busy) return@TvDialogButton
                        busy = true
                        failure = null
                        scope.launch {
                            failure = ListenTogether.createParty().exceptionOrNull()?.message
                            busy = false
                        }
                    },
                )
                TvDialogButton(text = "Join with a Code", onClick = { if (!busy) keypadOpen = true })
                TvDialogButton(text = "Close", onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun Notice(text: String) {
    Text(
        text = text,
        style = TvType.Callout,
        color = TvGlass.TextSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 14.dp),
    )
}

/** "Waking up" while a sleeping free-hosted server starts, and the last failure, if any. */
@Composable
private fun StatusLines(waking: Boolean, failure: String?) {
    if (waking) {
        Text(
            text = "Waking up the Jam server... this can take up to a minute.",
            style = TvType.Callout,
            color = TvGlass.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    failure?.let {
        Text(
            text = it,
            style = TvType.Callout,
            color = TvGlass.Destructive,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/**
 * Names in two columns. Up to fifteen people have to fit on a 1080p TV below a title, a code and
 * two buttons, and nothing here can be scrolled with a remote, so a single tall list would not.
 */
@Composable
private fun MemberColumns(names: List<String>) {
    val half = (names.size + 1) / 2
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        listOf(names.take(half), names.drop(half)).forEach { column ->
            Column(modifier = Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                column.forEach { name ->
                    Text(
                        text = name,
                        style = TvType.Callout,
                        color = TvGlass.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
