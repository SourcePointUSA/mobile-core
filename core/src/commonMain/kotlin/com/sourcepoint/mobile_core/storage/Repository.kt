package com.sourcepoint.mobile_core.storage

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.serialization.decodeValueOrNull
import com.russhwolf.settings.serialization.encodeValue
import com.russhwolf.settings.set
import com.sourcepoint.mobile_core.models.SPJson
import com.sourcepoint.mobile_core.models.consents.IABData
import com.sourcepoint.mobile_core.models.consents.SPUserData
import com.sourcepoint.mobile_core.models.consents.State
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive

class Repository(private val storage: Settings) {
    constructor(): this(storage = Settings())
    enum class Keys {
        TcData,
        GppData,
        UspString,
        UserData,
        LocalState,
        GdprChildPmId,
        CcpaChildPmId,
        SPState
    }

    var cachedTcData: IABData?
        get() = Json.decodeFromString<IABData>(storage[Keys.TcData.name, ":"])
        set(value) { if (value != null) storage[Keys.TcData.name] = Json.encodeToString(MapSerializer(String.serializer(),JsonPrimitive.serializer()), value) }
    var cachedGppData: IABData?
        get() = Json.decodeFromString<IABData>(storage[Keys.GppData.name, ":"])
        set(value) { if (value != null) storage[Keys.GppData.name] = Json.encodeToString(MapSerializer(String.serializer(),JsonPrimitive.serializer()), value) }
    var cachedUspString: String?
        get() = storage[Keys.UspString.name, ""]
        set(value) { storage[Keys.UspString.name] = value }
    @OptIn(ExperimentalSerializationApi::class, ExperimentalSettingsApi::class)
    var cachedUserData: SPUserData?
        get() = storage.decodeValueOrNull(SPUserData.serializer(), Keys.UserData.name)
        set(value) { if (value != null) storage.encodeValue(SPUserData.serializer(), Keys.UserData.name, value) }
    var cachedLocalState: SPJson?
        get() = storage[Keys.LocalState.name, ""]
        set(value) { storage[Keys.LocalState.name] = value }
    var cachedGdprChildPmId: String?
        get() = storage[Keys.GdprChildPmId.name, ""]
        set(value) { storage[Keys.GdprChildPmId.name] = value }
    var cachedCcpaChildPmId: String?
        get() = storage[Keys.CcpaChildPmId.name, ""]
        set(value) { storage[Keys.CcpaChildPmId.name] = value }
    @OptIn(ExperimentalSerializationApi::class, ExperimentalSettingsApi::class)
    var cachedSPState: State?
        get() = storage.decodeValueOrNull(State.serializer(), Keys.SPState.name)
        set(value) { if (value != null) storage.encodeValue(State.serializer(), Keys.SPState.name, value) }
}
