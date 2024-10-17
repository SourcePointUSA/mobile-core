package com.sourcepoint.mobile_core

import platform.UIKit.UIDevice

actual class DeviceInformation actual constructor() {
    actual val osName = OSName.iOS
    actual val osVersion = UIDevice.currentDevice.systemVersion
    actual val deviceFamily = UIDevice.currentDevice.name
}
