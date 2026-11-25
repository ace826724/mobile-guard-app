package com.mga.mobileguard

import android.content.Context
import org.json.JSONArray

/**
 * MGA - Blocklist Storage
 *
 * Engelleme listesini cihazın yerel depolamasında saklar.
 * Veriler uygulama kaldırılmadığı sürece korunur.
 */
class BlocklistStorage(context: Context) {

    companion object {
        private const val PREFS_NAME = "mga_blocklist_storage"
        private const val KEY_BLOCKED_DOMAINS = "blocked_domains"
    }

    private val preferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Alan adlarını yerel depolamaya kaydeder.
     *
     * commit() işlemi tamamlandığında true döndürür.
     */
    @Synchronized
    fun saveDomains(domains: Set<String>): Boolean {
        return try {
            val jsonArray = JSONArray()

            domains.forEach { domain ->
                jsonArray.put(domain)
            }

            preferences.edit()
                .putString(KEY_BLOCKED_DOMAINS, jsonArray.toString())
                .commit()

        } catch (exception: Exception) {
            false
        }
    }

    /**
     * Kaydedilmiş alan adlarını yükler.
     *
     * Kayıt bulunamazsa boş liste döndürür.
     */
    @Synchronized
    fun loadDomains(): Set<String> {
        val savedData = preferences.getString(
            KEY_BLOCKED_DOMAINS,
            null
        ) ?: return emptySet()

        return try {
            val jsonArray = JSONArray(savedData)
            val domains = linkedSetOf<String>()

            for (index in 0 until jsonArray.length()) {
                val domain = jsonArray.optString(index, "")

                if (domain.isNotBlank()) {
                    domains.add(domain)
                }
            }

            domains

        } catch (exception: Exception) {
            emptySet()
        }
    }

    /**
     * Kaydedilmiş tüm alan adlarını siler.
     */
    @Synchronized
    fun clear(): Boolean {
        return preferences.edit()
            .remove(KEY_BLOCKED_DOMAINS)
            .commit()
    }
}
