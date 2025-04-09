package com.sourcepoint.mobile_core.models.consents

import kotlinx.serialization.Serializable

@Serializable
data class PreferencesCampaign(
    val configurationId: String = "",
    val legalDocLiveDate: IABData? = null,
    val consentStatus: ConsentStatus = ConsentStatus(),
    val messageId: Int? = null,
)
