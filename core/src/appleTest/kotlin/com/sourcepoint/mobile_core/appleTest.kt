package com.sourcepoint.mobile_core

import com.sourcepoint.mobile_core.network.Client
import com.sourcepoint.mobile_core.network.requests.MetaData
import kotlinx.coroutines.test.runTest
import kotlin.experimental.ExperimentalNativeApi
import kotlin.test.Test

class AppleGreetingTest {
    @OptIn(ExperimentalNativeApi::class)
    @Test
    fun testExample() = runTest {
        val client = Client(accountId = 22, propertyId = 16893, propertyName = "mobile.multicampaign.demo")
        assert(client.getMetaData(MetaData.Campaigns(gdpr = MetaData.Campaigns.Campaign())).gdpr?.applies == true)
    }
}
