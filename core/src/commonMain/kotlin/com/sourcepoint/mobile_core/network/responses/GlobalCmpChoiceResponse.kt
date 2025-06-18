package com.sourcepoint.mobile_core.network.responses

import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import com.sourcepoint.mobile_core.models.consents.UserConsents
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class GlobalCmpChoiceResponse(
    val uuid: String?,
    val consentStatus: ConsentStatus = ConsentStatus(),
    val dateCreated: Instant? = null,
    val expirationDate: Instant? = null,
    val gpcEnabled: Boolean? = null,
    val userConsents: UserConsents = UserConsents(),
    val webConsentPayload: String? = null,
    val applicableSections: JsonElement? = null,  // TODO: change type when "REGION_undefined" is fixed on wrapper side
    val sectionId: JsonPrimitive? = null // TODO: change type when "REGION_undefined" is fixed on wrapper side
)
