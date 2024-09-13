package com.sourcepoint.mobile_core.network.requests

import com.sourcepoint.core.BuildConfig
import com.sourcepoint.mobile_core.Device
import kotlinx.serialization.Serializable

@Suppress("unused") // properties are used in every request inheriting from DefaultRequest
@Serializable
open class DefaultRequest {
    val env = "prod"
    val scriptType = "mobile-core-${Device.osName.name}"
    val scriptVersion = BuildConfig.Version
}
