package com.sourcepoint.mobile_core.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral

@OptIn(ExperimentalSerializationApi::class)
val json = Json {
    decodeEnumsCaseInsensitive = true
    encodeDefaults = true
    ignoreUnknownKeys = true
    explicitNulls = false
}

@OptIn(ExperimentalSerializationApi::class)
val jsonWithNulls = Json {
    decodeEnumsCaseInsensitive = true
    encodeDefaults = true
    ignoreUnknownKeys = true
    explicitNulls = true
}

fun String?.encodeToJsonObject() = this?.let { decodeFromString<JsonObject>(it) }

@OptIn(ExperimentalSerializationApi::class)
fun Any?.toJsonPrimitive(): JsonPrimitive = when (this) {
    null -> JsonNull
    is Number -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    else -> JsonPrimitive(this.toString())
}

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

