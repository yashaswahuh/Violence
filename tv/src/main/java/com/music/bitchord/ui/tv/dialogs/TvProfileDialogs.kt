package com.music.bitchord.ui.tv.dialogs

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.music.bitchord.data.profile.AvatarCatalog
import com.music.bitchord.data.profile.FriendCard
import com.music.bitchord.data.profile.Friends
import com.music.bitchord.data.profile.ProfileCard
import com.music.bitchord.data.profile.ProfileCardCodec
import com.music.bitchord.data.profile.ProfileManager
import com.music.bitchord.data.profile.ProfileRepository
import com.music.bitchord.data.profile.ProfileRules
import com.music.bitchord.ui.profile.ProfileAvatar
import com.music.bitchord.ui.profile.parseColor
import com.music.bitchord.ui.tv.components.TvButton
import com.music.bitchord.ui.tv.components.TvDialog
import com.music.bitchord.ui.tv.components.TvDialogButton
import com.music.bitchord.ui.tv.components.TvListRow
import com.music.bitchord.ui.tv.components.TvTextField
import com.music.bitchord.ui.tv.components.tvClick
import com.music.bitchord.ui.tv.components.tvInitialFocus
import com.music.bitchord.ui.tv.components.tvLift
import com.music.bitchord.ui.tv.theme.TvGlass
import com.music.bitchord.ui.tv.theme.TvType

private const val AVATARS_PER_ROW = 8

/**
 * Edit the Violence profile with a remote: a name (typed on the system TV keyboard), a picture and
 * a colour picked with the D-pad, then Save. No photo on the TV (there is no picker); the photo is
 * a phone-only extra and never shared anyway.
 *
 * Every control is focusable and nothing is disabled while it holds focus, so the D-pad never loses
 * its place. UNCLEAR on real hardware: built, not run on a TV.
 */
