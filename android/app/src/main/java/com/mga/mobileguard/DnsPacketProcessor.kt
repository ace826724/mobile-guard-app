package com.mga.mobileguard

import android.util.Log

/**
 * MGA - DNS Packet Processor
 *
 * IPv4/UDP DNS paketlerini filtreler ve DNS yanıtı oluşturur.
 *
 * Sorumlulukları:
 * - Gelen IPv4/UDP paketini ayrıştırmak.
 * - DNS sorgusunu filtreleme motoruna göndermek.
 * - Engellenen alan adlarına NXDOMAIN yanıtı üretmek.
 * - İzin verilen sorguları Cloudflare'a iletmek.
 * - DNS yanıtını IPv4/UDP paketi olarak hazırlamak.
 *
 * Bu sınıf TUN arayüzünü açmaz veya kapatmaz.
 */
class DnsPacketProcessor(
    private val filteringEngine: DnsFilteringEngine,
    private val upstreamClient: DnsUpstreamClient
) {

    companion object {
        private const val TAG = "MGA-DNS-PROCESSOR"
    }

    /**
     * Bir IPv4/UDP DNS paketini işler.
     *
     * Başarılı işlemde cihaza geri yazılabilecek
     * IPv4/UDP yanıt paketini döndürür.
     *
     * Geçersiz paket veya DNS hatasında null döndürür.
     */
    fun process(packet: ByteArray): ByteArray? {

        val parsedPacket = Ipv4UdpPacket.parse(packet)
            ?: return null

        return try {
            when (
                val evaluation = filteringEngine.evaluate(
                    parsedPacket.dnsPayload
                )
            ) {

                is DnsFilteringEngine.Evaluation.Blocked -> {

                    Log.i(
                        TAG,
                        "Alan adı engellendi: ${evaluation.domain}"
                    )

                    Ipv4UdpPacket.buildResponse(
                        parsedPacket,
                        evaluation.response
                    )
                }

                is DnsFilteringEngine.Evaluation.Forward -> {

                    Log.d(
                        TAG,
                        "DNS sorgusu Cloudflare'a iletiliyor: ${evaluation.domain}"
                    )

                    val response = upstreamClient.forward(
                        parsedPacket.dnsPayload
                    ) ?: return null

                    Ipv4UdpPacket.buildResponse(
                        parsedPacket,
                        response
                    )
                }

                DnsFilteringEngine.Evaluation.Invalid -> {
                    Log.w(TAG, "Geçersiz DNS sorgusu reddedildi.")
                    null
                }
            }

        } catch (exception: Exception) {
            Log.e(TAG, "DNS paketi işlenemedi.", exception)
            null
        }
    }
}
