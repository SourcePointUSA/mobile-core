package com.sourcepoint.mobile_core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SPCampaignType {
    @SerialName("GDPR") Gdpr,
    @SerialName("CCPA") Ccpa,
    @SerialName("usnat") UsNat,
    @SerialName("ios14") IOS14,
    @SerialName("preferences") Preferences,
    @SerialName("globalcmp") GlobalCmp,
    @SerialName("unknown") Unknown
}
