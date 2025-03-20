package com.sourcepoint.mobile_core.network

import com.sourcepoint.mobile_core.models.SPCampaignType
import kotlinx.serialization.Serializable

@Serializable
data class ErrorMetricsRequest(
    val code: String,
    val accountId: String,
    val scriptVersion: String,
    val legislation: SPCampaignType?,
    val sdkOsVersion: String,
    val deviceFamily: String,
    val propertyId: String,
)
