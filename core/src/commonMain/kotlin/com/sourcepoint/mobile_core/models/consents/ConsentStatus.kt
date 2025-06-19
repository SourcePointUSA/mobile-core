package com.sourcepoint.mobile_core.models.consents

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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
    var granularStatus: ConsentStatusGranularStatus? = null,
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
        var gpcStatus: Boolean? = null,
        val systemCategories: JsonElement? = null,
        val customCategories: JsonElement? = null,
    )
}
