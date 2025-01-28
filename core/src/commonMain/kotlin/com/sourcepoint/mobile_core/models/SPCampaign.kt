package com.sourcepoint.mobile_core.models

import com.sourcepoint.mobile_core.network.requests.IncludeData

data class SPCampaign (
    val targetingParams: SPTargetingParams,
    val groupPmId: String?,
    val gppConfig: IncludeData.GPPConfig?,
    val transitionCCPAAuth: Boolean?,
    val supportLegacyUSPString: Boolean?
)
