package com.sourcepoint.mobile_core.utils

import com.sourcepoint.mobile_core.network.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
enum class IntTestEnum(override val rawValue: Int) : IntEnum {
    Foo(2),
    Bar(99);

    companion object {
        val serializer = IntEnumSerializer(IntTestEnum.entries)
    }
}

class IntEnumSerializerTest {
    @Test
    fun encodeToIntString() = runTest {
        assertEquals("2", json.encodeToString(IntTestEnum.serializer, IntTestEnum.Foo))
    }

    @Test
    fun decodeFromInt() = runTest {
        assertEquals(IntTestEnum.Bar, json.decodeFromString(IntTestEnum.serializer, "99"))
    }
}
