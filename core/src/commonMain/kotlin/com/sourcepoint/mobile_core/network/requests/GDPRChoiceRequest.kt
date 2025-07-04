package com.sourcepoint.mobile_core.network.requests

import com.sourcepoint.mobile_core.models.SPIDFAStatus
import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GDPRChoiceRequest (
    val authId: String? = null,
    val uuid: String? = null,
    val messageId: String? = null,
    val consentAllRef: String? = null,
    val vendorListId: String? = null,
    val pubData: JsonObject = JsonObject(emptyMap()),
    val pmSaveAndExitVariables: JsonObject = JsonObject(emptyMap()),
    val sendPVData: Boolean,
    val propertyId: Int,
    val sampleRate: Float,
    val idfaStatus: SPIDFAStatus? = SPIDFAStatus.current(),
    val granularStatus: ConsentStatus.ConsentStatusGranularStatus? = null,
    val includeData: IncludeData = IncludeData(),
    val prtnUUID: String? = null
)
