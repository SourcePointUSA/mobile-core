package com.sourcepoint.mobile_core.models.consents

import kotlinx.serialization.Serializable

@Serializable
data class ConsentStatus (
    val rejectedAny: Boolean? = null,
    val rejectedLI: Boolean? = null,
    val consentedAll: Boolean? = null,
    val hasConsentData: Boolean? = null,
    val consentedToAny: Boolean? = null,
    val granularStatus: ConsentStatusGranularStatus? = null
) {
    @Serializable
    data class ConsentStatusGranularStatus (
        val vendorConsent: String? = null,
        val vendorLegInt: String? = null,
        val purposeConsent: String? = null,
        val purposeLegInt: String? = null,
        val previousOptInAll: Boolean? = null,
        val defaultConsent: Boolean? = null
    )
}
