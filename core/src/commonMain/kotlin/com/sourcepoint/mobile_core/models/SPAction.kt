package com.sourcepoint.mobile_core.models

import com.sourcepoint.mobile_core.network.encodeToJsonObject
import kotlinx.serialization.json.JsonObject

enum class SPActionType(val type: Int) {
    SaveAndExit(1),
    PMCancel(2),
    Custom(9),
    AcceptAll(11),
    ShowPrivacyManager(12),
    RejectAll(13),
    Dismiss(15),
    RequestATTAccess(16),
    IDFAAccepted(17),
    IDFADenied(18),
    Unknown(0)
}

data class SPAction(
    val type:SPActionType,
    val campaignType: SPCampaignType,
    val messageId: String? = null,
    val pmPayload: JsonObject = JsonObject(emptyMap()),
    val encodablePubData: JsonObject = JsonObject(emptyMap())
) {
    constructor(
        actionType: SPActionType,
        campaignType: SPCampaignType,
        messageId: String? = null,
        pmPayload: String? = null,
        encodablePubData: String? = null
    ):  this(
        type = actionType,
        campaignType = campaignType,
        messageId = messageId,
        pmPayload = pmPayload?.encodeToJsonObject() ?: JsonObject(emptyMap()),
        encodablePubData = encodablePubData?.encodeToJsonObject() ?: JsonObject(emptyMap())
    )
}
