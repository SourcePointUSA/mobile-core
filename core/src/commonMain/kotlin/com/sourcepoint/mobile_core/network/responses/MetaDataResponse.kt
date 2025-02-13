package com.sourcepoint.mobile_core.network.responses

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MetaDataResponse (
    val gdpr: MetaDataResponseGDPR?,
    val usnat: MetaDataResponseUSNat?,
    val ccpa: MetaDataResponseCCPA?
){
    @Serializable
    data class MetaDataResponseGDPR (
        val applies: Boolean,
        val sampleRate: Float,
        val additionsChangeDate: Instant,
        val legalBasisChangeDate: Instant,
        val childPmId: String?,
        @SerialName("_id") val vendorListId: String
    )

    @Serializable
    data class MetaDataResponseUSNat (
        val applies: Boolean,
        val sampleRate: Float,
        val additionsChangeDate: Instant,
        val applicableSections: List<Int>,
        @SerialName("_id") val vendorListId: String
    )

    @Serializable
    data class MetaDataResponseCCPA (
        val applies: Boolean,
        val sampleRate: Float
    )
}
