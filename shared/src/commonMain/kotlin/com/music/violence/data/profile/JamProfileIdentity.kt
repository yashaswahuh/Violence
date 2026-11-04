package com.music.violence.data.profile

/**
 * What a device with a profile tells a Jam party about itself, and nothing else.
 *
 * Pure and platform free so the rule is tested in one place; the app hashes [userIdSeed]
 * (SHA-256, first 32 hex digits) because hashing is not available in common code.
 */
data class JamProfileFields(
    /** The name members see, already cut to what the server accepts. */
    val name: String,
    val profileId: String,
    val avatarId: String,
    val avatarColor: String,
    /** Hashed into the server-side `userId`. Derived from the profile id only, never an account. */
    val userIdSeed: String,
)

object JamProfileIdentity {

    private const val SEED_PREFIX = "violence-profile:"

    /**
     * [nickname] is the optional per-party name the Jam screen already offers; when the user
     * typed one it wins, otherwise the profile name is used. The server rejects names over
     * 80 UTF-8 bytes, so the result is cut on a character boundary (a 24-emoji profile name is
     * 96 bytes).
     */
    fun of(profile: InAppProfile, nickname: String = ""): JamProfileFields {
        val wanted = nickname.trim().ifBlank { profile.displayName }
        val name = ProfileRules.truncateUtf8(wanted).ifBlank { profile.displayName.take(1) }
        return JamProfileFields(
            name = name,
            profileId = profile.id,
            avatarId = profile.avatarId,
            avatarColor = profile.color,
            userIdSeed = SEED_PREFIX + profile.id,
        )
    }
}
