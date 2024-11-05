package com.sourcepoint.mobile_core.network.responses

import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import com.sourcepoint.mobile_core.models.consents.GDPRConsent
import com.sourcepoint.mobile_core.models.consents.IABData
import com.sourcepoint.mobile_core.models.consents.SPGDPRVendorGrants
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GDPRChoiceResponse (
    val uuid: String,
    val dateCreated: String?,
    val expirationDate: String?,
    @SerialName("TCData") val tcData: IABData? = emptyMap(),
    val euconsent: String?,
    val consentStatus: ConsentStatus?,
    val grants: SPGDPRVendorGrants?,
    val webConsentPayload: String?,
    val gcmStatus: GDPRConsent.GCMStatus?,
    val acceptedLegIntCategories: List<String>?,
    val acceptedLegIntVendors: List<String>?,
    val acceptedVendors: List<String>?,
    val acceptedCategories: List<String>?,
    val acceptedSpecialFeatures: List<String>?
)
