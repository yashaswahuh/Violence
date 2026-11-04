package com.music.violence.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.sp
import com.music.violence.data.profile.AvatarCatalog
import com.music.violence.data.profile.FriendCard
import com.music.violence.data.profile.Friends
import com.music.violence.data.profile.ProfileCard
import com.music.violence.data.profile.ProfileCardCodec
import com.music.violence.data.profile.ProfileRepository
import com.music.violence.data.profile.ProfileRules
import com.music.violence.ui.profile.ProfileAvatar
import com.music.violence.ui.profile.parseColor

private const val AVATARS_PER_ROW = 8

/**
 * Edit or create the in-app Violence profile on Desktop.
 */
@Composable
internal fun DesktopProfileDialog(
    onDismiss: () -> Unit,
    onShareProfile: ((ProfileCard) -> Unit)? = null,
    onOpenFriends: (() -> Unit)? = null,
) {
    val saved by DesktopProfileManager.profile.collectAsState()
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
        DesktopDialogPanel(onDismiss = { confirmDelete = false }, maxWidth = 420) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Delete your profile?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Your name and picture are removed from this computer. Your friends list stays. " +
                        "In Listen Together you will appear with your YouTube Music name again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DesktopSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { confirmDelete = false }, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            DesktopProfileManager.deleteProfile()
                            confirmDelete = false
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DesktopDestructive),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Delete", color = Color.White)
                    }
                }
            }
        }
        return
    }

    DesktopDialogPanel(onDismiss = onDismiss, maxWidth = 560) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (saved == null) "Create Profile" else "Edit Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Used for Jam identity and friend cards; separate from YouTube Music.",
                        color = DesktopSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Close") }
            }

            Spacer(Modifier.height(16.dp))

            ProfileAvatar(avatarId = avatarId, color = color, name = name.ifBlank { "You" }, size = 72.dp)

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(ProfileRules.NAME_MAX_CODE_POINTS * 2) },
                label = { Text("Display Name") },
                singleLine = true,
                isError = problem != null,
                modifier = Modifier.fillMaxWidth(),
            )
            if (problem != null) {
                Text(
                    problem,
                    color = DesktopDestructive,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            Text("Choose an Avatar", style = MaterialTheme.typography.labelMedium, color = DesktopSecondary)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AvatarCatalog.all.chunked(AVATARS_PER_ROW).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowItems.forEach { item ->
                            val selected = item.id == avatarId
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable { avatarId = item.id }
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) Color.White else Color.Transparent,
                                        shape = CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                ProfileAvatar(avatarId = item.id, color = color, name = "", size = 32.dp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Text("Choose a Color", style = MaterialTheme.typography.labelMedium, color = DesktopSecondary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileRules.COLOR_PRESETS.forEach { hex ->
                    val selected = hex.equals(color, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { color = hex }
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) Color.White else Color.Transparent,
                                shape = CircleShape,
                            )
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(parseColor(hex)),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (DesktopProfileManager.save(name, avatarId, color) is ProfileRepository.SaveResult.Saved) {
                            onDismiss()
                        }
                    },
                    enabled = valid,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = DesktopAccent, contentColor = Color.Black),
                    modifier = Modifier.weight(1f).height(44.dp),
                ) {
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }

                if (saved != null && onShareProfile != null) {
                    OutlinedButton(
                        onClick = { onShareProfile(saved!!.toCard()) },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(44.dp),
                    ) {
                        Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share")
                    }
                }

                if (onOpenFriends != null) {
                    OutlinedButton(
                        onClick = onOpenFriends,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(44.dp),
                    ) {
                        Icon(Icons.Rounded.PersonAdd, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Friends")
                    }
                }

                if (saved != null) {
                    TextButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.height(44.dp),
                    ) {
                        Text("Delete", color = DesktopDestructive)
                    }
                }
            }
        }
    }
}

/**
 * Friends management dialog on Desktop: lists friends, renames, removes, and adds new friends.
 */
