package com.sourcepoint.mobile_core.network.responses

import kotlinx.serialization.Serializable

@Serializable
data class PvDataResponse (
    val gdpr: Campaign? = null,
    val ccpa: Campaign? = null,
    val usnat: Campaign? = null,
    val globalcmp: Campaign? = null,
    val preferences: PreferencesCampaign? = null // TODO: Preferences pvData response causes error, change type to Campaign? when preferences pvData returns uuid
) {
    @Serializable
    data class Campaign (
        val uuid: String
    )

    @Serializable
    data class PreferencesCampaign (
        val uuid: String?
    )
}
