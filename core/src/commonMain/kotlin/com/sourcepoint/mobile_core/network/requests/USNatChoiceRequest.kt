package com.sourcepoint.mobile_core.network.requests

import com.sourcepoint.mobile_core.models.SPIDFAStatus
import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import com.sourcepoint.mobile_core.network.SPJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class USNatChoiceRequest (
    val authId: String?,
    val uuid: String?,
    val messageId: String?,
    val vendorListId: String?,
    val pubData: JsonObject?,
    val pmSaveAndExitVariables: SPJson?,
    val sendPVData: Boolean,
    val propertyId: Int,
    val sampleRate: Float?,
    val idfaStatus: SPIDFAStatus? = SPIDFAStatus.current(),
    val granularStatus: ConsentStatus.ConsentStatusGranularStatus?,
    val includeData: IncludeData = IncludeData()
)
