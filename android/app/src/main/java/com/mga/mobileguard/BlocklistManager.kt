package com.mga.mobileguard

import android.content.Context

/**
 * MGA - Blocklist Manager
 *
 * Engellenecek alan adlarını yönetir.
 * Android Context ile oluşturulduğunda listeyi
 * cihazın yerel depolamasından yükler ve günceller.
 */
class BlocklistManager private constructor(
    initialDomains: Set<String>,
    private var storage: BlocklistStorage?
) {

    private val domains = linkedSetOf<String>()

    /**
     * Bellekte çalışan sürüm.
     * Mevcut birim testleriyle uyumludur.
     */
    constructor(
        initialDomains: Set<String> = emptySet()
    ) : this(initialDomains, null)

    /**
     * Kalıcı depolama kullanan Android sürümü.
     */
    constructor(context: Context) : this(
        emptySet(),
        BlocklistStorage(context)
    ) {
        domains.addAll(storage?.loadDomains() ?: emptySet())
    }

    init {
        initialDomains.forEach { addDomain(it) }
    }

    @Synchronized
    fun addDomain(domain: String): Boolean {
        val normalized = normalizeDomain(domain) ?: return false

        val added = domains.add(normalized)

        if (added) {
            persist()
        }

        return added
    }

    @Synchronized
    fun removeDomain(domain: String): Boolean {
        val normalized = normalizeDomain(domain) ?: return false

        val removed = domains.remove(normalized)

        if (removed) {
            persist()
        }

        return removed
    }

    @Synchronized
    fun contains(domain: String): Boolean {
        val normalized = normalizeDomain(domain) ?: return false
        return normalized in domains
    }

    @Synchronized
    fun getDomains(): Set<String> {
        return domains.toSet()
    }

    @Synchronized
    fun size(): Int {
        return domains.size
    }

    @Synchronized
    fun clear() {
        domains.clear()
        persist()
    }

    private fun persist() {
        storage?.saveDomains(domains)
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
