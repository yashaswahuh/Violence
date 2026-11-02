package com.music.bitchord.ui.v2.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.data.listentogether.ListenTogether
import com.music.bitchord.data.model.Song
import com.music.bitchord.playback.PlaybackPosition
import com.music.bitchord.ui.components.GlassBottomSheet
import com.music.bitchord.ui.profile.ProfileAvatar
import com.music.bitchord.ui.theme.rememberArtworkPalette
import com.music.bitchord.ui.v2.a11y.v2Interactive
import com.music.bitchord.ui.v2.theme.ArtworkAccent
import com.music.bitchord.ui.v2.theme.LocalV2Colors
import com.music.bitchord.ui.v2.theme.V2Dimens
import com.music.bitchord.ui.v2.theme.V2Type

/** Whether the Now Playing panel is up. Opened by tapping the orb when the New UI is on. */
object V2NowState {
    var open by mutableStateOf(false)
}

/** "m:ss" (or "h:mm:ss") for a position in milliseconds; negative and absurd values clamp to zero. */
fun formatClock(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) / 1000L)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/**
 * Now Playing, the New UI way: a glass sheet that rises from the orb with the artwork, title,
 * a seek bar, transport, the Jam members, and the quick actions. The queue, lyrics, audio output
 * and quality options are in the full player, one visible button away ("Queue, lyrics and more"),
 * until a later phase moves them here.
 *
 * Everything is a real control, labelled, 48 dp or more; nothing needs a gesture. Colour comes from
 * the artwork through [ArtworkAccent], which guarantees the accent used for text reads at 4.5:1
 * and picks black or white for text on the accent fill.
 */
@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun V2NowPanel(
    song: Song,
    isPlaying: Boolean,
    position: PlaybackPosition,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpenFullPlayer: () -> Unit,
    onShare: () -> Unit,
    onOpenJam: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalV2Colors.current
    val palette = rememberArtworkPalette(song.thumbnailUrl)
    val accent = remember(palette.accent, colors.dark, colors.background) {
        ArtworkAccent.from(palette.accent.toArgb(), colors.dark, colors.background.toArgb(), colors.voltage.toArgb())
    }
    val fill = Color(accent.fill)
    val onFill = Color(accent.onFill)
    val party by ListenTogether.state.collectAsStateWithLifecycle()
    val locked = party.controlsLocked
    GlassBottomSheet(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .fillMaxWidth()
                // The aurora wash: the artwork's colour fading out down the sheet, behind the text.
                .background(Brush.verticalGradient(listOf(palette.wash.copy(alpha = 0.28f), Color.Transparent))),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = V2Dimens.Gap5)
                    .padding(bottom = V2Dimens.Gap5),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(colors.surface),
                )
                Spacer(Modifier.height(V2Dimens.Gap4))
                Text(
                    text = song.title,
                    style = V2Type.title,
                    color = colors.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = song.artist,
                    style = V2Type.body,
                    color = colors.ink2,
                    textAlign = TextAlign.Center,
                )
                if (party.inParty) {
                    Spacer(Modifier.height(V2Dimens.Gap3))
                    V2JamChips(party.members.map { JamChip(it.displayName, it.avatarId, it.avatarColor, it.avatarUrl, it.isHost) }, onOpenJam)
                }
                Spacer(Modifier.height(V2Dimens.Gap4))
                V2SeekBar(position = position, durationMs = durationMs, accent = fill, enabled = !locked, onSeek = onSeek)
                Spacer(Modifier.height(V2Dimens.Gap2))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(V2Dimens.Gap5)) {
                    TransportButton(Icons.Rounded.SkipPrevious, stringResource(R.string.v2_action_previous), !locked, onPrevious)
                    val playLabel = stringResource(if (isPlaying) R.string.v2_action_pause else R.string.v2_action_play)
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(fill, CircleShape)
                            .v2Interactive(label = playLabel, role = Role.Button, onClick = onPlayPause, enabled = !locked, shape = RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = onFill,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    TransportButton(Icons.Rounded.SkipNext, stringResource(R.string.v2_action_next), !locked, onNext)
                }
                if (locked) {
                    Spacer(Modifier.height(V2Dimens.Gap2))
                    Text(stringResource(R.string.v2_controls_locked), style = V2Type.caption, color = colors.ink2, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(V2Dimens.Gap4))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(V2Dimens.Gap2, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(V2Dimens.Gap2),
                ) {
                    ActionChip(Icons.Rounded.OpenInFull, stringResource(R.string.v2_open_full_player), Color(accent.text), onOpenFullPlayer)
                    ActionChip(Icons.Rounded.Share, stringResource(R.string.v2_share_song), colors.ink, onShare)
                    ActionChip(Icons.Rounded.Groups, stringResource(R.string.listen_together), colors.ink, onOpenJam)
                }
            }
        }
    }
}

