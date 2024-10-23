package com.sourcepoint.mobile_core.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.json.JsonObject

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
