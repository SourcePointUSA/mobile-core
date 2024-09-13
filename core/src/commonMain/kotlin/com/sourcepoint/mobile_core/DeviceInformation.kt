package com.sourcepoint.mobile_core

enum class OSName {
    iOS,
    tvOS,
    Android
}

interface DeviceInformation {
    val osName: OSName
    val osVersion: String
    val deviceName: String
}

expect class DeviceInformationConcrete(): DeviceInformation

val Device = DeviceInformationConcrete()
