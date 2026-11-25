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
 * İzin verilen DNS sorgularını Cloudflare DNS sunucusuna iletir.
 *
 * Bu sınıf:
 * - UDP üzerinden Cloudflare 1.1.1.1:53 adresine bağlanır.
 * - Soketi VPN tünelinden korur.
 * - Yanıtı bekler ve çağırana döndürür.
 *
 * TUN paket okuma ve yanıt paketine dönüştürme işlemlerini yapmaz.
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
    }

    /**
     * DNS sorgusunu Cloudflare'a gönderir.
     *
     * Başarılıysa DNS yanıtını döndürür.
     * Hata veya zaman aşımında null döndürür.
     */
    fun forward(query: ByteArray): ByteArray? {

        if (query.size < 12 || query.size > MAX_DNS_PACKET_SIZE) {
            Log.w(TAG, "Geçersiz DNS sorgu boyutu.")
            return null
        }

        var socket: DatagramSocket? = null

        return try {
            socket = DatagramSocket()

            // DNS soketinin VPN tüneline geri girmesini önle.
            if (!vpnService.protect(socket)) {
                Log.e(TAG, "DNS soketi VPN korumasına alınamadı.")
                return null
            }

            socket.soTimeout = timeoutMillis

            val serverAddress = InetAddress.getByName(DNS_SERVER)

            val request = DatagramPacket(
                query,
                query.size,
                serverAddress,
                DNS_PORT
            )

            socket.send(request)

            val responseBuffer = ByteArray(MAX_DNS_PACKET_SIZE)

            val responsePacket = DatagramPacket(
                responseBuffer,
                responseBuffer.size
            )

            socket.receive(responsePacket)

            if (responsePacket.length < 12) {
                Log.w(TAG, "Cloudflare geçersiz DNS yanıtı gönderdi.")
                return null
            }

            responseBuffer.copyOf(responsePacket.length)

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
}
