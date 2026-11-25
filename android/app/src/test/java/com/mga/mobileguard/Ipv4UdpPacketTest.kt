package com.mga.mobileguard

import org.junit.Assert.*
import org.junit.Test

class DnsPacketProcessorTest {

    private class FakeDnsForwarder(
        private val response: ByteArray?
    ) : DnsQueryForwarder {

        var callCount = 0
            private set

        var lastQuery: ByteArray? = null
            private set

        override fun forward(query: ByteArray): ByteArray? {
            callCount++
            lastQuery = query.copyOf()
            return response?.copyOf()
        }
    }

    private fun createDnsQuery(): ByteArray {
        return byteArrayOf(
            0x12, 0x34,
            0x01, 0x00,
            0x00, 0x01,
            0x00, 0x00,
            0x00, 0x00,
            0x00, 0x00,
            0x07,
            0x65, 0x78, 0x61, 0x6d, 0x70, 0x6c, 0x65,
            0x03,
            0x63, 0x6f, 0x6d,
            0x00,
            0x00, 0x01,
            0x00, 0x01
        )
    }

    private fun createIpv4UdpPacket(dns: ByteArray): ByteArray {
        val packet = ByteArray(20 + 8 + dns.size)

        packet[0] = 0x45
        packet[1] = 0
        writeUInt16(packet, 2, packet.size)
        writeUInt16(packet, 4, 0x1234)
        writeUInt16(packet, 6, 0)
        packet[8] = 64
        packet[9] = 17

        packet[12] = 192.toByte()
        packet[13] = 0
        packet[14] = 2
        packet[15] = 10

        packet[16] = 8
        packet[17] = 8
        packet[18] = 8
        packet[19] = 8

        writeUInt16(packet, 20, 40000)
        writeUInt16(packet, 22, 53)
        writeUInt16(packet, 24, 8 + dns.size)
        writeUInt16(packet, 26, 0)

        dns.copyInto(packet, 28)
        writeUInt16(packet, 10, checksum(packet, 0, 20))

        return packet
    }

    private fun writeUInt16(
        data: ByteArray,
        offset: Int,
        value: Int
    ) {
        data[offset] = ((value shr 8) and 0xFF).toByte()
        data[offset + 1] = (value and 0xFF).toByte()
    }

    private fun checksum(
        data: ByteArray,
        offset: Int,
        length: Int
    ): Int {
        var sum = 0L
        var index = offset
        val end = offset + length

        while (index + 1 < end) {
            sum += (
                ((data[index].toInt() and 0xFF) shl 8) or
                    (data[index + 1].toInt() and 0xFF)
                ).toLong()
            index += 2
        }

        if (index < end) {
            sum += (
                (data[index].toInt() and 0xFF) shl 8
            ).toLong()
        }

        while ((sum shr 16) != 0L) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        return sum.inv().toInt() and 0xFFFF
    }

    private fun createProcessor(
        blockedDomains: Set<String> = emptySet(),
        upstreamResponse: ByteArray? = null
    ): Pair<DnsPacketProcessor, FakeDnsForwarder> {

        val blocklist = BlocklistManager()
        blockedDomains.forEach(blocklist::addDomain)

        val engine = DnsFilteringEngine(blocklist)
        val fakeForwarder = FakeDnsForwarder(upstreamResponse)

        return DnsPacketProcessor(
            engine,
            fakeForwarder
        ) to fakeForwarder
    }

    @Test
    fun allowedDomainIsForwardedToUpstream() {
        val query = createDnsQuery()
        val response = query.copyOf().also {
            it[2] = 0x81.toByte()
            it[3] = 0x80.toByte()
        }

        val (processor, fake) = createProcessor(
            upstreamResponse = response
        )

        val result = processor.process(
            createIpv4UdpPacket(query)
        )

        assertNotNull(result)
        assertEquals(1, fake.callCount)
        assertArrayEquals(query, fake.lastQuery)
    }

    @Test
    fun blockedDomainDoesNotReachUpstream() {
        val (processor, fake) = createProcessor(
            blockedDomains = setOf("example.com")
        )

        val result = processor.process(
            createIpv4UdpPacket(createDnsQuery())
        )

        assertNotNull(result)
        assertEquals(0, fake.callCount)
    }

    @Test
    fun upstreamFailureReturnsNull() {
        val (processor, fake) = createProcessor(
            upstreamResponse = null
        )

        val result = processor.process(
            createIpv4UdpPacket(createDnsQuery())
        )

        assertNull(result)
        assertEquals(1, fake.callCount)
    }

    @Test
    fun invalidIpPacketReturnsNull() {
        val (processor, fake) = createProcessor()

        val result = processor.process(
            byteArrayOf(1, 2, 3, 4)
        )

        assertNull(result)
        assertEquals(0, fake.callCount)
    }
}