@Composable
internal fun DesktopFriendsDialog(
    onDismiss: () -> Unit,
    onAddFriendClicked: () -> Unit,
) {
    val friends by DesktopProfileManager.friends.collectAsState()
    var renamingFriend by remember { mutableStateOf<FriendCard?>(null) }
    var nicknameDraft by remember { mutableStateOf("") }

    if (renamingFriend != null) {
        DesktopDialogPanel(onDismiss = { renamingFriend = null }, maxWidth = 420) {
            Column(Modifier.padding(24.dp)) {
                Text("Rename ${renamingFriend!!.displayName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Set a private local nickname for this friend. Leave blank to use their own name.", color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = nicknameDraft,
                    onValueChange = { nicknameDraft = it.take(ProfileRules.NAME_MAX_CODE_POINTS) },
                    label = { Text("Local Nickname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { renamingFriend = null }, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            DesktopProfileManager.renameFriend(renamingFriend!!.id, nicknameDraft)
                            renamingFriend = null
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = DesktopAccent, contentColor = Color.Black),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save")
                    }
                }
            }
        }
        return
    }

    DesktopDialogPanel(onDismiss = onDismiss, maxWidth = 560) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Friends", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${friends.size} friends", color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onAddFriendClicked) {
                    Icon(Icons.Rounded.PersonAdd, "Add Friend", tint = DesktopAccent)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Close") }
            }

            Spacer(Modifier.height(14.dp))

            if (friends.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No friends added yet", color = DesktopSecondary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = onAddFriendClicked) {
                            Icon(Icons.Rounded.PersonAdd, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add Friend from Link")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(friends, key = { it.id }) { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DesktopCardInsetFill)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ProfileAvatar(
                                avatarId = friend.avatarId,
                                color = friend.color,
                                name = friend.shownName,
                                size = 42.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(friend.shownName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (!friend.nickname.isNullOrBlank()) {
                                    Text("(${friend.displayName})", color = DesktopSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                }
                            }
                            IconButton(onClick = {
                                renamingFriend = friend
                                nicknameDraft = friend.nickname.orEmpty()
                            }) {
                                Icon(Icons.Rounded.Edit, "Rename", tint = DesktopSecondary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { DesktopProfileManager.removeFriend(friend.id) }) {
                                Icon(Icons.Rounded.Delete, "Remove", tint = DesktopDestructive, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog to add a friend by pasting a profile card link or code.
 */
@Composable
internal fun DesktopAddFriendDialog(onDismiss: () -> Unit) {
    var linkInput by remember { mutableStateOf("") }
    var parsedCard by remember { mutableStateOf<ProfileCard?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    fun checkLink(text: String) {
        linkInput = text
        notice = null
        val trimmed = text.trim()
        val parsed = ProfileCardCodec.parse(trimmed)
        if (parsed is ProfileCardCodec.Result.Ok) {
            parsedCard = parsed.card
        } else {
            parsedCard = null
            if (trimmed.isNotBlank()) {
                notice = "Invalid profile link. Check the link and try again."
            }
        }
    }

    DesktopDialogPanel(onDismiss = onDismiss, maxWidth = 480) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Add Friend", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Close") }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "Paste a friend's Violence profile link (e.g. from Discord, email, or scanned QR).",
                color = DesktopSecondary,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = linkInput,
                onValueChange = ::checkLink,
                label = { Text("Profile Link") },
                placeholder = { Text("violence://profile/... or https://...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            notice?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = DesktopDestructive, style = MaterialTheme.typography.bodySmall)
            }

            val card = parsedCard
            if (card != null) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DesktopCardInsetFill)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProfileAvatar(avatarId = card.avatarId, color = card.color, name = card.displayName, size = 48.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(card.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Ready to add", color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        val result = DesktopProfileManager.addFriend(card)
                        notice = when (result) {
                            is Friends.AddResult.Added -> "${card.displayName} added to friends!"
                            is Friends.AddResult.Updated -> "${card.displayName} updated in friends!"
                            Friends.AddResult.IsYou -> "That is your own profile."
                            Friends.AddResult.ListFull -> "Your friends list is full."
                        }
                        if (result is Friends.AddResult.Added || result is Friends.AddResult.Updated) {
                            onDismiss()
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = DesktopAccent, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                ) {
                    Text("Add Friend", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}