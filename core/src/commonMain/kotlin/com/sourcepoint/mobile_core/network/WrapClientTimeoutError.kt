package com.sourcepoint.mobile_core.network

import com.sourcepoint.mobile_core.models.SPClientTimeout
import com.sourcepoint.mobile_core.models.SPUnknownNetworkError
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import kotlin.coroutines.cancellation.CancellationException

class WrapHttpTimeoutErrorConfig {
    var timeoutInSeconds: Int = 5
}

// Wraps CancellationException (HttpTimeoutException) into SPClientTimeout
val WrapHttpTimeoutError = createClientPlugin(name = "WrapHttpTimeoutError", ::WrapHttpTimeoutErrorConfig) {
    on(Send) { request ->
        try {
            proceed(request)
        } catch (error: Exception) {
            val path = request.url.pathSegments.last()
            when (error) {
                is CancellationException,
                is HttpRequestTimeoutException -> throw SPClientTimeout(
                    timeoutInSeconds = pluginConfig.timeoutInSeconds,
                    path = path
                )
                else -> throw SPUnknownNetworkError(path)
            }
        }
    }
}
