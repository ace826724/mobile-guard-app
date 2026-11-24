package com.mga.mobileguard

/**
 * MGA - Domain Filter
 *
 * DNS sorgularında alan adının engellenip engellenmeyeceğine
 * karar veren bağımsız filtre bileşeni.
 *
 * Bu sınıf henüz ağ trafiğine müdahale etmez.
 */
class DomainFilter(
    blockedDomains: Set<String> = emptySet(),
    allowedDomains: Set<String> = emptySet()
) {

    private val blocked = normalizeDomains(blockedDomains)
    private val allowed = normalizeDomains(allowedDomains)

    enum class Decision {
        BLOCK,
        ALLOW
    }

    /**
     * Alan adının engellenip engellenmeyeceğini belirler.
     *
     * Öncelik:
     * 1. İzin listesi
     * 2. Engelleme listesi
     * 3. Varsayılan izin
     */
    fun check(domain: String): Decision {
        val normalized = normalizeDomain(domain)
            ?: return Decision.ALLOW

        if (matchesDomain(normalized, allowed)) {
            return Decision.ALLOW
        }

        if (matchesDomain(normalized, blocked)) {
            return Decision.BLOCK
        }

        return Decision.ALLOW
    }

    private fun matchesDomain(
        domain: String,
        rules: Set<String>
    ): Boolean {
        return rules.any { rule ->
            domain == rule || domain.endsWith(".$rule")
        }
    }

    private fun normalizeDomains(
        domains: Set<String>
    ): Set<String> {
        return domains.mapNotNull(::normalizeDomain).toSet()
    }

    private fun normalizeDomain(
        domain: String
    ): String? {
        val normalized = domain
            .trim()
            .lowercase()
            .trimEnd('.')

        if (normalized.isEmpty() || normalized.length > 253) {
            return null
        }

        if (normalized.any { it.isWhitespace() }) {
            return null
        }

        val labels = normalized.split(".")

        if (labels.any { label ->
            label.isEmpty() ||
            label.length > 63 ||
            label.startsWith("-") ||
            label.endsWith("-") ||
            label.any { char ->
                !char.isLetterOrDigit() && char != '-'
            }
        }) {
            return null
        }

        return normalized
    }
}
