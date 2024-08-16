package com.sourcepoint.mobile_core.network

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientTest {
    private val api = Client(accountId = 22, propertyId = 16893, propertyName = "https://mobile.multicampaign.demo")

    @Test
    fun generatedGdprApplies() = runTest {
        val response = api.getMetaData(MetaDataMetaDataCampaigns(
            gdpr = MetaDataMetaDataCampaigns.Campaign(),
            usnat = MetaDataMetaDataCampaigns.Campaign()
        ))
        assertEquals(response.gdpr?.applies, true)
        assertEquals(response.usnat?.applies, true)
    }
}
