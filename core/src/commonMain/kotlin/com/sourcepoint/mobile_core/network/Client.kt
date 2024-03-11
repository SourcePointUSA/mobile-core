package com.sourcepoint.mobile_core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.URLBuilder
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface SPClient {
    @Throws(Exception::class)
    suspend fun getMetaData(campaigns: MetaDataCampaigns): MetadataResponse
}

// TODO: implement propertyName class?
class Client(val accountId: Int, val propertyId: Int, val propertyName: String): SPClient {
    @OptIn(ExperimentalSerializationApi::class)
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }
    }

    private val defaultParams = mapOf(
        "env" to "prod",
        "scriptVersion" to "0.0.1",
        "scriptType" to "mobile-core",
    )

    @Throws(Exception::class)
    override suspend fun getMetaData(campaigns: MetaDataCampaigns): MetadataResponse {
        val baseUrl = "https://cdn.privacy-mgmt.com/wrapper/v2/meta-data"
        val urlBuilder = URLBuilder(baseUrl)
        urlBuilder.parameters.apply {
            MetadataRequest(accountId = accountId, propertyId = propertyId, metadata = campaigns)
                .toParams().forEach { append(it.key, it.value)  }
            defaultParams.forEach { append(it.key, it.value) }
        }
        val url = urlBuilder.build()
        println(url) // TODO: implement logging
        return httpClient.get(url).body()
    }
}
