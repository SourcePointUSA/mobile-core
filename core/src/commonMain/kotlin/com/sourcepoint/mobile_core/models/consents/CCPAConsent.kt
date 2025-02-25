package com.sourcepoint.mobile_core.models.consents

import com.sourcepoint.mobile_core.utils.inOneYear
import com.sourcepoint.mobile_core.utils.now
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO: implement USPString logic
@Serializable
data class CCPAConsent(
    val applies: Boolean = false,
    val uuid: String? = null,
    val dateCreated: Instant = now(),
    val expirationDate: Instant = dateCreated.inOneYear(),
    val signedLspa: Boolean? = null,
    val uspstring: String? = null,
    val childPmId: String? = null,
    val rejectedAll: Boolean = false,
    val consentedAll: Boolean = false,
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