@Composable
private fun TransportButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalV2Colors.current
    Box(
        modifier = Modifier
            .size(56.dp)
            .v2Interactive(label = label, role = Role.Button, onClick = onClick, enabled = enabled),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) colors.ink else colors.ink2, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun ActionChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    val colors = LocalV2Colors.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.surface, RoundedCornerShape(50))
            .v2Interactive(label = label, role = Role.Button, onClick = onClick)
            .padding(horizontal = V2Dimens.Gap4, vertical = V2Dimens.Gap2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(V2Dimens.Gap2),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, style = V2Type.label, color = colors.ink)
    }
}

/**
 * The seek bar. A Material slider (which already exposes progress range and set-progress to
 * accessibility) with a state description in time ("1:24 of 3:30"), custom actions to skip 10
 * seconds either way (so there is a non-drag alternative), and a track area at least 48 dp tall.
 * While dragging it shows the dragged value; the seek is sent once, when the finger lifts.
 */
@Composable
fun V2SeekBar(
    position: PlaybackPosition,
    durationMs: Long,
    accent: Color,
    enabled: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalV2Colors.current
    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = durationMs.coerceAtLeast(1L)
    val shownMs = dragging?.let { (it * duration).toLong() } ?: position.positionMs
    val fraction = (shownMs.toFloat() / duration).coerceIn(0f, 1f)
    val state = stringResource(R.string.v2_seek_state, formatClock(shownMs), formatClock(durationMs))
    val label = stringResource(R.string.v2_seek_label)
    val back = stringResource(R.string.v2_seek_back_10)
    val forward = stringResource(R.string.v2_seek_forward_10)
    Column(modifier.fillMaxWidth()) {
        // The semantics sit on a 48 dp box around the slider (whose own track is 44 dp), so the
        // control TalkBack announces and the area a finger can hit are at least 48 dp.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = V2Dimens.MinTouch)
                .semantics(mergeDescendants = true) {
                    contentDescription = label
                    stateDescription = state
                    customActions = listOf(
                        CustomAccessibilityAction(back) { onSeek((shownMs - 10_000L).coerceAtLeast(0L)); true },
                        CustomAccessibilityAction(forward) { onSeek((shownMs + 10_000L).coerceAtMost(durationMs)); true },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Slider(
                value = fraction,
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let { onSeek((it * duration).toLong()) }
                    dragging = null
                },
                enabled = enabled && durationMs > 0,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = colors.outline,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatClock(shownMs), style = V2Type.caption, color = colors.ink2)
            Text("-" + formatClock((durationMs - shownMs).coerceAtLeast(0L)), style = V2Type.caption, color = colors.ink2)
        }
    }
}

/** What a presence chip needs from a Jam member (kept apart from the model so the chips are testable). */
data class JamChip(val name: String, val avatarId: String?, val color: String?, val imageUrl: String?, val isHost: Boolean)

/**
 * Jam presence: one chip per member with their profile avatar. Each chip names the person (and the
 * host) for TalkBack; the whole row is one button to the Jam page. A host marker is a word, not only
 * a colour.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun V2JamChips(members: List<JamChip>, onOpenJam: () -> Unit) {
    val colors = LocalV2Colors.current
    val summary = stringResource(R.string.v2_jam_listening, members.size)
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .v2Interactive(label = summary, role = Role.Button, onClick = onOpenJam),
        horizontalArrangement = Arrangement.spacedBy(V2Dimens.Gap2, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(V2Dimens.Gap2),
    ) {
        members.forEach { m ->
            val host = stringResource(R.string.v2_jam_host)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.surface, RoundedCornerShape(50))
                    .padding(start = 4.dp, end = V2Dimens.Gap3, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfileAvatar(m.avatarId, m.color, m.name.ifBlank { "?" }, 28.dp, imageUrl = m.imageUrl)
                Spacer(Modifier.width(V2Dimens.Gap2))
                Text(
                    text = if (m.isHost) m.name + " · " + host else m.name,
                    style = V2Type.caption,
                    color = colors.mint,
                )
            }
        }
    }
}
