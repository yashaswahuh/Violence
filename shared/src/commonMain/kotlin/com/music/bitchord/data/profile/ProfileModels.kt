package com.music.bitchord.data.profile

import kotlinx.serialization.Serializable

/**
 * The bundled avatars. Each is an emoji glyph drawn on a circle in the profile colour, so there
 * are no image assets to ship and an id looks the same on the phone, the TV and the web.
 *
 * **Append only.** An id is part of the profile-card format and of the Jam wire format: never
 * rename or reuse one. The web copy lives in `web/app/src/profile/catalog.ts`.
 */
object AvatarCatalog {

    class Avatar(val id: String, val glyph: String)

    val all: List<Avatar> = listOf(
        Avatar("headphones", "\uD83C\uDFA7"),
        Avatar("note", "\uD83C\uDFB5"),
        Avatar("vinyl", "\uD83D\uDCBF"),
        Avatar("guitar", "\uD83C\uDFB8"),
        Avatar("mic", "\uD83C\uDFA4"),
        Avatar("piano", "\uD83C\uDFB9"),
        Avatar("drum", "\uD83E\uDD41"),
        Avatar("trumpet", "\uD83C\uDFBA"),
        Avatar("violin", "\uD83C\uDFBB"),
        Avatar("radio", "\uD83D\uDCFB"),
        Avatar("bolt", "\u26A1"),
        Avatar("flame", "\uD83D\uDD25"),
        Avatar("star", "\u2B50"),
        Avatar("moon", "\uD83C\uDF19"),
        Avatar("heart", "\uD83D\uDC9C"),
        Avatar("rocket", "\uD83D\uDE80"),
        Avatar("ghost", "\uD83D\uDC7B"),
        Avatar("alien", "\uD83D\uDC7D"),
        Avatar("robot", "\uD83E\uDD16"),
        Avatar("cat", "\uD83D\uDC31"),
        Avatar("fox", "\uD83E\uDD8A"),
        Avatar("panda", "\uD83D\uDC3C"),
        Avatar("wolf", "\uD83D\uDC3A"),
        Avatar("skull", "\uD83D\uDC80"),
    )

    private val byId = all.associateBy { it.id }

    fun isKnown(id: String?): Boolean = id != null && id in byId

    /** The glyph for [id], or null for an id this build does not know. */
    fun glyphOf(id: String?): String? = id?.let { byId[it]?.glyph }
}

/**
 * This device's own Violence identity.
 *
 * Deliberately has no link to any Google, Spotify or Discord account. [id] is a random UUID made
 * once on this device. A local photo, if the user picks one, is kept outside this class and is
 * never part of anything that leaves the device.
 */
@Serializable
data class InAppProfile(
    val id: String,
    val displayName: String,
    val avatarId: String = ProfileRules.DEFAULT_AVATAR_ID,
    val color: String = ProfileRules.DEFAULT_COLOR,
    val createdAt: Long = 0L,
) {
    /** The part of the profile that is shared (a card, or a Jam join). */
    fun toCard(): ProfileCard = ProfileCard(id, displayName, avatarId, color)
}

/**
 * The shareable part of a profile: exactly what travels in a profile link and nothing more.
 * Only [ProfileCardCodec.parse] and the constructors of the app's own profile make these, so a
 * value of this type has already passed the validation in [ProfileRules].
 */
@Serializable
data class ProfileCard(
    val id: String,
    val displayName: String,
    val avatarId: String,
    val color: String,
)

/** Somebody the user added from their profile card. */
@Serializable
data class FriendCard(
    val id: String,
    val displayName: String,
    val avatarId: String,
    val color: String,
    val addedAt: Long = 0L,
    /** A name the user gave them, local only; empty/null means "use their own name". */
    val nickname: String? = null,
) {
    /** What to show in lists: the nickname if there is one, else their own name. */
    val shownName: String get() = nickname?.takeIf { it.isNotBlank() } ?: displayName
}

/**
 * Cleans a profile read from storage, or returns null if it cannot be used.
 * Storage is untrusted too: it may have been edited, truncated or written by another version.
 */
fun InAppProfile.sanitized(): InAppProfile? {
    val cleanId = ProfileRules.normalizeId(id) ?: return null
    val cleanName = ProfileRules.normalizeName(displayName) ?: return null
    return copy(
        id = cleanId,
        displayName = cleanName,
        avatarId = if (AvatarCatalog.isKnown(avatarId)) avatarId else ProfileRules.DEFAULT_AVATAR_ID,
        color = ProfileRules.normalizeColor(color) ?: ProfileRules.DEFAULT_COLOR,
    )
}

/** Same as [sanitized] for a friend; an unknown avatar becomes the default one. */
fun FriendCard.sanitized(): FriendCard? {
    val cleanId = ProfileRules.normalizeId(id) ?: return null
    val cleanName = ProfileRules.normalizeName(displayName) ?: return null
    return copy(
        id = cleanId,
        displayName = cleanName,
        avatarId = if (AvatarCatalog.isKnown(avatarId)) avatarId else ProfileRules.DEFAULT_AVATAR_ID,
        color = ProfileRules.normalizeColor(color) ?: ProfileRules.DEFAULT_COLOR,
        nickname = nickname?.let(ProfileRules::normalizeName),
    )
}
