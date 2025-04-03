package com.sourcepoint.mobile_core.models.consents

import kotlinx.serialization.Serializable

@Serializable
data class PreferencesCampaign(
    val configurationId: String = "",
    val legalDocLiveDate: IABData? = null
)
