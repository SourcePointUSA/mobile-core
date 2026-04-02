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
        get() = storage.getKeysWithPrefix(TCF_PREFIX)
        set(value) = storage.replaceKeysWithPrefix(TCF_PREFIX, value)

    var gppData: IABData
        get() = storage.getKeysWithPrefix(GPP_PREFIX)
        set(value) = storage.replaceKeysWithPrefix(GPP_PREFIX, value)

    var uspString: String?
        get() = storage.readStringOrNull(USPSTRING_KEY)
        set(value) = storage.writeString(USPSTRING_KEY, value)

    var state: State?
        get() = runCatching {
            val json = storage.readString(SP_STATE_KEY, defaultValue = "")
            if (json.isBlank()) null else Json.decodeFromString<State>(json)
        }.getOrNull()
        set(value) {
            val json = value?.let { Json.encodeToString(it) }
            storage.writeString(SP_STATE_KEY, json)
        }

    fun clear() {
        storage.removeKeysStartingWith(TCF_PREFIX)
        storage.removeKeysStartingWith(GPP_PREFIX)
        storage.delete(USPSTRING_KEY)
        storage.delete(SP_STATE_KEY)
    }
}
