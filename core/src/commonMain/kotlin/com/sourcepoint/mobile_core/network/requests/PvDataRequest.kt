package com.sourcepoint.mobile_core.network.requests

import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject


@Serializable
data class PvDataRequest (
    val gdpr: GDPR?,
    val ccpa: CCPA?,
    val usnat: USNat?,
    val globalcmp: GlobalCmp?
) {
    @Serializable
    data class GDPR (
        val applies: Boolean,
        val uuid: String?,
        val accountId: Int,
        @SerialName("siteId") val propertyId: Int,
        val consentStatus: ConsentStatus,
        val pubData: JsonObject?,
        val sampleRate: Float?,
        val euconsent: String?,
        val msgId: Int?,
        val categoryId: Int?,
        val subCategoryId: Int?,
        val prtnUUID: String?
    )

    @Serializable
    data class CCPA (
        val applies: Boolean,
        val uuid: String?,
        val accountId: Int,
        @SerialName("siteId") val propertyId: Int,
        val consentStatus: ConsentStatus,
        val pubData: JsonObject?,
        val messageId: Int?,
        val sampleRate : Float?
    )

    @Serializable
    data class USNat (
        val applies: Boolean,
        val uuid: String?,
        val accountId: Int,
        @SerialName("siteId") val propertyId: Int,
        val consentStatus: ConsentStatus,
        val pubData: JsonObject?,
        val sampleRate: Float?,
        val msgId: Int?,
        val categoryId: Int?,
        val subCategoryId: Int?,
        val prtnUUID : String?
    )

    @Serializable
    data class GlobalCmp (
        val applies: Boolean,
        val uuid: String?,
        val accountId: Int,
        @SerialName("siteId") val propertyId: Int,
        val consentStatus: ConsentStatus,
        val pubData: JsonObject?,
        val sampleRate: Float?,
        val msgId: Int?,
        val categoryId: Int?,
        val subCategoryId: Int?,
        val prtnUUID : String?
    )
}
