package com.music.violence.data.listentogether

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
 * mDNS / DNS-SD Engine for zero-configuration discovery of Violence Jam sessions.
 * Standard mDNS group: 224.0.0.251, Port: 5353.
 */
class MdnsEngine(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val onDiscoveredJam: (DiscoveredJam) -> Unit = {},
) {
    private val _discoveredJams = MutableStateFlow<List<DiscoveredJam>>(emptyList())
    val discoveredJams: StateFlow<List<DiscoveredJam>> = _discoveredJams.asStateFlow()

    private var activeJam: DiscoveredJam? = null
    private var socket: MulticastSocket? = null
    private var receiveJob: Job? = null
    private var queryJob: Job? = null
    private var mdnsGroup: InetAddress? = null

    @Synchronized
    fun start() {
        if (socket != null && socket?.isClosed == false) return

        try {
            val group = InetAddress.getByName(MdnsPacket.MDNS_MULTICAST_IP)
            mdnsGroup = group

            val s = MulticastSocket(null).apply {
                reuseAddress = true
                bind(InetSocketAddress(MdnsPacket.MDNS_PORT))
                timeToLive = 255 // RFC 6762 mDNS requirement
            }

            joinMdnsInterfaces(s, group)
            socket = s

            receiveJob = scope.launch(Dispatchers.IO) {
                receiveLoop(s)
            }
        } catch (_: Exception) {
            // Port 5353 may be locked exclusively on some operating systems
        }
    }

    private fun joinMdnsInterfaces(s: MulticastSocket, group: InetAddress) {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return
            val groupSocketAddr = InetSocketAddress(group, MdnsPacket.MDNS_PORT)
            for (nif in interfaces) {
                try {
                    if (nif.isUp && nif.supportsMulticast() && !nif.isLoopback) {
                        s.joinGroup(groupSocketAddr, nif)
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            try {
                @Suppress("DEPRECATION")
                s.joinGroup(group)
            } catch (_: Exception) {}
        }
    }

    private suspend fun receiveLoop(s: MulticastSocket) {
        val buffer = ByteArray(4096)
        while (scope.isActive && !s.isClosed) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                s.receive(packet)
                handlePacket(packet)
            } catch (_: Exception) {
                if (s.isClosed) break
                delay(100)
            }
        }
    }

    private fun handlePacket(packet: DatagramPacket) {
        val data = packet.data
        val len = packet.length
        if (len < 12) return

        // 1. Try to parse response
        val jam = MdnsPacket.parseResponse(data, len, packet.address?.hostAddress)
        if (jam != null) {
            recordDiscovered(jam)
            onDiscoveredJam(jam)
            return
        }

        // 2. Check if it's a PTR query for _violence-jam._tcp.local.
        val current = activeJam
        if (current != null) {
            // If QR bit is 0 (query)
            val flags = ((data[2].toInt() and 0xFF) shl 8) or (data[3].toInt() and 0xFF)
            val isQuery = (flags and 0x8000) == 0
            if (isQuery) {
                sendResponse(current)
            }
        }
    }

    private fun recordDiscovered(jam: DiscoveredJam) {
        _discoveredJams.update { list ->
            val filtered = list.filter { it.partyCode != jam.partyCode }
            filtered + jam
        }
    }

    /**
     * Start continuous discovery: sends initial query and repeats periodically.
     */
    fun startDiscovery() {
        start()
        sendQuery()
        queryJob?.cancel()
        queryJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(3_000)
                sendQuery()
            }
        }
    }

    fun stopDiscovery() {
        queryJob?.cancel()
        queryJob = null
    }

    fun sendQuery() {
        try {
            val queryBytes = MdnsPacket.buildQuery()
            val group = mdnsGroup ?: InetAddress.getByName(MdnsPacket.MDNS_MULTICAST_IP)
            val packet = DatagramPacket(queryBytes, queryBytes.size, group, MdnsPacket.MDNS_PORT)
            scope.launch(Dispatchers.IO) {
                try {
                    socket?.send(packet)
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    /**
     * Advertise the active Jam party over mDNS.
     */
    fun advertise(jam: DiscoveredJam) {
        activeJam = jam
        start()
        sendResponse(jam)
        // RFC 6762: send a couple of unsolicited responses spaced by 1s
        scope.launch(Dispatchers.IO) {
            delay(1000)
            if (activeJam?.partyCode == jam.partyCode) {
                sendResponse(jam)
            }
        }
    }

    private fun sendResponse(jam: DiscoveredJam) {
        try {
            val responseBytes = MdnsPacket.buildResponse(jam)
            val group = mdnsGroup ?: InetAddress.getByName(MdnsPacket.MDNS_MULTICAST_IP)
            val packet = DatagramPacket(responseBytes, responseBytes.size, group, MdnsPacket.MDNS_PORT)
            scope.launch(Dispatchers.IO) {
                try {
                    socket?.send(packet)
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    fun stopAdvertising() {
        activeJam = null
    }

    @Synchronized
    fun stop() {
        stopDiscovery()
        stopAdvertising()
        receiveJob?.cancel()
        receiveJob = null
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
    }
}
