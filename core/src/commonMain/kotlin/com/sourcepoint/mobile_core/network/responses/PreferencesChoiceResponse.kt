package com.sourcepoint.mobile_core.network.responses

import com.sourcepoint.mobile_core.models.consents.PreferencesCampaign
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class PreferencesChoiceResponse (
    val uuid: String? = null,
    val configurationId: String? = null,
    val dateCreated: Instant? = null,
    val status: List<PreferencesCampaign.PreferencesStatus>? = null,
    val rejectedStatus: List<PreferencesCampaign.PreferencesStatus>? = null
)
