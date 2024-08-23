package com.sourcepoint.mobile_core.network.requests

import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import com.sourcepoint.mobile_core.models.SPCampaignEnv
import com.sourcepoint.mobile_core.models.SPIDFAStatus
import com.sourcepoint.mobile_core.models.SPMessageLanguage
import com.sourcepoint.mobile_core.models.SPPropertyName
import com.sourcepoint.mobile_core.models.SPTargetingParams
import kotlinx.serialization.Serializable

@Serializable
data class Messages(
    val body: Body,
    val metadata: MetaData,
    val nonKeyedLocalState: String?,
    val localState: String?
): DefaultRequest() {
    @Serializable
    data class Body(
        val propertyHref: SPPropertyName,
        val accountId: Int,
        val campaigns: Campaigns,
        val consentLanguage: SPMessageLanguage,
        val hasCSP: Boolean = true,
        val campaignEnv: SPCampaignEnv?,
        val idfaStatus: SPIDFAStatus? = SPIDFAStatus.current(),
        val includeData: IncludeData = IncludeData()
    ) {
        @Serializable
        data class Campaigns(
            val gdpr: GDPR?,
            val ios14: IOS14?,
            val usnat: USNat?
        ) {
            @Serializable
            data class GDPR(
                val targetingParams: SPTargetingParams?,
                val hasLocalData: Boolean,
                val consentStatus: ConsentStatus
            )

            @Serializable
            data class IOS14(
                val targetingParams: SPTargetingParams?,
                val idfaStatus: SPIDFAStatus
            )

            @Serializable
            data class USNat(
                val targetingParams: SPTargetingParams?,
                val hasLocalData: Boolean,
                val consentStatus: ConsentStatus
            )
        }
    }

    @Serializable
    data class MetaData(val gdpr: Campaign?, val usnat: Campaign?) {
        @Serializable
        data class Campaign(val applies: Boolean)
    }
}
