package com.music.bitchord.data.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JamProfileIdentityTest {

    private val profile = InAppProfile(
        id = "123e4567-e89b-42d3-a456-426614174000",
        displayName = "Alex",
        avatarId = "guitar",
        color = "0a84ff",
        createdAt = 1,
    )

    @Test
    fun usesTheProfileNameAvatarAndColour() {
        val f = JamProfileIdentity.of(profile)
        assertEquals("Alex", f.name)
        assertEquals("guitar", f.avatarId)
        assertEquals("0a84ff", f.avatarColor)
        assertEquals(profile.id, f.profileId)
    }

    @Test
    fun aPartyNicknameWinsAndBlankDoesNot() {
        assertEquals("DJ A", JamProfileIdentity.of(profile, "  DJ A ").name)
        assertEquals("Alex", JamProfileIdentity.of(profile, "   ").name)
    }

    @Test
    fun theUserIdSeedComesFromTheProfileIdOnly() {
        val seed = JamProfileIdentity.of(profile).userIdSeed
        assertEquals("violence-profile:" + profile.id, seed)
        assertFalse(seed.contains("google", ignoreCase = true))
        assertFalse(seed.contains("@"))
    }

    @Test
    fun theNameFitsTheServerLimitEvenForTwentyFourEmoji() {
        val face = "\uD83D\uDE00"
        val long = profile.copy(displayName = face.repeat(24))
        val f = JamProfileIdentity.of(long)
        assertTrue(f.name.encodeToByteArray().size <= ProfileRules.JAM_NAME_MAX_BYTES)
        assertTrue(f.name.isNotBlank())
    }

    @Test
    fun aLongNicknameIsCutToo() {
        val f = JamProfileIdentity.of(profile, "x".repeat(200))
        assertEquals(ProfileRules.JAM_NAME_MAX_BYTES, f.name.encodeToByteArray().size)
    }
}
