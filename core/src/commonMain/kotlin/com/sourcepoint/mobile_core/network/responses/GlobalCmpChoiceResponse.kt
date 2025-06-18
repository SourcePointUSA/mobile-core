package com.sourcepoint.mobile_core.network.responses

import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import com.sourcepoint.mobile_core.models.consents.USNatConsent
import com.sourcepoint.mobile_core.models.consents.UserConsents
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class GlobalCmpChoiceResponse(
    val uuid: String?,
    val consentStatus: ConsentStatus = ConsentStatus(),
    val dateCreated: Instant? = null,
    val expirationDate: Instant? = null,
    val gpcEnabled: Boolean? = null,
    val userConsents: UserConsents = UserConsents()
)
