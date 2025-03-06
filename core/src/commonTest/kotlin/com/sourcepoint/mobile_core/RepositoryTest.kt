package com.sourcepoint.mobile_core

import com.russhwolf.settings.MapSettings
import com.sourcepoint.mobile_core.asserters.assertDoesNotContain
import com.sourcepoint.mobile_core.asserters.assertIsEmpty
import com.sourcepoint.mobile_core.asserters.assertNotEmpty
import com.sourcepoint.mobile_core.models.consents.State
import com.sourcepoint.mobile_core.storage.Repository
import com.sourcepoint.mobile_core.storage.get
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RepositoryTest {
    @Test
    fun testInitialRepositoryValues() {
        Repository(storage = MapSettings()).apply {
            assertNull(state)
            assertNull(uspString)
            assertIsEmpty(gppData)
            assertIsEmpty(tcData)
        }
    }

    @Test
    fun testPersistsState() {
        val storage = MapSettings()
        val repository = Repository(storage)
        repository.state = State(accountId = 0, propertyId = 0)
        assertNotEmpty(storage.getString(Repository.SP_STATE_KEY, ""))
    }

    @Test
    fun settingTCDataResetsItFirst() {
        val storage = MapSettings()
        val repository = Repository(storage)
        repository.tcData = mapOf("IABTCF_foo" to JsonPrimitive("foo"))
        assertEquals(storage["IABTCF_foo"], JsonPrimitive("foo"))

        repository.tcData = mapOf("IABTCF_bar" to JsonPrimitive("bar"))
        assertEquals(storage["IABTCF_bar"], JsonPrimitive("bar"))
        assertDoesNotContain(storage.keys, "IABTCF_foo")
    }

    @Test
    fun settingGPPDataResetsItFirst() {
        val storage = MapSettings()
        val repository = Repository(storage)
        repository.gppData = mapOf("IABGPP_foo" to JsonPrimitive("foo"))
        assertEquals(storage["IABGPP_foo"], JsonPrimitive("foo"))
        assertEquals(repository.gppData, mapOf("IABGPP_foo" to JsonPrimitive("foo")))

        repository.gppData = mapOf("IABGPP_bar" to JsonPrimitive("bar"))
        assertEquals(storage["IABGPP_bar"], JsonPrimitive("bar"))
        assertDoesNotContain(storage.keys, "IABGPP_foo")
    }

    @Test
    fun clearClearsTheStorage() {
        val storage = MapSettings()
        Repository(storage).apply {
            tcData = mapOf("IABTCF_Foo" to JsonPrimitive(""))
            gppData = mapOf("IABTCF_Foo" to JsonPrimitive(""))
            uspString = "foo"
            state = State(accountId = 0, propertyId = 0)
            clear()
        }
        assertIsEmpty(storage.keys)
    }
}
