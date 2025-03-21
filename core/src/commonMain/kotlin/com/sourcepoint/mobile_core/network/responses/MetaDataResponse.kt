package com.sourcepoint.mobile_core.network.responses

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MetaDataResponse (
    val gdpr: MetaDataResponseGDPR? = null,
    val usnat: MetaDataResponseUSNat? = null,
    val ccpa: MetaDataResponseCCPA? = null
){
    @Serializable
    data class MetaDataResponseGDPR (
        val applies: Boolean,
        val sampleRate: Float,
        val additionsChangeDate: Instant? = null,
        val legalBasisChangeDate: Instant? = null,
        val childPmId: String? = null,
        @SerialName("_id") val vendorListId: String
    )

    @Serializable
    data class MetaDataResponseUSNat (
        val applies: Boolean,
        val sampleRate: Float,
        val additionsChangeDate: Instant? = null,
        val applicableSections: List<Int>,
        @SerialName("_id") val vendorListId: String
    )

    @Serializable
    data class MetaDataResponseCCPA (
        val applies: Boolean,
        val sampleRate: Float
    )
}
