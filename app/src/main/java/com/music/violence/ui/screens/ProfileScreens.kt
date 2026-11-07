package com.music.violence.ui.screens

import com.music.violence.ui.components.IosSwitch
import com.music.violence.ui.components.GlassAlertDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.violence.R
import com.music.violence.data.profile.AvatarCatalog
import com.music.violence.data.profile.FriendCard
import com.music.violence.data.profile.Friends
import com.music.violence.data.profile.ProfileCard
import com.music.violence.data.profile.ProfileCardCodec
import com.music.violence.data.profile.ProfileManager
import com.music.violence.data.profile.ProfileRepository
import com.music.violence.data.profile.ProfileRules
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.IosShare
import com.music.violence.share.ShareItem
import com.music.violence.ui.components.PillTextField
import com.music.violence.ui.icons.SfIcons
import com.music.violence.ui.profile.ProfileAvatar
import com.music.violence.ui.profile.parseColor
import kotlinx.coroutines.launch

private const val AVATAR_COLUMNS = 6

/**
 * Edit the Violence profile: name, picture, colour and an optional photo that stays on this
 * device. Nothing here is required to use the app.
 */
@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    onClose: () -> Unit,
    onFriends: () -> Unit,
    onReceive: () -> Unit,
    onRecent: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val shareVibration by ProfileManager.shareVibration.collectAsStateWithLifecycle()
    val shareChime by ProfileManager.shareChime.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val saved by ProfileManager.profile.collectAsStateWithLifecycle()
    val friendCount by ProfileManager.friends.collectAsStateWithLifecycle()
    val autoJoin by ProfileManager.autoJoinFriends.collectAsStateWithLifecycle()

    var name by rememberSaveable(saved?.id) { mutableStateOf(saved?.displayName.orEmpty()) }
    var avatarId by rememberSaveable(saved?.id) {
        mutableStateOf(saved?.avatarId ?: ProfileRules.DEFAULT_AVATAR_ID)
    }
    var color by rememberSaveable(saved?.id) { mutableStateOf(saved?.color ?: ProfileRules.DEFAULT_COLOR) }
    var photoStamp by remember { mutableIntStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(false) }

    val photo = remember(photoStamp, saved?.id) { ProfileManager.photo() }
    val check = ProfileRules.checkName(name)
    val valid = check is ProfileRules.NameResult.Valid
    val changed = saved == null || (check as? ProfileRules.NameResult.Valid)?.name != saved?.displayName ||
        avatarId != saved?.avatarId || color != saved?.color
    val problem = (check as? ProfileRules.NameResult.Invalid)?.problem?.takeIf { name.isNotEmpty() }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                if (ProfileManager.setPhoto(context, uri)) {
                    photoStamp++
                } else {
                    Toast.makeText(context, R.string.profile_photo_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SettingsPageTitle(stringResource(R.string.profile_title))

        // Reached from here rather than from the main Settings list, which would otherwise carry
        // three profile rows. Neither needs a profile to be made first.
        SettingsGroup(topSpacing = 0.dp) {
            SettingsRow(
                icon = SfIcons.Groups,
                title = stringResource(R.string.friends_title),
                subtitle = stringResource(R.string.friends_row_subtitle),
                badge = friendCount.size.takeIf { it > 0 }?.toString(),
                onClick = onFriends,
            )
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
            SettingsRow(
                icon = Icons.Rounded.Nfc,
                title = stringResource(R.string.receive_title),
                subtitle = stringResource(R.string.receive_row_subtitle),
                onClick = onReceive,
            )
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
            SettingsRow(
                icon = SfIcons.History,
                title = stringResource(R.string.recent_title),
                subtitle = stringResource(R.string.recent_row_subtitle),
                onClick = onRecent,
            )
        }

        // What other people will see, large, so the choices below have an obvious effect.
        Box(Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
            ProfileAvatar(avatarId = avatarId, color = color, name = name, size = 96.dp, photo = photo)
        }

        SettingsGroup(header = stringResource(R.string.profile_name_header), topSpacing = 8.dp) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                PillTextField(
                    value = name,
                    onValueChange = { name = it.take(MAX_TYPED) },
                    placeholder = stringResource(R.string.profile_name_hint),
                    container = MaterialTheme.colorScheme.background,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        text = when (problem) {
                            ProfileRules.NameProblem.TOO_LONG ->
                                stringResource(R.string.profile_name_too_long, ProfileRules.NAME_MAX_CODE_POINTS)
                            ProfileRules.NameProblem.CONTROL_CHARACTER,
                            ProfileRules.NameProblem.INVISIBLE_CHARACTER,
                            -> stringResource(R.string.profile_name_bad_chars)
                            ProfileRules.NameProblem.EMPTY -> stringResource(R.string.profile_name_empty)
                            null -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(
                            R.string.profile_name_counter,
                            name.codePointCount(0, name.length),
                            ProfileRules.NAME_MAX_CODE_POINTS,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        SettingsGroup(header = stringResource(R.string.profile_avatar_header)) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AvatarCatalog.all.chunked(AVATAR_COLUMNS).forEach { rowItems ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        rowItems.forEach { item ->
                            val selected = item.id == avatarId
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (selected) {
                                            Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .clickable { avatarId = item.id }
                                    .semantics { contentDescription = item.id },
                                contentAlignment = Alignment.Center,
                            ) {
                                ProfileAvatar(avatarId = item.id, color = color, name = "", size = 38.dp)
                            }
                        }
                    }
                }
            }
        }

        SettingsGroup(header = stringResource(R.string.profile_color_header)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ProfileRules.COLOR_PRESETS.forEach { hex ->
                    val selected = hex == color
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(parseColor(hex))
                            .then(
                                if (selected) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { color = hex }
                            .semantics { contentDescription = hex },
                    )
                }
            }
        }

        // Save sits before the optional bits so the main path is the short one.
        SettingsGroup(footer = stringResource(R.string.profile_auto_join_footer)) {
            SettingsRow(
                title = stringResource(R.string.profile_auto_join),
                onClick = { ProfileManager.setAutoJoinFriends(!autoJoin) },
                trailing = {
                    IosSwitch(checked = autoJoin, onCheckedChange = ProfileManager::setAutoJoinFriends)
                },
            )
        }

        SettingsGroup(header = stringResource(R.string.profile_feedback_header)) {
            SettingsRow(
                title = stringResource(R.string.profile_feedback_vibration),
                onClick = { ProfileManager.setShareVibration(!shareVibration) },
                trailing = { IosSwitch(checked = shareVibration, onCheckedChange = ProfileManager::setShareVibration) },
            )
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
            SettingsRow(
                title = stringResource(R.string.profile_feedback_chime),
                onClick = { ProfileManager.setShareChime(!shareChime) },
                trailing = { IosSwitch(checked = shareChime, onCheckedChange = ProfileManager::setShareChime) },
            )
        }

        SettingsGroup(footer = stringResource(R.string.profile_privacy_footer)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (valid && changed) {
                            Modifier.clickable {
                                when (ProfileManager.save(name, avatarId, color)) {
                                    is ProfileRepository.SaveResult.Saved -> {
                                        Toast.makeText(context, R.string.profile_saved, Toast.LENGTH_SHORT).show()
                                        onClose()
                                    }
                                    is ProfileRepository.SaveResult.InvalidName -> Unit
                                }
                            }
                        } else {
                            Modifier
                        },
                    )
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.profile_save),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (valid && changed) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                )
            }
        }

        if (saved != null) {
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Rounded.IosShare,
                    title = stringResource(R.string.share_my_profile),
                    subtitle = stringResource(R.string.share_tap_subtitle),
                    onClick = { sharing = true },
                )
            }
        }

        SettingsGroup(
            header = stringResource(R.string.profile_photo_header),
            footer = stringResource(R.string.profile_photo_footer),
        ) {
            SettingsRow(
                icon = SfIcons.Person,
                title = stringResource(R.string.profile_photo_choose),
                onClick = {
                    pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                trailing = if (photo != null) {
                    { ProfileAvatar(avatarId = null, color = null, name = name, size = 32.dp, photo = photo) }
                } else {
                    null
                },
            )
            if (photo != null) {
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                DestructiveRow(label = stringResource(R.string.profile_photo_remove)) {
                    ProfileManager.clearPhoto()
                    photoStamp++
                }
            }
        }

        if (saved != null) {
            SettingsGroup {
                DestructiveRow(label = stringResource(R.string.profile_delete)) { confirmDelete = true }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (sharing) {
        saved?.let { me -> ShareSheet(item = ShareItem.ProfileItem(me.toCard()), onDismiss = { sharing = false }) }
    }

    if (confirmDelete) {
        GlassAlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.profile_delete_title)) },
            text = { Text(stringResource(R.string.profile_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    ProfileManager.deleteProfile()
                    photoStamp++
                    Toast.makeText(context, R.string.profile_deleted, Toast.LENGTH_SHORT).show()
                    onClose()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** More than this is never accepted into the field, so a pasted essay cannot bloat state. */
private const val MAX_TYPED = 60

/**
 * The friends list: rename, remove, clear, and add one from a pasted profile link after a
 * confirmation. Everything stays on this device.
 */
@Composable
fun FriendsScreen(
    contentPadding: PaddingValues,
    onReceive: () -> Unit,
    /** Opens the share sheet for the song that is playing, if one is. */
    onSendSong: (() -> Unit)? = null,
    /** Opens the share sheet for the Jam this phone is in, if it is in one. */
    onInviteJam: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var showMyQr by remember { mutableStateOf(false) }
    val myCard = remember(ProfileManager.current()) { ProfileManager.current()?.toCard() }
    val context = LocalContext.current
    val friends by ProfileManager.friends.collectAsStateWithLifecycle()
    val sorted = remember(friends) { Friends.sorted(friends) }

    var showPaste by remember { mutableStateOf(false) }
    var candidate by remember { mutableStateOf<ProfileCard?>(null) }
    var editing by remember { mutableStateOf<FriendCard?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SettingsPageTitle(stringResource(R.string.friends_title))

        SettingsGroup(topSpacing = 0.dp, footer = stringResource(R.string.friends_footer)) {
            SettingsRow(
                icon = SfIcons.Add,
                title = stringResource(R.string.friends_paste),
                onClick = { showPaste = true },
            )
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
            // Tap phones, scan a QR code, paste a link: all of them end in the same confirmation.
            SettingsRow(
                icon = Icons.Rounded.Nfc,
                title = stringResource(R.string.receive_title),
                subtitle = stringResource(R.string.receive_row_subtitle),
                onClick = onReceive,
            )
            if (myCard != null) {
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                SettingsRow(
                    icon = SfIcons.Share,
                    title = stringResource(R.string.friends_my_qr),
                    subtitle = stringResource(R.string.friends_my_qr_subtitle),
                    onClick = { showMyQr = true },
                )
            }
            if (onSendSong != null) {
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                SettingsRow(
                    icon = SfIcons.Share,
                    title = stringResource(R.string.friends_send_song),
                    subtitle = stringResource(R.string.friends_send_subtitle),
                    onClick = onSendSong,
                )
            }
            if (onInviteJam != null) {
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                SettingsRow(
                    icon = SfIcons.Groups,
                    title = stringResource(R.string.friends_invite_jam),
                    subtitle = stringResource(R.string.friends_send_subtitle),
                    onClick = onInviteJam,
                )
            }
        }

        if (showMyQr && myCard != null) {
            ShareSheet(item = ShareItem.ProfileItem(myCard), onDismiss = { showMyQr = false }, startWithQr = true)
        }

        if (sorted.isEmpty()) {
            Text(
                text = stringResource(R.string.friends_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 28.dp),
            )
        } else {
            SettingsGroup {
                sorted.forEachIndexed { index, friend ->
                    if (index > 0) HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editing = friend }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ProfileAvatar(friend.avatarId, friend.color, friend.shownName, 40.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = friend.shownName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                            )
                            if (friend.nickname != null) {
                                Text(
                                    text = stringResource(R.string.friends_real_name, friend.displayName),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
            SettingsGroup {
                DestructiveRow(label = stringResource(R.string.friends_clear)) { confirmClear = true }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showPaste) {
        PasteProfileLinkDialog(
            onDismiss = { showPaste = false },
            onCard = {
                showPaste = false
                candidate = it
            },
        )
    }

    candidate?.let { card ->
        AddFriendDialog(card = card, onFinished = { candidate = null }, onCancel = { candidate = null })
    }

    editing?.let { friend ->
        var nickname by rememberSaveable(friend.id) { mutableStateOf(friend.nickname.orEmpty()) }
        val valid = nickname.isBlank() || ProfileRules.normalizeName(nickname) != null
        GlassAlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(stringResource(R.string.friends_edit_title)) },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar(friend.avatarId, friend.color, friend.displayName, 48.dp)
                        Spacer(Modifier.width(14.dp))
                        Text(friend.displayName, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(14.dp))
                    PillTextField(
                        value = nickname,
                        onValueChange = { nickname = it.take(MAX_TYPED) },
                        placeholder = stringResource(R.string.friends_nickname_hint),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    )
                    TextButton(onClick = {
                        ProfileManager.removeFriend(friend.id)
                        editing = null
                    }) { Text(stringResource(R.string.friends_remove), color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = valid,
                    onClick = {
                        ProfileManager.renameFriend(friend.id, nickname)
                        editing = null
                    },
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (confirmClear) {
        GlassAlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.friends_clear_title)) },
            text = { Text(stringResource(R.string.friends_clear_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    ProfileManager.clearFriends()
                    Toast.makeText(context, R.string.friends_cleared, Toast.LENGTH_SHORT).show()
                }) { Text(stringResource(R.string.clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** Takes the pasted text, checks it, and only hands over a card that passed every rule. */
@Composable
private fun PasteProfileLinkDialog(
    onDismiss: () -> Unit,
    onCard: (ProfileCard) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var text by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<ProfileCardCodec.Error?>(null) }

    fun submit() {
        when (val result = ProfileCardCodec.parse(text)) {
            is ProfileCardCodec.Result.Ok -> onCard(result.card)
            is ProfileCardCodec.Result.Rejected -> error = result.error
        }
    }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.friends_paste_title)) },
        text = {
            Column {
                PillTextField(
                    value = text,
                    onValueChange = {
                        text = it.take(ProfileCardCodec.MAX_INPUT_CHARS + 1)
                        error = null
                    },
                    placeholder = stringResource(R.string.friends_paste_hint),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
                TextButton(onClick = {
                    text = clipboard.getText()?.text.orEmpty().take(ProfileCardCodec.MAX_INPUT_CHARS + 1)
                    error = null
                }) { Text(stringResource(R.string.friends_paste_button)) }
                error?.let {
                    Text(
                        text = stringResource(
                            when (it) {
                                ProfileCardCodec.Error.NOT_A_CARD -> R.string.friends_error_not_card
                                ProfileCardCodec.Error.WRONG_VERSION -> R.string.friends_error_version
                                else -> R.string.friends_error_invalid
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = ::submit) {
                Text(stringResource(R.string.friends_check))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * The one-time offer to make a profile. A plain dialog over the normal UI: playback and
 * navigation are never blocked, Back dismisses it, and either answer means it is not shown again.
 */
@Composable
fun ProfileSuggestionDialog(
    onCreate: () -> Unit,
    onSkip: () -> Unit,
) {
    GlassAlertDialog(
        onDismissRequest = onSkip,
        title = { Text(stringResource(R.string.profile_suggest_title)) },
        text = { Text(stringResource(R.string.profile_suggest_body)) },
        confirmButton = {
            TextButton(onClick = onCreate) { Text(stringResource(R.string.profile_suggest_create)) }
        },
        dismissButton = {
            TextButton(onClick = onSkip) { Text(stringResource(R.string.profile_suggest_skip)) }
        },
    )
}

/**
 * "Add <name> as a friend?": the confirmation for a profile card, wherever it came from (a pasted
 * link, an NFC tap, a QR code, a link). The name is somebody else's text and is shown as plain
 * text. Adding the same person again only refreshes their details.
 */
@Composable
fun AddFriendDialog(card: ProfileCard, onFinished: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    // Friends live on each phone, so adding someone does not add you to their list. After a new
    // friend, offer the other half straight away: your own card, to be scanned or tapped.
    var offerBack by rememberSaveable { mutableStateOf(false) }
    var showingCard by rememberSaveable { mutableStateOf(false) }
    val me by ProfileManager.profile.collectAsStateWithLifecycle()
    val myCard = me?.toCard()
    if (offerBack && myCard != null) {
        if (showingCard) {
            ShareSheet(item = ShareItem.ProfileItem(myCard), onDismiss = onFinished, startWithQr = true)
        } else {
            GlassAlertDialog(
                onDismissRequest = onFinished,
                title = { Text(stringResource(R.string.friends_back_title, card.displayName)) },
                text = { Text(stringResource(R.string.friends_back_body, card.displayName)) },
                confirmButton = {
                    TextButton(onClick = { showingCard = true }) { Text(stringResource(R.string.friends_back_show)) }
                },
                dismissButton = { TextButton(onClick = onFinished) { Text(stringResource(R.string.friends_back_later)) } },
            )
        }
        return
    }
    GlassAlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.friends_add_title, card.displayName)) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(card.avatarId, card.color, card.displayName, 48.dp)
                Spacer(Modifier.width(14.dp))
                Text(card.displayName, style = MaterialTheme.typography.titleMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val result = ProfileManager.addFriend(card)
                val message = when (result) {
                    is Friends.AddResult.Added -> context.getString(R.string.friends_added, card.displayName)
                    is Friends.AddResult.Updated -> context.getString(R.string.friends_updated, card.displayName)
                    Friends.AddResult.IsYou -> context.getString(R.string.friends_is_you)
                    Friends.AddResult.ListFull -> context.getString(R.string.friends_full)
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                if (result is Friends.AddResult.Added && myCard != null) offerBack = true else onFinished()
            }) { Text(stringResource(R.string.friends_add_confirm)) }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
    )
}
