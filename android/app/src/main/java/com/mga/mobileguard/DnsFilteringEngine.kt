package com.mga.mobileguard

/**
 * MGA - DNS Filtering Engine
 *
 * DNS paketlerini ayrıştırır ve engelleme kararını verir.
 *
 * Bu bileşen henüz VPN TUN arayüzüne bağlı değildir.
 * İzin verilen sorguları internete kendisi iletmez.
 */
class DnsFilteringEngine(
    private val blocklistManager: BlocklistManager
) {

    sealed class Evaluation {

        data class Blocked(
            val domain: String,
            val response: ByteArray
        ) : Evaluation()

        data class Forward(
            val domain: String,
            val query: DnsPacket.Query
        ) : Evaluation()

        object Invalid : Evaluation()
    }

    /**
     * DNS sorgusunu değerlendirir.
     */
    fun evaluate(packet: ByteArray): Evaluation {

        val query = DnsPacket.parseQuery(packet)
            ?: return Evaluation.Invalid

        val filter = DomainFilter(
            blockedDomains = blocklistManager.getDomains()
        )

        return when (filter.check(query.domain)) {

            DomainFilter.Decision.BLOCK -> {
                Evaluation.Blocked(
                    domain = query.domain,
                    response = DnsPacket.buildBlockedResponse(query)
                )
            }

            DomainFilter.Decision.ALLOW -> {
                Evaluation.Forward(
                    domain = query.domain,
                    query = query
                )
            }
        }
    }
}
