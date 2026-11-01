package com.music.bitchord.data.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProfileCardCodecTest {

    private val id = "123e4567-e89b-42d3-a456-426614174000"

    private fun card(name: String = "Alex", avatar: String = "guitar", color: String = "0a84ff") =
        ProfileCard(id, name, avatar, color)

    private fun uri(
        v: String? = "1",
        id: String? = this.id,
        n: String? = "Alex",
        a: String? = "guitar",
        c: String? = "0a84ff",
        base: String = ProfileCardCodec.CUSTOM_PREFIX,
        extra: String = "",
    ): String {
        val parts = listOfNotNull(
            v?.let { "v=$it" }, id?.let { "id=$it" }, n?.let { "n=$it" },
            a?.let { "a=$it" }, c?.let { "c=$it" },
        )
        return "$base?${parts.joinToString("&")}$extra"
    }

    private fun rejected(text: String?): ProfileCardCodec.Result.Rejected {
        val result = ProfileCardCodec.parse(text)
        return assertIs<ProfileCardCodec.Result.Rejected>(result, "expected a rejection for: $text")
    }

    private fun ok(text: String?): ProfileCard =
        assertIs<ProfileCardCodec.Result.Ok>(ProfileCardCodec.parse(text), "expected a card for: $text").card

    // --------------------------------------------------------------- valid --

    @Test
    fun parsesAValidCustomSchemeCard() {
        assertEquals(card(), ok(uri()))
    }

    @Test
    fun parsesTheWebForm() {
        assertEquals(card(), ok(uri(base = ProfileCardCodec.WEB_BASE)))
        assertEquals(card(), ok(uri(base = ProfileCardCodec.WEB_BASE + "/")))
        assertEquals(card(), ok(uri(base = ProfileCardCodec.WEB_BASE.uppercase())))
    }

    @Test
    fun fieldOrderDoesNotMatterAndSurroundingWhitespaceIsIgnored() {
        val shuffled = "violence://profile?c=0a84ff&a=guitar&n=Alex&id=$id&v=1"
        assertEquals(card(), ok("  \n$shuffled \t"))
    }

    @Test
    fun aFragmentIsIgnored() {
        assertEquals(card(), ok(uri(extra = "#frag")))
    }

    @Test
    fun uuidMayBePlainHexOrUpperCaseAndColourIsLowerCased() {
        val parsed = ok(uri(id = "123E4567E89B42D3A456426614174000", c = "0A84FF"))
        assertEquals(id, parsed.id)
        assertEquals("0a84ff", parsed.color)
    }

    @Test
    fun plusIsASpaceAndPercentTwentyIsASpace() {
        assertEquals("Mary Ann", ok(uri(n = "Mary+Ann")).displayName)
        assertEquals("Mary Ann", ok(uri(n = "Mary%20Ann")).displayName)
    }

    @Test
    fun emojiAndNonLatinNamesRoundTrip() {
        for (name in listOf("DJ \uD83C\uDFA7", "\u5C71\u7530\u592A\u90CE", "\u0645\u062D\u0645\u062F", "Zo\u00EB", "\u03A3\u03CC\u03C6\u03B9\u03B1", "\uD83D\uDC69\u200D\uD83D\uDC69\u200D\uD83D\uDC67")) {
            val link = ProfileCardCodec.toUri(card(name = name))
            assertEquals(card(name = name), ok(link), "round trip of $name")
            val web = ProfileCardCodec.toWebUrl(card(name = name))
            assertEquals(card(name = name), ok(web), "web round trip of $name")
        }
    }

    @Test
    fun rawNonAsciiInTheLinkIsTolerated() {
        assertEquals("Zo\u00EB", ok(uri(n = "Zo\u00EB")).displayName)
    }

    @Test
    fun everyPresetAvatarIsAccepted() {
        for (avatar in AvatarCatalog.all) {
            assertEquals(avatar.id, ok(uri(a = avatar.id)).avatarId)
        }
    }

    @Test
    fun roundTripOfTheLongestAsciiName() {
        val c = card(name = "A".repeat(ProfileRules.NAME_MAX_CODE_POINTS))
        assertEquals(c, ok(ProfileCardCodec.toUri(c)))
    }

    @Test
    fun toUriHasAFixedShape() {
        assertEquals(
            "violence://profile?v=1&id=$id&n=Alex&a=guitar&c=0a84ff",
            ProfileCardCodec.toUri(card()),
        )
        assertEquals(
            "${ProfileCardCodec.WEB_BASE}?v=1&id=$id&n=Alex&a=guitar&c=0a84ff",
            ProfileCardCodec.toWebUrl(card()),
        )
    }

    @Test
    fun nameCharactersThatNeedEscapingAreEscaped() {
        val link = ProfileCardCodec.toUri(card(name = "A&B=C #1+2"))
        assertTrue(link.contains("n=A%26B%3DC%20%231%2B2"), link)
        assertEquals("A&B=C #1+2", ok(link).displayName)
    }

    // ---------------------------------------------------------------- size --

    @Test
    fun latinCardsFitTheNfcTargetAndCjkMayNot() {
        assertTrue(ProfileCardCodec.fitsNfcTarget(ProfileCardCodec.toUri(card("A".repeat(24)))))
        val cjk = "\u5C71".repeat(24)
        assertFalse(ProfileCardCodec.fitsNfcTarget(ProfileCardCodec.toUri(card(cjk))))
        assertEquals(card(cjk), ok(ProfileCardCodec.toUri(card(cjk))), "still a valid card, just a long one")
    }

    @Test
    fun oversizedInputIsRejectedBeforeParsing() {
        val big = uri(n = "A".repeat(600))
        assertEquals(ProfileCardCodec.Error.TOO_LONG, rejected(big).error)
        assertEquals(ProfileCardCodec.Error.TOO_LONG, rejected("x".repeat(513)).error)
    }

    // ------------------------------------------------------------- rejects --

    @Test
    fun notACard() {
        for (text in listOf(null, "", "   ", "hello", "violence://party/ABC123", "https://example.org/Violence/profile?v=1",
            "violence://profile", "violence://profilex?v=1", "https://yashaswahuh.is-a.dev/Violence/party/ABC123")) {
            assertEquals(ProfileCardCodec.Error.NOT_A_CARD, rejected(text).error, "text: $text")
        }
    }

    @Test
    fun wrongOrMissingVersion() {
        assertEquals(ProfileCardCodec.Error.WRONG_VERSION, rejected(uri(v = "2")).error)
        assertEquals(ProfileCardCodec.Error.WRONG_VERSION, rejected(uri(v = "")).error)
        assertEquals(ProfileCardCodec.Error.WRONG_VERSION, rejected(uri(v = "1.0")).error)
        assertEquals(ProfileCardCodec.Error.MISSING_FIELD, rejected(uri(v = null)).error)
    }

    @Test
    fun missingFields() {
        assertEquals(ProfileCardCodec.Error.MISSING_FIELD, rejected(uri(id = null)).error)
        assertEquals(ProfileCardCodec.Error.MISSING_FIELD, rejected(uri(n = null)).error)
        assertEquals(ProfileCardCodec.Error.MISSING_FIELD, rejected(uri(a = null)).error)
        assertEquals(ProfileCardCodec.Error.MISSING_FIELD, rejected(uri(c = null)).error)
    }

    @Test
    fun duplicateAndUnknownFields() {
        assertEquals(ProfileCardCodec.Error.DUPLICATE_FIELD, rejected(uri(extra = "&n=Other")).error)
        assertEquals(ProfileCardCodec.Error.DUPLICATE_FIELD, rejected(uri(extra = "&v=1")).error)
        assertEquals(ProfileCardCodec.Error.UNKNOWN_FIELD, rejected(uri(extra = "&email=a%40b.c")).error)
        assertEquals(ProfileCardCodec.Error.UNKNOWN_FIELD, rejected(uri(extra = "&photo=x")).error)
    }

    @Test
    fun badUuid() {
        for (bad in listOf("", "123", "not-a-uuid-at-all-not-a-uuid-at-all!", "123e4567-e89b-42d3-a456-42661417400",
            "123e4567-e89b-42d3-a456-4266141740000", "123e4567e89b-42d3-a456-426614174000",
            "123e4567-e89b-42d3-a456-42661417400g", "00000000-0000-0000-0000-000000000000",
            "00000000000000000000000000000000")) {
            assertEquals(ProfileCardCodec.Error.BAD_ID, rejected(uri(id = bad)).error, "id: $bad")
        }
    }

    @Test
    fun nameTooLongIsRejectedNotTruncated() {
        val result = rejected(uri(n = "A".repeat(25)))
        assertEquals(ProfileCardCodec.Error.BAD_NAME, result.error)
        assertEquals(ProfileRules.NameProblem.TOO_LONG, result.nameProblem)
    }

    @Test
    fun twentyFourEmojiAreFine_twentyFiveAreNot() {
        val face = "\uD83D\uDE00"
        assertEquals(face.repeat(24), ok(ProfileCardCodec.toUri(card(face.repeat(24)))).displayName)
        assertEquals(ProfileRules.NameProblem.TOO_LONG, rejected(uri(n = ProfileCardCodec.percentEncode(face.repeat(25)))).nameProblem)
    }

    @Test
    fun emptyOrBlankName() {
        assertEquals(ProfileRules.NameProblem.EMPTY, rejected(uri(n = "")).nameProblem)
        assertEquals(ProfileRules.NameProblem.EMPTY, rejected(uri(n = "%20%20")).nameProblem)
        assertEquals(ProfileRules.NameProblem.EMPTY, rejected(uri(n = "+++")).nameProblem)
    }

    @Test
    fun controlCharactersInTheName() {
        for (escaped in listOf("%00", "%01", "%07", "%08", "%09", "%0A", "%0D", "%1B", "%7F", "%C2%85", "%C2%9F")) {
            val result = rejected(uri(n = "Al${escaped}ex"))
            assertEquals(ProfileCardCodec.Error.BAD_NAME, result.error, "escape $escaped")
            assertEquals(ProfileRules.NameProblem.CONTROL_CHARACTER, result.nameProblem, "escape $escaped")
        }
    }

    @Test
    fun invisibleAndDirectionalTricksInTheName() {
        for (escaped in listOf(
            "%E2%80%8B", // zero width space
            "%E2%80%AE", // right-to-left override
            "%E2%80%8F", // right-to-left mark
            "%E2%81%A6", // left-to-right isolate
            "%EF%BB%BF", // BOM
            "%C2%AD", // soft hyphen
            "%E2%80%A8", // line separator
            "%E2%80%A9", // paragraph separator
            "%E3%85%A4", // hangul filler
            "%E2%A0%80", // braille blank
            "%EE%80%80", // private use
            "%F3%A0%80%81", // tag character
        )) {
            val result = rejected(uri(n = "Al${escaped}ex"))
            assertEquals(ProfileCardCodec.Error.BAD_NAME, result.error, "escape $escaped")
            assertEquals(ProfileRules.NameProblem.INVISIBLE_CHARACTER, result.nameProblem, "escape $escaped")
        }
    }

    @Test
    fun stackedCombiningMarksAreRejected() {
        val zalgo = "a" + "\u0300\u0301\u0302\u0303\u0304\u0305"
        assertEquals(ProfileRules.NameProblem.INVISIBLE_CHARACTER, rejected(uri(n = ProfileCardCodec.percentEncode(zalgo))).nameProblem)
        assertEquals("e\u0301", ok(uri(n = ProfileCardCodec.percentEncode("e\u0301"))).displayName)
    }

    @Test
    fun badPercentEncoding() {
        for (bad in listOf("%", "Al%", "Al%4", "Al%GG", "Al%zzex", "%C3", "%FF%FE", "%E2%80", "%ED%A0%80")) {
            assertEquals(ProfileCardCodec.Error.BAD_ENCODING, rejected(uri(n = bad)).error, "n=$bad")
        }
    }

    @Test
    fun unknownAvatar() {
        assertEquals(ProfileCardCodec.Error.BAD_AVATAR, rejected(uri(a = "unicorn")).error)
        assertEquals(ProfileCardCodec.Error.BAD_AVATAR, rejected(uri(a = "")).error)
        assertEquals(ProfileCardCodec.Error.BAD_AVATAR, rejected(uri(a = "GUITAR")).error, "ids are lower case and exact")
    }

    @Test
    fun badColour() {
        for (bad in listOf("", "fff", "#0a84ff", "0a84f", "0a84ff0", "gggggg", "0a84fg", "red")) {
            assertEquals(ProfileCardCodec.Error.BAD_COLOR, rejected(uri(c = bad)).error, "c=$bad")
        }
    }

    @Test
    fun rejectionNeverContainsTheInput() {
        val secret = "hunter2-should-not-appear"
        val result = rejected(uri(n = secret + "%00"))
        assertFalse(result.toString().contains(secret))
    }
}
