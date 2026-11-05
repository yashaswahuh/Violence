package com.music.violence.wear.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonColors
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.music.violence.wear.R
import com.music.violence.wear.data.NowPlaying
import com.music.violence.wear.data.PhoneLink
import com.music.violence.wear.data.PhoneOutput
import com.music.violence.wear.data.Queue
import com.music.violence.wear.data.QueueRow

/*
 * The list screens are Material 3 Expressive's TransformingLazyColumn: rows
 * shrink, fade and morph as they near the curved edge of the screen, and each
 * row is a real M3 Button with a label, a secondary label and an icon. The
 * playing row, and any toggle that is on, takes the Voltage fill.
 */

/** The fill for a row that is "on": Voltage, white label. Everything else is a quiet tonal row. */
@Composable
private fun rowColors(on: Boolean): ButtonColors =
    if (on) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors()

/**
 * Up Next. Text first, thumbnails a beat later — see PhoneLink.applyQueue —
 * and asked for again whenever the playing track changes, so the list is never
 * the one from two songs ago.
 */
@Composable
fun QueueScreen(
    queue: Queue?,
    playingId: String?,
    onRequest: () -> Unit,
    onPlay: (Int) -> Unit,
) {
    LaunchedEffect(playingId) { onRequest() }
    val state = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val live = queue?.takeIf { playingId == null || it.mediaId == playingId } ?: queue

    ScreenScaffold(scrollState = state) { padding ->
        TransformingLazyColumn(
            state = state,
            contentPadding = padding,
            modifier = Modifier.background(Color.Black),
        ) {
            item(key = "header") { Header(stringResource(R.string.up_next), spec) }
            when {
                live == null -> item(key = "loading") { Hint(stringResource(R.string.loading)) }
                live.rows.isEmpty() -> item(key = "empty") { Hint(stringResource(R.string.queue_empty)) }
                else -> items(live.rows, key = { it.index }) { row ->
                    QueueButton(
                        row = row,
                        isCurrent = row.index == live.currentIndex,
                        isPast = row.index < live.currentIndex,
                        spec = spec,
                        onClick = { onPlay(row.index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TransformingLazyColumnItemScope.QueueButton(
    row: QueueRow,
    isCurrent: Boolean,
    isPast: Boolean,
    spec: TransformationSpec,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .transformedHeight(this, spec)
            .alpha(if (isPast) 0.55f else 1f),
        transformation = SurfaceTransformation(spec),
        colors = rowColors(isCurrent),
        icon = {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.10f)),
            ) {
                row.art?.let {
                    Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(34.dp))
                }
            }
        },
        secondaryLabel = {
            Text(text = row.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = WearType.rowSub)
        },
        label = {
            Text(text = row.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = WearType.row)
        },
    )
}

/** Shuffle, repeat and love, with the state each is in on the phone. */
@Composable
fun MoreScreen(np: NowPlaying?, link: PhoneLink, onOpenOutput: () -> Unit) {
    val state = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val onWatch by link.watchSpeaker.collectAsStateWithLifecycle()
    ScreenScaffold(scrollState = state) { padding ->
        TransformingLazyColumn(
            state = state,
            contentPadding = padding,
            modifier = Modifier.background(Color.Black),
        ) {
            item(key = "header") { Header(stringResource(R.string.more), spec) }
            item(key = "output") {
                ToggleRow(
                    icon = if (onWatch) R.drawable.ic_watch else R.drawable.ic_phone,
                    label = stringResource(R.string.output),
                    value = stringResource(if (onWatch) R.string.this_watch else R.string.on_phone_output),
                    on = false,
                    spec = spec,
                    onClick = onOpenOutput,
                )
            }
            if (np == null) {
                item(key = "none") { Hint(stringResource(R.string.nothing_playing)) }
                return@TransformingLazyColumn
            }
            item(key = "shuffle") {
                ToggleRow(
                    icon = R.drawable.ic_shuffle,
                    label = stringResource(R.string.shuffle),
                    value = stringResource(if (np.shuffle) R.string.on else R.string.off),
                    on = np.shuffle,
                    spec = spec,
                    onClick = link::toggleShuffle,
                )
            }
            item(key = "repeat") {
                ToggleRow(
                    icon = R.drawable.ic_repeat,
                    label = stringResource(R.string.repeat),
                    value = stringResource(
                        when (np.repeatMode) {
                            PhoneLink.REPEAT_ALL -> R.string.repeat_all
                            PhoneLink.REPEAT_ONE -> R.string.repeat_one
                            else -> R.string.repeat_off
                        },
                    ),
                    on = np.repeatMode != PhoneLink.REPEAT_OFF,
                    spec = spec,
                    onClick = link::cycleRepeat,
                )
            }
            if (np.canLike) item(key = "love") {
                ToggleRow(
                    icon = if (np.liked) R.drawable.ic_heart_filled else R.drawable.ic_heart,
                    label = stringResource(if (np.liked) R.string.loved else R.string.love),
                    value = null,
                    on = np.liked,
                    spec = spec,
                    // Love is Ember, not Voltage.
                    colors = if (np.liked) {
                        ButtonDefaults.buttonColors(containerColor = Ember, contentColor = Color(0xFF14101F))
                    } else {
                        null
                    },
                    onClick = link::toggleLike,
                )
            }
        }
    }
}

/** One row of the More list: an icon, a label and, where there is one, its current value. */
@Composable
private fun TransformingLazyColumnItemScope.ToggleRow(
    @DrawableRes icon: Int,
    label: String,
    value: String?,
    on: Boolean,
    spec: TransformationSpec,
    onClick: () -> Unit,
    colors: ButtonColors? = null,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
        transformation = SurfaceTransformation(spec),
        colors = colors ?: rowColors(on),
        icon = { Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp)) },
        secondaryLabel = value?.let { v ->
            { Text(text = v, maxLines = 1, style = WearType.rowSub) }
        },
        label = { Text(text = label, maxLines = 1, style = WearType.row) },
    )
}

