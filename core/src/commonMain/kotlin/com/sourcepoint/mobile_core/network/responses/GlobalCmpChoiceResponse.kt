package com.sourcepoint.mobile_core.network.responses

import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import com.sourcepoint.mobile_core.models.consents.USNatConsent
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class GlobalCmpChoiceResponse(
    val uuid: String? = null,
    val consentStatus: ConsentStatus,
    val dateCreated: Instant? = null,
    val expirationDate: Instant? = null,
    val gpcEnabled: Boolean? = null,
    val userConsents: USNatConsent.USNatUserConsents
)
