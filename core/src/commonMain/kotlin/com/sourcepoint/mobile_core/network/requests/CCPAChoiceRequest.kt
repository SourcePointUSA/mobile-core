package com.sourcepoint.mobile_core.network.requests

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
class CCPAChoiceRequest (
    val authId: String? = null,
    val uuid: String? = null,
    val messageId: String? = null,
    val pubData: JsonObject = JsonObject(emptyMap()),
    val pmSaveAndExitVariables: JsonObject = JsonObject(emptyMap()),
    val sendPVData: Boolean,
    val propertyId: Int,
    val sampleRate: Float,
    val includeData: IncludeData = IncludeData(),
    val prtnUUID: String? = null
)
