package com.sourcepoint.mobile_core.network.responses

import kotlinx.serialization.Serializable

@Serializable
data class PvDataResponse (
    val gdpr: Campaign?,
    val ccpa: Campaign?,
    val usnat: Campaign?
) {
    @Serializable
    data class Campaign (
        val uuid: String
    )
}
