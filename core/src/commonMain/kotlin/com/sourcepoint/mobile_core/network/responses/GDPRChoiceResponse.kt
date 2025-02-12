package com.sourcepoint.mobile_core.network.responses

import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import com.sourcepoint.mobile_core.models.consents.GDPRConsent
import com.sourcepoint.mobile_core.models.consents.IABData
import com.sourcepoint.mobile_core.models.consents.SPGDPRVendorGrants
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GDPRChoiceResponse (
    val uuid: String,
    val dateCreated: Instant?,
    val expirationDate: Instant?,
    @SerialName("TCData") val tcData: IABData? = emptyMap(),
    val euconsent: String?,
    val consentStatus: ConsentStatus?,
    val grants: SPGDPRVendorGrants?,
    val webConsentPayload: String?,
    val gcmStatus: GDPRConsent.GCMStatus?,
    @SerialName("legIntCategories") val acceptedLegIntCategories: List<String>?,
    @SerialName("legIntVendors") val acceptedLegIntVendors: List<String>?,
    @SerialName("vendors") val acceptedVendors: List<String>?,
    @SerialName("categories") val acceptedCategories: List<String>?,
    @SerialName("specialFeatures") val acceptedSpecialFeatures: List<String>?
)
