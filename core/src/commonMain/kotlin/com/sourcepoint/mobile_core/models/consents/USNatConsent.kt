package com.sourcepoint.mobile_core.models.consents

import com.sourcepoint.mobile_core.utils.inOneYear
import com.sourcepoint.mobile_core.utils.now
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class USNatConsent(
    val applies: Boolean = false,
    val dateCreated: Instant = now(),
    val expirationDate: Instant = dateCreated.inOneYear(),
    val uuid: String? = null,
    val webConsentPayload: String? = null,
    var consentStatus: ConsentStatus = ConsentStatus(),
    val consentStrings: ConsentStrings = emptyList(),
    var userConsents: USNatUserConsents = USNatUserConsents(),
    @SerialName("GPPData") val gppData: IABData = emptyMap()
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
