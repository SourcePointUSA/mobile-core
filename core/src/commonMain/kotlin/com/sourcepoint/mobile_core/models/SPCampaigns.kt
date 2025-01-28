package com.sourcepoint.mobile_core.models

data class SPCampaigns (
    val environment: SPCampaignEnv,
    val gdpr: SPCampaign?,
    val ccpa: SPCampaign?,
    val usnat: SPCampaign?,
    val ios14: SPCampaign?
)
