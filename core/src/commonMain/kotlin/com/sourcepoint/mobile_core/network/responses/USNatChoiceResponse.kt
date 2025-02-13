package com.sourcepoint.mobile_core.network.responses

import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import com.sourcepoint.mobile_core.models.consents.ConsentStrings
import com.sourcepoint.mobile_core.models.consents.IABData
import com.sourcepoint.mobile_core.models.consents.USNatConsent
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class USNatChoiceResponse (
    val uuid: String?,
    val consentStatus: ConsentStatus,
    val consentStrings: ConsentStrings,
    val dateCreated: Instant?,
    val expirationDate: Instant?,
    val gpcEnabled: Boolean?,
    val webConsentPayload: String?,
    @SerialName("GPPData") val gppData: IABData = emptyMap(),
    val userConsents: USNatConsent.USNatUserConsents
)
