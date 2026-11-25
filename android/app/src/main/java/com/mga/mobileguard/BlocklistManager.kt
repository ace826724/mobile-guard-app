package com.mga.mobileguard

/**
 * MGA - Blocklist Manager
 *
 * Engellenecek alan adlarını bellekte yönetir.
 *
 * Bu sürüm henüz kalıcı depolama kullanmaz ve
 * VPN trafiğine müdahale etmez.
 */
class BlocklistManager(
    initialDomains: Set<String> = emptySet()
) {

    private val domains = linkedSetOf<String>()

    init {
        initialDomains.forEach { addDomain(it) }
    }

    /**
     * Geçerli bir alan adını listeye ekler.
     * Başarılı eklemede true döndürür.
     */
    @Synchronized
    fun addDomain(domain: String): Boolean {
        val normalized = normalizeDomain(domain) ?: return false
        return domains.add(normalized)
    }

    /**
     * Alan adını listeden kaldırır.
     */
    @Synchronized
    fun removeDomain(domain: String): Boolean {
        val normalized = normalizeDomain(domain) ?: return false
        return domains.remove(normalized)
    }

    /**
     * Alan adının listede olup olmadığını kontrol eder.
     */
    @Synchronized
    fun contains(domain: String): Boolean {
        val normalized = normalizeDomain(domain) ?: return false
        return normalized in domains
    }

    /**
     * Listenin güvenli bir kopyasını döndürür.
     */
    @Synchronized
    fun getDomains(): Set<String> {
        return domains.toSet()
    }

    /**
     * Listedeki kayıt sayısı.
     */
    @Synchronized
    fun size(): Int {
        return domains.size
    }

    /**
     * Tüm kayıtları siler.
     */
    @Synchronized
    fun clear() {
        domains.clear()
    }

    private fun normalizeDomain(domain: String): String? {
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
