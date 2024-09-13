package com.sourcepoint.mobile_core

import platform.UIKit.UIDevice

actual class DeviceInformationConcrete actual constructor() : DeviceInformation {
    override val osName = OSName.iOS
    override val osVersion = UIDevice.currentDevice.systemVersion
    override val deviceName = UIDevice.currentDevice.name
}
