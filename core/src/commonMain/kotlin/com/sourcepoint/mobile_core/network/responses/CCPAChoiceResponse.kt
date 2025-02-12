package com.sourcepoint.mobile_core.network.responses

import com.sourcepoint.mobile_core.models.consents.CCPAConsent
import com.sourcepoint.mobile_core.models.consents.IABData
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CCPAChoiceResponse (
    val uuid: String,
    val dateCreated: Instant?,
    val consentedAll: Boolean?,
    val rejectedAll: Boolean?,
    val status: CCPAConsent.CCPAConsentStatus?,
    val uspstring: String?,
    val gpcEnabled: Boolean?,
    val rejectedVendors: List<String>?,
    val rejectedCategories: List<String>?,
    val webConsentPayload: String?,
    @SerialName("GPPData") val gppData: IABData = emptyMap()
)
