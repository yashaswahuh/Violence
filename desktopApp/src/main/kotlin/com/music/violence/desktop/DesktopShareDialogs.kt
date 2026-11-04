package com.music.violence.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.music.violence.data.listentogether.JamInvite
import com.music.violence.data.model.Song
import com.music.violence.data.profile.ProfileCard
import com.music.violence.data.profile.ProfileCardCodec
import com.music.violence.data.share.PlaylistLinks
import com.music.violence.data.share.PlaylistRef
import com.music.violence.data.share.SongLinks
import com.music.violence.data.share.SongRef
import kotlinx.coroutines.delay
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

sealed interface ShareItem {
    val title: String
    val subtitle: String?
    val link: String
    val uri: String

    data class SongItem(val ref: SongRef) : ShareItem {
        override val title: String = ref.title ?: "Song"
        override val subtitle: String? = ref.artist
        override val uri: String = SongLinks.toUri(ref)
        override val link: String = SongLinks.toWebUrl(ref)
    }

    data class PlaylistItem(val ref: PlaylistRef) : ShareItem {
        override val title: String = ref.title
        override val subtitle: String = "Playlist"
        override val uri: String = PlaylistLinks.toUri(ref)
        override val link: String = PlaylistLinks.toWebUrl(ref)
    }

    data class ProfileItem(val card: ProfileCard) : ShareItem {
        override val title: String = card.displayName
        override val subtitle: String = "Violence Profile"
        override val uri: String = ProfileCardCodec.toUri(card)
        override val link: String = ProfileCardCodec.toWebUrl(card)
    }

    data class JamItem(val code: String, val customServer: String? = null) : ShareItem {
        override val title: String = "Jam Room $code"
        override val subtitle: String = "Listen Together"
        override val uri: String = "violence://party/$code"
        override val link: String = JamInvite.url(code)
    }

    companion object {
        fun of(song: Song, from: ProfileCard? = DesktopProfileManager.current()?.toCard()): ShareItem? {
            val ref = SongLinks.youtube(song.videoId, song.title, song.artist)
                ?: SongLinks.search(song.title, song.artist)
                ?: return null
            return SongItem(ref.copy(from = from))
        }

        fun ofPlaylist(title: String, songs: List<Song>, from: ProfileCard? = DesktopProfileManager.current()?.toCard()): ShareItem? =
            PlaylistLinks.songs(title, songs.map { it.videoId }, from)?.let(::PlaylistItem)

        fun ofPlaylist(browseId: String, title: String, from: ProfileCard? = DesktopProfileManager.current()?.toCard()): ShareItem? =
            PlaylistLinks.youtube(browseId, title, from)?.let(::PlaylistItem)
    }
}

@Composable
internal fun DesktopShareDialog(
    item: ShareItem,
    onDismiss: () -> Unit,
) {
    var copiedLink by remember { mutableStateOf(false) }
    var copiedCode by remember { mutableStateOf(false) }

    LaunchedEffect(copiedLink) {
        if (copiedLink) {
            delay(2000)
            copiedLink = false
        }
    }
    LaunchedEffect(copiedCode) {
        if (copiedCode) {
            delay(2000)
            copiedCode = false
        }
    }

    fun copyToClipboard(text: String) {
        runCatching {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        }
    }

    DesktopDialogPanel(onDismiss = onDismiss, maxWidth = 480) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    item.subtitle?.let {
                        Text(
                            it,
                            color = DesktopSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, "Close")
                }
            }

            Spacer(Modifier.height(16.dp))

            DesktopQrCode(content = item.link, size = 200.dp)

            Spacer(Modifier.height(14.dp))
            Text(
                "Scan with your phone's camera. The link opens in Violence.",
                color = DesktopSecondary,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DesktopCardInsetFill)
                    .border(0.5.dp, DesktopCardEdge, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    item.link,
                    style = MaterialTheme.typography.bodySmall,
                    color = DesktopSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = {
                        copyToClipboard(item.link)
                        copiedLink = true
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = DesktopAccent, contentColor = Color.Black),
                ) {
                    Icon(
                        if (copiedLink) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                        null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (copiedLink) "Copied!" else "Copy link", fontWeight = FontWeight.SemiBold)
                }

                if (item is ShareItem.JamItem) {
                    OutlinedButton(
                        onClick = {
                            copyToClipboard(item.code)
                            copiedCode = true
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(50),
                    ) {
                        Icon(
                            if (copiedCode) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                            null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (copiedCode) "Copied Code!" else "Copy code")
                    }
                }
            }
        }
    }
}

