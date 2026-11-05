package com.music.violence.data.listentogether

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * High-level coordinator managing Serverless LAN Jam Discovery (mDNS + UDP Multicast)
 * and Real-Time UDP Multicast Reactions across peers on the local Wi-Fi.
 */
class JamLanManager(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    val multicastChannel = JamMulticastChannel(scope)
    val mdnsEngine = MdnsEngine(scope) { jam ->
        recordDiscoveredJam(jam)
    }

    val reactions: SharedFlow<JamReaction> = multicastChannel.reactions

    private val _discoveredJams = MutableStateFlow<List<DiscoveredJam>>(emptyList())
    val discoveredJams: StateFlow<List<DiscoveredJam>> = _discoveredJams.asStateFlow()

    private var activePartyCode: String? = null
    private var activeHostName: String? = null
    private var activeTrack: String? = null
    private var announceJob: Job? = null

    init {
        multicastChannel.start()

        // Forward jams discovered via UDP multicast into unified discoveredJams
        scope.launch {
            multicastChannel.discoveredJams.collect { list ->
                for (jam in list) {
                    recordDiscoveredJam(jam)
                }
            }
        }

        // Cleanup stale jams (not heard from in > 15s)
        scope.launch {
            while (isActive) {
                delay(4000)
                val cutoff = System.currentTimeMillis() - 15_000
                _discoveredJams.update { list ->
                    list.filter { it.lastSeenMs > cutoff }
                }
            }
        }
    }

    fun recordDiscoveredJam(jam: DiscoveredJam) {
        // Don't show our own active room as a discovered nearby jam
        val currentCode = activePartyCode
        if (currentCode != null && jam.partyCode.equals(currentCode, ignoreCase = true)) {
            return
        }

        _discoveredJams.update { list ->
            val updated = list.filterNot { it.partyCode.equals(jam.partyCode, ignoreCase = true) }
            updated + jam
        }
    }

    fun startDiscovery() {
        multicastChannel.start()
        mdnsEngine.startDiscovery()
    }

    fun stopDiscovery() {
        mdnsEngine.stopDiscovery()
    }

    /**
     * Start hosting or participating in a party: sets filter hash and advertises on LAN.
     */
    fun startHosting(partyCode: String, hostName: String, currentTrack: String = "") {
        activePartyCode = partyCode
        activeHostName = hostName
        activeTrack = currentTrack

        multicastChannel.setActiveParty(partyCode)

        val jam = DiscoveredJam(
            partyCode = partyCode,
            hostName = hostName,
            currentTrack = currentTrack,
            lastSeenMs = System.currentTimeMillis(),
        )

        mdnsEngine.advertise(jam)
        multicastChannel.broadcastAnnouncement(jam)

        // Periodic announcement broadcast every 5s while in room
        announceJob?.cancel()
        announceJob = scope.launch {
            while (isActive) {
                delay(5_000)
                val code = activePartyCode ?: break
                val host = activeHostName.orEmpty()
                val track = activeTrack.orEmpty()
                val j = DiscoveredJam(code, host, track, lastSeenMs = System.currentTimeMillis())
                multicastChannel.broadcastAnnouncement(j)
            }
        }
    }

    fun updateCurrentTrack(currentTrack: String) {
        val code = activePartyCode ?: return
        val host = activeHostName.orEmpty()
        activeTrack = currentTrack
        val jam = DiscoveredJam(code, host, currentTrack, lastSeenMs = System.currentTimeMillis())
        mdnsEngine.advertise(jam)
        multicastChannel.broadcastAnnouncement(jam)
    }

    fun stopHosting() {
        announceJob?.cancel()
        announceJob = null
        activePartyCode = null
        activeHostName = null
        activeTrack = null
        multicastChannel.setActiveParty(null)
        mdnsEngine.stopAdvertising()
    }

    fun sendReaction(emoji: JamEmoji): Boolean {
        val code = activePartyCode ?: return false
        return multicastChannel.sendReaction(code, emoji)
    }

    fun shutdown() {
        stopHosting()
        stopDiscovery()
        multicastChannel.stop()
        mdnsEngine.stop()
    }
}
