package com.sourcepoint.mobile_core

import android.os.Build

actual class DeviceInformationConcrete actual constructor() : DeviceInformation {
    override val osName: OSName = OSName.Android
    override val osVersion = Build.VERSION.SDK_INT.toString()
    override val deviceFamily = Build.MODEL ?: "model-unknown"
}
