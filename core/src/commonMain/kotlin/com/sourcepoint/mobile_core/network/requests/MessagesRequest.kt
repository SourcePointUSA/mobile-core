package com.sourcepoint.mobile_core.network.requests

import com.sourcepoint.mobile_core.models.SPCampaignEnv
import com.sourcepoint.mobile_core.models.SPIDFAStatus
import com.sourcepoint.mobile_core.models.SPMessageLanguage
import com.sourcepoint.mobile_core.models.SPPropertyName
import com.sourcepoint.mobile_core.models.SPTargetingParams
import com.sourcepoint.mobile_core.models.consents.CCPAConsent
import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import kotlinx.serialization.Serializable

@Serializable
data class MessagesRequest(
    val body: Body,
    val metadata: MetaData,
    val nonKeyedLocalState: String? = null,
    val localState: String? = null
): DefaultRequest() {
    @Serializable
    data class Body(
        val propertyHref: SPPropertyName,
        val accountId: Int,
        val campaigns: Campaigns,
        val consentLanguage: SPMessageLanguage? = null,
        val hasCSP: Boolean = true,
        val campaignEnv: SPCampaignEnv = SPCampaignEnv.PUBLIC,
        val idfaStatus: SPIDFAStatus? = SPIDFAStatus.current(),
        val includeData: IncludeData = IncludeData()
    ) {
        @Serializable
        data class Campaigns(
            val gdpr: Campaign? = null,
            val ios14: IOS14Campaign? = null,
            val ccpa: CCPACampaign? = null,
            val globalcmp: Campaign? = null,
            val usnat: Campaign? = null,
            val preferences: Campaign? = null
        ) {
            @Serializable
            data class Campaign(
                val targetingParams: SPTargetingParams? = null,
                val hasLocalData: Boolean = false,
                val consentStatus: ConsentStatus = ConsentStatus()
            )

            @Serializable
            data class IOS14Campaign(
                val targetingParams: SPTargetingParams? = null,
                val idfaStatus: SPIDFAStatus? = null
            )

            @Serializable
            data class CCPACampaign(
                val targetingParams: SPTargetingParams? = null,
                val hasLocalData: Boolean,
                val status: CCPAConsent.CCPAConsentStatus? = null
            )
        }
    }

    @Serializable
    data class MetaData(
        val gdpr: Campaign? = null,
        val globalcmp: Campaign? = null,
        val usnat: Campaign? = null,
        val ccpa: Campaign? = null
    ) {
        @Serializable
        data class Campaign(val applies: Boolean)
    }
}
