package com.sourcepoint.mobile_core.network.responses

import com.sourcepoint.mobile_core.models.ConsentStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ConsentStatusResponse (
    val consentStatusData: ConsentStatusData,
    val localState: String
) {
    @Serializable
    data class ConsentStatusData (
        val gdpr: ConsentStatusDataGDPR?,
        val usnat: ConsentStatusUSNAT?
    ) {
        @Serializable
        data class ConsentStatusDataGDPR (
            val dateCreated: String,
            val expirationDate: String,
            val uuid: String,
            val euconsent: String,
            val legIntCategories: List<String>,
            val legIntVendors: List<String>,
            val vendors: List<String>,
            val categories: List<String>,
            val specialFeatures: List<String>,
            val grants: Map<String, VendorGrantsValue>,
            val gcmStatus: ConsentStatusGCMStatus?,
            val webConsentPayload: String,
            val consentStatus: ConsentStatus,
            @SerialName("TCData") val tcData: JsonObject,
        ) {
            @Serializable
            data class ConsentStatusGCMStatus (
                val analyticsStorage: String?,
                val adStorage: String?,
                val adUserData: String?,
                val adPersonalization: String?
            )

            @Serializable
            data class VendorGrantsValue (
                val vendorGrant: Boolean,
                val purposeGrants: Map<String, Boolean>
            )
        }

        @Serializable
        data class ConsentStatusUSNAT (
            val dateCreated: String,
            val expirationDate: String,
            val uuid: String,
            val webConsentPayload: String,
            val consentStatus: ConsentStatus,
            val consentStrings: List<USNatConsentSection>,
            val userConsents: USNatUserConsents,
            @SerialName("GPPData") val gppData: JsonObject,
        ) {
            @Serializable
            data class USNatConsentSection (
                val sectionId: Int,
                val sectionName: String,
                val consentString: String
            )

            @Serializable
            data class USNatUserConsents (
                val vendors: List<USNatConsentable>,
                val categories: List<USNatConsentable>
            )

            @Serializable
            data class USNatConsentable (
                @SerialName("_id") val id: String,
                val consented: Boolean
            )
        }
    }
}
