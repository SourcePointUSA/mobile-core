package com.sourcepoint.mobile_core.network.responses

import kotlinx.serialization.Serializable

@Serializable
data class UsnatLocationResponse(
    val countryCode: String? = null,
    val stateCode: String? = null,
    val regionCode: String? = null
)
