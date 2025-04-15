package com.sourcepoint.mobile_core.models.consents

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class PreferencesCampaign(
    val configurationId: String = "",
    val legalDocLiveDate: IABData? = null,
    val messageId: Int? = null,
    val status: List<PreferencesStatus>? = null,
    val rejectedStatus: List<PreferencesStatus>? = null,
    val uuid: String? = null
) {
    @Serializable
    data class PreferencesStatus(
        val categoryId: Int,
        val channels: List<PreferencesChannels>?,
        val changed: Boolean?,
        val dateConsented: Instant?,
        val subType: String?
    ) {
        @Serializable
        data class PreferencesChannels(
            val channelId: Int,
            val status: Boolean
        )
    }
}
