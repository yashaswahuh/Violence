package com.music.violence.ui.tv.dialogs

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import com.music.violence.data.profile.Friends
import com.music.violence.data.profile.ProfileManager
import com.music.violence.data.share.IncomingLink
import com.music.violence.playback.toMediaItem
import com.music.violence.share.IncomingLinkHub
import com.music.violence.share.ShareItem
import com.music.violence.share.SharedSongResolver
import com.music.violence.ui.components.QrCode
import com.music.violence.ui.profile.ProfileAvatar
import com.music.violence.ui.tv.components.TvDialog
import com.music.violence.ui.tv.components.TvDialogButton
import com.music.violence.ui.tv.theme.TvGlass
import com.music.violence.ui.tv.theme.TvType
import kotlinx.coroutines.launch

/**
 * A QR code and the link in words, for a phone to point its camera at. The TV has no NFC and
 * no camera: it shows, the phone scans. Only the shared item's own link is shown.
 */
@Composable
fun TvShareDialog(item: ShareItem, onDismiss: () -> Unit) {
    val title = when (item) {
        is ShareItem.SongItem -> item.ref.title ?: "Share a Song"
        is ShareItem.PlaylistItem -> item.ref.title
        is ShareItem.ProfileItem -> "Share ${item.card.displayName}"
        is ShareItem.JamItem -> "Invite to Jam ${item.code}"
    }
    TvDialog(
        onDismissRequest = onDismiss,
        title = title,
        message = "Point your phone's camera at this code. The link opens in Violence.",
        width = 560.dp,
    ) {
        QrCode(content = item.link, size = 220.dp)
        Spacer(Modifier.height(14.dp))
        Text(
            text = item.link,
            style = TvType.Caption,
            color = TvGlass.TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 3,
            modifier = Modifier.widthIn(max = 480.dp).padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(16.dp))
        TvDialogButton(text = "Done", initialFocus = true, onClick = onDismiss)
    }
}

/**
 * Songs and profiles that arrived as a link (opened on the TV, for example with `adb`) always ask
 * first. The title in a song link is only a label; the song is looked up only after a choice.
 */
@Composable
fun TvIncomingLinkDialogs(mediaController: MediaController?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val incoming by IncomingLinkHub.pending.collectAsState()
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        IncomingLinkHub.notices.collect {
            Toast.makeText(context, context.getString(com.music.violence.ui.screens.noticeRes(it)), Toast.LENGTH_SHORT).show()
        }
    }

    when (val link = incoming) {
        is IncomingLink.Song -> {
            val label = listOfNotNull(link.ref.title, link.ref.artist).joinToString(" — ").ifBlank { "A song from a shared link" }
            fun act(play: Boolean) {
                scope.launch {
                    busy = true
                    val song = SharedSongResolver.resolve(link.ref)
                    busy = false
                    val controller = mediaController
                    if (song == null || controller == null) {
                        Toast.makeText(context, "Could not find that song.", Toast.LENGTH_SHORT).show()
                    } else {
                        if (play) {
                            controller.setMediaItem(song.toMediaItem())
                            controller.prepare()
                            controller.play()
                        } else {
                            controller.addMediaItem(song.toMediaItem())
                            Toast.makeText(context, "Added to the queue", Toast.LENGTH_SHORT).show()
                        }
                        IncomingLinkHub.handled()
                    }
                }
            }
            TvDialog(
                onDismissRequest = { if (!busy) IncomingLinkHub.dismiss() },
                title = "Open This Song?",
                message = if (busy) "$label\nFinding the song…" else label,
            ) {
                TvDialogButton(text = "Play Now", initialFocus = true, enabled = !busy, onClick = { if (!busy) act(play = true) })
                TvDialogButton(text = "Add to Queue", enabled = !busy, onClick = { if (!busy) act(play = false) })
                TvDialogButton(text = "Cancel", onClick = { if (!busy) IncomingLinkHub.dismiss() })
            }
        }

        is IncomingLink.Profile -> {
            val card = link.card
            TvDialog(
                onDismissRequest = { IncomingLinkHub.dismiss() },
                title = "Add ${card.displayName} as a Friend?",
            ) {
                ProfileAvatar(card.avatarId, card.color, card.displayName, 64.dp)
                Spacer(Modifier.height(16.dp))
                TvDialogButton(
                    text = "Add Friend",
                    initialFocus = true,
                    onClick = {
                        val message = when (ProfileManager.addFriend(card)) {
                            is Friends.AddResult.Added -> "${card.displayName} added."
                            is Friends.AddResult.Updated -> "${card.displayName} is already a friend. Details updated."
                            Friends.AddResult.IsYou -> "That is your own profile."
                            Friends.AddResult.ListFull -> "Your friends list is full."
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        IncomingLinkHub.handled()
                    },
                )
                TvDialogButton(text = "Cancel", onClick = { IncomingLinkHub.dismiss() })
            }
        }

        else -> Unit
    }
}
