package com.sourcepoint.mobile_core.utils

import com.sourcepoint.mobile_core.network.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultEnumSerializerTest {
    @Serializable
    enum class MyEnum {
        Foo,
        @SerialName("BAR") Bar
    }

    @Test
    fun serializingEnumsIsCaseInsensitive() = runTest {
        assertEquals(MyEnum.Foo, json.decodeFromString("\"Foo\""))
        assertEquals(MyEnum.Foo, json.decodeFromString("\"foo\""))

        assertEquals(MyEnum.Bar, json.decodeFromString("\"Bar\""))
        assertEquals(MyEnum.Bar, json.decodeFromString("\"BAR\""))
        assertEquals(MyEnum.Bar, json.decodeFromString("\"bar\""))
    }
}
