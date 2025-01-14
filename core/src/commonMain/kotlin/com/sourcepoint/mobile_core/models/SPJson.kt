package com.sourcepoint.mobile_core.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonUnquotedLiteral

typealias SPJson = @Serializable(with = SPJsonSerializer::class) String

class SPJsonSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SPJson", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String = when (decoder) {
        is JsonDecoder -> decoder.decodeJsonElement().toString()
        else -> decoder.decodeString()
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: String) = when (encoder) {
        is JsonEncoder -> encoder.encodeJsonElement(JsonUnquotedLiteral(value))
        else -> encoder.encodeString(value)
    }
}
