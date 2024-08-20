package com.sourcepoint.mobile_core.network.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class MessagesResponse(
    val campaigns: List<Campaign>,
    val localState: String,
    val nonKeyedLocalState: String
) {
    @Serializable
    sealed class Campaign {
        abstract val type: String
    }

    @Serializable
    @SerialName("GDPR")
    data class GDPR(
        override val type: String = "GDPR",
        @SerialName("TCData") val tcData: JsonObject
    ): Campaign()

    @Serializable
    @SerialName("usnat")
    data class USNat(
        override val type: String = "usnat",
        @SerialName("GPPData") val gppData: JsonObject
    ): Campaign()
}
