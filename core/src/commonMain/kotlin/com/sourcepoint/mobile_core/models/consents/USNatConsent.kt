package com.sourcepoint.mobile_core.models.consents

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class USNatConsent(
    var dateCreated: String? = null,
    var expirationDate: String? = null,
    val uuid: String? = null,
    val webConsentPayload: String? = null,
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
        val vendors: List<USNatConsentable> = emptyList(),
        val categories: List<USNatConsentable> = emptyList()
    )

    @Serializable
    data class USNatConsentable (
        @SerialName("_id") val id: String,
        val consented: Boolean
    )
}

typealias ConsentStrings = List<USNatConsent.USNatConsentSection>
