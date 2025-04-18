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
        @SerialName("AI-POLICY") AIPolicy,
        @SerialName("TERMS-AND-CONDITIONS") TermsAndConditions,
        @SerialName("PRIVACY-POLICY") PrivacyPolicy,
        @SerialName("LEGAL-POLICY") LegalPolicy,
        @SerialName("TERMS-OF-SALE") TermsOfSale
    }
}

