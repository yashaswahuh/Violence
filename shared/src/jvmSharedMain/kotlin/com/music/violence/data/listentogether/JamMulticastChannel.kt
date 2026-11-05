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
            val currentHash = activePartyHash
            // Accept if in the same party
            if (currentHash != null && reaction.partyHash == currentHash) {
                _reactions.tryEmit(reaction)
            }
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

    /**
     * Broadcasts a reaction to all peers in the room on local Wi-Fi.
     */
    fun sendReaction(partyCode: String, emoji: JamEmoji): Boolean {
        return try {
            val hash = JamReactionProtocol.computePartyHash(partyCode)
            val reaction = JamReaction(hash, emoji, System.currentTimeMillis())
            val bytes = JamReactionProtocol.encode(reaction)

            val group = groupAddress ?: InetAddress.getByName(JamReactionProtocol.MULTICAST_GROUP)
            val packet = DatagramPacket(bytes, bytes.size, group, JamReactionProtocol.MULTICAST_PORT)

            scope.launch(Dispatchers.IO) {
                try {
                    socket?.send(packet)
                } catch (_: Exception) {}
            }
            // Emit locally as well so sender immediately sees particles
            _reactions.tryEmit(reaction)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Broadcasts a discovery announcement to peers on local Wi-Fi.
     */
    fun broadcastAnnouncement(jam: DiscoveredJam) {
        val bytes = JamDiscoveryProtocol.encodeAnnouncement(jam)
        val group = groupAddress ?: InetAddress.getByName(JamReactionProtocol.MULTICAST_GROUP)
        val packet = DatagramPacket(bytes, bytes.size, group, JamReactionProtocol.MULTICAST_PORT)

        scope.launch(Dispatchers.IO) {
            try {
                socket?.send(packet)
            } catch (_: Exception) {}
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
