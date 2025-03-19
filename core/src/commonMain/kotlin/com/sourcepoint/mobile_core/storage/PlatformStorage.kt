package com.sourcepoint.mobile_core.storage

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings

expect fun getStorage(): Settings

fun getStorageOrDefault() = try {
    getStorage()
} catch (e: NotImplementedError) {
    MapSettings()
} catch (e: NullPointerException) {
    MapSettings()
}
