package com.sourcepoint.mobile_core.network

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
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.http.URLBuilder
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json

interface SPClient {
    @Throws(Exception::class)
    suspend fun getMetaData(campaigns: MetaDataRequest.Campaigns): MetaDataResponse

    @Throws(Exception::class)
    suspend fun getConsentStatus(authId: String?, metadata: ConsentStatusRequest.MetaData): ConsentStatusResponse

    @Throws(Exception::class)
    suspend fun getMessages(request: MessagesRequest): MessagesResponse
}

class SourcepointClient(
    val accountId: Int,
    val propertyId: Int,
    val propertyName: SPPropertyName,
    private val http: HttpClient = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(Logging)
    }
): SPClient {
    private val baseWrapperUrl = "https://cdn.privacy-mgmt.com/"

    private fun getMetaDataUrl(campaigns: MetaDataRequest.Campaigns) =
        URLBuilder(baseWrapperUrl)
            .apply {
                path("wrapper", "v2", "meta-data")
                withParams(MetaDataRequest(accountId = accountId, propertyId = propertyId, metadata = campaigns))
            }.build()

    @Throws(Exception::class)
    override suspend fun getMetaData(campaigns: MetaDataRequest.Campaigns): MetaDataResponse =
        http.get(getMetaDataUrl(campaigns)).body()

    private fun getConsentStatusUrl(authId: String?, metadata: ConsentStatusRequest.MetaData) =
        URLBuilder(baseWrapperUrl)
            .apply {
                path("wrapper", "v2", "consent-status")
                withParams(ConsentStatusRequest(propertyId = propertyId, authId = authId, metadata = metadata))
            }.build()

    @Throws(Exception::class)
    override suspend fun getConsentStatus(authId: String?, metadata: ConsentStatusRequest.MetaData): ConsentStatusResponse =
        http.get(getConsentStatusUrl(authId, metadata)).body()

    private fun getMessagesUrl(request: MessagesRequest) =
        URLBuilder(baseWrapperUrl)
            .apply {
                path("wrapper", "v2", "messages")
                withParams(request)
            }.build()

    @Throws(Exception::class)
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
