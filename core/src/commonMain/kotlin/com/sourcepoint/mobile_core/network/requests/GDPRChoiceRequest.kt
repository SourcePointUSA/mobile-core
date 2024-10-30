package com.sourcepoint.mobile_core.network.requests

import com.sourcepoint.mobile_core.models.SPIDFAStatus
import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GDPRChoiceRequest (
    val authId: String?,
    val uuid: String?,
    val messageId: String?,
    val consentAllRef: String?,
    val vendorListId: String?,
    val pubData : JsonObject?,
    val pmSaveAndExitVariables : String?,
    val sendPVData: Boolean,
    val propertyId: Int,
    val sampleRate: Float?,
    val idfaStatus: SPIDFAStatus?,
    val granularStatus: ConsentStatus.ConsentStatusGranularStatus?,
    val includeData: IncludeData
)
