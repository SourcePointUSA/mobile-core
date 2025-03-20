package com.sourcepoint.mobile_core.network.requests

import kotlinx.serialization.Serializable

@Serializable
data class DeleteCustomConsentRequest(val consentUUID: String): DefaultRequest()