@Composable
fun TvProfileDialog(onDismiss: () -> Unit) {
    val saved by ProfileManager.profile.collectAsState()
    var name by remember { mutableStateOf(saved?.displayName.orEmpty()) }
    var avatarId by remember { mutableStateOf(saved?.avatarId ?: ProfileRules.DEFAULT_AVATAR_ID) }
    var color by remember { mutableStateOf(saved?.color ?: ProfileRules.DEFAULT_COLOR) }
    var confirmDelete by remember { mutableStateOf(false) }

    val check = ProfileRules.checkName(name)
    val valid = check is ProfileRules.NameResult.Valid
    val problem = when ((check as? ProfileRules.NameResult.Invalid)?.problem?.takeIf { name.isNotEmpty() }) {
        ProfileRules.NameProblem.TOO_LONG -> "Names can have up to ${ProfileRules.NAME_MAX_CODE_POINTS} characters."
        ProfileRules.NameProblem.CONTROL_CHARACTER,
        ProfileRules.NameProblem.INVISIBLE_CHARACTER,
        -> "That name has characters that cannot be used."
        else -> null
    }

    if (confirmDelete) {
        TvDialog(
            onDismissRequest = { confirmDelete = false },
            title = "Delete your profile?",
            message = "Your name and picture are removed from this TV. Your friends list stays. " +
                "In Listen Together you will appear with your YouTube Music name again.",
        ) {
            // Focus starts on the safe choice.
            TvDialogButton(text = "Cancel", initialFocus = true, onClick = { confirmDelete = false })
            TvDialogButton(
                text = "Delete Profile",
                destructive = true,
                onClick = {
                    ProfileManager.deleteProfile()
                    confirmDelete = false
                    onDismiss()
                },
            )
        }
        return
    }

    TvDialog(
        onDismissRequest = onDismiss,
        title = "Profile",
        message = "People in your Listen Together see your name, picture and colour. " +
            "Nothing else about you is shared, and this is separate from your YouTube Music sign-in.",
        width = 700.dp,
    ) {
        ProfileAvatar(avatarId = avatarId, color = color, name = name, size = 64.dp)
        Spacer(Modifier.height(12.dp))
        TvTextField(
            value = name,
            onValueChange = { name = it.take(60) },
            placeholder = "Your Name",
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
            modifier = Modifier.tvInitialFocus(),
        )
        if (problem != null) {
            Text(text = problem, style = TvType.Callout, color = TvGlass.Destructive, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AvatarCatalog.all.chunked(AVATARS_PER_ROW).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowItems.forEach { item ->
                        PickCell(selected = item.id == avatarId, onClick = { avatarId = item.id }) {
                            ProfileAvatar(avatarId = item.id, color = color, name = "", size = 36.dp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ProfileRules.COLOR_PRESETS.forEach { hex ->
                PickCell(selected = hex == color, onClick = { color = hex }) {
                    Box(Modifier.size(30.dp).clip(CircleShape).background(parseColor(hex)))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TvButton(
                text = "Save",
                isPrimary = true,
                // Dimmed until the name is valid; the focus is never on it before then.
                enabled = valid,
                onClick = {
                    if (ProfileManager.save(name, avatarId, color) is ProfileRepository.SaveResult.Saved) onDismiss()
                },
            )
            if (saved != null) {
                TvButton(text = "Delete Profile", destructive = true, onClick = { confirmDelete = true })
            }
            TvButton(text = "Cancel", onClick = onDismiss)
        }
    }
}

/** A round, focusable choice (an avatar or a colour) with a ring when it is the selected one. */
@Composable
private fun PickCell(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .size(44.dp)
            .tvLift(interaction, CircleShape, focusedScale = 1.12f, elevation = 8.dp)
            .clip(CircleShape)
            .then(
                when {
                    focused -> Modifier.border(3.dp, TvGlass.TextPrimary, CircleShape)
                    selected -> Modifier.border(3.dp, TvGlass.TextSecondary, CircleShape)
                    else -> Modifier
                },
            )
            .tvClick(interaction, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

private sealed interface FriendsView {
    data object Overview : FriendsView
    data object Paste : FriendsView
    data class Confirm(val card: ProfileCard) : FriendsView
    data class Edit(val friend: FriendCard) : FriendsView
    data object ConfirmClear : FriendsView
}

/**
 * The friends list on the TV: see them, give one a nickname, remove one, clear all, and add one by
 * pasting a profile link. A remote has no clipboard shortcut, so the link can be typed on the TV
 * keyboard or pulled in with the clipboard button.
 */
@Composable
fun TvFriendsDialog(onDismiss: () -> Unit) {
    val friends by ProfileManager.friends.collectAsState()
    var view by remember { mutableStateOf<FriendsView>(FriendsView.Overview) }
    var notice by remember { mutableStateOf<String?>(null) }

    when (val current = view) {
        FriendsView.Overview -> TvDialog(
            onDismissRequest = onDismiss,
            title = "Friends",
            message = "Saved only on this TV.",
            width = 700.dp,
        ) {
            notice?.let {
                Text(text = it, style = TvType.Callout, color = TvGlass.TextSecondary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
            }
            TvDialogButton(
                text = "Add From Profile Link",
                initialFocus = true,
                onClick = {
                    notice = null
                    view = FriendsView.Paste
                },
            )
            if (friends.isEmpty()) {
                Text(
                    text = "No friends yet.",
                    style = TvType.Callout,
                    color = TvGlass.TextSecondary,
                    modifier = Modifier.padding(vertical = 14.dp),
                )
            } else {
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    items(Friends.sorted(friends), key = { it.id }) { friend ->
                        TvListRow(
                            title = friend.shownName,
                            subtitle = if (friend.nickname != null) friend.displayName else null,
                            onClick = { view = FriendsView.Edit(friend) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                TvDialogButton(text = "Clear Friends List", destructive = true, onClick = { view = FriendsView.ConfirmClear })
            }
            TvDialogButton(text = "Done", onClick = onDismiss)
        }

        FriendsView.Paste -> {
            val clipboard = LocalClipboardManager.current
            var text by remember { mutableStateOf("") }
            var error by remember { mutableStateOf<String?>(null) }
            val check = {
                when (val result = ProfileCardCodec.parse(text)) {
                    is ProfileCardCodec.Result.Ok -> view = FriendsView.Confirm(result.card)
                    is ProfileCardCodec.Result.Rejected -> error = when (result.error) {
                        ProfileCardCodec.Error.NOT_A_CARD -> "That is not a Violence profile link."
                        ProfileCardCodec.Error.WRONG_VERSION -> "This link comes from a different version of Violence."
                        else -> "That profile link is damaged or not valid."
                    }
                }
            }
            TvDialog(
                onDismissRequest = { view = FriendsView.Overview },
                title = "Add From Profile Link",
                message = "Type or paste a link that starts with violence://profile",
                width = 700.dp,
            ) {
                TvTextField(
                    value = text,
                    onValueChange = {
                        text = it.take(ProfileCardCodec.MAX_INPUT_CHARS + 1)
                        error = null
                    },
                    placeholder = "violence://profile?…",
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Done,
                    onImeAction = { if (text.isNotBlank()) check() },
                    modifier = Modifier.tvInitialFocus(),
                )
                error?.let {
                    Text(text = it, style = TvType.Callout, color = TvGlass.Destructive, modifier = Modifier.padding(top = 6.dp))
                }
                Spacer(Modifier.height(12.dp))
                TvDialogButton(
                    text = "Paste From Clipboard",
                    onClick = {
                        text = clipboard.getText()?.text.orEmpty().take(ProfileCardCodec.MAX_INPUT_CHARS + 1)
                        error = null
                    },
                )
                // Stays focusable when empty (a no-op), so focus never drops while typing.
                TvDialogButton(text = "Check Link", onClick = { if (text.isNotBlank()) check() })
                TvDialogButton(text = "Back", onClick = { view = FriendsView.Overview })
            }
        }

        is FriendsView.Confirm -> TvDialog(
            onDismissRequest = { view = FriendsView.Overview },
            title = "Add ${current.card.displayName} as a friend?",
        ) {
            ProfileAvatar(current.card.avatarId, current.card.color, current.card.displayName, 64.dp)
            Spacer(Modifier.height(16.dp))
            TvDialogButton(
                text = "Add Friend",
                initialFocus = true,
                onClick = {
                    notice = when (ProfileManager.addFriend(current.card)) {
                        is Friends.AddResult.Added -> "${current.card.displayName} added."
                        is Friends.AddResult.Updated -> "${current.card.displayName} is already a friend. Details updated."
                        Friends.AddResult.IsYou -> "That is your own profile."
                        Friends.AddResult.ListFull -> "Your friends list is full."
                    }
                    view = FriendsView.Overview
                },
            )
            TvDialogButton(text = "Cancel", onClick = { view = FriendsView.Overview })
        }

        is FriendsView.Edit -> {
            val friend = current.friend
            var nickname by remember(friend.id) { mutableStateOf(friend.nickname.orEmpty()) }
            val valid = nickname.isBlank() || ProfileRules.normalizeName(nickname) != null
            TvDialog(
                onDismissRequest = { view = FriendsView.Overview },
                title = friend.displayName,
                message = "A nickname is only shown to you.",
            ) {
                ProfileAvatar(friend.avatarId, friend.color, friend.displayName, 64.dp)
                Spacer(Modifier.height(12.dp))
                TvTextField(
                    value = nickname,
                    onValueChange = { nickname = it.take(60) },
                    placeholder = "Nickname",
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.tvInitialFocus(),
                )
                Spacer(Modifier.height(12.dp))
                TvDialogButton(
                    text = "Save",
                    onClick = {
                        if (valid && ProfileManager.renameFriend(friend.id, nickname)) view = FriendsView.Overview
                    },
                )
                TvDialogButton(
                    text = "Remove Friend",
                    destructive = true,
                    onClick = {
                        ProfileManager.removeFriend(friend.id)
                        view = FriendsView.Overview
                    },
                )
                TvDialogButton(text = "Back", onClick = { view = FriendsView.Overview })
            }
        }

        FriendsView.ConfirmClear -> TvDialog(
            onDismissRequest = { view = FriendsView.Overview },
            title = "Clear your friends list?",
            message = "All friends are removed from this TV. Their own profiles are not affected.",
        ) {
            TvDialogButton(text = "Cancel", initialFocus = true, onClick = { view = FriendsView.Overview })
            TvDialogButton(
                text = "Clear Friends List",
                destructive = true,
                onClick = {
                    ProfileManager.clearFriends()
                    notice = "Friends list cleared."
                    view = FriendsView.Overview
                },
            )
        }
    }
}

/** The one-time, skippable offer to make a profile. Focus starts on "Not Now". */
@Composable
fun TvProfileSuggestionDialog(onCreate: () -> Unit, onSkip: () -> Unit) {
    TvDialog(
        onDismissRequest = onSkip,
        title = "Make Your Violence Profile?",
        message = "Pick a name and a picture so friends see you in Listen Together. " +
            "You can skip this: everything works without it, and you can make one later in Settings.",
    ) {
        TvDialogButton(text = "Not Now", initialFocus = true, onClick = onSkip)
        TvDialogButton(text = "Create Profile", onClick = onCreate)
    }
}
