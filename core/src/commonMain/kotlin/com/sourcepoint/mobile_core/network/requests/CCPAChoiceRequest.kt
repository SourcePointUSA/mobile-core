package com.sourcepoint.mobile_core.network.requests

import com.sourcepoint.mobile_core.network.SPJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
class CCPAChoiceRequest (
    val authId: String?,
    val uuid: String?,
    val messageId: String?,
    val pubData: JsonObject?,
    val pmSaveAndExitVariables: SPJson?,
    val sendPVData: Boolean,
    val propertyId: Int,
    val sampleRate: Float?,
    val includeData: IncludeData
)
