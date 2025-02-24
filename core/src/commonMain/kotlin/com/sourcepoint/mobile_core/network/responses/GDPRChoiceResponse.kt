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
    val dateCreated: Instant? = null,
    val expirationDate: Instant? = null,
    @SerialName("TCData") val tcData: IABData? = emptyMap(),
    val euconsent: String? = null,
    val consentStatus: ConsentStatus? = null,
    val grants: SPGDPRVendorGrants? = null,
    val webConsentPayload: String? = null,
    val gcmStatus: GDPRConsent.GCMStatus? = null,
    @SerialName("legIntCategories") val acceptedLegIntCategories: List<String>? = emptyList(),
    @SerialName("legIntVendors") val acceptedLegIntVendors: List<String>? = emptyList(),
    @SerialName("vendors") val acceptedVendors: List<String>? = emptyList(),
    @SerialName("categories") val acceptedCategories: List<String>? = emptyList(),
    @SerialName("specialFeatures") val acceptedSpecialFeatures: List<String>? = emptyList()
)
