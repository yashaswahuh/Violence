package com.music.violence.data.listentogether

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.music.violence.data.DebugLog as Log

/**
 * Android framework NsdManager implementation for mDNS / DNS-SD Jam discovery & advertisement.
 */
object AndroidNsdDiscovery {
    private const val TAG = "AndroidNsdDiscovery"

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var isDiscovering = false

    @Synchronized
    fun acquireMulticastLock(context: Context) {
        if (multicastLock == null) {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("violence_jam_multicast")?.apply {
                setReferenceCounted(false)
            }
        }
        try {
            if (multicastLock?.isHeld == false) {
                multicastLock?.acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire multicast lock: ${e.message}")
        }
    }

    @Synchronized
    fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {}
    }

    @Synchronized
    fun registerService(context: Context, jam: DiscoveredJam) {
        val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return
        unregisterService(context)

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "Violence Jam ${jam.partyCode}"
            serviceType = JamDiscoveryProtocol.SERVICE_TYPE
            port = jam.port

            setAttribute(JamDiscoveryProtocol.KEY_PARTY_CODE, jam.partyCode)
            setAttribute(JamDiscoveryProtocol.KEY_HOST_NAME, jam.hostName)
            setAttribute(JamDiscoveryProtocol.KEY_CURRENT_TRACK, jam.currentTrack)
        }

        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Log.d(TAG, "mDNS Service registered: ${info.serviceName}")
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "mDNS Registration failed: code=$errorCode")
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) {
                Log.d(TAG, "mDNS Service unregistered: ${info.serviceName}")
            }

            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "mDNS Unregistration failed: code=$errorCode")
            }
        }

        registrationListener = listener
        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.w(TAG, "Exception during registerService: ${e.message}")
        }
    }

    @Synchronized
    fun unregisterService(context: Context) {
        val listener = registrationListener ?: return
        val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return
        try {
            nsdManager.unregisterService(listener)
        } catch (_: Exception) {}
        registrationListener = null
    }

    @Synchronized
    fun startDiscovery(context: Context, onDiscovered: (DiscoveredJam) -> Unit) {
        if (isDiscovering) return
        val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return

        acquireMulticastLock(context)

        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "Discovery start failed: $errorCode")
                isDiscovering = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "Discovery stop failed: $errorCode")
                isDiscovering = false
            }

            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Discovery started for $serviceType")
                isDiscovering = true
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped for $serviceType")
                isDiscovering = false
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType.contains("violence-jam")) {
                    resolveService(nsdManager, serviceInfo, onDiscovered)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
        }

        discoveryListener = listener
        try {
            nsdManager.discoverServices(JamDiscoveryProtocol.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.w(TAG, "Exception during discoverServices: ${e.message}")
        }
    }

    private fun resolveService(
        nsdManager: NsdManager,
        serviceInfo: NsdServiceInfo,
        onDiscovered: (DiscoveredJam) -> Unit,
    ) {
        try {
            nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                    Log.w(TAG, "Resolve failed: $errorCode")
                }

                override fun onServiceResolved(info: NsdServiceInfo) {
                    val attrs = info.attributes
                    val partyCode = attrs[JamDiscoveryProtocol.KEY_PARTY_CODE]?.let { String(it, Charsets.UTF_8) }
                        ?: return
                    val hostName = attrs[JamDiscoveryProtocol.KEY_HOST_NAME]?.let { String(it, Charsets.UTF_8) }
                        ?: ""
                    val currentTrack = attrs[JamDiscoveryProtocol.KEY_CURRENT_TRACK]?.let { String(it, Charsets.UTF_8) }
                        ?: ""

                    val jam = DiscoveredJam(
                        partyCode = partyCode,
                        hostName = hostName,
                        currentTrack = currentTrack,
                        address = info.host?.hostAddress,
                        port = info.port,
                        lastSeenMs = System.currentTimeMillis(),
                    )
                    onDiscovered(jam)
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Exception during resolveService: ${e.message}")
        }
    }

    @Synchronized
    fun stopDiscovery(context: Context) {
        val listener = discoveryListener ?: return
        val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return
        try {
            nsdManager.stopServiceDiscovery(listener)
        } catch (_: Exception) {}
        discoveryListener = null
        isDiscovering = false
    }

    @Synchronized
    fun shutdown(context: Context) {
        unregisterService(context)
        stopDiscovery(context)
        releaseMulticastLock()
    }
}
