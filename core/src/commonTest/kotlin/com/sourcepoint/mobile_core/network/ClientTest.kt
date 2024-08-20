package com.sourcepoint.mobile_core.network

import com.sourcepoint.mobile_core.network.requests.ConsentStatus
import com.sourcepoint.mobile_core.network.requests.MetaData
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ClientTest {
    private val api = Client(accountId = 22, propertyId = 16893, propertyName = "https://mobile.multicampaign.demo")

    @Test
    fun getMetaData() = runTest {
        val response = api.getMetaData(
            MetaData.Campaigns(
                gdpr = MetaData.Campaigns.Campaign(),
                usnat = MetaData.Campaigns.Campaign()
            )
        )
        assertEquals(response.gdpr?.applies, true)
        assertEquals(response.usnat?.applies, true)
    }

    @Test
    fun getConsentStatus() = runTest {
        val response = api.getConsentStatus(
            authId = null,
            metadata = ConsentStatus.MetaData(
                gdpr = ConsentStatus.MetaData.GDPR(
                    applies = true,
                    uuid = "654c39d4-b75d-4aac-925c-6322a7cc1622_28",
                ),
                usnat = ConsentStatus.MetaData.USNat(
                    applies = true,
                    uuid = "11a0fe1c-bd4a-43bb-b179-c015f63882bc_7",
                )
            )
        )

        assertNotEquals("", response.localState)
        assertEquals("654c39d4-b75d-4aac-925c-6322a7cc1622_28", response.consentStatusData.gdpr?.uuid)
        assertEquals("11a0fe1c-bd4a-43bb-b179-c015f63882bc_7", response.consentStatusData.usnat?.uuid)
    }
}
