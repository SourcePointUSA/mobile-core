package com.sourcepoint.mobile_core.network.requests

import com.sourcepoint.mobile_core.models.SPIDFAStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChoiceAllRequest (
    val accountId: Int,
    val propertyId: Int,
    @SerialName("metadata") val campaigns: ChoiceAllCampaigns,
    val idfaStatus: SPIDFAStatus? = SPIDFAStatus.current(),
    val includeData: IncludeData? = IncludeData(),
    private val hasCsp: Boolean = true,
    private val withSiteActions: Boolean = false,
    private val includeCustomVendorsRes: Boolean = false
): DefaultRequest() {
    @Serializable
    data class ChoiceAllCampaigns (
        val gdpr: Campaign?,
        val ccpa: Campaign?,
        val usnat: Campaign?
    ) {
        @Serializable
        data class Campaign (val applies: Boolean)
    }
}
