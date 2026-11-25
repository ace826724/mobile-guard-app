package com.mga.mobileguard

/**
 * MGA - IPv4 UDP Packet Handler
 *
 * IPv4 üzerinden taşınan UDP DNS paketlerini ayrıştırır
 * ve DNS yanıtı için yeni IPv4/UDP paketi oluşturur.
 *
 * Bu sınıf henüz VpnService'e bağlı değildir.
 */
object Ipv4UdpPacket {

    private const val IPV4_VERSION = 4
    private const val UDP_PROTOCOL = 17
    private const val DNS_PORT = 53
    private const val UDP_HEADER_SIZE = 8
    private const val IPV4_MIN_HEADER_SIZE = 20

    data class ParsedPacket(
        val sourceAddress: ByteArray,
        val destinationAddress: ByteArray,
        val sourcePort: Int,
        val destinationPort: Int,
        val identification: Int,
        val dnsPayload: ByteArray
    )

    fun parse(packet: ByteArray): ParsedPacket? {
        if (packet.size < IPV4_MIN_HEADER_SIZE) return null

        val version = (packet[0].toInt() ushr 4) and 0x0F
        if (version != IPV4_VERSION) return null

        val headerLength =
            (packet[0].toInt() and 0x0F) * 4

        if (headerLength < IPV4_MIN_HEADER_SIZE) return null
        if (headerLength > packet.size) return null

        val totalLength = readUInt16(packet, 2)

        if (totalLength < headerLength + UDP_HEADER_SIZE) return null
        if (totalLength > packet.size) return null

        // IPv4 header checksum doğrulaması.
        if (calculateChecksum(packet, 0, headerLength) != 0) {
            return null
        }

        val protocol = packet[9].toInt() and 0xFF
        if (protocol != UDP_PROTOCOL) return null

        // MF veya fragment offset varsa paketi reddet.
        val fragmentField = readUInt16(packet, 6)
        if ((fragmentField and 0x3FFF) != 0) return null

        val udpOffset = headerLength
        val udpLength = readUInt16(packet, udpOffset + 4)

        if (udpLength < UDP_HEADER_SIZE) return null

        // UDP datagramı IPv4 toplam uzunluğuyla tam örtüşmelidir.
        if (udpOffset + udpLength != totalLength) return null

        val sourcePort = readUInt16(packet, udpOffset)
        val destinationPort = readUInt16(packet, udpOffset + 2)

        if (destinationPort != DNS_PORT) return null

        // IPv4 UDP checksum sıfır olabilir.
        val receivedUdpChecksum = readUInt16(packet, udpOffset + 6)

        if (receivedUdpChecksum != 0) {
            val checksumData = ByteArray(12 + udpLength)

            packet.copyInto(checksumData, 0, 12, 20)

            checksumData[8] = 0
            checksumData[9] = UDP_PROTOCOL.toByte()

            writeUInt16(checksumData, 10, udpLength)

            packet.copyInto(
                checksumData,
                12,
                udpOffset,
                udpOffset + udpLength
            )

            if (calculateChecksum(
                    checksumData,
                    0,
                    checksumData.size
                ) != 0
            ) {
                return null
            }
        }

        val dnsStart = udpOffset + UDP_HEADER_SIZE
        val dnsEnd = udpOffset + udpLength

        if (dnsEnd <= dnsStart) return null

        return ParsedPacket(
            sourceAddress = packet.copyOfRange(12, 16),
            destinationAddress = packet.copyOfRange(16, 20),
            sourcePort = sourcePort,
            destinationPort = destinationPort,
            identification = readUInt16(packet, 4),
            dnsPayload = packet.copyOfRange(dnsStart, dnsEnd)
        )
    }

    fun buildResponse(
        request: ParsedPacket,
        dnsResponse: ByteArray
    ): ByteArray? {

        if (dnsResponse.isEmpty()) return null

        if (request.sourceAddress.size != 4) return null
        if (request.destinationAddress.size != 4) return null
        if (request.sourcePort !in 1..65535) return null

        val udpLength = UDP_HEADER_SIZE.toLong() + dnsResponse.size
        val totalLength = IPV4_MIN_HEADER_SIZE.toLong() + udpLength

        if (totalLength > 65535) return null

        val response = ByteArray(totalLength.toInt())

        // IPv4 header
        response[0] = 0x45.toByte()
        response[1] = 0
        writeUInt16(response, 2, totalLength.toInt())
        writeUInt16(response, 4, request.identification)
        writeUInt16(response, 6, 0)

        response[8] = 64
        response[9] = UDP_PROTOCOL.toByte()

        request.destinationAddress.copyInto(response, 12)
        request.sourceAddress.copyInto(response, 16)

        // UDP header
        val udpOffset = IPV4_MIN_HEADER_SIZE

        writeUInt16(response, udpOffset, DNS_PORT)
        writeUInt16(response, udpOffset + 2, request.sourcePort)
        writeUInt16(response, udpOffset + 4, udpLength.toInt())
        writeUInt16(response, udpOffset + 6, 0)

        dnsResponse.copyInto(
            response,
            destinationOffset = udpOffset + UDP_HEADER_SIZE
        )

        val ipChecksum = calculateChecksum(
            response,
            0,
            IPV4_MIN_HEADER_SIZE
        )

        writeUInt16(response, 10, ipChecksum)

        val udpChecksum = calculateUdpChecksum(
            response,
            udpOffset,
            udpLength.toInt()
        )

        writeUInt16(
            response,
            udpOffset + 6,
            if (udpChecksum == 0) 0xFFFF else udpChecksum
        )

        return response
    }

    private fun calculateUdpChecksum(
        packet: ByteArray,
        udpOffset: Int,
        udpLength: Int
    ): Int {

        val checksumData = ByteArray(12 + udpLength)

        packet.copyInto(checksumData, 0, 12, 20)

        checksumData[8] = 0
        checksumData[9] = UDP_PROTOCOL.toByte()

        writeUInt16(checksumData, 10, udpLength)

        packet.copyInto(
            checksumData,
            12,
            udpOffset,
            udpOffset + udpLength
        )

        return calculateChecksum(
            checksumData,
            0,
            checksumData.size
        )
    }

    private fun calculateChecksum(
        data: ByteArray,
        offset: Int,
        length: Int
    ): Int {

        var sum = 0L
        var index = offset
        val end = offset + length

        while (index + 1 < end) {
            val value =
                ((data[index].toInt() and 0xFF) shl 8) or
                    (data[index + 1].toInt() and 0xFF)

            sum += value.toLong()
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

    private fun readUInt16(
        data: ByteArray,
        offset: Int
    ): Int {
        return (
            ((data[offset].toInt() and 0xFF) shl 8) or
                (data[offset + 1].toInt() and 0xFF)
        )
    }

    private fun writeUInt16(
        data: ByteArray,
        offset: Int,
        value: Int
    ) {
        data[offset] = ((value shr 8) and 0xFF).toByte()
        data[offset + 1] = (value and 0xFF).toByte()
    }
}
