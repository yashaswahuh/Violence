package com.music.violence.data.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileRulesAndFriendsTest {

    private val a = ProfileCard("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa", "Ann", "cat", "ff9500")
    private val b = ProfileCard("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb", "Ben", "fox", "34c759")

    // ----------------------------------------------------------------- names --

    @Test
    fun namesAreTrimmedAndSpacesCollapsed() {
        assertEquals("Mary Ann", ProfileRules.normalizeName("  Mary \u00A0  Ann  "))
        assertEquals("Ann", ProfileRules.normalizeName("Ann"))
    }

    @Test
    fun lengthIsCountedInCodePoints() {
        val face = "\uD83D\uDE00"
        assertEquals(24, ProfileRules.normalizeName(face.repeat(24))?.let { it.length / 2 })
        assertNull(ProfileRules.normalizeName(face.repeat(25)))
        assertEquals("a".repeat(24), ProfileRules.normalizeName("a".repeat(24)))
        assertNull(ProfileRules.normalizeName("a".repeat(25)))
    }

    @Test
    fun loneSurrogatesAreRejected() {
        assertNull(ProfileRules.normalizeName("a\uD83Db"))
        assertNull(ProfileRules.normalizeName("a\uDE00b"))
    }

    @Test
    fun controlAndFormatCharactersAreRejected() {
        assertNull(ProfileRules.normalizeName("a\nb"))
        assertNull(ProfileRules.normalizeName("a\tb"))
        assertNull(ProfileRules.normalizeName("a\u0000b"))
        assertNull(ProfileRules.normalizeName("a\u200Bb"))
        assertNull(ProfileRules.normalizeName("a\u202Eb"))
    }

    @Test
    fun zeroWidthJoinerEmojiSequencesAreKept() {
        val family = "\uD83D\uDC69\u200D\uD83D\uDC69\u200D\uD83D\uDC67"
        assertEquals(family, ProfileRules.normalizeName(family))
        val heart = "\u2764\uFE0F"
        assertEquals(heart, ProfileRules.normalizeName(heart))
    }

    @Test
    fun jamNameIsCutOnACharacterBoundary() {
        assertEquals("Alex", ProfileRules.truncateUtf8("Alex"))
        val face = "\uD83D\uDE00"
        val cut = ProfileRules.truncateUtf8(face.repeat(24))
        assertEquals(20, cut.length / 2)
        assertTrue(cut.encodeToByteArray().size <= ProfileRules.JAM_NAME_MAX_BYTES)
        // 24 CJK characters are 72 bytes: they already fit, so nothing is cut.
        assertEquals(24, ProfileRules.truncateUtf8("\u5C71".repeat(24)).length)
        // 4-byte characters mixed with 3-byte ones still end on a boundary and within the limit.
        val mixed = ProfileRules.truncateUtf8(("\u5C71" + face).repeat(12))
        assertTrue(mixed.encodeToByteArray().size <= ProfileRules.JAM_NAME_MAX_BYTES)
        assertEquals(mixed, ProfileRules.normalizeName(mixed))
    }

    @Test
    fun idsAndColoursAreNormalised() {
        assertEquals("123e4567-e89b-42d3-a456-426614174000", ProfileRules.normalizeId(" 123E4567E89B42D3A456426614174000 "))
        assertNull(ProfileRules.normalizeId("nope"))
        assertEquals("0a84ff", ProfileRules.normalizeColor("0A84FF"))
        assertNull(ProfileRules.normalizeColor("#0a84ff"))
    }

    @Test
    fun presetsAreAllValidAndUnique() {
        val ids = AvatarCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it.length in 1..16 && it.all { c -> c in 'a'..'z' || c in '0'..'9' } })
        assertTrue(AvatarCatalog.isKnown(ProfileRules.DEFAULT_AVATAR_ID))
        assertTrue(ProfileRules.COLOR_PRESETS.all { ProfileRules.normalizeColor(it) == it })
        assertTrue(AvatarCatalog.all.all { it.glyph.isNotEmpty() })
    }

    // --------------------------------------------------------------- friends --

    @Test
    fun addsAFriend() {
        val result = assertIs<Friends.AddResult.Added>(Friends.add(emptyList(), a, selfId = null, now = 5))
        assertEquals(listOf(FriendCard(a.id, "Ann", "cat", "ff9500", 5, null)), result.list)
    }

    @Test
    fun refusesYourOwnCard() {
        assertEquals(Friends.AddResult.IsYou, Friends.add(emptyList(), a, selfId = a.id, now = 1))
    }

    @Test
    fun addingAgainUpdatesTheCardButKeepsNicknameAndDate() {
        val first = (Friends.add(emptyList(), a, null, 5) as Friends.AddResult.Added).list
        val renamed = Friends.rename(first, a.id, "Annie")!!
        val newer = a.copy(displayName = "Ann B", avatarId = "fox")
        val result = assertIs<Friends.AddResult.Updated>(Friends.add(renamed, newer, null, 99))
        val friend = result.list.single()
        assertEquals("Ann B", friend.displayName)
        assertEquals("fox", friend.avatarId)
        assertEquals("Annie", friend.nickname)
        assertEquals(5, friend.addedAt)
        assertEquals("Annie", friend.shownName)
    }

    @Test
    fun renameSetsAndClearsTheNickname() {
        val list = (Friends.add(emptyList(), a, null, 1) as Friends.AddResult.Added).list
        val named = Friends.rename(list, a.id, "  Annie  ")!!
        assertEquals("Annie", named.single().nickname)
        val cleared = Friends.rename(named, a.id, "   ")!!
        assertNull(cleared.single().nickname)
        assertEquals("Ann", cleared.single().shownName)
    }

    @Test
    fun renameRejectsBadNicknamesAndLeavesTheListAlone() {
        val list = (Friends.add(emptyList(), a, null, 1) as Friends.AddResult.Added).list
        assertNull(Friends.rename(list, a.id, "a".repeat(25)))
        assertNull(Friends.rename(list, a.id, "bad\nname"))
        assertEquals(list, Friends.rename(list, "unknown-id", "x"))
    }

    @Test
    fun removeAndSort() {
        var list = (Friends.add(emptyList(), b, null, 1) as Friends.AddResult.Added).list
        list = (Friends.add(list, a, null, 2) as Friends.AddResult.Added).list
        assertEquals(listOf("Ann", "Ben"), Friends.sorted(list).map { it.shownName })
        assertEquals(listOf(b.id), Friends.remove(list, a.id).map { it.id })
        assertEquals(list, Friends.remove(list, "zzz"))
    }

    @Test
    fun theListHasAGenerousCap() {
        var list = emptyList<FriendCard>()
        for (i in 0 until Friends.MAX) {
            val hex = i.toString(16).padStart(8, '0')
            val card = ProfileCard("$hex-aaaa-4aaa-8aaa-aaaaaaaaaaaa", "F$i", "cat", "ff9500")
            list = (Friends.add(list, card, null, i.toLong()) as Friends.AddResult.Added).list
        }
        assertEquals(Friends.MAX, list.size)
        assertEquals(Friends.AddResult.ListFull, Friends.add(list, a, null, 1))
        // an existing friend can still be updated at the cap
        assertIs<Friends.AddResult.Updated>(Friends.add(list, ProfileCard(list[0].id, "New", "cat", "ff9500"), null, 1))
    }
}
