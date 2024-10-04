package com.sourcepoint.mobile_core.network.requests

import kotlinx.serialization.Serializable

@Serializable
data class CustomConsentRequest (
    val consentUUID: String,
    val propertyId: Int,
    val vendors: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val legIntCategories: List<String> = emptyList()
): DefaultRequest()