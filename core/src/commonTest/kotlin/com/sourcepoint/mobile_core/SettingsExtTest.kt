package com.sourcepoint.mobile_core

import com.russhwolf.settings.MapSettings
import com.sourcepoint.mobile_core.storage.get
import com.sourcepoint.mobile_core.storage.set
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
