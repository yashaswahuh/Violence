package com.music.bitchord.ui.screens

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
import com.music.bitchord.R
import com.music.bitchord.data.profile.AvatarCatalog
import com.music.bitchord.data.profile.FriendCard
import com.music.bitchord.data.profile.Friends
import com.music.bitchord.data.profile.ProfileCard
import com.music.bitchord.data.profile.ProfileCardCodec
import com.music.bitchord.data.profile.ProfileManager
import com.music.bitchord.data.profile.ProfileRepository
import com.music.bitchord.data.profile.ProfileRules
import com.music.bitchord.ui.components.PillTextField
import com.music.bitchord.ui.icons.SfIcons
import com.music.bitchord.ui.profile.ProfileAvatar
import com.music.bitchord.ui.profile.parseColor
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
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saved by ProfileManager.profile.collectAsStateWithLifecycle()

    var name by rememberSaveable(saved?.id) { mutableStateOf(saved?.displayName.orEmpty()) }
    var avatarId by rememberSaveable(saved?.id) {
        mutableStateOf(saved?.avatarId ?: ProfileRules.DEFAULT_AVATAR_ID)
    }
    var color by rememberSaveable(saved?.id) { mutableStateOf(saved?.color ?: ProfileRules.DEFAULT_COLOR) }
    var photoStamp by remember { mutableIntStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }

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

        // What other people will see, large, so the choices below have an obvious effect.
        Box(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
            ProfileAvatar(avatarId = avatarId, color = color, name = name, size = 96.dp)
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

    if (confirmDelete) {
        AlertDialog(
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
    modifier: Modifier = Modifier,
) {
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
        AlertDialog(
            onDismissRequest = { candidate = null },
            title = { Text(stringResource(R.string.friends_add_title, card.displayName)) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfileAvatar(card.avatarId, card.color, card.displayName, 48.dp)
                    Spacer(Modifier.width(14.dp))
                    // Plain text, always: the name came from somebody else.
                    Text(card.displayName, style = MaterialTheme.typography.titleMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    candidate = null
                    val message = when (ProfileManager.addFriend(card)) {
                        is Friends.AddResult.Added -> context.getString(R.string.friends_added, card.displayName)
                        is Friends.AddResult.Updated -> context.getString(R.string.friends_updated, card.displayName)
                        Friends.AddResult.IsYou -> context.getString(R.string.friends_is_you)
                        Friends.AddResult.ListFull -> context.getString(R.string.friends_full)
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }) { Text(stringResource(R.string.friends_add_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { candidate = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    editing?.let { friend ->
        var nickname by rememberSaveable(friend.id) { mutableStateOf(friend.nickname.orEmpty()) }
        val valid = nickname.isBlank() || ProfileRules.normalizeName(nickname) != null
        AlertDialog(
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
        AlertDialog(
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

    AlertDialog(
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
    AlertDialog(
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
