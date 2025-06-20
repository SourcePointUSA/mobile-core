package com.sourcepoint.mobile_core.network.responses

import com.sourcepoint.mobile_core.models.consents.CCPAConsent
import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import com.sourcepoint.mobile_core.models.consents.ConsentStrings
import com.sourcepoint.mobile_core.models.consents.GDPRConsent
import com.sourcepoint.mobile_core.models.consents.IABData
import com.sourcepoint.mobile_core.models.consents.SPGDPRVendorGrants
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class ChoiceAllResponse (
    val gdpr: GDPR? = null,
    val ccpa: CCPA? = null,
    val usnat: USNAT? = null,
    val globalcmp: GLOBALCMP? = null
) {
    @Serializable
    data class GDPR (
        val addtlConsent: String?,
        val childPmId: String?,
        val euconsent: String,
        val hasLocalData: Boolean?,
        val dateCreated: Instant?,
        val expirationDate: Instant?,
        @SerialName("TCData") val tcData: IABData? = emptyMap(),
        val consentStatus: ConsentStatus,
        val grants: SPGDPRVendorGrants,
        val postPayload: PostPayload?,
        val webConsentPayload: String?,
        val gcmStatus: GDPRConsent.GCMStatus?,
        @SerialName("legIntCategories") val acceptedLegIntCategories: List<String>,
        @SerialName("legIntVendors") val acceptedLegIntVendors: List<String>,
        @SerialName("vendors") val acceptedVendors: List<String>,
        @SerialName("categories") val acceptedCategories: List<String>,
        @SerialName("specialFeatures") val acceptedSpecialFeatures: List<String>
    ) {
        @Serializable
        data class PostPayload (
            val consentAllRef: String?,
            val vendorListId: String,
            val granularStatus : ConsentStatus.ConsentStatusGranularStatus?
        )
    }

    @Serializable
    data class CCPA (
        val consentedAll: Boolean,
        val dateCreated: Instant?,
        val expirationDate: Instant?,
        val rejectedAll: Boolean,
        val status: CCPAConsent.CCPAConsentStatus,
        val rejectedVendors: List<String>,
        val rejectedCategories: List<String>,
        val gpcEnabled: Boolean?,
        val webConsentPayload: String?,
        @SerialName("GPPData") val gppData: IABData = emptyMap()
    )

    @Serializable
    data class USNAT (
        val categories: List<String>,
        val consentStatus: ConsentStatus,
        val consentStrings: ConsentStrings = emptyList(),
        val consentedToAll: Boolean,
        val dateCreated: Instant?,
        val expirationDate: Instant?,
        val rejectedAny: Boolean,
        val gpcEnabled: Boolean?,
        val webConsentPayload: String?,
        @SerialName("GPPData") val gppData: IABData = emptyMap()
    )

    @Serializable
    data class GLOBALCMP (
        val categories: List<String>,
        val consentStatus: ConsentStatus,
        val consentedToAll: Boolean,
        val dateCreated: Instant?,
        val expirationDate: Instant?,
        val rejectedAny: Boolean,
        val gpcEnabled: Boolean?,
        val webConsentPayload: String?,
    )
}
