package com.sourcepoint.mobile_core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ConsentStatusMetaData(
    val gdpr: GDPR?,
    val usnat: USNat?
) {
    @Serializable
    data class GDPR(
        val applies: Boolean,
        val dateCreated: String? = null,
        val uuid: String? = null,
        val hasLocalData: Boolean = false,
        val idfaStatus: String? = null
    ) {
        override fun toString() = Json.encodeToString(this)
    }

    @Serializable
    data class USNat(
        val applies: Boolean,
        val dateCreated: String? = null,
        val uuid: String? = null,
        val hasLocalData: Boolean = false,
        val idfaStatus: String? = null,
        val transitionCCPAAuth: Boolean? = null,
        val optedOut: Boolean? = null
    ) {
        override fun toString() = Json.encodeToString(this)
    }
    override fun toString() = Json.encodeToString(this)
}