package com.sourcepoint.mobile_core.network.requests

import com.sourcepoint.mobile_core.models.SPIDFAStatus
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ConsentStatusRequest(
    val propertyId: Int,
    val metadata: MetaData,
    val includeData: IncludeData = IncludeData(),
    val authId: String? = null,
    val withSiteActions: Boolean = false,
    val hasCsp: Boolean = true
): DefaultRequest() {
    @Serializable
    data class MetaData(
        val gdpr: Campaign? = null,
        val usnat: USNatCampaign? = null,
        val ccpa: Campaign? = null,
        val preferences: PreferencesCampaign? = null
    ) {
        @Serializable
        data class Campaign(
            val applies: Boolean,
            val dateCreated: Instant? = null,
            val uuid: String? = null,
            val hasLocalData: Boolean = false, // TODO check when this can be true
            val idfaStatus: SPIDFAStatus? = SPIDFAStatus.current()
        )

        @Serializable
        data class USNatCampaign(
            val applies: Boolean,
            val dateCreated: Instant? = null,
            val uuid: String? = null,
            val hasLocalData: Boolean = false, // TODO check when this can be true
            val idfaStatus: SPIDFAStatus? = SPIDFAStatus.current(),
            val transitionCCPAAuth: Boolean? = null,
            val optedOut: Boolean? = null
        )

        @Serializable
        data class PreferencesCampaign(
            val hasLocalData: Boolean = false, // TODO check when this can be true
            val configurationId: String? = null
        )
    }
}
