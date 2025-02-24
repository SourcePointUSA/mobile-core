package com.sourcepoint.mobile_core.network.responses

import kotlinx.serialization.Serializable

@Serializable
data class PvDataResponse (
    val gdpr: Campaign? = null,
    val ccpa: Campaign? = null,
    val usnat: Campaign? = null
) {
    @Serializable
    data class Campaign (
        val uuid: String
    )
}