@Composable
private fun TransformingLazyColumnItemScope.Header(text: String, spec: TransformationSpec) {
    ListHeader(
        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
        transformation = SurfaceTransformation(spec),
    ) {
        Text(text = text, style = WearType.header, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = WearType.subtitle,
        color = Secondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    )
}

/**
 * Where the music plays: this watch, or any output the phone has. Picking the
 * watch hands the queue over from the phone; picking a phone output while the
 * watch plays hands it back, through that output.
 */
@Composable
fun OutputScreen(link: PhoneLink, onDone: () -> Unit) {
    LaunchedEffect(Unit) { link.requestOutputs() }
    val outputs by link.outputs.collectAsStateWithLifecycle()
    val onWatch by link.watchSpeaker.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val state = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(scrollState = state) { padding ->
        TransformingLazyColumn(
            state = state,
            contentPadding = padding,
            modifier = Modifier.background(Color.Black),
        ) {
            item(key = "header") { Header(stringResource(R.string.output), spec) }
            item(key = "watch") {
                OutputRow(
                    icon = R.drawable.ic_watch,
                    label = stringResource(R.string.this_watch),
                    detail = stringResource(R.string.watch_speaker),
                    selected = onWatch,
                    spec = spec,
                    onClick = {
                        link.playOnWatch(context)
                        onDone()
                    },
                )
            }
            val list = outputs
            if (list == null) {
                item(key = "loading") { Hint(stringResource(R.string.loading)) }
            } else {
                items(list, key = { "out-${it.id}" }) { output ->
                    OutputRow(
                        icon = when (output.kind) {
                            "PHONE" -> R.drawable.ic_speaker
                            "WIRED", "BLUETOOTH", "USB" -> R.drawable.ic_headphones
                            else -> R.drawable.ic_phone
                        },
                        label = outputLabel(output),
                        detail = stringResource(R.string.on_phone_output),
                        selected = output.active && !onWatch,
                        spec = spec,
                        onClick = {
                            link.playOnPhone(output)
                            onDone()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun outputLabel(output: PhoneOutput): String = output.name.takeIf(String::isNotBlank) ?: stringResource(
    when (output.kind) {
        "PHONE" -> R.string.phone_speaker
        "WIRED" -> R.string.wired_headphones
        "USB" -> R.string.usb_audio
        "HDMI" -> R.string.hdmi
        else -> R.string.other_output
    },
)

@Composable
private fun TransformingLazyColumnItemScope.OutputRow(
    @DrawableRes icon: Int,
    label: String,
    detail: String,
    selected: Boolean,
    spec: TransformationSpec,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
        transformation = SurfaceTransformation(spec),
        colors = rowColors(selected),
        icon = { Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp)) },
        secondaryLabel = {
            Text(text = detail, maxLines = 1, overflow = TextOverflow.Ellipsis, style = WearType.rowSub)
        },
        label = { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = WearType.row) },
    )
}
