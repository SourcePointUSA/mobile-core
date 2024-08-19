package com.sourcepoint.mobile_core.network.requests

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class MetaDataMetaDataCampaigns(
    val gdpr: Campaign? = null,
    val usnat: Campaign? = null,
) {
    @Serializable
    data class Campaign(val groupPmId: String? = null)

    override fun toString() = Json.encodeToString(this)
}
