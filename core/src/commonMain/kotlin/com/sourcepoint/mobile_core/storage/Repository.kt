package com.sourcepoint.mobile_core.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import com.sourcepoint.mobile_core.models.consents.IABData
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
        SPState,
        UspString, // TODO: use the IAB naming convention
    }

    var cachedTcData: IABData?
        get() = Json.decodeFromString<IABData>(storage[Keys.TcData.name, "{}"])
        set(value) { if (value != null) storage[Keys.TcData.name] = Json.encodeToString(MapSerializer(String.serializer(),JsonPrimitive.serializer()), value) }
    var cachedGppData: IABData?
        get() = Json.decodeFromString<IABData>(storage[Keys.GppData.name, "{}"])
        set(value) { if (value != null) storage[Keys.GppData.name] = Json.encodeToString(MapSerializer(String.serializer(),JsonPrimitive.serializer()), value) }
    var cachedSPState: State?
        get() = try { Json.decodeFromString<State>(storage[Keys.SPState.name, "{}"]) } catch (error: Throwable) { null }
        set(value) { storage[Keys.SPState.name] = Json.encodeToString(value) }
    var cachedUspString: String?
        get() = storage[Keys.UspString.name, ""]
        set(value) { storage[Keys.UspString.name] = value }

    fun clear() {
        storage.remove(Keys.TcData.name)
        storage.remove(Keys.GppData.name)
        storage.remove(Keys.UspString.name)
        storage.remove(Keys.SPState.name)
    }
}
