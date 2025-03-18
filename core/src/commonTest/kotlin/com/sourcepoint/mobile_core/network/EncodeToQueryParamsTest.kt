package com.sourcepoint.mobile_core.network

import com.sourcepoint.mobile_core.network.requests.toQueryParams
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Serializable
data class Dummy(
    val intValue: Int,
    val stringValue: String,
    val defaultValue: String = "default val",
    val boolValue: Boolean,
    val objectValue: ComplexDummy,
    val nullableValue: String?,
    @SerialName("__renamed") val renamedValue: String? = "renamed val"
) {
    @Serializable
    data class ComplexDummy(val foo: String, val anotherNullable: String?)
}

class EncodeToQueryParamsTest {
    private val dummyClass = Dummy(
        intValue = 42,
        boolValue = true,
        stringValue = "string val",
        objectValue = Dummy.ComplexDummy(foo = "hello world", anotherNullable = null),
        nullableValue = null
    )

    @Test
    fun encodesSerializableClassToQueryParams() {
        assertEquals(
            mapOf(
                "intValue" to "42",
                "stringValue" to "string val",
                "boolValue" to "true",
                "defaultValue" to "default val",
                "__renamed" to "renamed val",
                "objectValue" to "{\"foo\":\"hello world\"}"
            ),
            dummyClass.toQueryParams()
        )
    }

    @Test
    fun canEncodeNulls() {
        val encoded = dummyClass.toQueryParams(omitNulls = false)
        assertEquals(encoded["objectValue"], "{\"foo\":\"hello world\",\"anotherNullable\":null}")
        assertNull(encoded["nullableValue"])
    }
}
