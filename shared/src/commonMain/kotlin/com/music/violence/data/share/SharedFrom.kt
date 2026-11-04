package com.music.violence.data.share

import com.music.violence.data.profile.AvatarCatalog
import com.music.violence.data.profile.ProfileCard
import com.music.violence.data.profile.ProfileCardCodec
import com.music.violence.data.profile.ProfileRules

/**
 * "Sent by": the sender's profile card, optionally carried in a song or Jam link as
 * `&f=<id>&fn=<name>&fa=<avatar>&fc=<colour>` so the receiver can say "From Alex" and, for a Jam
 * invite from somebody already on their friends list, join without the preview.
 *
 * All four fields or none; every value goes through the same validation as a profile card. It is
 * only a claim made by whoever built the link, so it is never trusted for anything that matters
 * more than a label and a convenience the receiver has switched on.
 */
object SharedFrom {

    val KEYS = setOf("f", "fn", "fa", "fc")

    sealed interface Read {
        data object None : Read
        data class Ok(val card: ProfileCard) : Read
        data object Bad : Read
    }

    /** `&f=...&fn=...&fa=...&fc=...` for [card]. */
    fun query(card: ProfileCard): String =
        "&f=${card.id}&fn=${ProfileCardCodec.percentEncode(card.displayName)}&fa=${card.avatarId}&fc=${card.color}"

    /** Reads the four fields out of the (still percent-encoded) query values. */
    fun read(fields: Map<String, String>): Read {
        val present = KEYS.count { it in fields }
        if (present == 0) return Read.None
        if (present != KEYS.size) return Read.Bad
        val id = ProfileRules.normalizeId(fields.getValue("f")) ?: return Read.Bad
        val decoded = ProfileCardCodec.percentDecode(fields.getValue("fn")) ?: return Read.Bad
        val name = (ProfileRules.checkName(decoded) as? ProfileRules.NameResult.Valid)?.name ?: return Read.Bad
        val avatar = fields.getValue("fa").takeIf(AvatarCatalog::isKnown) ?: return Read.Bad
        val color = ProfileRules.normalizeColor(fields.getValue("fc")) ?: return Read.Bad
        return Read.Ok(ProfileCard(id, name, avatar, color))
    }
}
