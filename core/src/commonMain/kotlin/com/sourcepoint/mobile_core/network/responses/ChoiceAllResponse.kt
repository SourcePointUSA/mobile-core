package com.sourcepoint.mobile_core.network.responses

import com.sourcepoint.mobile_core.models.consents.CCPAConsent
import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import com.sourcepoint.mobile_core.models.consents.ConsentStrings
import com.sourcepoint.mobile_core.models.consents.GDPRConsent
import com.sourcepoint.mobile_core.models.consents.IABData
import com.sourcepoint.mobile_core.models.consents.SPGDPRVendorGrants
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChoiceAllResponse (
    val gdpr: GDPR?,
    val ccpa: CCPA?,
    val usnat: USNAT?
) {
    @Serializable
    data class GDPR (
        val addtlConsent: String?,
        val childPmId: String?,
        val euconsent: String,
        val hasLocalData: Boolean?,
        val dateCreated: String?,
        val expirationDate: String?,
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
        val dateCreated: String?,
        val expirationDate: String?,
        val rejectedAll: Boolean,
        val status: CCPAConsent.CCPAConsentStatus,
        @SerialName("uspString") val uspstring: String,
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
        val dateCreated: String?,
        val expirationDate: String?,
        val rejectedAny: Boolean,
        val gpcEnabled: Boolean?,
        val webConsentPayload: String?,
        @SerialName("GPPData") val gppData: IABData = emptyMap()
    )
}
