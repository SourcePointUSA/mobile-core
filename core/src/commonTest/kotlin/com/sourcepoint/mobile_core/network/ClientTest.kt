package com.sourcepoint.mobile_core.network

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientTest {
    @Test
    fun generatedGdprApplies() = runTest {
        val api = Client(accountId = 22, propertyId = 16893, propertyName = "https://mobile.multicampaign.demo")
        val response = api.getMetaData(MetaDataCampaigns(
            gdpr = MetaDataCampaigns.Campaign(),
            usnat = MetaDataCampaigns.Campaign()
        ))
        assertEquals(response.gdpr?.applies, true)
        assertEquals(response.usnat?.applies, true)
    }
}
