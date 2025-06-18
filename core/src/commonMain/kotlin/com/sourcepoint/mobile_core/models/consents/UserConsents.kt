package com.sourcepoint.mobile_core.models.consents

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserConsents(
    val vendors: List<Consentable> = emptyList(),
    val categories: List<Consentable> = emptyList()
) {
    @Serializable
    data class Consentable (
        @SerialName("_id") val id: String,
        val systemId: Int? = null,
        val consented: Boolean
    )
}
