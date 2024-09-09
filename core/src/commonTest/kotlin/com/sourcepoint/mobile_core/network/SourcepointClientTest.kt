package com.sourcepoint.mobile_core.network

import com.sourcepoint.mobile_core.network.requests.ConsentStatusRequest
import com.sourcepoint.mobile_core.network.requests.MetaDataRequest
import com.sourcepoint.mobile_core.models.SPCampaignEnv
import com.sourcepoint.mobile_core.models.SPMessageLanguage
import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import com.sourcepoint.mobile_core.network.responses.MessagesResponse
import com.sourcepoint.mobile_core.network.requests.MessagesRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SourcepointClientTest {
    private val api = SourcepointClient(
        accountId = 22,
        propertyId = 16893,
        propertyName = "https://mobile.multicampaign.demo"
    )

    @Test
    fun getMetaData() = runTest {
        val response = api.getMetaData(
            MetaDataRequest.Campaigns(
                gdpr = MetaDataRequest.Campaigns.Campaign(),
                usnat = MetaDataRequest.Campaigns.Campaign(),
                ccpa = MetaDataRequest.Campaigns.Campaign()
            )
        )
        assertEquals(response.gdpr?.applies, true)
        assertEquals(response.usnat?.applies, true)
        assertEquals(response.ccpa?.applies, true)
    }

    @Test
    fun getConsentStatus() = runTest {
        val response = api.getConsentStatus(
            authId = null,
            metadata = ConsentStatusRequest.MetaData(
                gdpr = ConsentStatusRequest.MetaData.Campaign(
                    applies = true,
                    uuid = "654c39d4-b75d-4aac-925c-6322a7cc1622_28",
                ),
                usnat = ConsentStatusRequest.MetaData.USNatCampaign(
                    applies = true,
                    uuid = "11a0fe1c-bd4a-43bb-b179-c015f63882bc_7",
                ),
                ccpa = ConsentStatusRequest.MetaData.Campaign(
                    applies = true,
                    uuid = "ebf53e4d-e0da-4f47-95f0-7e286e8124c3",
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
        assertEquals(
            "ebf53e4d-e0da-4f47-95f0-7e286e8124c3",
            response.consentStatusData.ccpa?.uuid
        )
    }

    private fun assertCampaignConsents(campaign: MessagesResponse.Campaign) {
        when(campaign) {
            is MessagesResponse.GDPR -> assertCampaignConsents(campaign)
            is MessagesResponse.USNat -> assertCampaignConsents(campaign)
        }
    }

    private fun assertCampaignConsents(gdpr: MessagesResponse.GDPR) {
        assertNotEquals("", gdpr.euconsent)
        assertTrue(gdpr.tcData.isNotEmpty())
        assertTrue(gdpr.grants.isNotEmpty())
    }

    private fun assertCampaignConsents(usnat: MessagesResponse.USNat) {
        assertTrue(usnat.gppData.isNotEmpty())
        assertTrue(usnat.consentStrings.isNotEmpty())
        assertTrue(usnat.userConsents.vendors.isEmpty())
        assertTrue(usnat.userConsents.categories.isNotEmpty())
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
                            consentStatus = ConsentStatus()
                        ),
                        usnat = MessagesRequest.Body.Campaigns.USNat(
                            targetingParams = null,
                            hasLocalData = false,
                            consentStatus = ConsentStatus()
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

        response.campaigns.forEach { campaign ->
            assertNotNull(campaign.url)
            assertNotNull(campaign.message)
            assertNotNull(campaign.dateCreated)
            assertNotNull(campaign.messageMetaData)
            assertNotNull(campaign.webConsentPayload)

            assertCampaignConsents(campaign)
        }

        assertTrue(response.localState.isNotEmpty())
        assertTrue(response.nonKeyedLocalState.isNotEmpty())
    }
}
