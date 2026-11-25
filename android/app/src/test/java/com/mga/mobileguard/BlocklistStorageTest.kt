package com.mga.mobileguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BlocklistStorageTest {

    private val context
        get() = RuntimeEnvironment.getApplication()

    @Before
    fun clearPreferences() {
        context.getSharedPreferences(
            "mga_blocklist_storage",
            android.content.Context.MODE_PRIVATE
        ).edit().clear().commit()
    }

    @Test
    fun savedDomainsCanBeLoaded() {
        val storage = BlocklistStorage(context)

        assertTrue(
            storage.saveDomains(
                setOf("example.com", "ads.example.net")
            )
        )

        assertEquals(
            setOf("example.com", "ads.example.net"),
            storage.loadDomains()
        )
    }

    @Test
    fun emptyStorageReturnsEmptySet() {
        val storage = BlocklistStorage(context)

        assertTrue(storage.loadDomains().isEmpty())
    }

    @Test
    fun clearRemovesSavedDomains() {
        val storage = BlocklistStorage(context)

        storage.saveDomains(setOf("example.com"))

        assertTrue(storage.clear())
        assertTrue(storage.loadDomains().isEmpty())
    }

    @Test
    fun managerPersistsAddedDomains() {
        val firstManager = BlocklistManager(context)

        assertTrue(firstManager.addDomain("example.com"))

        val secondManager = BlocklistManager(context)

        assertTrue(secondManager.contains("example.com"))
        assertEquals(1, secondManager.size())
    }

    @Test
    fun managerPersistsRemovedDomains() {
        val firstManager = BlocklistManager(context)

        firstManager.addDomain("example.com")
        assertTrue(firstManager.removeDomain("example.com"))

        val secondManager = BlocklistManager(context)

        assertFalse(secondManager.contains("example.com"))
    }

    @Test
    fun managerPersistsClearOperation() {
        val firstManager = BlocklistManager(context)

        firstManager.addDomain("example.com")
        firstManager.addDomain("example.net")
        firstManager.clear()

        val secondManager = BlocklistManager(context)

        assertEquals(0, secondManager.size())
    }
}
