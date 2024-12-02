package com.sourcepoint.mobile_core.network.requests

import com.sourcepoint.mobile_core.models.SPCampaignType
import com.sourcepoint.mobile_core.models.SPIDFAStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IDFAStatusReportRequest (
    val accountId: Int,
    val propertyId: Int?,
    val uuid: String?,
    val uuidType: SPCampaignType?,
    val requestUUID: String,
    val iosVersion: String,
    val appleTracking: AppleTrackingPayload
) {
    @Serializable
    data class AppleTrackingPayload(
        val appleChoice: SPIDFAStatus,
        val appleMsgId: Int?,
        @SerialName("partition_uuid") val messagePartitionUUID: String?
    )
}
