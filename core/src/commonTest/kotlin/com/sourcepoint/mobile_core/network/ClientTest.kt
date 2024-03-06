package com.sourcepoint.mobile_core.network

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientTest {

    @Test
    fun gdprApplies() = runTest {
        val client = Client(accountId = 22, propertyId = 16893, propertyName = "")
        assertEquals(client.getMetaData(campaigns = MetaDataCampaigns(gdpr = MetaDataCampaigns.Campaign())).gdpr?.applies, true)
    }
}