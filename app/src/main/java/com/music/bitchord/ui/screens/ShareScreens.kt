package com.music.bitchord.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.music.bitchord.R
import com.music.bitchord.data.listentogether.JamInviteLink
import com.music.bitchord.data.share.IncomingLink
import com.music.bitchord.data.share.IncomingLinks
import com.music.bitchord.data.share.SongRef
import com.music.bitchord.data.share.SongSource
import com.music.bitchord.nfc.NfcReceiveState
import com.music.bitchord.nfc.NfcReceiver
import com.music.bitchord.nfc.NfcSendState
import com.music.bitchord.nfc.NfcShareSession
import com.music.bitchord.share.IncomingLinkHub
import com.music.bitchord.share.LinkNotice
import com.music.bitchord.share.LinkSource
import com.music.bitchord.share.QrDecoder
import com.music.bitchord.share.ShareItem
import com.music.bitchord.ui.components.PillTextField
import com.music.bitchord.ui.components.QrCode
import com.music.bitchord.ui.profile.ProfileAvatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

// ------------------------------------------------------------------ helpers --

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

/** Increments each time the screen comes back to the foreground (NFC may have been switched on meanwhile). */
@Composable
private fun rememberResumeTick(): Int {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) tick++ }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return tick
}

private fun openNfcSettings(context: Context) {
    val intent = Intent(Settings.ACTION_NFC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure {
        runCatching { context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

/** The friendly message for a refused link. */
internal fun noticeRes(notice: LinkNotice): Int = when (notice) {
    LinkNotice.NOT_A_VIOLENCE_LINK -> R.string.incoming_invalid
    LinkNotice.UNSUPPORTED_VERSION -> R.string.incoming_unsupported
    LinkNotice.MALFORMED -> R.string.incoming_malformed
    LinkNotice.TOO_LONG -> R.string.incoming_too_long
    LinkNotice.EMPTY -> R.string.incoming_empty
    LinkNotice.UNREADABLE -> R.string.receive_read_failed
}

@Composable
internal fun noticeText(notice: LinkNotice): String = stringResource(noticeRes(notice))

// ---------------------------------------------------------------- share sheet --

@Composable
private fun GroupDivider() {
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
}

/**
 * Everything you can do with one shareable thing: tap two phones together (NFC phones only), show
 * a QR code, copy the link, or hand it to another app. Only the thing's own link is ever used.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(item: ShareItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val tick = rememberResumeTick()
    val nfc = remember(tick) { NfcShareSession.sendState(context) }
    val fits = remember(item) { NfcShareSession.fits(item.uri) }
    var tapping by rememberSaveable { mutableStateOf(false) }
    var showQr by rememberSaveable { mutableStateOf(false) }

    val title = when (item) {
        is ShareItem.SongItem -> item.ref.title ?: stringResource(R.string.share_song_fallback)
        is ShareItem.ProfileItem -> item.card.displayName
        is ShareItem.JamItem -> stringResource(R.string.share_jam_label) + " " + item.code
    }
    val subtitle = when (item) {
        is ShareItem.SongItem -> item.ref.artist
        is ShareItem.ProfileItem -> stringResource(R.string.share_profile_label)
        is ShareItem.JamItem -> null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (item is ShareItem.ProfileItem) {
                    ProfileAvatar(item.card.avatarId, item.card.color, item.card.displayName, 64.dp)
                    Spacer(Modifier.height(10.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }

            SettingsGroup(topSpacing = 12.dp) {
                when (nfc) {
                    NfcSendState.NO_NFC -> Unit
                    NfcSendState.NFC_OFF -> {
                        SettingsRow(
                            icon = Icons.Rounded.Nfc,
                            title = stringResource(R.string.share_nfc_off),
                            subtitle = stringResource(R.string.share_nfc_off_subtitle),
                            value = stringResource(R.string.share_open_nfc_settings),
                            onClick = { openNfcSettings(context) },
                        )
                    }
                    NfcSendState.NO_HCE -> {
                        SettingsRow(
                            icon = Icons.Rounded.Nfc,
                            title = stringResource(R.string.share_tap),
                            subtitle = stringResource(R.string.share_nfc_unsupported),
                            enabled = false,
                        )
                    }
                    NfcSendState.READY -> {
                        SettingsRow(
                            icon = Icons.Rounded.Nfc,
                            title = stringResource(R.string.share_tap),
                            subtitle = stringResource(if (fits) R.string.share_tap_subtitle else R.string.share_nfc_too_long),
                            enabled = fits,
                            onClick = { tapping = true },
                        )
                    }
                }
                if (nfc != NfcSendState.NO_NFC) GroupDivider()
                SettingsRow(
                    icon = Icons.Rounded.QrCode2,
                    title = stringResource(if (showQr) R.string.share_hide_qr else R.string.share_show_qr),
                    onClick = { showQr = !showQr },
                )
                if (showQr) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        QrCode(content = item.link, size = 220.dp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.share_qr_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                }
                GroupDivider()
                SettingsRow(
                    icon = Icons.Rounded.ContentCopy,
                    title = stringResource(R.string.share_copy_link),
                    onClick = {
                        clipboard.setText(AnnotatedString(item.link))
                        Toast.makeText(context, R.string.share_link_copied, Toast.LENGTH_SHORT).show()
                    },
                )
                GroupDivider()
                SettingsRow(
                    icon = Icons.Rounded.Link,
                    title = stringResource(R.string.share_via_apps),
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, item.link)
                        }
                        context.startActivity(Intent.createChooser(send, title))
                    },
                )
                if (item is ShareItem.SongItem && item.ref.source == SongSource.YOUTUBE) {
                    GroupDivider()
                    SettingsRow(
                        icon = Icons.Rounded.IosShare,
                        title = stringResource(R.string.share_youtube_link),
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${item.ref.id}")
                            }
                            context.startActivity(Intent.createChooser(send, title))
                        },
                    )
                }
            }
        }
    }

    if (tapping) TapToShareDialog(uri = item.uri, onDismiss = { tapping = false })
}

// ------------------------------------------------------------- tap to share --

/**
 * The Tap to share screen: while it is showing (and the phone is unlocked) this phone looks like
 * an NFC tag holding [uri]; the moment it stops being the foreground screen, or is closed, the
 * tag goes away. Nothing else is ever offered.
 */
@Composable
fun TapToShareDialog(uri: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.activity() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var active by remember { mutableStateOf(false) }

    DisposableEffect(lifecycle, uri) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> active = activity?.let { NfcShareSession.start(it, uri) } ?: false
                Lifecycle.Event.ON_PAUSE -> {
                    activity?.let { NfcShareSession.stop(it) }
                    active = false
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            activity?.let { NfcShareSession.stop(it) }
        }
    }

    val pulse by rememberInfiniteTransition(label = "tapPulse").animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "tapPulseScale",
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Rounded.Nfc,
                    contentDescription = null,
                    tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(120.dp).scale(if (active) pulse else 1f),
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.tap_to_share_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(if (active) R.string.tap_to_share_body else R.string.tap_to_share_failed),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                Button(onClick = onDismiss) { Text(stringResource(R.string.tap_to_share_done)) }
            }
        }
    }
}

