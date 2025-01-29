package com.sourcepoint.mobile_core.models.consents

import com.sourcepoint.mobile_core.models.SPIDFAStatus

data class AttCampaign (
    val status: SPIDFAStatus? = SPIDFAStatus.current(),
    val messageId: Int? = null,
    val partitionUUID: String? = null
)
