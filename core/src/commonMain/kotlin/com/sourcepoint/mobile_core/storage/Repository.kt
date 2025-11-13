package com.sourcepoint.mobile_core.storage

import com.russhwolf.settings.Settings
import com.sourcepoint.mobile_core.models.consents.IABData
import com.sourcepoint.mobile_core.models.consents.State
import kotlinx.serialization.json.Json

class Repository(private val storage: Settings) {
    constructor(): this(storage = getStorageOrDefault())

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
            value.entries.forEach { storage[it.key] = it.value }
        }

    var gppData: IABData
        get() = storage.keys
            .filter { it.startsWith(GPP_PREFIX) }
            .associateWith { storage[it]!! }
        set(value) {
            storage.removeKeysStartingWith(prefix = GPP_PREFIX)
            value.entries.forEach { storage[it.key] = it.value }
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
