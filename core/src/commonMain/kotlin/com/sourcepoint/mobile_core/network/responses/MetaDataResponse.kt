package com.sourcepoint.mobile_core.network.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MetaDataResponse (
    val gdpr: MetaDataResponseGDPR?,
    val usnat: MetaDataResponseUSNat?
){
    @Serializable
    data class MetaDataResponseGDPR (
        val applies: Boolean,
        val sampleRate: Float,
        val additionsChangeDate: String,
        val legalBasisChangeDate: String,
        val childPmId: String?,
        @SerialName("_id") val id: String
    )

    @Serializable
    data class MetaDataResponseUSNat (
        val applies: Boolean,
        val sampleRate: Float,
        val additionsChangeDate: String,
        @SerialName("_id") val id: String
    )
}