package com.sourcepoint.mobile_core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURLCredential
import platform.Foundation.credentialForTrust
import platform.Foundation.serverTrust

actual class PlatformHttpClient {
    actual companion object {
        @OptIn(ExperimentalForeignApi::class)
        actual fun create() = HttpClient(Darwin) {
            engine {
                handleChallenge { _, _, challenge, completionHandler ->
                    completionHandler(
                        0, // -> NSURLSessionAuthChallengeUseCredential (using the enum directly for some reason gets the kotlin compiler confused)
                        challenge.protectionSpace.serverTrust?.let {
                            NSURLCredential.credentialForTrust(it)
                        }
                    )
                }
            }
        }
    }
}
