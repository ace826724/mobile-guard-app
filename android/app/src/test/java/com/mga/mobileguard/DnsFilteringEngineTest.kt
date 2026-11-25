package com.mga.mobileguard

import org.junit.Assert.*
import org.junit.Test

class DnsFilteringEngineTest {

    private fun createDnsQuery(domain: String): ByteArray {
        val packet = mutableListOf<Byte>()

        // Transaction ID
        packet.add(0x12.toByte())
        packet.add(0x34.toByte())

        // Standard query, recursion desired
        packet.add(0x01.toByte())
        packet.add(0x00.toByte())

        // One question
        packet.add(0x00.toByte())
        packet.add(0x01.toByte())

        // Answer, authority and additional counts
        repeat(6) {
            packet.add(0x00.toByte())
        }

        // Domain labels
        domain.split(".").forEach { label ->
            val bytes = label.toByteArray(Charsets.US_ASCII)
            packet.add(bytes.size.toByte())
            bytes.forEach { packet.add(it) }
        }

        // End of domain
        packet.add(0x00.toByte())

        // Query type A
        packet.add(0x00.toByte())
        packet.add(0x01.toByte())

        // Query class IN
        packet.add(0x00.toByte())
        packet.add(0x01.toByte())

        return packet.toByteArray()
    }

    @Test
    fun blockedDomainProducesBlockedResponse() {
        val manager = BlocklistManager(
            setOf("ads.example.com")
        )

        val engine = DnsFilteringEngine(manager)

        val result = engine.evaluate(
            createDnsQuery("ads.example.com")
        )

        assertTrue(result is DnsFilteringEngine.Evaluation.Blocked)

        val blocked = result as DnsFilteringEngine.Evaluation.Blocked

        assertEquals("ads.example.com", blocked.domain)
        assertTrue(blocked.response.isNotEmpty())

        // DNS response flag
        assertTrue((blocked.response[2].toInt() and 0x80) != 0)

        // NXDOMAIN response code
        assertEquals(3, blocked.response[3].toInt() and 0x0F)
    }

    @Test
    fun allowedDomainIsMarkedForForwarding() {
        val manager = BlocklistManager(
            setOf("ads.example.com")
        )

        val engine = DnsFilteringEngine(manager)

        val result = engine.evaluate(
            createDnsQuery("example.org")
        )

        assertTrue(result is DnsFilteringEngine.Evaluation.Forward)

        val forward = result as DnsFilteringEngine.Evaluation.Forward

        assertEquals("example.org", forward.domain)
    }

    @Test
    fun subdomainOfBlockedDomainIsBlocked() {
        val manager = BlocklistManager(
            setOf("example.com")
        )

        val engine = DnsFilteringEngine(manager)

        val result = engine.evaluate(
            createDnsQuery("ads.example.com")
        )

        assertTrue(result is DnsFilteringEngine.Evaluation.Blocked)
    }

    @Test
    fun invalidPacketIsRejected() {
        val manager = BlocklistManager(
            setOf("example.com")
        )

        val engine = DnsFilteringEngine(manager)

        val result = engine.evaluate(
            byteArrayOf(0x12, 0x34, 0x01, 0x00)
        )

        assertTrue(result is DnsFilteringEngine.Evaluation.Invalid)
    }
}
