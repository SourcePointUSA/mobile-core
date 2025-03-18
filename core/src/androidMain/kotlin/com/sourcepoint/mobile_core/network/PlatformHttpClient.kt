package com.sourcepoint.mobile_core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual class PlatformHttpClient {
    actual companion object {
        actual fun create() = HttpClient(OkHttp)
    }
}
