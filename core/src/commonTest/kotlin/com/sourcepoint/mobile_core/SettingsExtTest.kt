package com.sourcepoint.mobile_core

import com.russhwolf.settings.MapSettings
import com.sourcepoint.mobile_core.asserters.assertContains
import com.sourcepoint.mobile_core.asserters.assertDoesNotContain
import com.sourcepoint.mobile_core.asserters.assertIsEmpty
import com.sourcepoint.mobile_core.asserters.assertTrue
import com.sourcepoint.mobile_core.storage.delete
import com.sourcepoint.mobile_core.storage.get
import com.sourcepoint.mobile_core.storage.getKeysWithPrefix
import com.sourcepoint.mobile_core.storage.readString
import com.sourcepoint.mobile_core.storage.readStringOrNull
import com.sourcepoint.mobile_core.storage.removeKeysStartingWith
import com.sourcepoint.mobile_core.storage.replaceKeysWithPrefix
import com.sourcepoint.mobile_core.storage.set
import com.sourcepoint.mobile_core.storage.writeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsExtTest {
    @Test
    fun testSetGetJsonPrimitiveWithAString() {
        val storage = MapSettings()
        storage["foo"] = JsonPrimitive("bar")
        assertEquals(JsonPrimitive("bar"), storage["foo"])
        assertEquals("bar", storage["foo"])
    }

    @Test
    fun testSetGetJsonPrimitiveWithAInt() {
        val storage = MapSettings()
        storage["foo"] = JsonPrimitive(1)
        assertEquals(JsonPrimitive(1), storage["foo"])
        assertEquals(1, storage["foo"])
    }

    @Test
    fun testSetGetJsonPrimitiveWithABoolean() {
        val storage = MapSettings()
        storage["foo"] = JsonPrimitive(true)
        assertEquals(JsonPrimitive(true), storage["foo"])
        assertEquals(true, storage["foo"])
    }

    @Test
    fun testSetGetJsonPrimitiveWithAFloat() {
        val storage = MapSettings()
        storage["foo"] = JsonPrimitive(0.0f)
        assertEquals(JsonPrimitive(0.0f), storage["foo"])
        assertEquals(0.0f, storage["foo"])
    }

    @Test
    fun testSetGetJsonPrimitiveWithADouble() {
        val storage = MapSettings()
        storage["foo"] = JsonPrimitive(Double.MAX_VALUE)
        assertEquals(JsonPrimitive(Double.MAX_VALUE), storage["foo"])
        assertEquals(Double.MAX_VALUE, storage["foo"]!!, absoluteTolerance = 0.00001)
    }

    @Test
    fun testSetGetJsonPrimitiveWithALong() {
        val storage = MapSettings()
        storage["foo"] = JsonPrimitive(1234567890123456789L)
        assertEquals(JsonPrimitive(1234567890123456789L), storage["foo"])
        assertEquals(1234567890123456789L, storage["foo"])
    }

    @Test
    fun removesKeysWithPrefix() {
        val settings = MapSettings(
            "PREFIX_key" to "value",
            "OTHER_key" to "other"
        )

        settings.removeKeysStartingWith("PREFIX_")

        assertDoesNotContain(settings.keys, "PREFIX_key")
        assertContains(settings.keys, "OTHER_key")
    }

    @Test
    fun getsKeysWithPrefixReturnsCorrectMap() {
        val settings = MapSettings(
            "PREFIX_key" to "value",
            "OTHER_key" to "other"
        )

        val result = settings.getKeysWithPrefix("PREFIX_")

        assertEquals(1, result.size)
        assertContains(result.keys, "PREFIX_key")
        assertDoesNotContain(result.keys, "OTHER_key")
        assertEquals(JsonPrimitive("value"), result["PREFIX_key"])
    }

    @Test
    fun replaceKeysWithPrefixRemovesOldAndAddsNew() {
        val settings = MapSettings(
            "PREFIX_old" to "old_value",
            "OTHER_key" to "keep_me"
        )

        settings.replaceKeysWithPrefix(
            "PREFIX_",
            mapOf("PREFIX_new" to JsonPrimitive("new_value"))
        )

        val prefixKeys = settings.keys.filter { it.startsWith("PREFIX_") }
        assertEquals(1, prefixKeys.size)
        assertContains(prefixKeys, "PREFIX_new")
        assertDoesNotContain(prefixKeys, "PREFIX_old")
        assertContains(settings.keys, "OTHER_key")
    }

    @Test
    fun readWriteStringOperations() {
        val settings = MapSettings()

        settings.writeString("key", "value")
        assertEquals("value", settings.readString("key"))
        assertEquals("value", settings.readStringOrNull("key"))

        settings.writeString("key", null)
        assertDoesNotContain(settings.keys, "key")
        assertNull(settings.readStringOrNull("key"))
    }

    @Test
    fun deleteRemovesKey() {
        val settings = MapSettings("key" to "value")

        settings.delete("key")

        assertNull(settings.readStringOrNull("key"))
        assertDoesNotContain(settings.keys, "key")
    }

    @Test
    fun concurrentPrefixRemovalIsAtomic() = runTest {
        val settings = MapSettings(
            "PREFIX_A_key" to "value_a",
            "PREFIX_B_key" to "value_b"
        )

        val jobs = (0 until 100).map { i ->
            async(Dispatchers.Default) {
                val prefix = if (i % 2 == 0) "PREFIX_A_" else "PREFIX_B_"
                settings.removeKeysStartingWith(prefix)
                settings.replaceKeysWithPrefix(
                    prefix,
                    mapOf("${prefix}new" to JsonPrimitive("new_value"))
                )
            }
        }
        jobs.awaitAll()

        val keysA = settings.keys.filter { it.startsWith("PREFIX_A_") }
        val keysB = settings.keys.filter { it.startsWith("PREFIX_B_") }
        assertTrue(keysA.isEmpty() || keysA.size == 1)
        assertTrue(keysB.isEmpty() || keysB.size == 1)
    }

    @Test
    fun concurrentReplaceKeysWithPrefixIsAtomic() = runTest {
        val settings = MapSettings()

        val jobs = (0 until 100).map { id ->
            async(Dispatchers.Default) {
                settings.replaceKeysWithPrefix(
                    "PREFIX_",
                    mapOf(
                        "PREFIX_key1" to JsonPrimitive("value1_$id"),
                        "PREFIX_key2" to JsonPrimitive("value2_$id"),
                        "PREFIX_key3" to JsonPrimitive("value3_$id")
                    )
                )
            }
        }
        jobs.awaitAll()

        val prefixKeys = settings.keys.filter { it.startsWith("PREFIX_") }
        assertEquals(3, prefixKeys.size)

        val ids = prefixKeys.map {
            settings.getKeysWithPrefix("PREFIX_")[it]?.content?.substringAfterLast("_")
        }.toSet()
        assertEquals(1, ids.size, "All keys must be from same atomic operation")
    }

    @Test
    fun concurrentGetKeysWithPrefixReturnsConsistentSnapshots() = runTest {
        val settings = MapSettings(
            "PREFIX_a" to "initial",
            "PREFIX_b" to "initial",
            "PREFIX_c" to "initial"
        )

        val writeJobs = (0 until 50).map { id ->
            async(Dispatchers.Default) {
                settings.replaceKeysWithPrefix(
                    "PREFIX_",
                    mapOf(
                        "PREFIX_a" to JsonPrimitive("$id"),
                        "PREFIX_b" to JsonPrimitive("$id"),
                        "PREFIX_c" to JsonPrimitive("$id")
                    )
                )
            }
        }

        val readJobs = (0 until 100).map {
            async(Dispatchers.Default) {
                val data = settings.getKeysWithPrefix("PREFIX_")
                if (data.isNotEmpty()) {
                    val values = data.values.map { it.content }.toSet()
                    assertEquals(1, values.size, "All values in snapshot must be the same")
                }
            }
        }

        (writeJobs + readJobs).awaitAll()
    }

    @Test
    fun concurrentReadWriteStringOperations() = runTest {
        val settings = MapSettings("counter" to "0")

        val jobs = (0 until 100).map {
            async(Dispatchers.Default) {
                val current = settings.readString("counter", "0").toInt()
                settings.writeString("counter", "${current + 1}")
            }
        }
        jobs.awaitAll()

        val finalValue = settings.readString("counter", "0").toInt()
        assertTrue(finalValue > 0)
    }

    @Test
    fun concurrentDeleteOperations() = runTest {
        val settings = MapSettings()
        repeat(50) { i ->
            settings.putString("key_$i", "value")
        }

        val jobs = (0 until 50).map { i ->
            async(Dispatchers.Default) {
                settings.delete("key_$i")
            }
        }
        jobs.awaitAll()

        assertIsEmpty(settings.keys)
    }

    @Test
    fun replaceKeysWithPrefixHandlesEmptyData() {
        val settings = MapSettings(
            "PREFIX_key1" to "value1",
            "PREFIX_key2" to "value2"
        )

        settings.replaceKeysWithPrefix("PREFIX_", emptyMap())

        assertDoesNotContain(settings.keys, "PREFIX_key1")
        assertDoesNotContain(settings.keys, "PREFIX_key2")
    }

    @Test
    fun getKeysWithPrefixReturnsEmptyForNonExistentPrefix() {
        val settings = MapSettings("key" to "value")

        val result = settings.getKeysWithPrefix("NONEXISTENT_")

        assertIsEmpty(result)
    }
}
