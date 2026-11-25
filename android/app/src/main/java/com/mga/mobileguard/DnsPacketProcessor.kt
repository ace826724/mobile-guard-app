package com.mga.mobileguard

import android.util.Log

/**
 * MGA - DNS Packet Processor
 *
 * IPv4/UDP DNS paketlerini filtreler ve DNS yanıtı oluşturur.
 */
class DnsPacketProcessor(
    private val filteringEngine: DnsFilteringEngine,
    private val upstreamClient: DnsQueryForwarder
) {

    companion object {
        private const val TAG = "MGA-DNS-PROCESSOR"
    }

    /**
     * IPv4/UDP DNS paketini işler.
     *
     * Geçerli işlemde yanıt paketini döndürür.
     * Geçersiz paket veya upstream hatasında null döndürür.
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
                        "Engellenen DNS sorgusu."
                    )

                    Ipv4UdpPacket.buildResponse(
                        parsedPacket,
                        evaluation.response
                    )
                }

                is DnsFilteringEngine.Evaluation.Forward -> {

                    Log.d(
                        TAG,
                        "DNS sorgusu upstream sunucusuna iletiliyor."
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
