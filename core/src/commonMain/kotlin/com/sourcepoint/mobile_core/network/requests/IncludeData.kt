package com.sourcepoint.mobile_core.network.requests

import com.sourcepoint.mobile_core.network.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class IncludeData(
    @SerialName("TCData") val tcData: TypeString = TypeString(),
    val webConsentPayload: TypeString = TypeString(),
    val localState: TypeString = TypeString(),
    val categories: Boolean = true,
    val translateMessage: Boolean? = null,
    @SerialName("GPPData") val gppData: GPPConfig = GPPConfig()
) {
    @Serializable
    data class TypeString(val type: String = "string") {
        override fun toString() = json.encodeToString(this)
    }

    @Serializable
    data class GPPConfig(
        val MspaCoveredTransaction: MspaBinaryFlag? = null,
        val MspaOptOutOptionMode: MspaTernaryFlag? = null,
        val MspaServiceProviderMode: MspaTernaryFlag? = null,
        val uspString: Boolean? = true
    ) {
        override fun toString() = json.encodeToString(this)
    }

    @Serializable
    enum class MspaBinaryFlag(val value: String) {
        yes("yes"),
        no("no");

        override fun toString() = value
    }

    @Serializable
    enum class MspaTernaryFlag(val value: String) {
        yes("yes"),
        no("no"),
        na("na");

        override fun toString() = value
    }

    override fun toString() = json.encodeToString(this)
}
