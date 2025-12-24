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
        get() = storage.withLock {
            keys
                .filter { it.startsWith(TCF_PREFIX) }
                .associateWith { this[it]!! }
        }
        set(value) {
            storage.withLock {
                removeKeysStartingWith(prefix = TCF_PREFIX)
                value.entries.forEach { this[it.key] = it.value }
            }
        }

    var gppData: IABData
        get() = storage.withLock {
            keys
                .filter { it.startsWith(GPP_PREFIX) }
                .associateWith { this[it]!! }
        }
        set(value) {
            storage.withLock {
                removeKeysStartingWith(prefix = GPP_PREFIX)
                value.entries.forEach { this[it.key] = it.value }
            }
        }

    var uspString: String?
        get() = storage.withLock { this[USPSTRING_KEY] }
        set(value) { storage.withLock { this[USPSTRING_KEY] = value } }

    var state: State?
        get() = runCatching {
            storage.withLock {
                Json.decodeFromString<State>(getString(SP_STATE_KEY, defaultValue = ""))
            }
        }.getOrNull()
        set(value) {
            storage.withLock { this[SP_STATE_KEY] = Json.encodeToString(value) }
        }

    fun clear() {
        storage.withLock {
            removeKeysStartingWith(prefix = TCF_PREFIX)
            removeKeysStartingWith(prefix = GPP_PREFIX)
            remove(USPSTRING_KEY)
            remove(SP_STATE_KEY)
        }
    }
}
