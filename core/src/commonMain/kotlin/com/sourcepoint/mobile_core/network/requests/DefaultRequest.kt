package com.sourcepoint.mobile_core.network.requests

import com.sourcepoint.core.BuildConfig
import kotlinx.serialization.Serializable

@Serializable
open class DefaultRequest {
    val env: String = ENV_PARAM
    val scriptType: String = SCRIPT_TYPE
    val scriptVersion: String = SCRIPT_VERSION

    companion object {
        const val ENV_PARAM = "prod"
        const val SCRIPT_TYPE = "mobile-core" // TODO: postfix it with the platform the module is running
        const val SCRIPT_VERSION = BuildConfig.Version
    }
}
