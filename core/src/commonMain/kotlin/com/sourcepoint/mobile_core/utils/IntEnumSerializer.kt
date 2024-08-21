package com.sourcepoint.mobile_core.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.enums.EnumEntries

interface IntEnum {
    val rawValue: Int
}

class IntEnumSerializer<T>(private val values: EnumEntries<T>) : KSerializer<T> where T : Enum<T>, T : IntEnum {
    private val enumClassName =
        values.firstOrNull()?.let { it::class.simpleName } ?: "Enum"

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(enumClassName, PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeInt(value.rawValue)
    }

    override fun deserialize(decoder: Decoder): T {
        val intValue = decoder.decodeInt()
        return values.firstOrNull { it.rawValue == intValue }
            ?: throw SerializationException("Unknown enum value: $intValue for enum class $enumClassName")
    }
}
