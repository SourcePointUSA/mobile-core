package com.sourcepoint.mobile_core.models.consents

import com.sourcepoint.mobile_core.utils.inOneYear
import com.sourcepoint.mobile_core.utils.now
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class GlobalCmpConsent (
    val applies: Boolean = false,
    val categories: List<String> = emptyList(),
    var consentStatus: ConsentStatus = ConsentStatus(),
    val dateCreated: Instant = now(),
    val expirationDate: Instant = dateCreated.inOneYear(),
    val gpcEnabled: Boolean? = null,
    val uuid: String? = null,
    val userConsents: UserConsents = UserConsents(),
    val webConsentPayload: String? = null
)
