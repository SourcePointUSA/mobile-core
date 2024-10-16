package com.sourcepoint.mobile_core.network.requests

import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

typealias SPPublisherData = Map<String, @Contextual Any>

@Serializable
data class PvDataRequest (
    val gdpr: GDPR?,
    val ccpa: CCPA?,
    val usnat: USNat?
) {
    @Serializable
    data class GDPR (
        val applies: Boolean,
        val uuid: String?,
        val accountId: Int,
        val siteId: Int,
        val consentStatus: ConsentStatus,
        val pubData: SPPublisherData?,
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
        val siteId: Int,
        val consentStatus: ConsentStatus,
        val pubData: SPPublisherData?,
        val messageId: Int?,
        val sampleRateval : Float?
    )

    @Serializable
    data class USNat (
        val applies: Boolean,
        val uuid: String?,
        val accountId: Int,
        val siteId: Int,
        val consentStatus: ConsentStatus,
        val pubData: SPPublisherData?,
        val sampleRate: Float?,
        val msgId: Int?,
        val categoryId: Int?,
        val subCategoryId: Int?,
        val prtnUUIDval : String?
    )
}