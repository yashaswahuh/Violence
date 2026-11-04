package com.music.violence.desktop

import com.music.violence.data.profile.FriendCard
import com.music.violence.data.profile.Friends
import com.music.violence.data.profile.ProfileCard
import com.music.violence.data.profile.ProfileCardCodec
import com.music.violence.data.profile.ProfileRules
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopProfileManagerTest {

    @BeforeTest
    @AfterTest
    fun cleanup() {
        DesktopProfileManager.deleteProfile()
        DesktopProfileManager.clearFriends()
    }

    @Test
    fun `profile saving, reading, toCard, and deletion works`() {
        assertNull(DesktopProfileManager.current())

        val saveResult = DesktopProfileManager.save("Alice", "headphones", "fa2d48")
        assertNotNull(saveResult)

        val current = DesktopProfileManager.current()
        assertNotNull(current)
        assertEquals("Alice", current.displayName)
        assertEquals("headphones", current.avatarId)
        assertEquals("fa2d48", current.color)

        val card = current.toCard()
        assertEquals(current.id, card.id)
        assertEquals("Alice", card.displayName)

        DesktopProfileManager.deleteProfile()
        assertNull(DesktopProfileManager.current())
    }

    @Test
    fun `friends list CRUD works`() {
        assertEquals(0, DesktopProfileManager.friends.value.size)

        val friend1 = ProfileCard(UUID.randomUUID().toString(), "Bob", "cat", "0a84ff")
        val addRes = DesktopProfileManager.addFriend(friend1)
        assertTrue(addRes is Friends.AddResult.Added)
        assertTrue(DesktopProfileManager.isFriend(friend1.id))
        assertEquals("Bob", DesktopProfileManager.friendName(friend1.id))

        // Rename friend
        DesktopProfileManager.renameFriend(friend1.id, "Bobby")
        assertEquals("Bobby", DesktopProfileManager.friendName(friend1.id))

        // Remove friend
        DesktopProfileManager.removeFriend(friend1.id)
        assertFalse(DesktopProfileManager.isFriend(friend1.id))
        assertNull(DesktopProfileManager.friendName(friend1.id))
    }

    @Test
    fun `profile card codec roundtrip produces valid link and parses back`() {
        val validUuid = UUID.randomUUID().toString()
        val card = ProfileCard(validUuid, "Charlie", "guitar", "ff9500")
        val uri = ProfileCardCodec.toUri(card)
        assertTrue(uri.startsWith("violence://profile?"))

        val parsed = ProfileCardCodec.parse(uri)
        assertTrue(parsed is ProfileCardCodec.Result.Ok)
        // Normalize UUID formatting (without dashes or lower)
        assertEquals(ProfileRules.normalizeId(card.id), ProfileRules.normalizeId(parsed.card.id))
        assertEquals(card.displayName, parsed.card.displayName)
        assertEquals(card.avatarId, parsed.card.avatarId)
        assertEquals(card.color, parsed.card.color)
    }
}
