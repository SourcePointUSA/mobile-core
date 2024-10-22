package com.sourcepoint.mobile_core.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

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
