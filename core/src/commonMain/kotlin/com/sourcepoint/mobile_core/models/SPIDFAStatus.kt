package com.sourcepoint.mobile_core.models

// TODO implement IDFA logic for both platforms
// - Android should default to null
// - iOS should have logic to get it from OS
enum class SPIDFAStatus(val value: String) {
    Unknown("unknown"),
    Accepted("accepted"),
    Denied("denied"),
    Unavailable("unavailable");

    companion object {
        fun current(): SPIDFAStatus? {
            return null
        }
    }
}
