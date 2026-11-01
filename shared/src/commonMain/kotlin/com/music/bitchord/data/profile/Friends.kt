package com.music.bitchord.data.profile

/**
 * The rules of the friends list, as plain functions on a list, so the same behaviour is
 * testable without storage and shared by every screen.
 */
object Friends {

    /** A sanity bound on stored data, far above any circle of friends this app is for. */
    const val MAX = 200

    sealed interface AddResult {
        data class Added(val list: List<FriendCard>) : AddResult

        /** Already a friend: the stored copy took the card's newer name/avatar/colour. */
        data class Updated(val list: List<FriendCard>) : AddResult
        data object IsYou : AddResult
        data object ListFull : AddResult
    }

    fun add(list: List<FriendCard>, card: ProfileCard, selfId: String?, now: Long): AddResult {
        if (selfId != null && card.id == selfId) return AddResult.IsYou
        val existing = list.indexOfFirst { it.id == card.id }
        if (existing >= 0) {
            val old = list[existing]
            val updated = old.copy(displayName = card.displayName, avatarId = card.avatarId, color = card.color)
            return AddResult.Updated(list.toMutableList().also { it[existing] = updated })
        }
        if (list.size >= MAX) return AddResult.ListFull
        return AddResult.Added(list + FriendCard(card.id, card.displayName, card.avatarId, card.color, now))
    }

    /**
     * Sets or clears the local nickname of [id]. Blank clears it; an invalid nickname (control
     * characters, too long) leaves the list as it was and returns null.
     */
    fun rename(list: List<FriendCard>, id: String, nickname: String): List<FriendCard>? {
        val index = list.indexOfFirst { it.id == id }
        if (index < 0) return list
        val clean = if (nickname.isBlank()) {
            null
        } else {
            ProfileRules.normalizeName(nickname) ?: return null
        }
        return list.toMutableList().also { it[index] = it[index].copy(nickname = clean) }
    }

    fun remove(list: List<FriendCard>, id: String): List<FriendCard> = list.filterNot { it.id == id }

    /** Alphabetical by the name the user sees (nickname first), ignoring case. */
    fun sorted(list: List<FriendCard>): List<FriendCard> =
        list.sortedBy { it.shownName.lowercase() }
}
