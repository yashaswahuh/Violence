package com.music.violence.data.listentogether

/**
 * Real-time reaction emojis supported for serverless LAN P2P bursts.
 */
enum class JamEmoji(val id: Byte, val symbol: String) {
    FIRE(1.toByte(), "🔥"),
    HEART(2.toByte(), "❤️"),
    SKULL(3.toByte(), "💀"),
    MUSIC(4.toByte(), "🎵"),
    CLAP(5.toByte(), "👏");

    companion object {
        fun fromId(id: Byte): JamEmoji? = entries.firstOrNull { it.id == id }
    }
}

/**
 * A reaction sent or received between peers on the local Wi-Fi subnet.
 */
data class JamReaction(
    val partyHash: Int,
    val emoji: JamEmoji,
    val timestampMs: Long = System.currentTimeMillis(),
)

/**
 * Wire codec for serverless LAN P2P UDP multicast reactions.
 *
 * Binary Frame Format (15 bytes total):
 * - [0x56, 0x4C] (2 bytes magic: "VL")
 * - [PARTY_HASH] (4 bytes integer hash of room code to ignore other parties)
 * - [EMOJI_ID]   (1 byte: 1=🔥, 2=❤️, 3=💀, 4=🎵, 5=👏)
 * - [TIMESTAMP]  (8 bytes epoch millis, big-endian)
 */
object JamReactionProtocol {
    const val MAGIC_BYTE_0: Byte = 0x56 // 'V'
    const val MAGIC_BYTE_1: Byte = 0x4C // 'L'
    const val FRAME_LENGTH: Int = 15
    const val MULTICAST_GROUP: String = "239.255.42.99"
    const val MULTICAST_PORT: Int = 42421

    /**
     * Computes a deterministic integer hash of the party code.
     * Normalized to trimmed uppercase so room codes match case-insensitively.
     */
    fun computePartyHash(partyCode: String): Int {
        val normalized = partyCode.trim().uppercase()
        var hash = 0
        for (i in 0 until normalized.length) {
            hash = 31 * hash + normalized[i].code
        }
        return hash
    }

    /**
     * Serializes a reaction into the 15-byte binary frame format.
     */
    fun encode(reaction: JamReaction): ByteArray {
        val bytes = ByteArray(FRAME_LENGTH)
        bytes[0] = MAGIC_BYTE_0
        bytes[1] = MAGIC_BYTE_1

        val hash = reaction.partyHash
        bytes[2] = (hash ushr 24).toByte()
        bytes[3] = (hash ushr 16).toByte()
        bytes[4] = (hash ushr 8).toByte()
        bytes[5] = hash.toByte()

        bytes[6] = reaction.emoji.id

        val ts = reaction.timestampMs
        bytes[7] = (ts ushr 56).toByte()
        bytes[8] = (ts ushr 48).toByte()
        bytes[9] = (ts ushr 40).toByte()
        bytes[10] = (ts ushr 32).toByte()
        bytes[11] = (ts ushr 24).toByte()
        bytes[12] = (ts ushr 16).toByte()
        bytes[13] = (ts ushr 8).toByte()
        bytes[14] = ts.toByte()

        return bytes
    }

    /**
     * Deserializes a reaction from a UDP datagram payload.
     * Returns null if magic bytes don't match, length is insufficient, or emoji id is unknown.
     */
    fun decode(bytes: ByteArray, length: Int = bytes.size): JamReaction? {
        if (length < FRAME_LENGTH) return null
        if (bytes[0] != MAGIC_BYTE_0 || bytes[1] != MAGIC_BYTE_1) return null

        val partyHash = ((bytes[2].toInt() and 0xFF) shl 24) or
            ((bytes[3].toInt() and 0xFF) shl 16) or
            ((bytes[4].toInt() and 0xFF) shl 8) or
            (bytes[5].toInt() and 0xFF)

        val emoji = JamEmoji.fromId(bytes[6]) ?: return null

        val timestampMs = ((bytes[7].toLong() and 0xFF) shl 56) or
            ((bytes[8].toLong() and 0xFF) shl 48) or
            ((bytes[9].toLong() and 0xFF) shl 40) or
            ((bytes[10].toLong() and 0xFF) shl 32) or
            ((bytes[11].toLong() and 0xFF) shl 24) or
            ((bytes[12].toLong() and 0xFF) shl 16) or
            ((bytes[13].toLong() and 0xFF) shl 8) or
            (bytes[14].toLong() and 0xFF)

        return JamReaction(partyHash, emoji, timestampMs)
    }
}
