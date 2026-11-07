package com.music.violence.data.listentogether

/**
 * A nearby Jam session detected on the local Wi-Fi network.
 */
data class DiscoveredJam(
    val partyCode: String,
    val hostName: String,
    val currentTrack: String,
    val address: String? = null,
    val port: Int = JamDiscoveryProtocol.DEFAULT_PORT,
    val lastSeenMs: Long = System.currentTimeMillis(),
)

object JamDiscoveryProtocol {
    const val SERVICE_TYPE: String = "_violence-jam._tcp"
    const val SERVICE_DOMAIN: String = "local."
    const val FULL_SERVICE_TYPE: String = "_violence-jam._tcp.local."
    const val DEFAULT_PORT: Int = 42421

    const val KEY_PARTY_CODE: String = "partyCode"
    const val KEY_HOST_NAME: String = "hostName"
    const val KEY_CURRENT_TRACK: String = "currentTrack"

    const val ANNOUNCEMENT_MAGIC_0: Byte = 0x56 // 'V'
    const val ANNOUNCEMENT_MAGIC_1: Byte = 0x4A // 'J' (Violence Jam)

    /**
     * Serializes a lightweight LAN announcement frame over UDP multicast.
     */
    fun encodeAnnouncement(jam: DiscoveredJam): ByteArray {
        val payload = "${jam.partyCode}\n${jam.hostName}\n${jam.currentTrack}\n${jam.port}".encodeToByteArray()
        val result = ByteArray(2 + payload.size)
        result[0] = ANNOUNCEMENT_MAGIC_0
        result[1] = ANNOUNCEMENT_MAGIC_1
        payload.copyInto(result, destinationOffset = 2)
        return result
    }

    /**
     * Deserializes an announcement frame. Returns null if invalid.
     */
    fun decodeAnnouncement(bytes: ByteArray, length: Int, sourceAddress: String? = null): DiscoveredJam? {
        if (length < 3) return null
        if (bytes[0] != ANNOUNCEMENT_MAGIC_0 || bytes[1] != ANNOUNCEMENT_MAGIC_1) return null

        val text = bytes.decodeToString(startIndex = 2, endIndex = length)
        val lines = text.split('\n')
        if (lines.size < 3) return null

        val partyCode = lines[0].trim()
        val hostName = lines[1].trim()
        val currentTrack = lines[2].trim()
        val port = lines.getOrNull(3)?.toIntOrNull() ?: DEFAULT_PORT

        if (partyCode.isEmpty()) return null
        return DiscoveredJam(
            partyCode = partyCode,
            hostName = hostName,
            currentTrack = currentTrack,
            address = sourceAddress,
            port = port,
            lastSeenMs = System.currentTimeMillis(),
        )
    }
}
