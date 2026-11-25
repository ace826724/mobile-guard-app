package com.mga.mobileguard

import org.junit.Assert.*
import org.junit.Test

class Ipv4UdpPacketTest {

    private fun createUdpDnsPacket(
        destinationPort: Int = 53,
        fragmentField: Int = 0
    ): ByteArray {
        val dns = byteArrayOf(
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

        val packet = ByteArray(20 + 8 + dns.size)

        packet[0] = 0x45
        packet[1] = 0
        writeUInt16(packet, 2, packet.size)
        writeUInt16(packet, 4, 0x1234)
        writeUInt16(packet, 6, fragmentField)
        packet[8] = 64
        packet[9] = 17

        // 192.0.2.10
        packet[12] = 192.toByte()
        packet[13] = 0
        packet[14] = 2
        packet[15] = 10

        // 8.8.8.8
        packet[16] = 8
        packet[17] = 8
        packet[18] = 8
        packet[19] = 8

        val udpOffset = 20
        writeUInt16(packet, udpOffset, 40000)
        writeUInt16(packet, udpOffset + 2, destinationPort)
        writeUInt16(packet, udpOffset + 4, 8 + dns.size)
        writeUInt16(packet, udpOffset + 6, 0)

        dns.copyInto(packet, udpOffset + 8)

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

    private fun readUInt16(
        data: ByteArray,
        offset: Int
    ): Int {
        return (
            ((data[offset].toInt() and 0xFF) shl 8) or
            (data[offset + 1].toInt() and 0xFF)
        )
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

    @Test
    fun validDnsPacketIsParsed() {
        val parsed = Ipv4UdpPacket.parse(createUdpDnsPacket())

        assertNotNull(parsed)
        assertEquals(40000, parsed?.sourcePort)
        assertEquals(53, parsed?.destinationPort)
        assertEquals(0x1234, parsed?.identification)
        assertEquals(30, parsed?.dnsPayload?.size)
    }

    @Test
    fun nonDnsDestinationPortIsRejected() {
        val parsed = Ipv4UdpPacket.parse(
            createUdpDnsPacket(destinationPort = 1234)
        )

        assertNull(parsed)
    }

    @Test
    fun fragmentedPacketIsRejected() {
        val parsed = Ipv4UdpPacket.parse(
            createUdpDnsPacket(fragmentField = 0x2000)
        )

        assertNull(parsed)
    }

    @Test
    fun truncatedPacketIsRejected() {
        val packet = createUdpDnsPacket().copyOf(25)

        assertNull(Ipv4UdpPacket.parse(packet))
    }

    @Test
    fun responseReversesAddressesAndPorts() {
        val request = Ipv4UdpPacket.parse(createUdpDnsPacket())
        assertNotNull(request)

        val dnsResponse = byteArrayOf(
            0x12, 0x34,
            0x81.toByte(), 0x83.toByte(),
            0x00, 0x01,
            0x00, 0x00,
            0x00, 0x00,
            0x00, 0x00
        )

        val response = Ipv4UdpPacket.buildResponse(
            request!!,
            dnsResponse
        )

        assertNotNull(response)

        val packet = response!!

        // Source address becomes original destination.
        assertArrayEquals(
            byteArrayOf(8, 8, 8, 8),
            packet.copyOfRange(12, 16)
        )

        // Destination address becomes original source.
        assertArrayEquals(
            byteArrayOf(192.toByte(), 0, 2, 10),
            packet.copyOfRange(16, 20)
        )

        assertEquals(53, readUInt16(packet, 20))
        assertEquals(40000, readUInt16(packet, 22))
    }

    @Test
    fun responseHasValidIpv4Checksum() {
        val request = Ipv4UdpPacket.parse(createUdpDnsPacket())!!

        val response = Ipv4UdpPacket.buildResponse(
            request,
            byteArrayOf(0x12, 0x34, 0x81.toByte(), 0x83.toByte())
        )!!

        assertEquals(
            0,
            checksum(response, 0, 20)
        )
    }

    @Test
    fun responseHasValidUdpChecksum() {
        val request = Ipv4UdpPacket.parse(createUdpDnsPacket())!!

        val response = Ipv4UdpPacket.buildResponse(
            request,
            byteArrayOf(0x12, 0x34, 0x81.toByte(), 0x83.toByte())
        )!!

        val udpLength = readUInt16(response, 24)
        val pseudoHeader = ByteArray(12 + udpLength)

        response.copyInto(pseudoHeader, 0, 12, 20)
        pseudoHeader[8] = 0
        pseudoHeader[9] = 17
        writeUInt16(pseudoHeader, 10, udpLength)

        response.copyInto(
            pseudoHeader,
            12,
            20,
            20 + udpLength
        )

        assertEquals(
            0,
            checksum(pseudoHeader, 0, pseudoHeader.size)
        )
    }
}
