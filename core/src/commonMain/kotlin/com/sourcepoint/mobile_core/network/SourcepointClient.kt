package com.sourcepoint.mobile_core.network

import com.sourcepoint.core.BuildConfig
import com.sourcepoint.mobile_core.Device
import com.sourcepoint.mobile_core.DeviceInformation
import com.sourcepoint.mobile_core.models.SPError
import com.sourcepoint.mobile_core.models.SPPropertyName
import com.sourcepoint.mobile_core.network.requests.ConsentStatusRequest
import com.sourcepoint.mobile_core.network.requests.MetaDataRequest
import com.sourcepoint.mobile_core.network.requests.MessagesRequest
import com.sourcepoint.mobile_core.network.requests.toQueryParams
import com.sourcepoint.mobile_core.network.responses.ConsentStatusResponse
import com.sourcepoint.mobile_core.network.responses.MessagesResponse
import com.sourcepoint.mobile_core.network.responses.MetaDataResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.get
import io.ktor.http.URLBuilder
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json

interface SPClient {
    suspend fun getMetaData(campaigns: MetaDataRequest.Campaigns): MetaDataResponse

    suspend fun getConsentStatus(authId: String?, metadata: ConsentStatusRequest.MetaData): ConsentStatusResponse

    suspend fun getMessages(request: MessagesRequest): MessagesResponse

    suspend fun errorMetrics(error: SPError): Unit
}

class SourcepointClient(
    private val accountId: Int,
    private val propertyId: Int,
    private val propertyName: SPPropertyName,
    private val http: HttpClient,
): SPClient {
    constructor(accountId: Int, propertyId: Int, propertyName: SPPropertyName) : this(
        accountId,
        propertyId,
        propertyName,
        HttpClient {
            install(ContentNegotiation) { json(json) }
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.BODY
            }
        },
    )

    private val baseWrapperUrl = "https://cdn.privacy-mgmt.com/"

    private fun getMetaDataUrl(campaigns: MetaDataRequest.Campaigns) =
        URLBuilder(baseWrapperUrl)
            .apply {
                path("wrapper", "v2", "meta-data")
                withParams(MetaDataRequest(accountId = accountId, propertyId = propertyId, metadata = campaigns))
            }.build()

    override suspend fun getMetaData(campaigns: MetaDataRequest.Campaigns): MetaDataResponse =
        http.get(getMetaDataUrl(campaigns)).body()

    private fun getConsentStatusUrl(authId: String?, metadata: ConsentStatusRequest.MetaData) =
        URLBuilder(baseWrapperUrl)
            .apply {
                path("wrapper", "v2", "consent-status")
                withParams(ConsentStatusRequest(propertyId = propertyId, authId = authId, metadata = metadata))
            }.build()

    override suspend fun getConsentStatus(authId: String?, metadata: ConsentStatusRequest.MetaData): ConsentStatusResponse =
        http.get(getConsentStatusUrl(authId, metadata)).body()

    private fun getMessagesUrl(request: MessagesRequest) =
        URLBuilder(baseWrapperUrl)
            .apply {
                path("wrapper", "v2", "messages")
                withParams(request)
            }.build()

    override suspend fun getMessages(request: MessagesRequest): MessagesResponse =
        http.get(getMessagesUrl(request)).body()
}

// Maps a Serializable class into query params using toQueryParams function
inline fun <reified T> URLBuilder.withParams(paramsObject: T) where T : Any {
    paramsObject
        .toQueryParams()
        .map { param ->
            param.value?.let { parameters.append(param.key, it) }
        }
}
