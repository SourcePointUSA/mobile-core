package com.sourcepoint.mobile_core.models

import com.sourcepoint.mobile_core.network.responses.MessagesResponse
import io.ktor.http.Url

data class MessageToDisplay(
    val message: MessagesResponse.Message,
    val metaData: MessagesResponse.MessageMetaData,
    val url: String,
    val type: SPCampaignType
) {
    companion object {
        fun <T> initFromCampaign(campaign: MessagesResponse.Campaign<T>): MessageToDisplay? =
            if (campaign.message != null && campaign.messageMetaData != null && campaign.url != null) {
                MessageToDisplay(
                    message = campaign.message,
                    metaData = campaign.messageMetaData,
                    url = campaign.url,
                    type = campaign.type
                )
            } else {
                null
            }
    }
}
