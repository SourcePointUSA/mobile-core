package com.sourcepoint.mobile_core.models.consents

import com.sourcepoint.mobile_core.utils.inOneYear
import com.sourcepoint.mobile_core.utils.now
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CCPAConsent(
    val applies: Boolean = false,
    val uuid: String? = null,
    val dateCreated: Instant = now(),
    val expirationDate: Instant = dateCreated.inOneYear(),
    val signedLspa: Boolean? = null,
    val rejectedAll: Boolean = false,
    val consentedAll: Boolean = false,
    val rejectedVendors: List<String> = emptyList(),
    val rejectedCategories: List<String> = emptyList(),
    val status: CCPAConsentStatus? = null,
    val webConsentPayload: String? = null,
    @SerialName("GPPData") var gppData: IABData = emptyMap(),
) {
    val uspstring: String get() {
        val version = 1
        val hadChanceToOptOut = true

        return if (applies) {
            "$version" +
                (if (hadChanceToOptOut) "Y" else "N") +
                (if (status == CCPAConsentStatus.RejectedAll || status == CCPAConsentStatus.RejectedSome) "Y" else "N") +
                (if (signedLspa == true) "Y" else "N")
        } else {
            "$version---"
        }
    }

    @Serializable
    enum class CCPAConsentStatus {
        @SerialName("consentedAll") ConsentedAll,
        @SerialName("rejectedAll") RejectedAll,
        @SerialName("rejectedSome") RejectedSome,
        @SerialName("rejectedNone") RejectedNone,
        @SerialName("linkedNoAction") LinkedNoAction
    }
}
