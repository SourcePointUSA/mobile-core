package com.sourcepoint.mobile_core.models.consents

import com.sourcepoint.mobile_core.utils.StringEnumWithDefaultSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject


// TODO: implement USPString logic
@Serializable
data class CCPAConsent(
    val uuid: String? = null,
    val dateCreated: String? = null,
    val expirationDate: String? = null,
    val signedLspa: Boolean? = null,
    var uspstring: String? = null,
    val rejectedVendors: List<String> = emptyList(),
    val rejectedCategories: List<String> = emptyList(),
    val status: CCPAConsentStatus = CCPAConsentStatus.Unknown,
    val webConsentPayload: String? = null,
    @SerialName("GPPData") val gppData: JsonObject = JsonObject(emptyMap()),
) {
    @Serializable(with = CCPAConsentStatus.Serializer::class)
    enum class CCPAConsentStatus {
        @SerialName("consentedAll") ConsentedAll,
        @SerialName("rejectedAll") RejectedAll,
        @SerialName("rejectedSome") RejectedSome,
        @SerialName("rejectedNone") RejectedNone,
        @SerialName("linkedNoAction") LinkedNoAction,
        Unknown;
        object Serializer: StringEnumWithDefaultSerializer<CCPAConsentStatus>(entries, Unknown)
    }
}
