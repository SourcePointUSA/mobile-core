package com.sourcepoint.mobile_core.utils

import com.sourcepoint.mobile_core.network.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable(with = IntTestEnum.Serializer::class)
enum class IntTestEnum(override val rawValue: Int) : IntEnum {
    Foo(2),
    Bar(99),
    Unknown(-1);

    object Serializer : IntEnumSerializer<IntTestEnum>(entries, Unknown)
}

@Serializable
data class DummyWithEnum(val enumProperty: IntTestEnum)

class IntEnumSerializerTest {
    @Test
    fun encodeToIntString() {
        assertEquals("2", json.encodeToString(IntTestEnum.Foo))
    }

    @Test
    fun decodeFromInt() {
        assertEquals(IntTestEnum.Bar, json.decodeFromString( "99"))
    }

    @Test
    fun encodeToIntInsideObject() {
        assertEquals(
            "{\"enumProperty\":2}",
            json.encodeToString(DummyWithEnum(enumProperty = IntTestEnum.Foo))
        )
    }

    @Test
    fun decodeFromIntInsideObject() {
        assertEquals(
            DummyWithEnum(enumProperty = IntTestEnum.Bar),
            json.decodeFromString("{\"enumProperty\":99}")
        )
    }

    @Test
    fun decodeToDefaultValue() {
        assertEquals(IntTestEnum.Unknown, json.decodeFromString( "0"))
    }
}
