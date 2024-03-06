package com.sourcepoint.mobile_core.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set

class Repository(private val storage: Settings) {
    constructor(): this(storage = Settings())
    enum class Keys {
        MetaData
    }

    var cachedMetaData: String
        get() = storage[Keys.MetaData.name, ""]
        set(value) { storage[Keys.MetaData.name] = value }
}