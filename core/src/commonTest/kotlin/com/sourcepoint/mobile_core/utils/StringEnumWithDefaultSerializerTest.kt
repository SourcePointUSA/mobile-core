package com.sourcepoint.mobile_core.utils

import com.sourcepoint.mobile_core.network.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

class StringEnumWithDefaultSerializerTest {
    @Serializable(with = StringTestEnum.Serializer::class)
    enum class StringTestEnum {
        Foo,
        Bar,
        Unknown;

        object Serializer : StringEnumWithDefaultSerializer<StringTestEnum>(entries, Unknown)
    }

    @Serializable
    data class DummyWithStringEnum(val enumProperty: StringTestEnum)

    @Test
    fun encodeToString() {
        assertEquals("\"Foo\"", json.encodeToString(StringTestEnum.Foo))
    }

    @Test
    fun decodeFromString() {
        assertEquals(StringTestEnum.Bar, json.decodeFromString("\"Bar\""))
    }

    @Test
    fun encodeToStringInsideObject() {
        assertEquals(
            "{\"enumProperty\":\"Foo\"}",
            json.encodeToString(DummyWithStringEnum(enumProperty = StringTestEnum.Foo))
        )
    }

    @Test
    fun decodeFromStringInsideObject() {
        assertEquals(
            DummyWithStringEnum(enumProperty = StringTestEnum.Bar),
            json.decodeFromString("{\"enumProperty\":\"Bar\"}")
        )
    }

    @Test
    fun decodeToDefaultValue() {
        assertEquals(StringTestEnum.Unknown, json.decodeFromString( "\"etc\""))
    }
}
