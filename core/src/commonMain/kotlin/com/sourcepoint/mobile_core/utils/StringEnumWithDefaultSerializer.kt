package com.sourcepoint.mobile_core.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.enums.EnumEntries

open class StringEnumWithDefaultSerializer<T>(
    private val values: EnumEntries<T>,
    private val default: T
) : KSerializer<T> where T : Enum<T> {
    private val enumClassName =
        values.firstOrNull()?.let { it::class.simpleName } ?: "Enum"

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(enumClassName, PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): T =
        values.find { it.name == decoder.decodeString() } ?: default
}
