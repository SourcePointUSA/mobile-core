package com.sourcepoint.mobile_core.network.requests

import com.sourcepoint.core.BuildConfig
import com.sourcepoint.mobile_core.DeviceInformation
import kotlinx.serialization.Serializable

@Serializable
open class DefaultRequest(val geoOverride: String? = null) {
    val env = "prod"
    val scriptType = "mobile-core-${DeviceInformation().osName.name}"
    val scriptVersion = BuildConfig.Version
}
