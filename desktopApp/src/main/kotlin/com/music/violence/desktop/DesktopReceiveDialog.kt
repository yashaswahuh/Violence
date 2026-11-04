package com.music.violence.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.violence.data.share.IncomingLink
import com.music.violence.data.share.IncomingLinks
import com.music.violence.ui.profile.ProfileAvatar
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

@Composable
internal fun DesktopReceiveDialog(
    initialLink: IncomingLink? = null,
    onDismiss: () -> Unit,
    onOpenLink: (IncomingLink) -> Unit,
) {
    var input by remember {
        mutableStateOf(
            when (initialLink) {
                is IncomingLink.Song -> "https://music.youtube.com/watch?v=${initialLink.ref.id}"
                is IncomingLink.Party -> "violence://party/${initialLink.invite.code}"
                is IncomingLink.Profile -> "violence://profile/${initialLink.card.displayName}"
                else -> ""
            }
        )
    }
    var parsedLink by remember { mutableStateOf<IncomingLink?>(initialLink) }
    var clipboardText by remember { mutableStateOf<String?>(null) }

    fun checkAndParse(text: String) {
        input = text
        val trimmed = text.trim()
        if (trimmed.isNotBlank()) {
            val candidate = IncomingLinks.extract(trimmed)
            parsedLink = if (candidate !is IncomingLink.Invalid) candidate else null
        } else {
            parsedLink = null
        }
    }

    LaunchedEffect(Unit) {
        if (initialLink != null) return@LaunchedEffect
        val clip = runCatching {
            val contents = Toolkit.getDefaultToolkit().systemClipboard.getContents(null)
            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                contents.getTransferData(DataFlavor.stringFlavor) as? String
            } else null
        }.getOrNull()?.trim()

        if (!clip.isNullOrBlank()) {
            clipboardText = clip
            val link = IncomingLinks.extract(clip)
            if (link !is IncomingLink.Invalid) {
                input = clip
                parsedLink = link
            }
        }
    }

    DesktopDialogPanel(onDismiss = onDismiss, maxWidth = 500) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Receive Link", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Open a shared song, playlist, profile, or Jam room", color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Close") }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = input,
                onValueChange = ::checkAndParse,
                label = { Text("Violence Link or Message") },
                placeholder = { Text("violence://... or https://...") },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            if (!clipboardText.isNullOrBlank() && input != clipboardText) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    OutlinedButton(
                        onClick = { clipboardText?.let { checkAndParse(it) } },
                        shape = RoundedCornerShape(50),
                    ) {
                        Icon(Icons.Rounded.ContentPaste, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Paste from clipboard", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val link = parsedLink
            if (link != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DesktopCardInsetFill)
                        .padding(14.dp),
                ) {
                    when (link) {
                        is IncomingLink.Song -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.MusicNote, null, tint = DesktopAccent, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(link.ref.title ?: "Song", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    link.ref.artist?.let {
                                        Text(it, color = DesktopSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                    }
                                }
                            }
                        }
                        is IncomingLink.Playlist -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.QueueMusic, null, tint = DesktopAccent, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(link.ref.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Shared playlist", color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        is IncomingLink.Profile -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProfileAvatar(link.card.avatarId, link.card.color, link.card.displayName, 42.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(link.card.displayName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Violence profile", color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        is IncomingLink.Party -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Radio, null, tint = DesktopAccent, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Jam Room ${link.invite.code}", fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("Listen Together", color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        else -> Unit
                    }
                }

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        onOpenLink(link)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = DesktopAccent, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                ) {
                    val label = when (link) {
                        is IncomingLink.Song -> "Play Song"
                        is IncomingLink.Playlist -> "Open Playlist"
                        is IncomingLink.Profile -> "Add Friend"
                        is IncomingLink.Party -> "Join Jam"
                        else -> "Open"
                    }
                    Text(label, fontWeight = FontWeight.SemiBold)
                }
            } else if (input.isNotBlank()) {
                Text(
                    "No Violence link recognized in the input.",
                    color = DesktopSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}