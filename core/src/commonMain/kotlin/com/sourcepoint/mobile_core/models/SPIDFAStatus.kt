package com.sourcepoint.mobile_core.models

import kotlinx.serialization.SerialName

enum class SPIDFAStatus {
    @SerialName("unknown") Unknown,
    @SerialName("accepted") Accepted,
    @SerialName("denied") Denied,
    @SerialName("unavailable") Unavailable;

    companion object {
        fun current(): SPIDFAStatus? {
            return null
        }
    }
}
