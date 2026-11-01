package com.music.bitchord.data.profile

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Where the profile is kept. The Android apps back it with a private `SharedPreferences` file. */
interface ProfileStorage {
    fun read(key: String): String?

    /** A null [value] removes the key. */
    fun write(key: String, value: String?)
}

/**
 * The local profile and friends list, with their rules.
 *
 * Local only: nothing in here talks to a network, and nothing is logged (a name or an id never
 * reaches a logger from this class). Reads are defensive — damaged or hand-edited storage yields
 * "no profile" or a shorter list, never an exception.
 */
class ProfileRepository(
    private val storage: ProfileStorage,
    private val newId: () -> String,
    private val nowMs: () -> Long,
) {
    private val lock = Any()
    private val json = Json { ignoreUnknownKeys = true }

    private val _profile = MutableStateFlow(loadProfile())
    private val _friends = MutableStateFlow(loadFriends())

    val profile: StateFlow<InAppProfile?> = _profile.asStateFlow()
    val friends: StateFlow<List<FriendCard>> = _friends.asStateFlow()

    sealed interface SaveResult {
        data class Saved(val profile: InAppProfile) : SaveResult
        data class InvalidName(val problem: ProfileRules.NameProblem) : SaveResult
    }

    /**
     * Creates the profile (a new random id, made once) or edits the existing one. The id and
     * creation time of an existing profile never change.
     */
    fun save(name: String, avatarId: String, color: String): SaveResult = synchronized(lock) {
        val checked = ProfileRules.checkName(name)
        val clean = (checked as? ProfileRules.NameResult.Valid)?.name
            ?: return SaveResult.InvalidName((checked as ProfileRules.NameResult.Invalid).problem)
        val avatar = avatarId.takeIf(AvatarCatalog::isKnown) ?: ProfileRules.DEFAULT_AVATAR_ID
        val colour = ProfileRules.normalizeColor(color) ?: ProfileRules.DEFAULT_COLOR
        val current = _profile.value
        val next = if (current == null) {
            InAppProfile(newId(), clean, avatar, colour, nowMs())
        } else {
            current.copy(displayName = clean, avatarId = avatar, color = colour)
        }
        storage.write(KEY_PROFILE, json.encodeToString(InAppProfile.serializer(), next))
        _profile.value = next
        SaveResult.Saved(next)
    }

    /** Removes the profile. The friends list is a separate thing and is kept. */
    fun deleteProfile() = synchronized(lock) {
        storage.write(KEY_PROFILE, null)
        _profile.value = null
    }

    fun addFriend(card: ProfileCard): Friends.AddResult = synchronized(lock) {
        val result = Friends.add(_friends.value, card, _profile.value?.id, nowMs())
        when (result) {
            is Friends.AddResult.Added -> persistFriends(result.list)
            is Friends.AddResult.Updated -> persistFriends(result.list)
            else -> Unit
        }
        result
    }

    /** False when the nickname is not acceptable (the list is unchanged). */
    fun renameFriend(id: String, nickname: String): Boolean = synchronized(lock) {
        val next = Friends.rename(_friends.value, id, nickname) ?: return false
        persistFriends(next)
        true
    }

    fun removeFriend(id: String) = synchronized(lock) {
        persistFriends(Friends.remove(_friends.value, id))
    }

    /** The one-action "clear the friends list". */
    fun clearFriends() = synchronized(lock) {
        persistFriends(emptyList())
    }

    /** Whether the first-run suggestion was already answered. */
    fun suggestionHandled(): Boolean = storage.read(KEY_SUGGESTED) == "1"

    fun markSuggestionHandled() = storage.write(KEY_SUGGESTED, "1")

    /** Whether to offer the first-run suggestion now: no profile, never answered. */
    fun shouldSuggest(): Boolean = _profile.value == null && !suggestionHandled()

    // ---------------------------------------------------------------- storage --

    private fun persistFriends(list: List<FriendCard>) {
        storage.write(
            KEY_FRIENDS,
            if (list.isEmpty()) null else json.encodeToString(ListSerializer(FriendCard.serializer()), list),
        )
        _friends.value = list
    }

    private fun loadProfile(): InAppProfile? {
        val raw = storage.read(KEY_PROFILE) ?: return null
        return runCatching { json.decodeFromString(InAppProfile.serializer(), raw).sanitized() }.getOrNull()
    }

    private fun loadFriends(): List<FriendCard> {
        val raw = storage.read(KEY_FRIENDS) ?: return emptyList()
        val parsed = runCatching { json.decodeFromString(ListSerializer(FriendCard.serializer()), raw) }
            .getOrElse { return emptyList() }
        return parsed.mapNotNull { it.sanitized() }.distinctBy { it.id }.take(Friends.MAX)
    }

    companion object {
        const val KEY_PROFILE = "profile"
        const val KEY_FRIENDS = "friends"
        const val KEY_SUGGESTED = "suggested"
    }
}
