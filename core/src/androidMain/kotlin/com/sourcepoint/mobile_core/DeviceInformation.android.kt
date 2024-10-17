package com.sourcepoint.mobile_core

import android.os.Build

actual class DeviceInformation actual constructor() {
    actual val osName: OSName = OSName.Android
    actual val osVersion = Build.VERSION.SDK_INT.toString()
    actual val deviceFamily = Build.MODEL ?: "model-unknown"
}
