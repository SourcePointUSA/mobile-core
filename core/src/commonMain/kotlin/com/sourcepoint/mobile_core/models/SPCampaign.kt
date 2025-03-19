package com.sourcepoint.mobile_core.models

import com.sourcepoint.mobile_core.network.requests.IncludeData

data class SPCampaign (
    val targetingParams: SPTargetingParams = emptyMap(),
    val groupPmId: String? = null,
    val gppConfig: IncludeData.GPPConfig? = null,
    val transitionCCPAAuth: Boolean? = null,
    val supportLegacyUSPString: Boolean? = null
)
