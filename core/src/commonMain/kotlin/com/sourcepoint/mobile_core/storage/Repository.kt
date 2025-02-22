package com.sourcepoint.mobile_core.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import com.sourcepoint.mobile_core.models.consents.IABData
import com.sourcepoint.mobile_core.models.consents.State
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

class Repository(private val storage: Settings) {
    constructor(): this(storage = Settings())

    companion object {
        const val TCF_PREFIX = "IABTCF_"
        const val GPP_PREFIX = "IABGPP_"
        const val USPSTRING_KEY = "IABUSPrivacy_String"
        const val SP_STATE_KEY = "sp.state"
    }

    var tcData: IABData
        get() = storage.keys
            .filter { it.startsWith(TCF_PREFIX) }
            .associateWith { storage[it]!! }
        set(value) {
            storage.removeKeysStartingWith(prefix = TCF_PREFIX)
            value.entries.forEach {
                storage.setJsonPrimitive(it.key, it.value)
            }
        }

    var gppData: IABData
        get() = storage.keys
            .filter { it.startsWith(GPP_PREFIX) }
            .associateWith { storage[it]!! }
        set(value) {
            storage.removeKeysStartingWith(prefix = GPP_PREFIX)
            value.entries.forEach {
                storage.setJsonPrimitive(it.key, it.value)
            }
        }

    var uspString: String?
        get() = storage[USPSTRING_KEY]
        set(value) { storage[USPSTRING_KEY] = value }

    var state: State?
        get() = runCatching {
            Json.decodeFromString<State>(storage.getString(SP_STATE_KEY, defaultValue = ""))
        }.getOrNull()
        set(value) { storage[SP_STATE_KEY] = Json.encodeToString(value) }

    fun clear() {
        storage.removeKeysStartingWith(prefix = TCF_PREFIX)
        storage.removeKeysStartingWith(prefix = GPP_PREFIX)
        storage.remove(USPSTRING_KEY)
        storage.remove(SP_STATE_KEY)
    }
}

fun Settings.removeKeysStartingWith(prefix: String) {
    keys.filter { it.startsWith(prefix) }.forEach { remove(it) }
}

fun Settings.setJsonPrimitive(key: String, value: JsonPrimitive) {
    when {
        value.isString -> set(key, value.content)
        value.booleanOrNull != null -> set(key, value.boolean)
        value.intOrNull != null -> set(key, value.int)
        value.longOrNull != null -> set(key, value.long)
        value.doubleOrNull != null -> set(key, value.double)
        value.floatOrNull != null -> set(key, value.float)
        value is JsonNull -> set(key, null)
    }
}
