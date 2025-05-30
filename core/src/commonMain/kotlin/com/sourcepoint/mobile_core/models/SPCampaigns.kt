package com.sourcepoint.mobile_core.models

data class SPCampaigns (
    val environment: SPCampaignEnv = SPCampaignEnv.PUBLIC,
    val gdpr: SPCampaign? = null,
    val ccpa: SPCampaign? = null,
    val usnat: SPCampaign? = null,
    val globalcmp: SPCampaign? = null,
    val ios14: SPCampaign? = null,
    val preferences: SPCampaign? = null
)
