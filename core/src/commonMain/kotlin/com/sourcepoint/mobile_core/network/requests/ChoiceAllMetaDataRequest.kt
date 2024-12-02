package com.sourcepoint.mobile_core.network.requests

import kotlinx.serialization.Serializable

@Serializable
data class ChoiceAllMetaDataRequest (
    val gdpr: Campaign?,
    val ccpa: Campaign?,
    val usnat: Campaign?
) {
    @Serializable
    data class Campaign (
        val applies: Boolean
    )
}
