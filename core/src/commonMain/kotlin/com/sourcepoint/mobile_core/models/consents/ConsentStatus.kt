package com.sourcepoint.mobile_core.models.consents

import kotlinx.serialization.Serializable

@Serializable
data class ConsentStatus (
    val rejectedAny: Boolean? = null,
    val rejectedLI: Boolean? = null,
    val rejectedAll: Boolean? = null,
    val consentedAll: Boolean? = null,
    val consentedToAll: Boolean? = null,
    val consentedToAny: Boolean? = null,
    val hasConsentData: Boolean? = null,
    val vendorListAdditions: Boolean? = null,
    val legalBasisChanges: Boolean? = null,
    val granularStatus: ConsentStatusGranularStatus? = null,
    val rejectedVendors: List<String?>? = emptyList(),
    val rejectedCategories: List<String?>? = emptyList()
) {
    @Serializable
    data class ConsentStatusGranularStatus (
        val vendorConsent: String? = null,
        val vendorLegInt: String? = null,
        val purposeConsent: String? = null,
        val purposeLegInt: String? = null,
        val previousOptInAll: Boolean? = null,
        val defaultConsent: Boolean? = null,
        val sellStatus: Boolean? = null,
        val shareStatus: Boolean? = null,
        val sensitiveDataStatus: Boolean? = null,
        val gpcStatus: Boolean? = null
    )
}
