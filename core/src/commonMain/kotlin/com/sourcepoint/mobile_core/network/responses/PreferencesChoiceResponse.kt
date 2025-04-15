package com.sourcepoint.mobile_core.network.responses

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class PreferencesChoiceResponse (
    val uuid: String? = null,
    val configurationId: String? = null,
    val dateCreated: Instant? = null,
    val status: List<JsonObject>? = null,
    val rejectedStatus: List<JsonObject>? = null
)
