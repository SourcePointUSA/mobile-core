package com.sourcepoint.mobile_core.models

import com.sourcepoint.mobile_core.network.requests.IncludeData.GPPConfig

data class SPCampaign (
    val targetingParams: SPTargetingParams = emptyMap(),
    val groupPmId: String? = null,
    val supportLegacyUSPString: Boolean? = null,
    val gppConfig: GPPConfig = GPPConfig(uspString = supportLegacyUSPString ?: false),
    val transitionCCPAAuth: Boolean? = null
)
