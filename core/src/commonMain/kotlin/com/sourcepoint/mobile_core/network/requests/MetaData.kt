package com.sourcepoint.mobile_core.network.requests

import kotlinx.serialization.Serializable

@Serializable
data class MetaData(
    val accountId: Int,
    val propertyId: Int,
    val metadata: Campaigns
): DefaultRequest() {
    @Serializable
    data class Campaigns(val gdpr: Campaign? = null, val usnat: Campaign? = null) {
        @Serializable
        data class Campaign(val groupPmId: String? = null)
    }
}