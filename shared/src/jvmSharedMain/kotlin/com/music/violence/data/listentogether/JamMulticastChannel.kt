package com.music.violence.data.listentogether

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface

/**
 * Serverless P2P UDP Multicast Channel for real-time reactions and zero-config discovery.
 * Group: 239.255.42.99, Port: 42421.
 */
class JamMulticastChannel(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val _reactions = MutableSharedFlow<JamReaction>(extraBufferCapacity = 64)
    val reactions: SharedFlow<JamReaction> = _reactions.asSharedFlow()

    private val _discoveredJams = MutableStateFlow<List<DiscoveredJam>>(emptyList())
    val discoveredJams: StateFlow<List<DiscoveredJam>> = _discoveredJams.asStateFlow()

    private var activePartyHash: Int? = null

    private var socket: MulticastSocket? = null
    private var listenJob: Job? = null
    private var cleanupJob: Job? = null
    private var groupAddress: InetAddress? = null

    @Synchronized
    fun start() {
        if (socket != null && socket?.isClosed == false) return

        try {
            val group = InetAddress.getByName(JamReactionProtocol.MULTICAST_GROUP)
            groupAddress = group
            val s = MulticastSocket(null).apply {
                reuseAddress = true
                broadcast = true
                bind(InetSocketAddress(JamReactionProtocol.MULTICAST_PORT))
                timeToLive = 4 // LAN scope
            }

            // Join group on all multicast-capable interfaces
            joinMulticastInterfaces(s, group)
            socket = s

            listenJob = scope.launch(Dispatchers.IO) {
                receiveLoop(s)
            }

            cleanupJob = scope.launch(Dispatchers.IO) {
                while (isActive) {
                    delay(5_000)
                    pruneStaleJams()
                }
            }
        } catch (_: Exception) {
            // Sockets may fail in isolated sandboxes or restricted network profiles
        }
    }

    private fun joinMulticastInterfaces(s: MulticastSocket, group: InetAddress) {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return
            val groupSocketAddr = InetSocketAddress(group, JamReactionProtocol.MULTICAST_PORT)
            for (nif in interfaces) {
                try {
                    if (nif.isUp && nif.supportsMulticast() && !nif.isLoopback) {
                        s.joinGroup(groupSocketAddr, nif)
                    }
                } catch (_: Exception) {
                    // Interface may not support group join
                }
            }
        } catch (_: Exception) {
            try {
                @Suppress("DEPRECATION")
                s.joinGroup(group)
            } catch (_: Exception) {}
        }
    }

    private suspend fun receiveLoop(s: MulticastSocket) {
        val buffer = ByteArray(2048)
        while (scope.isActive && !s.isClosed) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                s.receive(packet)
                handleIncomingPacket(packet)
            } catch (_: Exception) {
                if (s.isClosed) break
                delay(100)
            }
        }
    }

    private fun handleIncomingPacket(packet: DatagramPacket) {
        val data = packet.data
        val len = packet.length
        if (len < 2) return

        // 1. Check if reaction frame: [0x56, 0x4C] ("VL")
        if (data[0] == JamReactionProtocol.MAGIC_BYTE_0 && data[1] == JamReactionProtocol.MAGIC_BYTE_1) {
            val reaction = JamReactionProtocol.decode(data, len) ?: return
            recordReaction(reaction)
            return
        }

        // 2. Check if discovery announcement: [0x56, 0x4A] ("VJ")
        if (data[0] == JamDiscoveryProtocol.ANNOUNCEMENT_MAGIC_0 && data[1] == JamDiscoveryProtocol.ANNOUNCEMENT_MAGIC_1) {
            val jam = JamDiscoveryProtocol.decodeAnnouncement(
                bytes = data,
                length = len,
                sourceAddress = packet.address?.hostAddress,
            ) ?: return

            recordDiscoveredJam(jam)
        }
    }

    fun recordDiscoveredJam(jam: DiscoveredJam) {
        _discoveredJams.update { list ->
            val filtered = list.filter { it.partyCode != jam.partyCode }
            filtered + jam
        }
    }

    private fun pruneStaleJams() {
        val now = System.currentTimeMillis()
        _discoveredJams.update { list ->
            list.filter { now - it.lastSeenMs < 15_000 }
        }
    }

    /**
     * Set the current active party room code to filter incoming reactions.
     */
    fun setActiveParty(partyCode: String?) {
        activePartyHash = partyCode?.let { JamReactionProtocol.computePartyHash(it) }
    }

    private data class SeenReaction(val receivedAtMs: Long, val reactionTimestampMs: Long, val emojiId: Byte)
    private val recentReactions = ArrayDeque<SeenReaction>()

    /**
     * Records an incoming or sent reaction, deduplicating bursts within 100ms.
     */
    @Synchronized
    fun recordReaction(reaction: JamReaction): Boolean {
        val currentHash = activePartyHash ?: return false
        if (reaction.partyHash != currentHash) return false

        val now = System.currentTimeMillis()
        while (recentReactions.isNotEmpty() && now - recentReactions.first().receivedAtMs > 3000) {
            recentReactions.removeFirst()
        }

        val emojiId = reaction.emoji.id
        for (seen in recentReactions) {
            if (seen.emojiId == emojiId && kotlin.math.abs(reaction.timestampMs - seen.reactionTimestampMs) < 100) {
                return false // Duplicate reaction (e.g. dual transport or loopback)
            }
        }
        recentReactions.addLast(SeenReaction(now, reaction.timestampMs, emojiId))
        _reactions.tryEmit(reaction)
        return true
    }

    /**
     * Broadcasts a reaction to all peers in the room on local Wi-Fi (multicast + broadcast).
     */
    fun sendReaction(
        partyCode: String,
        emoji: JamEmoji,
        timestampMs: Long = System.currentTimeMillis(),
    ): JamReaction? {
        return try {
            val hash = JamReactionProtocol.computePartyHash(partyCode)
            val reaction = JamReaction(hash, emoji, timestampMs)
            recordReaction(reaction)

            val bytes = JamReactionProtocol.encode(reaction)

            scope.launch(Dispatchers.IO) {
                val targets = mutableListOf<InetAddress>()
                runCatching {
                    targets.add(groupAddress ?: InetAddress.getByName(JamReactionProtocol.MULTICAST_GROUP))
                }
                runCatching {
                    targets.add(InetAddress.getByName("255.255.255.255"))
                }
                runCatching {
                    val interfaces = NetworkInterface.getNetworkInterfaces() ?: return@runCatching
                    for (nif in interfaces) {
                        if (!nif.isUp || nif.isLoopback) continue
                        for (addr in nif.interfaceAddresses) {
                            addr.broadcast?.let { targets.add(it) }
                        }
                    }
                }

                val currentSocket = socket ?: return@launch
                for (target in targets.distinct()) {
                    runCatching {
                        val packet = DatagramPacket(bytes, bytes.size, target, JamReactionProtocol.MULTICAST_PORT)
                        currentSocket.send(packet)
                    }
                }
            }
            reaction
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Broadcasts a discovery announcement to peers on local Wi-Fi.
     */
    fun broadcastAnnouncement(jam: DiscoveredJam) {
        val bytes = JamDiscoveryProtocol.encodeAnnouncement(jam)

        scope.launch(Dispatchers.IO) {
            val targets = mutableListOf<InetAddress>()
            runCatching {
                targets.add(groupAddress ?: InetAddress.getByName(JamReactionProtocol.MULTICAST_GROUP))
            }
            runCatching {
                targets.add(InetAddress.getByName("255.255.255.255"))
            }
            val currentSocket = socket ?: return@launch
            for (target in targets.distinct()) {
                runCatching {
                    val packet = DatagramPacket(bytes, bytes.size, target, JamReactionProtocol.MULTICAST_PORT)
                    currentSocket.send(packet)
                }
            }
        }
    }

    @Synchronized
    fun stop() {
        listenJob?.cancel()
        cleanupJob?.cancel()
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        listenJob = null
        cleanupJob = null
    }
}
