package com.sourcepoint.mobile_core.network

import com.sourcepoint.core.BuildConfig
import com.sourcepoint.mobile_core.DeviceInformation
import com.sourcepoint.mobile_core.models.InvalidChoiceAllParamsError
import com.sourcepoint.mobile_core.models.SPActionType
import com.sourcepoint.mobile_core.models.SPCampaignType
import com.sourcepoint.mobile_core.models.SPError
import com.sourcepoint.mobile_core.models.SPIDFAStatus
import com.sourcepoint.mobile_core.models.SPNetworkError
import com.sourcepoint.mobile_core.models.SPPropertyName
import com.sourcepoint.mobile_core.models.SPUnableToParseBodyError
import com.sourcepoint.mobile_core.models.consents.GDPRConsent
import com.sourcepoint.mobile_core.network.requests.CCPAChoiceRequest
import com.sourcepoint.mobile_core.network.requests.ChoiceAllRequest
import com.sourcepoint.mobile_core.network.requests.ConsentStatusRequest
import com.sourcepoint.mobile_core.network.requests.CustomConsentRequest
import com.sourcepoint.mobile_core.network.requests.DefaultRequest
import com.sourcepoint.mobile_core.network.requests.GDPRChoiceRequest
import com.sourcepoint.mobile_core.network.requests.IDFAStatusReportRequest
import com.sourcepoint.mobile_core.network.requests.IncludeData
import com.sourcepoint.mobile_core.network.requests.MetaDataRequest
import com.sourcepoint.mobile_core.network.requests.MessagesRequest
import com.sourcepoint.mobile_core.network.requests.PvDataRequest
import com.sourcepoint.mobile_core.network.requests.USNatChoiceRequest
import com.sourcepoint.mobile_core.network.requests.toQueryParams
import com.sourcepoint.mobile_core.network.responses.CCPAChoiceResponse
import com.sourcepoint.mobile_core.network.responses.ChoiceAllResponse
import com.sourcepoint.mobile_core.network.responses.ConsentStatusResponse
import com.sourcepoint.mobile_core.network.responses.GDPRChoiceResponse
import com.sourcepoint.mobile_core.network.responses.MessagesResponse
import com.sourcepoint.mobile_core.network.responses.MetaDataResponse
import com.sourcepoint.mobile_core.network.responses.PvDataResponse
import com.sourcepoint.mobile_core.network.responses.USNatChoiceResponse
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlin.reflect.KSuspendFunction1

interface SPClient {
    @Throws(Exception::class) suspend fun getMetaData(campaigns: MetaDataRequest.Campaigns): MetaDataResponse

    @Throws(Exception::class) suspend fun postPvData(request: PvDataRequest): PvDataResponse

    @Throws(Exception::class) suspend fun getConsentStatus(authId: String?, metadata: ConsentStatusRequest.MetaData): ConsentStatusResponse

    @Throws(Exception::class) suspend fun postChoiceGDPRAction(
        actionType: SPActionType,
        request: GDPRChoiceRequest
    ): GDPRChoiceResponse

    @Throws(Exception::class) suspend fun postChoiceCCPAAction(
        actionType: SPActionType,
        request: CCPAChoiceRequest
    ): CCPAChoiceResponse

    @Throws(Exception::class) suspend fun postChoiceUSNatAction(
        actionType: SPActionType,
        request: USNatChoiceRequest
    ): USNatChoiceResponse

    @Throws(Exception::class) suspend fun getChoiceAll(
        actionType: SPActionType,
        campaigns: ChoiceAllRequest.ChoiceAllCampaigns
    ): ChoiceAllResponse

    @Throws(Exception::class) suspend fun getMessages(request: MessagesRequest): MessagesResponse

    suspend fun postReportIdfaStatus(
        propertyId: Int?,
        uuid: String?,
        requestUUID: String,
        uuidType: SPCampaignType?,
        messageId: Int?,
        idfaStatus: SPIDFAStatus,
        iosVersion: String,
        partitionUUID: String?
    )

    @Throws(Exception::class) suspend fun customConsentGDPR(
        consentUUID: String,
        propertyId: Int,
        vendors: List<String>,
        categories: List<String>,
        legIntCategories: List<String>
    ): GDPRConsent

