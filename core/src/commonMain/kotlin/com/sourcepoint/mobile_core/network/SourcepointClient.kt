package com.sourcepoint.mobile_core.network

import com.sourcepoint.core.BuildConfig
import com.sourcepoint.mobile_core.Device
import com.sourcepoint.mobile_core.DeviceInformation
import com.sourcepoint.mobile_core.models.SPCampaignType
import com.sourcepoint.mobile_core.models.SPError
import com.sourcepoint.mobile_core.models.SPNetworkError
import com.sourcepoint.mobile_core.models.SPPropertyName
import com.sourcepoint.mobile_core.models.SPUnableToParseBodyError
import com.sourcepoint.mobile_core.network.requests.ConsentStatusRequest
import com.sourcepoint.mobile_core.network.requests.MetaDataRequest
import com.sourcepoint.mobile_core.network.requests.MessagesRequest
import com.sourcepoint.mobile_core.network.requests.toQueryParams
import com.sourcepoint.mobile_core.network.responses.ConsentStatusResponse
import com.sourcepoint.mobile_core.network.responses.MessagesResponse
import com.sourcepoint.mobile_core.network.responses.MetaDataResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
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
    httpEngine: HttpClientEngine?,
    private val device: DeviceInformation,
    private val version: String
): SPClient {
    private val http = if (httpEngine != null) HttpClient(httpEngine) {
        install(ContentNegotiation) { json(json) }
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.BODY
        }
    } else HttpClient {
        install(ContentNegotiation) { json(json) }
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.BODY
        }
    }

    constructor(accountId: Int, propertyId: Int, propertyName: SPPropertyName) : this(
        accountId,
        propertyId,
        propertyName,
        httpEngine = null,
        device = Device,
        version = BuildConfig.Version
    )

    constructor(accountId: Int, propertyId: Int, propertyName: SPPropertyName, httpEngine: HttpClientEngine) : this(
        accountId,
        propertyId,
        propertyName,
        httpEngine = httpEngine,
        device = Device,
        version = BuildConfig.Version
    )

    private val baseWrapperUrl = "https://cdn.privacy-mgmt.com/"

    /**
     * @throws SPError if the network request fails or if cannot parse body to Body type
     */
    private suspend inline fun <reified Body> loggingError(
        campaignType: SPCampaignType? = null,
        requestBlock: () -> HttpResponse
    ): Body {
        val response = requestBlock()
        if (response.status.value !in 200..299) {
            throw reportErrorAndThrow(SPNetworkError(
                statusCode = response.status.value,
                path = response.request.url.pathSegments.last(),
                campaignType = campaignType
            ))
        }
        try {
            return response.body()
        } catch (_: Exception) {
            throw reportErrorAndThrow(SPUnableToParseBodyError(bodyName = Body::class.qualifiedName))
        }
    }

    override suspend fun getMetaData(campaigns: MetaDataRequest.Campaigns): MetaDataResponse = loggingError {
        http.get(
            URLBuilder(baseWrapperUrl).apply {
                path("wrapper", "v2", "meta-data")
                withParams(
                    MetaDataRequest(
                        accountId = accountId,
                        propertyId = propertyId,
                        metadata = campaigns
                    )
                )
            }.build()
        )
    }

    override suspend fun getConsentStatus(authId: String?, metadata: ConsentStatusRequest.MetaData): ConsentStatusResponse =
        http.get(URLBuilder(baseWrapperUrl).apply {
            path("wrapper", "v2", "consent-status")
            withParams(
                ConsentStatusRequest(
                    propertyId = propertyId,
                    authId = authId,
                    metadata = metadata
                )
            )}.build()
        ).body()

    override suspend fun getMessages(request: MessagesRequest): MessagesResponse =
        http.get(URLBuilder(baseWrapperUrl).apply {
            path("wrapper", "v2", "messages")
            withParams(request)
        }.build()).body()

    override suspend fun errorMetrics(error: SPError) {
        http.get(URLBuilder(baseWrapperUrl).apply {
            path("metrics", "v1", "custom-metrics")
            withParams(ErrorMetricsRequest(
                accountId = accountId.toString(),
                propertyId = propertyId.toString(),
                propertyName = propertyName,
                osVersion = device.osVersion,
                deviceFamily = device.deviceFamily,
                sdkVersion = version,
                code = error.code,
                description = error.description,
                campaignType = error.campaignType
            ))
        }.build())
    }

    suspend fun reportErrorAndThrow(error: SPError): SPError {
        errorMetrics(error)
        return error
    }
}

// Maps a Serializable class into query params using toQueryParams function
inline fun <reified T> URLBuilder.withParams(paramsObject: T) where T : Any {
    paramsObject
        .toQueryParams()
        .map { param ->
            param.value?.let { parameters.append(param.key, it) }
        }
}
