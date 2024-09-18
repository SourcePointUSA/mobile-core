package com.sourcepoint.mobile_core.network

import com.sourcepoint.mobile_core.models.SPCampaignType
import com.sourcepoint.mobile_core.network.requests.DefaultRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorMetricsRequest(
    val code: String,
    val accountId: String,
    val description: String,
    val sdkVersion: String,
    @SerialName("OSVersion") val osVersion: String,
    val deviceFamily: String,
    val propertyId: String,
    val propertyName: String,
    val campaignType: SPCampaignType?
): DefaultRequest()
