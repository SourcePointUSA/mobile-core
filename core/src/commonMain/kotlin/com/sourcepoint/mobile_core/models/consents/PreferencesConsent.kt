package com.sourcepoint.mobile_core.models.consents

import com.sourcepoint.mobile_core.utils.now
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PreferencesConsent(
    val dateCreated: Instant? = null,
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
        val subType: PreferencesSubType? = PreferencesSubType.Unknown
    ) {
        @Serializable
        data class PreferencesChannels(
            val channelId: Int,
            val status: Boolean
        )
    }

    @Serializable
    enum class PreferencesSubType{
        Unknown,
        @SerialName("AI-POLICY") AI_POLICY,
        @SerialName("TERMS-AND-CONDITIONS") TERMS_AND_CONDITIONS,
        @SerialName("PRIVACY-POLICY") PRIVACY_POLICY,
        @SerialName("LEGAL-POLICY") LEGAL_POLICY,
        @SerialName("TERMS-OF-SALE") TERMS_OF_SALE
    }
}