// ------------------------------------------------------------------ receive --

/**
 * Receive: read a tap from another phone (NFC), scan a QR code, or paste a link. All three hand
 * the text to [IncomingLinkHub], which asks before doing anything.
 */
@Composable
fun ReceiveScreen(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = remember(context) { context.activity() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val tick = rememberResumeTick()
    val nfc = remember(tick) { NfcReceiver.state(context) }
    var scanning by remember { mutableStateOf(false) }
    var pasted by rememberSaveable { mutableStateOf("") }

    // Reader mode only while this screen is in the foreground.
    DisposableEffect(lifecycle, nfc) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (nfc == NfcReceiveState.READY) {
                    activity?.let {
                        NfcReceiver.enable(
                            it,
                            onLink = { text -> IncomingLinkHub.offer(text, LinkSource.NFC) },
                            onUnreadable = { IncomingLinkHub.unreadable() },
                        )
                    }
                }
                Lifecycle.Event.ON_PAUSE -> activity?.let { NfcReceiver.disable(it) }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            activity?.let { NfcReceiver.disable(it) }
        }
    }

    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(contentPadding)) {
        SettingsPageTitle(stringResource(R.string.receive_title))
        Text(
            text = stringResource(R.string.receive_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        // No NFC chip: the option is simply not shown.
        if (nfc != NfcReceiveState.NO_NFC) {
            SettingsGroup(
                header = stringResource(R.string.receive_nfc_header),
                footer = stringResource(if (nfc == NfcReceiveState.READY) R.string.receive_nfc_ready else R.string.receive_nfc_off),
            ) {
                SettingsRow(
                    icon = Icons.Rounded.Nfc,
                    title = stringResource(R.string.receive_nfc_header),
                    value = if (nfc == NfcReceiveState.NFC_OFF) stringResource(R.string.share_open_nfc_settings) else null,
                    onClick = if (nfc == NfcReceiveState.NFC_OFF) ({ openNfcSettings(context) }) else null,
                )
            }
        }

        SettingsGroup(topSpacing = 18.dp) {
            SettingsRow(
                icon = Icons.Rounded.QrCodeScanner,
                title = stringResource(R.string.receive_scan),
                subtitle = stringResource(R.string.receive_scan_subtitle),
                onClick = { scanning = true },
            )
        }

        SettingsGroup(header = stringResource(R.string.receive_paste_header)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                PillTextField(
                    value = pasted,
                    onValueChange = { pasted = it.take(600) },
                    placeholder = stringResource(R.string.receive_paste_hint),
                    container = MaterialTheme.colorScheme.background,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
                TextButton(
                    enabled = pasted.isNotBlank(),
                    onClick = {
                        if (IncomingLinkHub.offer(pasted, LinkSource.PASTE, extract = true)) pasted = ""
                    },
                ) { Text(stringResource(R.string.receive_paste_button)) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (scanning) QrScannerDialog(onClose = { scanning = false })
}

// --------------------------------------------------------------- QR scanner --

/**
 * Scans a QR code with the camera (CameraX + ZXing) or reads one from a picture. The camera
 * permission is asked for here, when the scanner opens, and never before.
 */
@Composable
fun QrScannerDialog(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var denied by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<LinkNotice?>(null) }
    var nothingFound by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        denied = !ok
    }
    LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }

    val bases = listOfNotNull(JamInviteLink.inviteBaseUrl.takeIf { it.isNotBlank() })
    val onText: (String) -> Unit = { text ->
        if (!finished) {
            when (val link = IncomingLinks.extract(text, bases)) {
                // A QR code for something else is not an error worth stopping for: say so, keep looking.
                is IncomingLink.Invalid -> problem = when (link.reason) {
                    com.music.bitchord.data.share.InvalidReason.UNSUPPORTED_VERSION -> LinkNotice.UNSUPPORTED_VERSION
                    com.music.bitchord.data.share.InvalidReason.MALFORMED -> LinkNotice.MALFORMED
                    else -> LinkNotice.NOT_A_VIOLENCE_LINK
                }
                else -> {
                    finished = true
                    IncomingLinkHub.offer(text, LinkSource.QR, extract = true)
                    onClose()
                }
            }
        }
    }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val text = decodePicture(context, uri)
                if (text == null) nothingFound = true else onText(text)
            }
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (granted) CameraPreview(onText = onText)
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.scanner_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.padding(top = 24.dp),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val message = when {
                        nothingFound -> stringResource(R.string.scanner_nothing_found)
                        problem != null -> noticeText(problem!!)
                        denied -> stringResource(R.string.scanner_denied)
                        granted -> stringResource(R.string.scanner_hint)
                        else -> stringResource(R.string.scanner_permission_body)
                    }
                    Text(text = message, color = Color.White, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = {
                        nothingFound = false
                        pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.scanner_choose_picture), color = Color.White)
                    }
                    TextButton(onClick = onClose) { Text(stringResource(R.string.scanner_close), color = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(onText: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latest by rememberUpdatedState(onText)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PreviewView(ctx).also { view ->
                view.scaleType = PreviewView.ScaleType.FILL_CENTER
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    runCatching {
                        val provider = future.get()
                        val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        var last = 0L
                        analysis.setAnalyzer(executor) { image ->
                            try {
                                val now = System.nanoTime() / 1_000_000
                                if (now - last >= 120) {
                                    last = now
                                    val plane = image.planes[0]
                                    val buffer = plane.buffer
                                    val data = ByteArray(buffer.remaining())
                                    buffer.get(data)
                                    QrDecoder.decodeLuminance(data, plane.rowStride, image.width, image.height)
                                        ?.let { text -> mainExecutor.execute { latest(text) } }
                                }
                            } finally {
                                image.close()
                            }
                        }
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
    )
}

private suspend fun decodePicture(context: Context, uri: Uri): String? = withContext(Dispatchers.Default) {
    runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return@runCatching null
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val text = QrDecoder.decodePixels(pixels, bitmap.width, bitmap.height)
        bitmap.recycle()
        text
    }.getOrNull()
}

// ------------------------------------------------------------- incoming song --

/**
 * "Open this song?": shown for a song that arrived by tap, QR or link. The title and artist are
 * only labels from the link, shown as plain text; nothing is looked up until the user chooses.
 */
@Composable
fun IncomingSongDialog(
    ref: SongRef,
    busy: Boolean,
    onPlayNow: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDismiss: () -> Unit,
) {
    val label = listOfNotNull(ref.title, ref.artist).joinToString(" — ").ifBlank { stringResource(R.string.incoming_song_unknown) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.incoming_song_title)) },
        text = {
            Column {
                Text(label, style = MaterialTheme.typography.titleMedium)
                if (busy) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.incoming_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                TextButton(enabled = !busy, onClick = onAddToQueue) { Text(stringResource(R.string.incoming_add_queue)) }
                TextButton(enabled = !busy, onClick = onAddToPlaylist) { Text(stringResource(R.string.incoming_add_playlist)) }
            }
        },
        confirmButton = { TextButton(enabled = !busy, onClick = onPlayNow) { Text(stringResource(R.string.incoming_play_now)) } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