    @Throws(Exception::class) suspend fun deleteCustomConsentGDPR(
        consentUUID: String,
        propertyId: Int,
        vendors: List<String>,
        categories: List<String>,
        legIntCategories: List<String>
    ): GDPRConsent

    suspend fun errorMetrics(error: SPError)

}

class SourcepointClient(
    private val accountId: Int,
    private val propertyId: Int,
    private val propertyName: SPPropertyName,
    httpEngine: HttpClientEngine?,
    private val device: DeviceInformation,
    private val version: String,
    private val requestTimeoutInSeconds: Int
): SPClient {
    private val config:  HttpClientConfig<*>.() -> Unit = {
        install(HttpTimeout) { requestTimeoutMillis = requestTimeoutInSeconds.toLong() * 1000 }
        install(WrapHttpTimeoutError) { timeoutInSeconds = requestTimeoutInSeconds }
        install(ContentNegotiation) { json(json) }
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.BODY
        }
        expectSuccess = false
        HttpResponseValidator {
            validateResponse { response ->
                if (response.request.url.segments.contains("custom-metrics")) {
                    return@validateResponse
                }

                if (response.status.value !in 200..299) {
                    throw reportErrorAndThrow(SPNetworkError(
                        statusCode = response.status.value,
                        path = response.request.url.segments.last(),
                        campaignType = null
                    ))
                }
            }
        }
    }
    private val http = if (httpEngine != null) HttpClient(httpEngine, config) else HttpClient(config)

    constructor(
        accountId: Int,
        propertyId: Int,
        propertyName: SPPropertyName,
        requestTimeoutInSeconds: Int = 5
    ) : this(
        accountId,
        propertyId,
        propertyName,
        httpEngine = null,
        device = DeviceInformation(),
        version = BuildConfig.Version,
        requestTimeoutInSeconds = requestTimeoutInSeconds
    )

    constructor(
        accountId: Int,
        propertyId: Int,
        propertyName: SPPropertyName,
        httpEngine: HttpClientEngine,
        requestTimeoutInSeconds: Int = 5,
    ) : this(
        accountId,
        propertyId,
        propertyName,
        httpEngine = httpEngine,
        device = DeviceInformation(),
        version = BuildConfig.Version,
        requestTimeoutInSeconds = requestTimeoutInSeconds
    )

    private val baseWrapperUrl = "https://cdn.privacy-mgmt.com/"

    override suspend fun getMetaData(campaigns: MetaDataRequest.Campaigns): MetaDataResponse = http.get(
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
    ).bodyOr(::reportErrorAndThrow)

    override suspend fun postPvData(request: PvDataRequest): PvDataResponse =
        http.post(URLBuilder(baseWrapperUrl).apply {
            path("wrapper", "v2", "pv-data")
            withParams(DefaultRequest())
            }.build()) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.bodyOr(::reportErrorAndThrow)

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
        ).bodyOr(::reportErrorAndThrow)

    override suspend fun postChoiceGDPRAction(
        actionType: SPActionType,
        request: GDPRChoiceRequest
    ): GDPRChoiceResponse =
        http.post(URLBuilder(baseWrapperUrl).apply {
            path("wrapper", "v2", "choice", "gdpr", actionType.type.toString())
            withParams(DefaultRequest())
        }.build()) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.bodyOr(::reportErrorAndThrow)

    override suspend fun postChoiceUSNatAction(
        actionType: SPActionType,
        request: USNatChoiceRequest
    ): USNatChoiceResponse =
        http.post(URLBuilder(baseWrapperUrl).apply {
            path("wrapper", "v2", "choice", "usnat", actionType.type.toString())
            withParams(DefaultRequest())
        }.build()) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.bodyOr(::reportErrorAndThrow)

    override suspend fun postChoiceCCPAAction(
        actionType: SPActionType,
        request: CCPAChoiceRequest
    ): CCPAChoiceResponse =
        http.post(URLBuilder(baseWrapperUrl).apply {
            path("wrapper", "v2", "choice", "ccpa", actionType.type.toString())
            withParams(DefaultRequest())
        }.build()) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.bodyOr(::reportErrorAndThrow)

    override suspend fun getChoiceAll(
        actionType: SPActionType,
        campaigns: ChoiceAllRequest.ChoiceAllCampaigns
    ): ChoiceAllResponse {
        val choicePath = when (actionType) {
            SPActionType.AcceptAll -> { "consent-all" }
            SPActionType.RejectAll -> { "reject-all" }
            else -> throw InvalidChoiceAllParamsError()
        }
        return http.get(URLBuilder(baseWrapperUrl).apply {
            path("wrapper", "v2", "choice", choicePath)
            withParams(ChoiceAllRequest(accountId, propertyId, campaigns))
        }.build()).bodyOr(::reportErrorAndThrow)
    }

    override suspend fun getMessages(request: MessagesRequest): MessagesResponse =
        http.get(URLBuilder(baseWrapperUrl).apply {
            path("wrapper", "v2", "messages")
            withParams(request)
        }.build()).bodyOr(::reportErrorAndThrow)

    override suspend fun postReportIdfaStatus(
        propertyId: Int?,
        uuid: String?,
        requestUUID: String,
        uuidType: SPCampaignType?,
        messageId: Int?,
        idfaStatus: SPIDFAStatus,
        iosVersion: String,
        partitionUUID: String?
    ) {
        http.post(URLBuilder(baseWrapperUrl).apply {
            path("wrapper", "metrics", "v1", "apple-tracking")
            withParams(DefaultRequest())
        }.build()) {
            contentType(ContentType.Application.Json)
            setBody(
                IDFAStatusReportRequest(
                    accountId = accountId,
                    propertyId = propertyId,
                    uuid = uuid,
                    uuidType = uuidType,
                    requestUUID = requestUUID,
                    iosVersion = iosVersion,
                    appleTracking = IDFAStatusReportRequest.AppleTrackingPayload(
                        appleChoice = idfaStatus,
                        appleMsgId = messageId,
                        messagePartitionUUID = partitionUUID
                    )
                )
            )
        }
    }

    override suspend fun customConsentGDPR(
        consentUUID: String,
        propertyId: Int,
        vendors: List<String>,
        categories: List<String>,
        legIntCategories: List<String>
    ): GDPRConsent =
        http.post(URLBuilder(baseWrapperUrl).apply {
            path("wrapper", "tcfv2", "v1", "gdpr", "custom-consent")
            withParams(DefaultRequest())
        }.build()) {
            contentType(ContentType.Application.Json)
            setBody(
                CustomConsentRequest(
                    consentUUID = consentUUID,
                    propertyId = propertyId,
                    vendors = vendors,
                    categories = categories,
                    legIntCategories = legIntCategories
            ))
        }.bodyOr(::reportErrorAndThrow)

    override suspend fun deleteCustomConsentGDPR(
        consentUUID: String,
        propertyId: Int,
        vendors: List<String>,
        categories: List<String>,
        legIntCategories: List<String>
    ): GDPRConsent =
        http.delete(URLBuilder(baseWrapperUrl).apply {
            path("consent", "tcfv2", "consent", "v3", "custom")
            appendPathSegments(propertyId.toString())
            withParams(mapOf("consentUUID" to consentUUID))
        }.build()) {
            contentType(ContentType.Application.Json)
            setBody(
                CustomConsentRequest(
                    consentUUID = consentUUID,
                    propertyId = propertyId,
                    vendors = vendors,
                    categories = categories,
                    legIntCategories = legIntCategories
            ))
        }.bodyOr(::reportErrorAndThrow)

    override suspend fun errorMetrics(error: SPError) {
        http.post(URLBuilder(baseWrapperUrl).apply {
            path("wrapper", "metrics", "v1", "custom-metrics")
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

    private suspend fun reportErrorAndThrow(error: SPError): SPError {
        errorMetrics(error)
        return error
    }
}

suspend inline fun <reified T> HttpResponse.bodyOr(loggingFunction: KSuspendFunction1<SPError, SPError>): T {
    try {
        return body()
    } catch (_: Exception) {
        throw loggingFunction(SPUnableToParseBodyError(bodyName = T::class.qualifiedName))
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
