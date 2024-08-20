package com.sourcepoint.mobile_core.network.requests

import com.sourcepoint.mobile_core.models.SPIDFAStatus
import kotlinx.serialization.Serializable

@Serializable
data class ConsentStatusRequest(
    val propertyId: Int,
    val metadata: MetaData,
    val includeData: IncludeData = IncludeData(),
    val authId: String?,
    val withSiteActions: Boolean = false,
    val hasCsp: Boolean = true
): DefaultRequest() {
    @Serializable
    data class MetaData(val gdpr: GDPR?, val usnat: USNat?) {
        @Serializable
        data class GDPR(
            val applies: Boolean,
            val dateCreated: String? = null,
            val uuid: String? = null,
            val hasLocalData: Boolean = false,
            val idfaStatus: SPIDFAStatus? = SPIDFAStatus.current()
        )

        @Serializable
        data class USNat(
            val applies: Boolean,
            val dateCreated: String? = null,
            val uuid: String? = null,
            val hasLocalData: Boolean = false,
            val idfaStatus: SPIDFAStatus? = SPIDFAStatus.current(),
            val transitionCCPAAuth: Boolean? = null,
            val optedOut: Boolean? = null
        )
    }
}
