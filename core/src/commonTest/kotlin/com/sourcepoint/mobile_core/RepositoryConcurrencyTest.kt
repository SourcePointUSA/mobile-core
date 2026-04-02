package com.sourcepoint.mobile_core

import com.russhwolf.settings.MapSettings
import com.sourcepoint.mobile_core.models.consents.State
import com.sourcepoint.mobile_core.storage.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import com.sourcepoint.mobile_core.asserters.assertDoesNotContain
import com.sourcepoint.mobile_core.asserters.assertIsEmpty
import com.sourcepoint.mobile_core.asserters.assertNotEmpty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.milliseconds

class RepositoryConcurrencyTest {

    @Test
    fun concurrentWritesToTcDataProduceSingleEntry() = runTest {
        val repository = Repository(MapSettings())
        repository.tcData = mapOf("IABTCF_initial" to JsonPrimitive("should_be_removed"))

        val jobs = (0 until 10).map { threadId ->
            async(Dispatchers.Default) {
                repeat(100) { iteration ->
                    repository.tcData = mapOf(
                        "IABTCF_key1" to JsonPrimitive("thread${threadId}_iter${iteration}"),
                        "IABTCF_key2" to JsonPrimitive("thread${threadId}_iter${iteration}")
                    )
                }
            }
        }
        jobs.awaitAll()

        val tcData = repository.tcData
        assertEquals(2, tcData.size, "Expected exactly 2 keys after concurrent writes")
        assertDoesNotContain(tcData.keys, "IABTCF_initial")
        assertEquals(tcData["IABTCF_key1"], tcData["IABTCF_key2"])
    }

    @Test
    fun concurrentWritesToGppDataProduceSingleEntry() = runTest {
        val repository = Repository(MapSettings())
        repository.gppData = mapOf("IABGPP_initial" to JsonPrimitive("should_be_removed"))

        val jobs = (0 until 10).map { threadId ->
            async(Dispatchers.Default) {
                repeat(100) { iteration ->
                    repository.gppData = mapOf(
                        "IABGPP_key1" to JsonPrimitive("thread${threadId}_iter${iteration}"),
                        "IABGPP_key2" to JsonPrimitive("thread${threadId}_iter${iteration}")
                    )
                }
            }
        }
        jobs.awaitAll()

        val gppData = repository.gppData
        assertEquals(2, gppData.size, "Expected exactly 2 keys after concurrent writes")
        assertDoesNotContain(gppData.keys, "IABGPP_initial")
        assertEquals(gppData["IABGPP_key1"], gppData["IABGPP_key2"])
    }

    @Test
    fun concurrentReadsNeverSeeInconsistentTcData() = runTest {
        val repository = Repository(MapSettings())
        repository.tcData = mapOf("IABTCF_initial" to JsonPrimitive("initial_value"))

        val writeJobs = (0 until 50).map { writeId ->
            async(Dispatchers.Default) {
                repository.tcData = mapOf("IABTCF_key" to JsonPrimitive("value_$writeId"))
            }
        }

        val readJobs = (0 until 50).map {
            async(Dispatchers.Default) {
                assertNotEmpty(repository.tcData)
            }
        }

        (writeJobs + readJobs).awaitAll()
        assertEquals(1, repository.tcData.size)
    }

    @Test
    fun concurrentReadsNeverSeeInconsistentGppData() = runTest {
        val repository = Repository(MapSettings())
        repository.gppData = mapOf("IABGPP_initial" to JsonPrimitive("initial_value"))

        val writeJobs = (0 until 50).map { writeId ->
            async(Dispatchers.Default) {
                repository.gppData = mapOf("IABGPP_key" to JsonPrimitive("value_$writeId"))
            }
        }

        val readJobs = (0 until 50).map {
            async(Dispatchers.Default) {
                assertNotEmpty(repository.gppData)
            }
        }

        (writeJobs + readJobs).awaitAll()
        assertEquals(1, repository.gppData.size)
    }

    @Test
    fun concurrentClearOperationsLeaveStorageEmpty() = runTest {
        val storage = MapSettings()
        val repository = Repository(storage)

        val jobs = (0 until 100).map { iteration ->
            async(Dispatchers.Default) {
                repository.tcData = mapOf("IABTCF_key" to JsonPrimitive("value"))
                repository.gppData = mapOf("IABGPP_key" to JsonPrimitive("value"))
                repository.uspString = "uspString_$iteration"
                delay(1.milliseconds)
                repository.clear()
            }
        }
        jobs.awaitAll()

        assertIsEmpty(storage.keys)
    }

    @Test
    fun multiKeyWritesAreAtomic() = runTest {
        val repository = Repository(MapSettings())

        val jobs = (0 until 100).map { id ->
            async(Dispatchers.Default) {
                repository.tcData = mapOf(
                    "IABTCF_key1" to JsonPrimitive("value1_$id"),
                    "IABTCF_key2" to JsonPrimitive("value2_$id"),
                    "IABTCF_key3" to JsonPrimitive("value3_$id")
                )
            }
        }
        jobs.awaitAll()

        val tcData = repository.tcData
        assertEquals(3, tcData.size)

        val ids = tcData.values.map { it.content.substringAfterLast("_") }.toSet()
        assertEquals(1, ids.size, "All values must be from same write operation")
    }

    @Test
    fun stateSerializationIsThreadSafe() = runTest {
        val repository = Repository(MapSettings())

        val writeJobs = (0 until 100).map { id ->
            async(Dispatchers.Default) {
                repository.state = State(accountId = id, propertyId = id * 2)
            }
        }

        val readJobs = (0 until 100).map {
            async(Dispatchers.Default) {
                repository.state?.let { state ->
                    assertNotNull(state.accountId)
                    assertNotNull(state.propertyId)
                    assertEquals(state.accountId * 2, state.propertyId)
                }
            }
        }

        (writeJobs + readJobs).awaitAll()

        val finalState = repository.state
        assertNotNull(finalState)
        assertEquals(finalState.accountId * 2, finalState.propertyId)
    }
}
