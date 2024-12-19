package com.sourcepoint.mobile_core.models.consents

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GDPRConsent (
    var dateCreated: String? = null,
    var expirationDate: String? = null,
    val uuid: String? = null,
    var euconsent: String? = null,
    val legIntCategories: List<String> = emptyList(),
    val legIntVendors: List<String> = emptyList(),
    val vendors: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val specialFeatures: List<String> = emptyList(),
    var grants: SPGDPRVendorGrants = emptyMap(),
    var gcmStatus: GCMStatus? = null,
    val webConsentPayload: String? = null,
    var consentStatus: ConsentStatus = ConsentStatus(),
    @SerialName("TCData") var tcData: IABData = emptyMap(),
) {
    @Serializable
    data class GCMStatus (
        @SerialName("analytics_storage") val analyticsStorage: String?,
        @SerialName("ad_storage") val adStorage: String?,
        @SerialName("ad_user_data") val adUserData: String?,
        @SerialName("ad_personalization") val adPersonalization: String?
    )

    @Serializable
    data class VendorGrantsValue (
        val vendorGrant: Boolean,
        val purposeGrants: Map<String, Boolean>
    )
}

typealias SPGDPRVendorGrants = Map<String, GDPRConsent.VendorGrantsValue>
