package com.music.bitchord.data.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileRepositoryTest {

    private class MemoryStorage : ProfileStorage {
        val data = HashMap<String, String>()
        override fun read(key: String) = data[key]
        override fun write(key: String, value: String?) {
            if (value == null) data.remove(key) else data[key] = value
        }
    }

    private var idCounter = 0
    private fun repo(storage: ProfileStorage = MemoryStorage(), now: Long = 1000) =
        ProfileRepository(
            storage,
            newId = { "00000000-0000-4000-8000-" + (++idCounter).toString().padStart(12, '0') },
            nowMs = { now },
        )

    private val friendCard = ProfileCard("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa", "Ann", "cat", "ff9500")

    @Test
    fun startsWithNothing() {
        val r = repo()
        assertNull(r.profile.value)
        assertTrue(r.friends.value.isEmpty())
        assertTrue(r.shouldSuggest())
    }

    @Test
    fun savingCreatesTheProfileOnceAndKeepsItsIdOnEdit() {
        val storage = MemoryStorage()
        val r = repo(storage, now = 42)
        val created = assertIs<ProfileRepository.SaveResult.Saved>(r.save("  Alex ", "guitar", "0A84FF")).profile
        assertEquals("Alex", created.displayName)
        assertEquals("0a84ff", created.color)
        assertEquals(42, created.createdAt)
        val edited = assertIs<ProfileRepository.SaveResult.Saved>(r.save("Alexandra", "fox", "34c759")).profile
        assertEquals(created.id, edited.id, "the id is made once")
        assertEquals(42, edited.createdAt)
        assertEquals("fox", edited.avatarId)
        assertEquals(edited, repo(storage).profile.value, "a new instance reads it back from storage")
    }

    @Test
    fun invalidNamesAreRefusedAndNothingIsWritten() {
        val storage = MemoryStorage()
        val r = repo(storage)
        val bad = assertIs<ProfileRepository.SaveResult.InvalidName>(r.save("a\nb", "cat", "ff9500"))
        assertEquals(ProfileRules.NameProblem.CONTROL_CHARACTER, bad.problem)
        assertIs<ProfileRepository.SaveResult.InvalidName>(r.save("", "cat", "ff9500"))
        assertIs<ProfileRepository.SaveResult.InvalidName>(r.save("x".repeat(25), "cat", "ff9500"))
        assertNull(r.profile.value)
        assertTrue(storage.data.isEmpty())
    }

    @Test
    fun unknownAvatarAndBadColourFallBackToDefaults() {
        val r = repo()
        val p = assertIs<ProfileRepository.SaveResult.Saved>(r.save("Alex", "unicorn", "zzz")).profile
        assertEquals(ProfileRules.DEFAULT_AVATAR_ID, p.avatarId)
        assertEquals(ProfileRules.DEFAULT_COLOR, p.color)
    }

    @Test
    fun deletingTheProfileRemovesItButKeepsFriends() {
        val storage = MemoryStorage()
        val r = repo(storage)
        r.save("Alex", "cat", "ff9500")
        r.addFriend(friendCard)
        r.deleteProfile()
        assertNull(r.profile.value)
        assertNull(storage.data[ProfileRepository.KEY_PROFILE])
        assertEquals(1, r.friends.value.size)
        val again = assertIs<ProfileRepository.SaveResult.Saved>(r.save("Alex", "cat", "ff9500")).profile
        assertNotEquals("00000000-0000-4000-8000-000000000001", again.id, "a new profile gets a new id")
    }

    @Test
    fun friendsAreStoredRenamedRemovedAndClearedInOneAction() {
        val storage = MemoryStorage()
        val r = repo(storage, now = 7)
        assertIs<Friends.AddResult.Added>(r.addFriend(friendCard))
        assertTrue(r.renameFriend(friendCard.id, "Annie"))
        assertEquals("Annie", repo(storage).friends.value.single().shownName)
        assertFalse(r.renameFriend(friendCard.id, "bad\u0000"))
        r.addFriend(friendCard.copy(id = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb", displayName = "Ben"))
        r.removeFriend(friendCard.id)
        assertEquals(listOf("Ben"), r.friends.value.map { it.displayName })
        r.clearFriends()
        assertTrue(r.friends.value.isEmpty())
        assertNull(storage.data[ProfileRepository.KEY_FRIENDS])
        assertTrue(repo(storage).friends.value.isEmpty())
    }

    @Test
    fun addingYourOwnCardIsRefused() {
        val r = repo()
        val me = assertIs<ProfileRepository.SaveResult.Saved>(r.save("Alex", "cat", "ff9500")).profile
        assertEquals(Friends.AddResult.IsYou, r.addFriend(me.toCard()))
        assertTrue(r.friends.value.isEmpty())
    }

    @Test
    fun theSuggestionIsOfferedOnceAndNeverWithAProfile() {
        val storage = MemoryStorage()
        val r = repo(storage)
        assertTrue(r.shouldSuggest())
        r.markSuggestionHandled()
        assertFalse(r.shouldSuggest())
        assertFalse(repo(storage).shouldSuggest())
        val other = repo()
        other.save("Alex", "cat", "ff9500")
        assertFalse(other.shouldSuggest())
    }

    @Test
    fun damagedStorageGivesAnEmptyStateNotACrash() {
        val storage = MemoryStorage()
        storage.data[ProfileRepository.KEY_PROFILE] = "{not json"
        storage.data[ProfileRepository.KEY_FRIENDS] = "[1,2,3]"
        val r = repo(storage)
        assertNull(r.profile.value)
        assertTrue(r.friends.value.isEmpty())
    }

    @Test
    fun editedStorageIsCleanedOnLoad() {
        val storage = MemoryStorage()
        storage.data[ProfileRepository.KEY_PROFILE] =
            """{"id":"123E4567E89B42D3A456426614174000","displayName":"  Al  ex ","avatarId":"nope","color":"#zzz","createdAt":5}"""
        storage.data[ProfileRepository.KEY_FRIENDS] = """[
            {"id":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa","displayName":"Ann","avatarId":"cat","color":"ff9500","nickname":"bad\u0000"},
            {"id":"not-an-id","displayName":"Evil","avatarId":"cat","color":"ff9500"},
            {"id":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa","displayName":"Duplicate","avatarId":"cat","color":"ff9500"},
            {"id":"bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb","displayName":"line\nbreak","avatarId":"cat","color":"ff9500"}
        ]"""
        val r = repo(storage)
        val p = r.profile.value!!
        assertEquals("123e4567-e89b-42d3-a456-426614174000", p.id)
        assertEquals("Al ex", p.displayName)
        assertEquals(ProfileRules.DEFAULT_AVATAR_ID, p.avatarId)
        assertEquals(ProfileRules.DEFAULT_COLOR, p.color)
        assertEquals(listOf("Ann"), r.friends.value.map { it.displayName })
        assertNull(r.friends.value.single().nickname)
    }

    @Test
    fun noPhotoOrAccountDataIsEverStored() {
        val storage = MemoryStorage()
        val r = repo(storage)
        r.save("Alex", "cat", "ff9500")
        r.addFriend(friendCard)
        val all = storage.data.values.joinToString(" ")
        for (forbidden in listOf("photo", "email", "cookie", "token", "google")) {
            assertFalse(all.contains(forbidden, ignoreCase = true), forbidden)
        }
    }
}
