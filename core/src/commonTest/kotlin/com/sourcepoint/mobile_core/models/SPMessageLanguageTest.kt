package com.sourcepoint.mobile_core.models

import com.sourcepoint.mobile_core.network.json
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SPMessageLanguageTest {
    @Test
    fun `SPMessageLanguage can be deserialized from a case insensitive value`() {
        assertEquals(SPMessageLanguage.ENGLISH, json.decodeFromString("\"en\""))
        assertEquals(SPMessageLanguage.ENGLISH, json.decodeFromString("\"En\""))
        assertEquals(SPMessageLanguage.ENGLISH, json.decodeFromString("\"EN\""))
    }

    @Test
    fun `SPMessageLanguage throws if the short code can't be deserialized`() {
        assertFailsWith(SerializationException::class) { json.decodeFromString("\"foo\"") }
    }
}
