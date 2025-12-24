package com.sourcepoint.mobile_core.storage

import com.russhwolf.settings.Settings
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized
import com.russhwolf.settings.set as originalSet
import com.russhwolf.settings.get as originalGet
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

@OptIn(InternalCoroutinesApi::class)
private val settingsLock = SynchronizedObject()

@OptIn(InternalCoroutinesApi::class)
internal fun <T> synchronizedLock(lock: SynchronizedObject, block: () -> T): T = synchronized(lock, block)

@OptIn(InternalCoroutinesApi::class)
internal fun <T> Settings.withLock(block: Settings.() -> T): T = synchronizedLock(settingsLock) {
    block()
}

internal fun Settings.removeKeysStartingWith(prefix: String) = withLock {
    val toRemove = keys.filter { it.startsWith(prefix) }
    toRemove.forEach { remove(it) }
}

internal operator fun Settings.set(key: String, value: JsonPrimitive) = putJsonPrimitive(key, value)

internal fun Settings.putJsonPrimitive(key: String, value: JsonPrimitive) {
    when {
        value.isString -> originalSet(key, value.content)
        value.intOrNull != null -> originalSet(key, value.int)
        value.booleanOrNull != null -> originalSet(key, value.boolean)
        value.longOrNull != null -> originalSet(key, value.long)
        value.floatOrNull != null && value.floatOrNull?.isFinite() == true -> originalSet(key, value.float)
        value.doubleOrNull != null -> originalSet(key, value.double)
        value is JsonNull -> originalSet(key, null)
    }
}

internal inline operator fun <reified T : Any> Settings.set(key: String, value: T?) = when (T::class) {
    JsonPrimitive::class -> putJsonPrimitive(key, value as JsonPrimitive)
    else -> originalSet(key, value)
}

internal fun Settings.getJsonPrimitive(key: String): JsonPrimitive {
    getStringOrNull(key)?.let { return JsonPrimitive(it) }
    getIntOrNull(key)?.let { return JsonPrimitive(it) }
    getBooleanOrNull(key)?.let { return JsonPrimitive(it) }
    getLongOrNull(key)?.let { return JsonPrimitive(it) }
    getFloatOrNull(key)?.let { return JsonPrimitive(it) }
    getDoubleOrNull(key)?.let { return JsonPrimitive(it) }
    return JsonNull
}

internal inline operator fun <reified T : Any> Settings.get(key: String): T? = when (T::class) {
    JsonPrimitive::class -> getJsonPrimitive(key) as T?
    else -> originalGet(key)
}
