package com.mga.mobileguard

import org.junit.Assert.*
import org.junit.Test

class BlocklistManagerTest {

    @Test
    fun validDomainCanBeAdded() {
        val manager = BlocklistManager()

        assertTrue(manager.addDomain("example.com"))
        assertTrue(manager.contains("example.com"))
    }

    @Test
    fun duplicateDomainIsNotAdded() {
        val manager = BlocklistManager()

        assertTrue(manager.addDomain("example.com"))
        assertFalse(manager.addDomain("EXAMPLE.COM"))

        assertEquals(1, manager.size())
    }

    @Test
    fun domainCanBeRemoved() {
        val manager = BlocklistManager(
            setOf("example.com")
        )

        assertTrue(manager.removeDomain("example.com"))
        assertFalse(manager.contains("example.com"))
    }

    @Test
    fun invalidDomainIsRejected() {
        val manager = BlocklistManager()

        assertFalse(manager.addDomain("invalid domain"))
        assertEquals(0, manager.size())
    }

    @Test
    fun uppercaseDomainIsNormalized() {
        val manager = BlocklistManager()

        assertTrue(manager.addDomain("ADS.EXAMPLE.COM"))

        assertTrue(manager.contains("ads.example.com"))
    }

    @Test
    fun trailingDotIsNormalized() {
        val manager = BlocklistManager()

        assertTrue(manager.addDomain("example.com."))

        assertTrue(manager.contains("example.com"))
    }

    @Test
    fun initialDomainsAreLoaded() {
        val manager = BlocklistManager(
            setOf("example.com", "ads.example.net")
        )

        assertEquals(2, manager.size())
    }

    @Test
    fun clearRemovesAllDomains() {
        val manager = BlocklistManager(
            setOf("example.com", "example.net")
        )

        manager.clear()

        assertEquals(0, manager.size())
        assertTrue(manager.getDomains().isEmpty())
    }

    @Test
    fun returnedDomainSetCannotModifyManager() {
        val manager = BlocklistManager(
            setOf("example.com")
        )

        val returned = manager.getDomains()

        assertTrue(returned is Set<String>)
        assertEquals(1, manager.size())
    }
}
