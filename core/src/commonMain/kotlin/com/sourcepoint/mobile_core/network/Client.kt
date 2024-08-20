package com.sourcepoint.mobile_core.network

import com.sourcepoint.mobile_core.models.SPPropertyName
import com.sourcepoint.mobile_core.network.requests.ConsentStatus
import com.sourcepoint.mobile_core.network.requests.MetaData
import com.sourcepoint.mobile_core.network.requests.toQueryParams
import com.sourcepoint.mobile_core.network.responses.GetConsentStatusResponse
import com.sourcepoint.mobile_core.network.responses.MetaDataResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.http.ParametersBuilder
import io.ktor.http.URLBuilder
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json

interface SPClient {
    @Throws(Exception::class)
    suspend fun getMetaData(campaigns: MetaData.Campaigns): MetaDataResponse

    @Throws(Exception::class)
    suspend fun getConsentStatus(authId: String?, metadata: ConsentStatus.MetaData): GetConsentStatusResponse
}

class Client(val accountId: Int, val propertyId: Int, val propertyName: SPPropertyName): SPClient {
    private val http = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(Logging)
    }

    private val baseWrapperUrl = "https://cdn.privacy-mgmt.com/"

    private fun getMetaDataUrl(campaigns: MetaData.Campaigns) = URLBuilder(baseWrapperUrl)
        .apply {
            path("wrapper", "v2", "meta-data")
            MetaData(
                accountId = accountId,
                propertyId = propertyId,
                metadata = campaigns
            )
                .toQueryParams()
                .map { param ->
                    param.value?.let { parameters.append(param.key, it) }
                }
        }.build()

    @Throws(Exception::class)
    override suspend fun getMetaData(campaigns: MetaData.Campaigns): MetaDataResponse =
        http.get(getMetaDataUrl(campaigns)).body()

    private fun getConsentStatusUrl(authId: String?, metadata: ConsentStatus.MetaData) = URLBuilder(baseWrapperUrl)
        .apply {
            path("wrapper", "v2", "consent-status")
            ConsentStatus(
                propertyId = propertyId,
                authId = authId,
                metadata = metadata,
            )
                .toQueryParams()
                .map { param ->
                    param.value?.let { parameters.append(param.key, it) }
                }
        }.build()

    @Throws(Exception::class)
    override suspend fun getConsentStatus(authId: String?, metadata: ConsentStatus.MetaData): GetConsentStatusResponse =
        http.get(getConsentStatusUrl(authId, metadata)).body()
}

fun ParametersBuilder.appendIfPresent(name: String, value: String?) = value?.let { append(name, it) }
