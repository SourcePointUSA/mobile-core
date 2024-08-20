package com.sourcepoint.mobile_core.models

import kotlinx.serialization.Serializable

@Serializable
data class ConsentStatus (
    val rejectedAny: Boolean?,
    val rejectedLI: Boolean?,
    val consentedAll: Boolean?,
    val hasConsentData: Boolean?,
    val consentedToAny: Boolean?,
    val granularStatus: ConsentStatusGranularStatus?
) {
    @Serializable
    data class ConsentStatusGranularStatus (
        val vendorConsent: String?,
        val vendorLegInt: String?,
        val purposeConsent: String?,
        val purposeLegInt: String?,
        val previousOptInAll: Boolean?,
        val defaultConsent: Boolean?
    )
}
