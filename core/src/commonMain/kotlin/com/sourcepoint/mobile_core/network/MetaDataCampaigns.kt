package com.sourcepoint.mobile_core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class MetaDataCampaigns(
    val gdpr: Campaign? = null,
    val usnat: Campaign? = null,
) {
    @Serializable
    data class Campaign(val groupPmId: String? = null)

    /// NOTE: this is important because the kotlin open API generator
    /// doesn't seem to encode query param objects correctly.
    override fun toString() = Json.encodeToString(this)
}
