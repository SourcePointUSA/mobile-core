package com.sourcepoint.mobile_core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SPCampaignEnv(val value: String) {
    @SerialName("stage") STAGE("stage"),
    @SerialName("prod") PUBLIC("prod")
}
