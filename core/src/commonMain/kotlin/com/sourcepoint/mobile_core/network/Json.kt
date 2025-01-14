package com.sourcepoint.mobile_core.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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
