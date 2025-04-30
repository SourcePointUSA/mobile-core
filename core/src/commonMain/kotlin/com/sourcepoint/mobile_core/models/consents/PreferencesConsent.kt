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
        val channels: List<PreferencesChannels>? = null,
        val changed: Boolean? = null,
        val dateConsented: Instant? = null,
        val subType: PreferencesSubType? = PreferencesSubType.Unknown
    ) {
        @Serializable
        data class PreferencesChannels(val channelId: Int, val status: Boolean)
    }

    @Serializable
    enum class PreferencesSubType(val value: String) {
        Unknown("Unknown"),
        @SerialName("AI-POLICY") AIPolicy("AI-POLICY"),
        @SerialName("TERMS-AND-CONDITIONS") TermsAndConditions("TERMS-AND-CONDITIONS"),
        @SerialName("PRIVACY-POLICY") PrivacyPolicy("PRIVACY-POLICY"),
        @SerialName("LEGAL-POLICY") LegalPolicy("LEGAL-POLICY"),
        @SerialName("TERMS-OF-SALE") TermsOfSale("TERMS-OF-SALE")
    }
}
