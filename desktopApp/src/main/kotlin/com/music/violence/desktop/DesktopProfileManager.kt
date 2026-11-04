package com.music.violence.desktop

import com.music.violence.data.profile.FriendCard
import com.music.violence.data.profile.InAppProfile
import com.music.violence.data.profile.ProfileCard
import com.music.violence.data.profile.ProfileRepository
import com.music.violence.data.profile.ProfileRules
import com.music.violence.data.profile.ProfileStorage
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Desktop handle on the local Violence in-app profile and friends list.
 *
 * Backed by [DesktopPersistence] preferences via [ProfileStorage].
 * Zero network dependencies and zero logging of names/IDs.
 */
internal object DesktopProfileManager {

    private val persistence by lazy { DesktopPersistence() }

    private val storage = object : ProfileStorage {
        override fun read(key: String): String? {
            val value = persistence.string("profile_$key")
            return value.ifBlank { null }
        }

        override fun write(key: String, value: String?) {
            if (value == null) {
                DesktopPreferenceChunks.remove(persistence.preferences, "profile_$key")
            } else {
                persistence.saveString("profile_$key", value)
            }
        }
    }

    private val repository = ProfileRepository(
        storage = storage,
        newId = { UUID.randomUUID().toString() },
        nowMs = { System.currentTimeMillis() },
    )

    val profile: StateFlow<InAppProfile?> get() = repository.profile
    val friends: StateFlow<List<FriendCard>> get() = repository.friends

    fun current(): InAppProfile? = repository.profile.value

    fun isFriend(id: String): Boolean = repository.friends.value.any { it.id == id }

    fun friendName(id: String): String? =
        repository.friends.value.firstOrNull { it.id == id }?.shownName

    fun save(name: String, avatarId: String, color: String): ProfileRepository.SaveResult =
        repository.save(name, avatarId, color)

    fun deleteProfile() = repository.deleteProfile()

    fun addFriend(card: ProfileCard) = repository.addFriend(card)

    fun renameFriend(id: String, nickname: String): Boolean = repository.renameFriend(id, nickname)

    fun removeFriend(id: String) = repository.removeFriend(id)

    fun clearFriends() = repository.clearFriends()

    fun shouldSuggest(): Boolean = repository.shouldSuggest()

    fun markSuggestionHandled() = repository.markSuggestionHandled()
}

