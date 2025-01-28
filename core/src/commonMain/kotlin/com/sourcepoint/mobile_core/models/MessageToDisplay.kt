package com.sourcepoint.mobile_core.models

import com.sourcepoint.mobile_core.network.responses.MessagesResponse
import io.ktor.http.Url

data class MessageToDisplay(
    val message: MessagesResponse.Message,
    val metaData: MessagesResponse.MessageMetaData,
    val url: Url,
    val type: SPCampaignType
)
