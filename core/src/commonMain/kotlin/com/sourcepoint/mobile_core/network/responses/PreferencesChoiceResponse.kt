package com.sourcepoint.mobile_core.network.responses

import com.sourcepoint.mobile_core.models.consents.PreferencesConsent
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class PreferencesChoiceResponse (
    val uuid: String? = null,
    val configurationId: String? = null,
    val dateCreated: Instant? = null,
    val status: List<PreferencesConsent.PreferencesStatus>? = null,
    val rejectedStatus: List<PreferencesConsent.PreferencesStatus>? = null
)
