package com.sourcepoint.mobile_core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class MetadataResponse(
    val gdpr: GDPR? = null,
    val ccpa: CCPA? = null,
    val usnat: UsNat? = null,
) {
    @Serializable
    data class GDPR(
        val applies: Boolean,
        val sampleRate: Float,
        val additionsChangeDate: String, // TODO: implement SPDate
        val legalBasisChangeDate: String, // TODO: implement SPDate
        val childPmId: String?,
        @SerialName("_id") val vendorListId: String,
    )

    @Serializable
    data class CCPA(
        val applies: Boolean,
        val sampleRate: Float,
    )

    @Serializable
    data class UsNat(
        val applies: Boolean,
        val sampleRate: Float,
        val additionsChangeDate: String, // TODO: implement SPDate
        val applicableSections: List<Int>,
        @SerialName("_id") val vendorListId: String,
    )
}

@Serializable
data class MetadataRequest(
    val accountId: Int,
    val propertyId: Int,
    val metadata: MetaDataCampaigns
) {
    fun toParams(): Map<String, String> {
        return mapOf(
            "accountId" to accountId.toString(),
            "propertyId" to propertyId.toString(),
            "metadata" to Json.encodeToString(metadata)
        )
    }
}

@Serializable
data class MetaDataCampaigns(
    val gdpr: Campaign? = null,
    val ccpa: Campaign? = null,
    val usnat: Campaign? = null,
) {
    @Serializable
    data class Campaign(val groupPmId: String? = null)
}






