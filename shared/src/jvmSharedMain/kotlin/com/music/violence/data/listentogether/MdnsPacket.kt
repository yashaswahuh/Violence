package com.music.violence.data.listentogether

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress

/**
 * Lightweight, zero-dependency DNS-SD / mDNS packet serializer and parser
 * for `_violence-jam._tcp.local.` services.
 */
object MdnsPacket {
    const val MDNS_MULTICAST_IP: String = "224.0.0.251"
    const val MDNS_PORT: Int = 5353

    const val TYPE_A = 1
    const val TYPE_PTR = 12
    const val TYPE_TXT = 16
    const val TYPE_SRV = 33
    const val CLASS_IN = 1

    /**
     * Builds a standard DNS PTR query packet for `_violence-jam._tcp.local.`.
     */
    fun buildQuery(serviceType: String = JamDiscoveryProtocol.FULL_SERVICE_TYPE): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        // Transaction ID: 0 for mDNS
        dos.writeShort(0)
        // Flags: Standard query
        dos.writeShort(0x0000)
        // QDCOUNT = 1
        dos.writeShort(1)
        // ANCOUNT = 0
        dos.writeShort(0)
        // NSCOUNT = 0
        dos.writeShort(0)
        // ARCOUNT = 0
        dos.writeShort(0)

        // Question: Name, Type (PTR=12), Class (IN=1, with unicast-response QU bit or multicast)
        writeDnsName(dos, serviceType)
        dos.writeShort(TYPE_PTR)
        dos.writeShort(CLASS_IN)

