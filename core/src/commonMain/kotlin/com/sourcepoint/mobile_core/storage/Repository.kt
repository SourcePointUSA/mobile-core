package com.sourcepoint.mobile_core.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import com.sourcepoint.mobile_core.models.SPJson
import com.sourcepoint.mobile_core.models.consents.IABData
import com.sourcepoint.mobile_core.models.consents.SPUserData
import com.sourcepoint.mobile_core.models.consents.State
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive

class Repository(private val storage: Settings) {
    constructor(): this(storage = Settings())
    enum class Keys {
        TcData,
        GppData,
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
    var cachedUserData: SPUserData?
        get() = try { Json.decodeFromString<SPUserData>(storage[Keys.UserData.name, ""]) } catch (error: Throwable) { null }
        set(value) { storage[Keys.UserData.name] = Json.encodeToString(value) }
    var cachedLocalState: SPJson?
        get() = storage[Keys.LocalState.name, ""]
        set(value) { storage[Keys.LocalState.name] = value }
    var cachedGdprChildPmId: String?
        get() = storage[Keys.GdprChildPmId.name, ""]
        set(value) { storage[Keys.GdprChildPmId.name] = value }
    var cachedCcpaChildPmId: String?
        get() = storage[Keys.CcpaChildPmId.name, ""]
        set(value) { storage[Keys.CcpaChildPmId.name] = value }
    var cachedSPState: State?
        get() = try { Json.decodeFromString<State>(storage[Keys.SPState.name, ""]) } catch (error: Throwable) { null }
        set(value) { storage[Keys.SPState.name] = Json.encodeToString(value) }
}
