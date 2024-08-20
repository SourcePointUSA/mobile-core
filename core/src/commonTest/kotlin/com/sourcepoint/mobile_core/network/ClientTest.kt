package com.sourcepoint.mobile_core.network

import com.sourcepoint.mobile_core.network.requests.ConsentStatusRequest
import com.sourcepoint.mobile_core.network.requests.MetaDataRequest
import com.sourcepoint.mobile_core.models.SPCampaignEnv
import com.sourcepoint.mobile_core.models.SPMessageLanguage
import com.sourcepoint.mobile_core.network.responses.MessagesResponse
import com.sourcepoint.mobile_core.network.requests.MessagesRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class ClientTest {
    private val api = Client(
        accountId = 22,
        propertyId = 16893,
        propertyName = "https://mobile.multicampaign.demo"
    )

    @Test
    fun getMetaData() = runTest {
        val response = api.getMetaData(
            MetaDataRequest.Campaigns(
                gdpr = MetaDataRequest.Campaigns.Campaign(),
                usnat = MetaDataRequest.Campaigns.Campaign()
            )
        )
        assertEquals(response.gdpr?.applies, true)
        assertEquals(response.usnat?.applies, true)
    }

    @Test
    fun getConsentStatus() = runTest {
        val response = api.getConsentStatus(
            authId = null,
            metadata = ConsentStatusRequest.MetaData(
                gdpr = ConsentStatusRequest.MetaData.GDPR(
                    applies = true,
                    uuid = "654c39d4-b75d-4aac-925c-6322a7cc1622_28",
                ),
                usnat = ConsentStatusRequest.MetaData.USNat(
                    applies = true,
                    uuid = "11a0fe1c-bd4a-43bb-b179-c015f63882bc_7",
                )
            )
        )

        assertNotEquals("", response.localState)
        assertEquals(
            "654c39d4-b75d-4aac-925c-6322a7cc1622_28",
            response.consentStatusData.gdpr?.uuid
        )
        assertEquals(
            "11a0fe1c-bd4a-43bb-b179-c015f63882bc_7",
            response.consentStatusData.usnat?.uuid
        )
    }

    @Test
    fun getMessages() = runTest {
        val response = api.getMessages(
            MessagesRequest(
                body = MessagesRequest.Body(
                    propertyHref = api.propertyName,
                    accountId = api.accountId,
                    campaigns = MessagesRequest.Body.Campaigns(
                        gdpr = MessagesRequest.Body.Campaigns.GDPR(
                            targetingParams = null,
                            hasLocalData = false,
                            consentStatus = com.sourcepoint.mobile_core.models.ConsentStatus()
                        ),
                        usnat = MessagesRequest.Body.Campaigns.USNat(
                            targetingParams = null,
                            hasLocalData = false,
                            consentStatus = com.sourcepoint.mobile_core.models.ConsentStatus()
                        ),
                        ios14 = null
                    ),
                    consentLanguage = SPMessageLanguage.ENGLISH,
                    campaignEnv = SPCampaignEnv.PUBLIC
                ),
                metadata = MessagesRequest.MetaData(
                    gdpr = MessagesRequest.MetaData.Campaign(applies = true),
                    usnat = MessagesRequest.MetaData.Campaign(applies = true)
                ),
                localState = null,
                nonKeyedLocalState = null
            )
        )

        assertNotNull((response.campaigns.find { it.type == "GDPR" } as MessagesResponse.GDPR).tcData)
        assertNotNull((response.campaigns.find { it.type == "usnat" } as MessagesResponse.USNat).gppData)
        assertNotEquals("", response.localState)
        assertNotEquals("", response.nonKeyedLocalState)
    }
}
