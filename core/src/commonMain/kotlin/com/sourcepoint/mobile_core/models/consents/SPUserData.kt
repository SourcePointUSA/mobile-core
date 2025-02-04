package com.sourcepoint.mobile_core.models.consents

import kotlinx.serialization.Serializable

@Serializable
data class SPUserData(
    val gdpr: SPConsent<GDPRConsent>? = null,
    val ccpa: SPConsent<CCPAConsent>? = null,
    val usnat: SPConsent<USNatConsent>? = null
) {
    val webConsents: SPWebConsents get() = SPWebConsents(
        gdpr = if (gdpr?.consents?.uuid != null) SPWebConsents.SPWebConsent(gdpr.consents.uuid, gdpr.consents.webConsentPayload) else null,
        ccpa = if (ccpa?.consents?.uuid != null) SPWebConsents.SPWebConsent(ccpa.consents.uuid, ccpa.consents.webConsentPayload) else null,
        usnat = if (usnat?.consents?.uuid != null) SPWebConsents.SPWebConsent(usnat.consents.uuid, usnat.consents.webConsentPayload) else null,
    )

    @Serializable
    data class SPConsent<ConsentType>(
        val consents: ConsentType?
    )

    data class SPWebConsents(
        val gdpr: SPWebConsent?,
        val ccpa: SPWebConsent?,
        val usnat: SPWebConsent?
    ) {
        data class SPWebConsent(
            val uuid: String,
            val webConsentPayload: String?
        )
    }
}
