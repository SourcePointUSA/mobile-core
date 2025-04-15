package com.sourcepoint.mobile_core.network.requests

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class PreferencesChoiceRequest (
    val accountId: Int,
    val messageId: String? = null,
    val propertyId: Int,
    val sendPVData: Boolean,
    val authId: String? = null,
    val uuid: String? = null,
    val pmSaveAndExitVariables: JsonObject = JsonObject(emptyMap()),
    val sampleRate: Float,
    val includeData: IncludeData = IncludeData()
)
