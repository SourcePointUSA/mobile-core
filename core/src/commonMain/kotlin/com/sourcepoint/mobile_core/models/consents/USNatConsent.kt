package com.sourcepoint.mobile_core.models.consents

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class USNatConsent(
    var applies: Boolean = false,
    var dateCreated: String? = null,
    var expirationDate: String? = null,
    var uuid: String? = null,
    var webConsentPayload: String? = null,
    var consentStatus: ConsentStatus = ConsentStatus(),
    var consentStrings: ConsentStrings = emptyList(),
    val userConsents: USNatUserConsents = USNatUserConsents(),
    @SerialName("GPPData") var gppData: IABData = emptyMap()
) {
    @Serializable
    data class USNatConsentSection (
        val sectionId: Int,
        val sectionName: String,
        val consentString: String
    )

    @Serializable
    data class USNatUserConsents (
        var vendors: List<USNatConsentable> = emptyList(),
        var categories: List<USNatConsentable> = emptyList()
    )

    @Serializable
    data class USNatConsentable (
        @SerialName("_id") val id: String,
        val consented: Boolean
    )
}

typealias ConsentStrings = List<USNatConsent.USNatConsentSection>
