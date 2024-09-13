package com.sourcepoint.mobile_core.models

open class SPError(
    val code: String = "",
    val description: String = "",
    val campaignType: SPCampaignType? = null
): Exception(description)
