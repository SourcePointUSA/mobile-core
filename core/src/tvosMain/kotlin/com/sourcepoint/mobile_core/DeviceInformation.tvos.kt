package com.sourcepoint.mobile_core

import platform.UIKit.UIDevice

actual class DeviceInformation actual constructor() {
    actual val osName: OSName = OSName.tvOS
    actual val osVersion: String = UIDevice.currentDevice.systemVersion
    actual val deviceFamily = UIDevice.currentDevice.name
}
