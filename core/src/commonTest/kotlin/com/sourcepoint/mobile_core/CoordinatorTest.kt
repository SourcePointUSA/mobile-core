package com.sourcepoint.mobile_core

import com.russhwolf.settings.MapSettings
import com.sourcepoint.mobile_core.network.requests.MetaDataRequest
import com.sourcepoint.mobile_core.storage.Repository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains

class CoordinatorTest {

    @Test
    fun metaDataIsCached() = runTest {
        val storage = MapSettings()
        val repository = Repository(storage)
        val coordinator = Coordinator(accountId = 22, propertyId = 16893, propertyName = "",  repository)
        val campaigns = MetaDataRequest.Campaigns(gdpr = MetaDataRequest.Campaigns.Campaign())
        val metaData = coordinator.getMetaData(campaigns)
        assertContains(metaData, "/meta-data")
        assertContains(storage.keys, "MetaData")
    }
}
