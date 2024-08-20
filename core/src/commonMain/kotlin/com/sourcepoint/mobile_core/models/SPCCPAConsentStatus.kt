package com.sourcepoint.mobile_core.models

enum class SPCCPAConsentStatus(val status: String) {
    ConsentedAll("consentedAll"),
    RejectedAll("rejectedAll"),
    RejectedSome("rejectedSome"),
    RejectedNone("rejectedNone"),
    LinkedNoAction("linkedNoAction"),
    Unknown("unknown");
}
