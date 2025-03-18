package com.sourcepoint.mobile_core.network.requests

import com.sourcepoint.mobile_core.network.json
import com.sourcepoint.mobile_core.network.jsonWithNulls
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import kotlin.reflect.typeOf

// Encodes a @Serializable class into query params. Param names will maintain the class' json
// naming. Primitive types are directly converted to string. Objects are converted to JsonObject
internal inline fun <reified T> T.toQueryParams(omitNulls: Boolean = true): Map<String, String?> {
    val jsonFormater = if (omitNulls) json else jsonWithNulls

    return jsonFormater.encodeToJsonElement(serializer(typeOf<T>()), this)
        .jsonObject
        .entries
        .fold(mutableMapOf()) { entries, entry ->
            val value = entry.value
            entries[entry.key] = when (value) {
                is JsonNull -> null
                is JsonObject -> jsonFormater.encodeToString(value)
                else -> (value as? JsonPrimitive)?.content
            }
            entries
        }
}
