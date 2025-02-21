package com.sourcepoint.mobile_core.network.responses

import com.sourcepoint.mobile_core.models.consents.CCPAConsent
import com.sourcepoint.mobile_core.models.consents.IABData
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CCPAChoiceResponse (
    val uuid: String,
    val dateCreated: Instant? = null,
    val consentedAll: Boolean? = null,
    val rejectedAll: Boolean? = null,
    val status: CCPAConsent.CCPAConsentStatus? = null,
    val uspstring: String? = null,
    val gpcEnabled: Boolean? = null,
    val rejectedVendors: List<String>? = null,
    val rejectedCategories: List<String>? = null,
    val webConsentPayload: String? = null,
    @SerialName("GPPData") val gppData: IABData = emptyMap()
)
