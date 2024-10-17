package com.sourcepoint.mobile_core

enum class OSName {
    iOS,
    tvOS,
    Android
}

expect class DeviceInformation() {
    val osName: OSName
    val osVersion: String
    val deviceFamily: String
}
