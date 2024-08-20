package com.sourcepoint.mobile_core.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    explicitNulls = false
}

@OptIn(ExperimentalSerializationApi::class)
val jsonWithNulls = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    explicitNulls = true
}