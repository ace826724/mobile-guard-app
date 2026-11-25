
package com.mga.mobileguard

/**
 * MGA - DNS Packet Processor
 *
 * IPv4/UDP DNS paketlerini filtreler ve DNS yanıtı oluşturur.
 */
class DnsPacketProcessor(
    private val filteringEngine: DnsFilteringEngine,
    private val upstreamClient: DnsQueryForwarder
) {

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
                    Ipv4UdpPacket.buildResponse(
                        parsedPacket,
                        evaluation.response
                    )
                }

                is DnsFilteringEngine.Evaluation.Forward -> {
                    val response = upstreamClient.forward(
                        parsedPacket.dnsPayload
                    ) ?: return null

                    Ipv4UdpPacket.buildResponse(
                        parsedPacket,
                        response
                    )
                }

                DnsFilteringEngine.Evaluation.Invalid -> null
            }

        } catch (exception: Exception) {
            null
        }
    }
}
