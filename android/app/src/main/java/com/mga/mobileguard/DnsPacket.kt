package com.mga.mobileguard

/**
 * MGA DNS Packet Parser
 *
 * DNS sorgu paketlerini güvenli şekilde ayrıştırır.
 * Bu sınıf henüz VPN servisine bağlı değildir.
 */
object DnsPacket {

    data class Query(
        val transactionId: Int,
        val domain: String,
        val queryType: Int,
        val queryClass: Int,
        val questionBytes: ByteArray,
        val recursionDesired: Boolean
    )

    /**
     * DNS sorgusunu ayrıştırır.
     * Desteklenen yapı: Tek sorulu standart DNS sorgusu.
     */
    fun parseQuery(packet: ByteArray): Query? {
        if (packet.size < 17) return null

        val transactionId = readUInt16(packet, 0)
        val flags = readUInt16(packet, 2)
        val questionCount = readUInt16(packet, 4)

        val isResponse = (flags and 0x8000) != 0
        val opcode = (flags shr 11) and 0x0F

        if (isResponse) return null
        if (opcode != 0) return null
        if (questionCount != 1) return null

        var offset = 12
        val labels = mutableListOf<String>()
        var totalLength = 0

        while (true) {
            if (offset >= packet.size) return null

            val labelLength = packet[offset].toInt() and 0xFF
            offset++

            if (labelLength == 0) break

            // Sıkıştırılmış veya geçersiz DNS isimlerini kabul etme.
            if ((labelLength and 0xC0) != 0) return null
            if (labelLength > 63) return null
            if (offset + labelLength > packet.size) return null

            totalLength += labelLength + 1
            if (totalLength > 254) return null

            val labelBytes = packet.copyOfRange(
                offset,
                offset + labelLength
            )

            if (labelBytes.any {
                val value = it.toInt() and 0xFF
                value < 0x21 || value > 0x7E
            }) {
                return null
            }

            val label = labelBytes.toString(Charsets.US_ASCII)
                .lowercase()

            labels.add(label)
            offset += labelLength
        }

        if (labels.isEmpty()) return null
        if (offset + 4 > packet.size) return null

        val domain = labels.joinToString(".")

        if (domain.length > 253) return null

        val queryType = readUInt16(packet, offset)
        val queryClass = readUInt16(packet, offset + 2)

        // Yalnızca internet sınıfındaki DNS sorgularını kabul et.
        if (queryClass != 1) return null

        val questionEnd = offset + 4

        val questionBytes = packet.copyOfRange(
            12,
            questionEnd
        )

        return Query(
            transactionId = transactionId,
            domain = domain,
            queryType = queryType,
            queryClass = queryClass,
            questionBytes = questionBytes,
            recursionDesired = (flags and 0x0100) != 0
        )
    }

    /**
     * Engellenen bir alan adı için NXDOMAIN yanıtı oluşturur.
     *
     * Bu metot yalnızca DNS mesajı üretir.
     * Paketi cihaza iletmez veya VPN bağlantısı başlatmaz.
     */
    fun buildBlockedResponse(query: Query): ByteArray {

        val response = ByteArray(12 + query.questionBytes.size)

        // İşlem kimliği
        writeUInt16(response, 0, query.transactionId)

        // QR=1, RD istekteki gibi korunur, RCODE=3 (NXDOMAIN).
        var flags = 0x8000 or 0x0003

        if (query.recursionDesired) {
            flags = flags or 0x0100
        }

        writeUInt16(response, 2, flags)

        // Bir soru, sıfır cevap ve sıfır ek kayıt.
        writeUInt16(response, 4, 1)
        writeUInt16(response, 6, 0)
        writeUInt16(response, 8, 0)
        writeUInt16(response, 10, 0)

        // Orijinal DNS soru bölümü.
        query.questionBytes.copyInto(
            response,
            destinationOffset = 12
        )

        return response
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
