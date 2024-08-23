package com.sourcepoint.mobile_core.models.consents

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class USNatConsent(
    val dateCreated: String? = null,
    val expirationDate: String? = null,
    val uuid: String? = null,
    val webConsentPayload: String? = null,
    val consentStatus: ConsentStatus = ConsentStatus(),
    val consentStrings: ConsentStrings = emptyList(),
    val userConsents: USNatUserConsents = USNatUserConsents(),
    @SerialName("GPPData") val gppData: JsonObject = JsonObject(emptyMap())
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
