package com.sourcepoint.mobile_core.network

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
import mobile_core.core.BuildConfig

interface SPClient {
    @Throws(Exception::class)
    suspend fun getMetaData(campaigns: MetaDataMetaDataCampaigns): MetaDataResponse

    @Throws(Exception::class)
    suspend fun getConsentStatus(authId: String?, metadata: ConsentStatusMetaData): GetConsentStatusResponse
}

// TODO: implement propertyName class?
class Client(val accountId: Int, val propertyId: Int, val propertyName: String): SPClient {
    companion object {
        const val ENV_PARAM = "prod"
        const val SCRIPT_TYPE = "mobile-core" // TODO: postfix it with the platform the module is running
        const val SCRIPT_VERSION = BuildConfig.Version
    }

    private val http = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(Logging)
    }

    private val baseWrapperUrl = "https://cdn.privacy-mgmt.com/"

    private fun getMetaDataUrl(campaigns: MetaDataMetaDataCampaigns) = URLBuilder(baseWrapperUrl)
        .apply {
            path("wrapper", "v2", "meta-data")
            parameters.append("accountId", accountId.toString())
            parameters.append("propertyId", propertyId.toString())
            parameters.append("metadata", campaigns.toString())
            parameters.append("env", ENV_PARAM)
            parameters.append("scriptType", SCRIPT_TYPE)
            parameters.append("scriptVersion", SCRIPT_VERSION)
        }.build()

    @Throws(Exception::class)
    override suspend fun getMetaData(campaigns: MetaDataMetaDataCampaigns): MetaDataResponse =
        http.get(getMetaDataUrl(campaigns)).body()

    private fun getConsentStatusUrl(authId: String?, metadata: ConsentStatusMetaData) = URLBuilder(baseWrapperUrl)
        .apply {
            path("wrapper", "v2", "consent-status")
            parameters.append("propertyId", propertyId.toString())
            parameters.append("metadata", metadata.toString())
            parameters.append("includeData", IncludeData().toString())
            parameters.appendIfPresent("authId", authId)
            parameters.append("withSiteActions", "false")
            parameters.append("hasCsp", "true")
            parameters.append("env", ENV_PARAM)
            parameters.append("scriptType", SCRIPT_TYPE)
            parameters.append("scriptVersion", SCRIPT_VERSION)
        }.build()

    @Throws(Exception::class)
    override suspend fun getConsentStatus(authId: String?, metadata: ConsentStatusMetaData): GetConsentStatusResponse =
        http.get(getConsentStatusUrl(authId, metadata)).body()
}

fun ParametersBuilder.appendIfPresent(name: String, value: String?) = value?.let { append(name, it) }