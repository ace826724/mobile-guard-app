package com.mga.mobileguard

import org.junit.Assert.assertEquals
import org.junit.Test

class DomainFilterTest {

    @Test
    fun blockedDomainIsBlocked() {
        val filter = DomainFilter(
            blockedDomains = setOf("ads.example.com")
        )

        assertEquals(
            DomainFilter.Decision.BLOCK,
            filter.check("ads.example.com")
        )
    }

    @Test
    fun subdomainOfBlockedDomainIsBlocked() {
        val filter = DomainFilter(
            blockedDomains = setOf("example.com")
        )

        assertEquals(
            DomainFilter.Decision.BLOCK,
            filter.check("ads.example.com")
        )
    }

    @Test
    fun unrelatedDomainIsAllowed() {
        val filter = DomainFilter(
            blockedDomains = setOf("example.com")
        )

        assertEquals(
            DomainFilter.Decision.ALLOW,
            filter.check("safe-example.com")
        )
    }

    @Test
    fun allowedDomainOverridesBlockRule() {
        val filter = DomainFilter(
            blockedDomains = setOf("example.com"),
            allowedDomains = setOf("safe.example.com")
        )

        assertEquals(
            DomainFilter.Decision.ALLOW,
            filter.check("safe.example.com")
        )
    }

    @Test
    fun subdomainOfAllowedDomainIsAllowed() {
        val filter = DomainFilter(
            blockedDomains = setOf("example.com"),
            allowedDomains = setOf("safe.example.com")
        )

        assertEquals(
            DomainFilter.Decision.ALLOW,
            filter.check("shop.safe.example.com")
        )
    }

    @Test
    fun uppercaseDomainIsNormalized() {
        val filter = DomainFilter(
            blockedDomains = setOf("example.com")
        )

        assertEquals(
            DomainFilter.Decision.BLOCK,
            filter.check("ADS.EXAMPLE.COM")
        )
    }

    @Test
    fun trailingDotIsNormalized() {
        val filter = DomainFilter(
            blockedDomains = setOf("example.com")
        )

        assertEquals(
            DomainFilter.Decision.BLOCK,
            filter.check("example.com.")
        )
    }

    @Test
    fun invalidDomainIsAllowedByDefault() {
        val filter = DomainFilter(
            blockedDomains = setOf("example.com")
        )

        assertEquals(
            DomainFilter.Decision.ALLOW,
            filter.check("invalid domain")
        )
    }
}
