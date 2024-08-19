package com.sourcepoint.mobile_core

import com.sourcepoint.mobile_core.network.Client
import com.sourcepoint.mobile_core.network.requests.MetaDataMetaDataCampaigns
import kotlinx.coroutines.test.runTest
import kotlin.experimental.ExperimentalNativeApi
import kotlin.test.Test

class AppleGreetingTest {
    @OptIn(ExperimentalNativeApi::class)
    @Test
    fun testExample() = runTest {
        val client = Client(accountId = 22, propertyId = 16893, propertyName = "mobile.multicampaign.demo")
        assert(client.getMetaData(MetaDataMetaDataCampaigns(gdpr = MetaDataMetaDataCampaigns.Campaign())).gdpr?.applies == true)
    }
}