        dos.flush()
        return baos.toByteArray()
    }

    /**
     * Builds an authoritative mDNS response packet announcing or answering for a Violence Jam session.
     */
    fun buildResponse(
        jam: DiscoveredJam,
        instanceName: String = "Violence Jam ${jam.partyCode}",
        serviceType: String = JamDiscoveryProtocol.FULL_SERVICE_TYPE,
        hostTarget: String = "violence-host.local.",
        ipAddress: InetAddress? = null,
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        // Transaction ID = 0
        dos.writeShort(0)
        // Flags = 0x8400 (Response, Authoritative Answer)
        dos.writeShort(0x8400)
        // QDCOUNT = 0
        dos.writeShort(0)
        // ANCOUNT = 2 (PTR, SRV)
        val hasIp = (ipAddress != null && ipAddress.address.size == 4)
        dos.writeShort(if (hasIp) 4 else 3) // PTR, SRV, TXT, (optional A)
        // NSCOUNT = 0
        dos.writeShort(0)
        // ARCOUNT = 0
        dos.writeShort(0)

        val fullInstanceName = "$instanceName.$serviceType"
        val ttl = 120 // 120 seconds TTL

        // 1. PTR record: serviceType -> fullInstanceName
        writeDnsName(dos, serviceType)
        dos.writeShort(TYPE_PTR)
        dos.writeShort(CLASS_IN or 0x8000) // cache-flush bit
        dos.writeInt(ttl)
        val ptrRdata = ByteArrayOutputStream()
        writeDnsName(DataOutputStream(ptrRdata), fullInstanceName)
        val ptrBytes = ptrRdata.toByteArray()
        dos.writeShort(ptrBytes.size)
        dos.write(ptrBytes)

        // 2. SRV record: fullInstanceName -> hostTarget, port
        writeDnsName(dos, fullInstanceName)
        dos.writeShort(TYPE_SRV)
        dos.writeShort(CLASS_IN or 0x8000)
        dos.writeInt(ttl)
        val srvRdata = ByteArrayOutputStream()
        val srvDos = DataOutputStream(srvRdata)
        srvDos.writeShort(0) // priority
        srvDos.writeShort(0) // weight
        srvDos.writeShort(jam.port)
        writeDnsName(srvDos, hostTarget)
        val srvBytes = srvRdata.toByteArray()
        dos.writeShort(srvBytes.size)
        dos.write(srvBytes)

        // 3. TXT record: fullInstanceName -> attributes
        writeDnsName(dos, fullInstanceName)
        dos.writeShort(TYPE_TXT)
        dos.writeShort(CLASS_IN or 0x8000)
        dos.writeInt(ttl)
        val txtRdata = ByteArrayOutputStream()
        writeTxtAttribute(txtRdata, JamDiscoveryProtocol.KEY_PARTY_CODE, jam.partyCode)
        writeTxtAttribute(txtRdata, JamDiscoveryProtocol.KEY_HOST_NAME, jam.hostName)
        writeTxtAttribute(txtRdata, JamDiscoveryProtocol.KEY_CURRENT_TRACK, jam.currentTrack)
        val txtBytes = txtRdata.toByteArray()
        dos.writeShort(txtBytes.size)
        dos.write(txtBytes)

        // 4. Optional A record for hostTarget -> IPv4
        if (hasIp) {
            writeDnsName(dos, hostTarget)
            dos.writeShort(TYPE_A)
            dos.writeShort(CLASS_IN or 0x8000)
            dos.writeInt(ttl)
            dos.writeShort(4)
            dos.write(ipAddress!!.address)
        }

        dos.flush()
        return baos.toByteArray()
    }

    /**
     * Parses an incoming DNS packet and extracts any DiscoveredJam from TXT / SRV / PTR records.
     */
    fun parseResponse(bytes: ByteArray, length: Int, sourceAddress: String? = null): DiscoveredJam? {
        if (length < 12) return null
        return try {
            val dis = DataInputStream(ByteArrayInputStream(bytes, 0, length))
            val id = dis.readUnsignedShort()
            val flags = dis.readUnsignedShort()
            val qdCount = dis.readUnsignedShort()
            val anCount = dis.readUnsignedShort()
            val nsCount = dis.readUnsignedShort()
            val arCount = dis.readUnsignedShort()

            val totalRecords = anCount + nsCount + arCount

            // Skip questions
            for (i in 0 until qdCount) {
                readDnsName(bytes, dis)
                dis.readUnsignedShort() // type
                dis.readUnsignedShort() // class
            }

            var partyCode: String? = null
            var hostName: String? = null
            var currentTrack: String? = null
            var port = JamDiscoveryProtocol.DEFAULT_PORT

            // Read answers and additional records
            for (i in 0 until totalRecords) {
                val recordName = readDnsName(bytes, dis)
                val type = dis.readUnsignedShort()
                val rClass = dis.readUnsignedShort()
                val ttl = dis.readInt()
                val rdLength = dis.readUnsignedShort()

                val rdata = ByteArray(rdLength)
                dis.readFully(rdata)

                when (type) {
                    TYPE_TXT -> {
                        val txtMap = parseTxtRdata(rdata)
                        txtMap[JamDiscoveryProtocol.KEY_PARTY_CODE]?.let { partyCode = it }
                        txtMap[JamDiscoveryProtocol.KEY_HOST_NAME]?.let { hostName = it }
                        txtMap[JamDiscoveryProtocol.KEY_CURRENT_TRACK]?.let { currentTrack = it }
                    }
                    TYPE_SRV -> {
                        if (rdata.size >= 6) {
                            port = ((rdata[4].toInt() and 0xFF) shl 8) or (rdata[5].toInt() and 0xFF)
                        }
                    }
                }
            }

            val code = partyCode
            if (!code.isNullOrBlank()) {
                DiscoveredJam(
                    partyCode = code,
                    hostName = hostName.orEmpty(),
                    currentTrack = currentTrack.orEmpty(),
                    address = sourceAddress,
                    port = port,
                    lastSeenMs = System.currentTimeMillis(),
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun writeDnsName(dos: DataOutputStream, name: String) {
        val parts = name.trim('.').split('.')
        for (part in parts) {
            if (part.isEmpty()) continue
            val partBytes = part.toByteArray(Charsets.UTF_8)
            dos.writeByte(partBytes.size)
            dos.write(partBytes)
        }
        dos.writeByte(0)
    }

    private fun readDnsName(packet: ByteArray, dis: DataInputStream): String {
        val sb = StringBuilder()
        while (true) {
            val len = dis.readUnsignedByte()
            if (len == 0) break
            if ((len and 0xC0) == 0xC0) {
                // Compression pointer
                val low = dis.readUnsignedByte()
                val pointerOffset = ((len and 0x3F) shl 8) or low
                val pointedName = readDnsNameAt(packet, pointerOffset)
                if (sb.isNotEmpty()) sb.append('.')
                sb.append(pointedName)
                return sb.toString()
            } else {
                val label = ByteArray(len)
                dis.readFully(label)
                if (sb.isNotEmpty()) sb.append('.')
                sb.append(String(label, Charsets.UTF_8))
            }
        }
        return sb.toString()
    }

    private fun readDnsNameAt(packet: ByteArray, initialOffset: Int): String {
        var offset = initialOffset
        val sb = StringBuilder()
        while (offset < packet.size) {
            val len = packet[offset].toInt() and 0xFF
            offset++
            if (len == 0) break
            if ((len and 0xC0) == 0xC0) {
                if (offset >= packet.size) break
                val low = packet[offset].toInt() and 0xFF
                val pointerOffset = ((len and 0x3F) shl 8) or low
                val pointed = readDnsNameAt(packet, pointerOffset)
                if (sb.isNotEmpty()) sb.append('.')
                sb.append(pointed)
                return sb.toString()
            }
            if (offset + len > packet.size) break
            val label = String(packet, offset, len, Charsets.UTF_8)
            offset += len
            if (sb.isNotEmpty()) sb.append('.')
            sb.append(label)
        }
        return sb.toString()
    }

    private fun writeTxtAttribute(baos: ByteArrayOutputStream, key: String, value: String) {
        val str = "$key=$value"
        val bytes = str.toByteArray(Charsets.UTF_8)
        val len = minOf(bytes.size, 255)
        baos.write(len)
        baos.write(bytes, 0, len)
    }

    fun parseTxtRdata(rdata: ByteArray): Map<String, String> {
        val result = mutableMapOf<String, String>()
        var offset = 0
        while (offset < rdata.size) {
            val len = rdata[offset].toInt() and 0xFF
            offset++
            if (len == 0 || offset + len > rdata.size) break
            val str = String(rdata, offset, len, Charsets.UTF_8)
            offset += len
            val eqIdx = str.indexOf('=')
            if (eqIdx > 0) {
                val k = str.substring(0, eqIdx)
                val v = str.substring(eqIdx + 1)
                result[k] = v
            }
        }
        return result
    }
}
