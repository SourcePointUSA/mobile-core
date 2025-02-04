package com.sourcepoint.mobile_core.models.consents

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO: implement USPString logic
@Serializable
data class CCPAConsent(
    val applies: Boolean = false,
    val uuid: String? = null,
    val dateCreated: String? = null,
    val expirationDate: String? = null,
    val signedLspa: Boolean? = null,
    val uspstring: String? = null,
    val childPmId: String? = null,
    val rejectedAll: Boolean? = null,
    val consentedAll: Boolean? = null,
    val rejectedVendors: List<String> = emptyList(),
    val rejectedCategories: List<String> = emptyList(),
    val status: CCPAConsentStatus? = null,
    val webConsentPayload: String? = null,
    @SerialName("GPPData") var gppData: IABData = emptyMap(),
) {
    @Serializable
    enum class CCPAConsentStatus {
        @SerialName("consentedAll") ConsentedAll,
        @SerialName("rejectedAll") RejectedAll,
        @SerialName("rejectedSome") RejectedSome,
        @SerialName("rejectedNone") RejectedNone,
        @SerialName("linkedNoAction") LinkedNoAction
    }
}
