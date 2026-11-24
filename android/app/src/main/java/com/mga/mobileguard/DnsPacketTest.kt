package com.mga.mobileguard

import org.junit.Assert.*
import org.junit.Test

class DnsPacketTest {

    private fun createDnsQuery(
        domain: String,
        queryType: Int = 1
    ): ByteArray {
        val packet = mutableListOf<Byte>()

        // Transaction ID
        packet.add(0x12)
        packet.add(0x34)

        // Flags: Standard query, recursion desired
        packet.add(0x01)
        packet.add(0x00)

        // Question count: 1
        packet.add(0x00)
        packet.add(0x01)

        // Answer, authority and additional counts
        repeat(6) {
            packet.add(0x00)
        }

        // Domain labels
        domain.split(".").forEach { label ->
            val bytes = label.toByteArray(Charsets.US_ASCII)
            packet.add(bytes.size.toByte())
            bytes.forEach { packet.add(it) }
        }

        // End of domain
        packet.add(0x00)

        // Query type
        packet.add((queryType shr 8).toByte())
        packet.add(queryType.toByte())

        // Query class: IN
        packet.add(0x00)
        packet.add(0x01)

        return packet.toByteArray()
    }

    @Test
    fun validDomainQueryIsParsed() {
        val packet = createDnsQuery("example.com")

        val result = DnsPacket.parseQuery(packet)

        assertNotNull(result)
        assertEquals("example.com", result?.domain)
        assertEquals(0x1234, result?.transactionId)
        assertEquals(1, result?.queryType)
        assertEquals(1, result?.queryClass)
        assertTrue(result?.recursionDesired == true)
    }

    @Test
    fun uppercaseDomainIsNormalized() {
        val packet = createDnsQuery("Example.COM")

        val result = DnsPacket.parseQuery(packet)

        assertNotNull(result)
        assertEquals("example.com", result?.domain)
    }

    @Test
    fun aaaaQueryIsParsed() {
        val packet = createDnsQuery("example.com", 28)

        val result = DnsPacket.parseQuery(packet)

        assertNotNull(result)
        assertEquals(28, result?.queryType)
    }

    @Test
    fun responsePacketIsRejected() {
        val packet = createDnsQuery("example.com")
        packet[2] = 0x81.toByte()

        val result = DnsPacket.parseQuery(packet)

        assertNull(result)
    }

    @Test
    fun truncatedPacketIsRejected() {
        val packet = byteArrayOf(
            0x12,
            0x34,
            0x01,
            0x00
        )

        val result = DnsPacket.parseQuery(packet)

        assertNull(result)
    }

    @Test
    fun blockedResponsePreservesTransactionId() {
        val query = DnsPacket.parseQuery(
            createDnsQuery("blocked.example")
        )

        assertNotNull(query)

        val response = DnsPacket.buildBlockedResponse(query!!)

        assertEquals(0x12, response[0].toInt() and 0xFF)
        assertEquals(0x34, response[1].toInt() and 0xFF)

        // QR bit: response
        assertTrue((response[2].toInt() and 0x80) != 0)

        // RCODE: NXDOMAIN
        assertEquals(3, response[3].toInt() and 0x0F)
    }
}
