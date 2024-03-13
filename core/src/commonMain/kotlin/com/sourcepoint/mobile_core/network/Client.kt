package com.sourcepoint.mobile_core.network

import com.sourcepoint.wrapper.client.api.DefaultApi
import com.sourcepoint.wrapper.client.model.GetMetaData200Response
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import mobile_core.core.BuildConfig

interface SPClient {
    @Throws(Exception::class)
    suspend fun getMetaData(campaigns: MetaDataCampaigns): GetMetaData200Response
}

// TODO: implement propertyName class?
class Client(val accountId: Int, val propertyId: Int, val propertyName: String): SPClient {
    companion object {
        const val ENV_PARAM = "prod"
        const val SCRIPT_TYPE = "mobile-core"
        const val SCRIPT_VERSION = BuildConfig.Version
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val httpClient = DefaultApi(
        httpClientConfig = {
            it.install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    explicitNulls = false
                })
            }

            it.install(Logging)
        }
    )

    @Throws(Exception::class)
    override suspend fun getMetaData(campaigns: MetaDataCampaigns) =
        httpClient.getMetaData(
            accountId = accountId,
            propertyId = propertyId,
            metadata = campaigns,
            env = ENV_PARAM,
            scriptType = SCRIPT_TYPE,
            scriptVersion = SCRIPT_VERSION
        ).body()
}
