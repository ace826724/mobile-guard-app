package com.mga.mobileguard

import android.net.VpnService
import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException

/**
 * MGA - DNS Upstream Client
 *
 * DNS sorgularını Cloudflare 1.1.1.1:53 adresine iletir.
 * Gelen yanıtın kaynak adresini ve sorguyla eşleşmesini doğrular.
 */
class DnsUpstreamClient(
    private val vpnService: VpnService,
    private val timeoutMillis: Int = 5000
) {

    companion object {
        private const val TAG = "MGA-DNS-UPSTREAM"
        private const val DNS_SERVER = "1.1.1.1"
        private const val DNS_PORT = 53
        private const val MAX_DNS_PACKET_SIZE = 4096
        private const val DNS_HEADER_SIZE = 12
    }

    /**
     * DNS sorgusunu Cloudflare'a gönderir.
     *
     * Geçerli yanıt alınırsa ByteArray döndürür.
     * Hata, geçersiz yanıt veya zaman aşımında null döndürür.
     */
    fun forward(query: ByteArray): ByteArray? {

        if (query.size < DNS_HEADER_SIZE ||
            query.size > MAX_DNS_PACKET_SIZE
        ) {
            Log.w(TAG, "Geçersiz DNS sorgu boyutu.")
            return null
        }

        // Gelen veri bir DNS sorgusu olmalı.
        val queryFlags = readUInt16(query, 2)

        if ((queryFlags and 0x8000) != 0) {
            Log.w(TAG, "DNS yanıtı sorgu olarak gönderilemez.")
            return null
        }

        if (readUInt16(query, 4) != 1) {
            Log.w(TAG, "Yalnızca tek sorulu DNS sorguları desteklenir.")
            return null
        }

        var socket: DatagramSocket? = null

        return try {
            socket = DatagramSocket()

            // Soketi VPN tünelinden çıkar.
            if (!vpnService.protect(socket)) {
                Log.e(TAG, "DNS soketi VPN korumasına alınamadı.")
                return null
            }

            socket.soTimeout = timeoutMillis

            val serverAddress = InetAddress.getByName(DNS_SERVER)

            // Yalnızca belirtilen Cloudflare sunucusuyla iletişim kur.
            socket.connect(
                InetSocketAddress(serverAddress, DNS_PORT)
            )

            val request = DatagramPacket(
                query,
                query.size
            )

            socket.send(request)

            val responseBuffer = ByteArray(MAX_DNS_PACKET_SIZE)

            val responsePacket = DatagramPacket(
                responseBuffer,
                responseBuffer.size
            )

            socket.receive(responsePacket)

            val response = responseBuffer.copyOf(
                responsePacket.length
            )

            if (!isMatchingResponse(query, response)) {
                Log.w(TAG, "DNS yanıtı sorguyla eşleşmiyor.")
                return null
            }

            response

        } catch (exception: SocketTimeoutException) {
            Log.w(TAG, "Cloudflare DNS zaman aşımı.")
            null

        } catch (exception: Exception) {
            Log.e(TAG, "DNS yönlendirme hatası.", exception)
            null

        } finally {
            socket?.close()
        }
    }

    /**
     * Yanıtın gönderilen DNS sorgusuyla eşleşmesini kontrol eder.
     */
    private fun isMatchingResponse(
        query: ByteArray,
        response: ByteArray
    ): Boolean {

        if (response.size < DNS_HEADER_SIZE) return false

        // Transaction ID aynı olmalı.
        if (readUInt16(query, 0) != readUInt16(response, 0)) {
            return false
        }

        val queryFlags = readUInt16(query, 2)
        val responseFlags = readUInt16(response, 2)

        // QR biti yanıtı göstermeli.
        if ((responseFlags and 0x8000) == 0) return false

        // Opcode aynı olmalı.
        if ((queryFlags and 0x7800) !=
            (responseFlags and 0x7800)
        ) {
            return false
        }

        // Sorgu ve yanıt bir soru içermeli.
        if (readUInt16(query, 4) != 1) return false
        if (readUInt16(response, 4) != 1) return false

        // Soru bölümünü karşılaştır.
        val queryQuestionEnd = findQuestionEnd(query)
            ?: return false

        val responseQuestionEnd = findQuestionEnd(response)
            ?: return false

        val queryQuestion = query.copyOfRange(
            DNS_HEADER_SIZE,
            queryQuestionEnd
        )

        val responseQuestion = response.copyOfRange(
            DNS_HEADER_SIZE,
            responseQuestionEnd
        )

        return queryQuestion.contentEquals(responseQuestion)
    }

    /**
     * DNS soru bölümünün bitiş konumunu bulur.
     */
    private fun findQuestionEnd(packet: ByteArray): Int? {

        if (packet.size < DNS_HEADER_SIZE + 5) return null

        var offset = DNS_HEADER_SIZE

        while (offset < packet.size) {

            val labelLength = packet[offset].toInt() and 0xFF
            offset++

            if (labelLength == 0) break

            // Sıkıştırılmış veya geçersiz soru adı kabul edilmez.
            if ((labelLength and 0xC0) != 0) return null
            if (labelLength > 63) return null
            if (offset + labelLength > packet.size) return null

            offset += labelLength
        }

        if (offset + 4 > packet.size) return null

        return offset + 4
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
}
