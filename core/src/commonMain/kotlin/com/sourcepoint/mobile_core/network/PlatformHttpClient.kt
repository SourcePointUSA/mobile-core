package com.sourcepoint.mobile_core.network

import io.ktor.client.HttpClient

expect class PlatformHttpClient {
    companion object {
        fun create(): HttpClient
    }
}
