package com.sourcepoint.mobile_core.models.consents

import com.sourcepoint.mobile_core.network.encodeToJsonObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SPUserData(
    val gdpr: SPConsent<GDPRConsent>? = null,
    val ccpa: SPConsent<CCPAConsent>? = null,
    val usnat: SPConsent<USNatConsent>? = null,
    val preferences: SPConsent<PreferencesConsent>? = null
) {
    val webConsents: SPWebConsents get() = SPWebConsents(
        gdpr = gdpr?.consents?.uuid?.let {
            SPWebConsents.SPWebConsent(it, gdpr.consents.webConsentPayload?.encodeToJsonObject())
        },
        ccpa = ccpa?.consents?.uuid?.let {
            SPWebConsents.SPWebConsent(it, ccpa.consents.webConsentPayload?.encodeToJsonObject())
        },
        usnat = usnat?.consents?.uuid?.let {
            SPWebConsents.SPWebConsent(it, usnat.consents.webConsentPayload?.encodeToJsonObject())
        },
    )

    @Serializable
    data class SPConsent<ConsentType>(
        val consents: ConsentType?,
        val childPmId: String? = null
    )

    @Serializable
    data class SPWebConsents(
        val gdpr: SPWebConsent?,
        val ccpa: SPWebConsent?,
        val usnat: SPWebConsent?
    ) {
        @Serializable
        data class SPWebConsent(val uuid: String, val webConsentPayload: JsonObject?)
    }
}
