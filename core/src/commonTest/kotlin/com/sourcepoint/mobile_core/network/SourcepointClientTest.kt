package com.sourcepoint.mobile_core.network

import com.sourcepoint.mobile_core.asserters.assertContains
import com.sourcepoint.mobile_core.asserters.assertFalse
import com.sourcepoint.mobile_core.asserters.assertIsEmpty
import com.sourcepoint.mobile_core.asserters.assertNotEmpty
import com.sourcepoint.mobile_core.asserters.assertTrue
import com.sourcepoint.mobile_core.models.SPActionType
import com.sourcepoint.mobile_core.models.SPCampaignEnv
import com.sourcepoint.mobile_core.models.SPClientTimeout
import com.sourcepoint.mobile_core.models.SPIDFAStatus
import com.sourcepoint.mobile_core.models.SPMessageLanguage
import com.sourcepoint.mobile_core.models.SPNetworkError
import com.sourcepoint.mobile_core.models.SPPropertyName
import com.sourcepoint.mobile_core.models.SPUnableToParseBodyError
import com.sourcepoint.mobile_core.models.consents.CCPAConsent
import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import com.sourcepoint.mobile_core.models.consents.GDPRConsent
import com.sourcepoint.mobile_core.models.consents.USNatConsent
import com.sourcepoint.mobile_core.network.requests.CCPAChoiceRequest
import com.sourcepoint.mobile_core.network.requests.ChoiceAllRequest
import com.sourcepoint.mobile_core.network.requests.ConsentStatusRequest
import com.sourcepoint.mobile_core.network.requests.GDPRChoiceRequest
import com.sourcepoint.mobile_core.network.requests.MessagesRequest
import com.sourcepoint.mobile_core.network.requests.MetaDataRequest
import com.sourcepoint.mobile_core.network.requests.USNatChoiceRequest
import com.sourcepoint.mobile_core.network.responses.MessagesResponse
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SourcepointClientTest {
    private val accountId = 22
    private val propertyId = 16893
    private val propertyName = SPPropertyName.create("mobile.multicampaign.demo")
    private val api = SourcepointClient(
        accountId = accountId,
        propertyId = propertyId,
        propertyName = propertyName
    )

    private fun mock(response: String = """{}""", status: Int = 200, delayInSeconds: Int = 0) = MockEngine { _ ->
        delay(delayInSeconds.toLong() * 1000)
        respond(
            content = ByteReadChannel(response),
            status = HttpStatusCode.fromValue(status),
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }

    @Test
    fun getMetaData() = runTest {
        val response = api.getMetaData(
            MetaDataRequest.Campaigns(
                gdpr = MetaDataRequest.Campaigns.Campaign(),
                usnat = MetaDataRequest.Campaigns.Campaign(),
                ccpa = MetaDataRequest.Campaigns.Campaign()
            )
        )
        assertNotNull(response.gdpr)
        assertEquals(response.gdpr?.applies, true)
        assertEquals(response.gdpr?.sampleRate, 1.0f)
        assertNotEmpty(response.gdpr?.vendorListId)

        assertNotNull(response.usnat)
        assertEquals(response.usnat?.applies, true)
        assertEquals(response.usnat?.sampleRate, 1.0f)
        assertNotEmpty(response.usnat?.vendorListId)

        assertNotNull(response.ccpa)
        assertEquals(response.ccpa?.applies, true)
        assertEquals(response.ccpa?.sampleRate, 1.0f)
    }

    @Test
    fun getConsentStatus() = runTest {
        val response = api.getConsentStatus(
            authId = null,
            metadata = ConsentStatusRequest.MetaData(
                gdpr = ConsentStatusRequest.MetaData.Campaign(
                    applies = true,
                    uuid = "7acd0bf7-41fb-4d26-b12b-de5e2605ef81",
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
            "7acd0bf7-41fb-4d26-b12b-de5e2605ef81",
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

    private fun assertCampaignConsentsFromMessages(campaign: MessagesResponse.Campaign<*>) {
        when(campaign) {
            is MessagesResponse.GDPR -> assertCampaignConsentsFromMessages(campaign.derivedConsents)
            is MessagesResponse.USNat -> assertCampaignConsentsFromMessages(campaign.derivedConsents)
            is MessagesResponse.CCPA -> assertCampaignConsentsFromMessages(campaign.derivedConsents)
            is MessagesResponse.Ios14 -> assertNull(campaign.derivedConsents)
        }
    }

    private fun assertCampaignConsentsFromMessages(consents: GDPRConsent?) {
        assertNotNull(consents)
        assertNotEmpty(consents.euconsent)
        assertNotEmpty(consents.tcData)
        assertNotEmpty(consents.grants)
        assertNotNull(consents.dateCreated)
        assertNotNull(consents.expirationDate)
        assertNotEmpty(consents.webConsentPayload)
    }

    private fun assertCampaignConsentsFromMessages(consents: USNatConsent?) {
        assertNotNull(consents)
        assertNotEmpty(consents.gppData)
        assertNotEmpty(consents.consentStrings)
        assertIsEmpty(consents.userConsents.vendors)
        assertNotEmpty(consents.userConsents.categories)
        assertNotNull(consents.dateCreated)
        assertNotNull(consents.expirationDate)
        assertNotEmpty(consents.webConsentPayload)
    }

    private fun assertCampaignConsentsFromMessages(consents: CCPAConsent?) {
        assertNotNull(consents)
        assertNotEmpty(consents.gppData)
        assertNotNull(consents.signedLspa)
        assertEquals(CCPAConsent.CCPAConsentStatus.RejectedNone, consents.status)
        assertIsEmpty(consents.rejectedCategories)
        assertIsEmpty(consents.rejectedVendors)
        assertNotNull(consents.dateCreated)
        assertNotNull(consents.expirationDate)
        assertNotEmpty(consents.webConsentPayload)
    }

    @Test
    fun getMessages() = runTest {
        val response = api.getMessages(
            MessagesRequest(
                body = MessagesRequest.Body(
                    propertyHref = propertyName,
                    accountId = accountId,
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
                        ios14 = MessagesRequest.Body.Campaigns.IOS14(
                            idfaStatus = SPIDFAStatus.Unknown,
                            targetingParams = null
                        ),
                        ccpa = MessagesRequest.Body.Campaigns.CCPA(
                            targetingParams = null,
                            hasLocalData = false,
                            status = CCPAConsent.CCPAConsentStatus.RejectedNone
                        )
                    ),
                    idfaStatus = SPIDFAStatus.Unknown,
                    consentLanguage = SPMessageLanguage.ENGLISH,
                    campaignEnv = SPCampaignEnv.PUBLIC
                ),
                metadata = MessagesRequest.MetaData(
                    gdpr = MessagesRequest.MetaData.Campaign(applies = true),
                    usnat = MessagesRequest.MetaData.Campaign(applies = true),
                    ccpa = MessagesRequest.MetaData.Campaign(applies = true)
                ),
                localState = null,
                nonKeyedLocalState = null
            )
        )

        assertEquals(4, response.campaigns.size)

        response.campaigns.forEach { campaign ->
            assertNotNull(campaign.url, "Empty url for ${campaign.type}")
            assertNotNull(campaign.message, "Empty message for ${campaign.type}")
            assertNotEmpty(campaign.message?.messageJson, "Empty message_json for ${campaign.type}")
            assertNotNull(campaign.messageMetaData, "Empty messageMetaData for ${campaign.type}")
            assertCampaignConsentsFromMessages(campaign)
        }

        assertNotEmpty(response.localState)
        assertNotEmpty(response.nonKeyedLocalState)
    }

    @Test
    fun loggingWithErrorWhenRequestTimeoutInTheClient() = runTest {
        val mockEngine = mock(delayInSeconds = 2)

        assertFailsWith<SPClientTimeout> {
            SourcepointClient(accountId, propertyId, propertyName, requestTimeoutInSeconds = 1, httpEngine = mockEngine)
                .getMetaData(campaigns = MetaDataRequest.Campaigns())
        }
    }

    @Test
    fun loggingWithErrorWhenStatusCodeNot200() = runTest {
        val mockEngine = mock(status = HttpStatusCode.GatewayTimeout.value)

        assertFailsWith<SPNetworkError> {
            SourcepointClient(accountId, propertyId, propertyName, httpEngine = mockEngine)
                .getMetaData(campaigns = MetaDataRequest.Campaigns())
        }

        assertNotNull(mockEngine.requestHistory.firstOrNull { it.url.segments.contains("meta-data") })
        assertNotNull(mockEngine.requestHistory.firstOrNull { it.url.segments.contains("custom-metrics") })
    }

    @Test
    fun loggingWithErrorWhenResponseCantBeParsed() = runTest {
        val mockEngine = mock(response = """{"gdpr":{}}""")

        assertFailsWith<SPUnableToParseBodyError> {
            SourcepointClient(accountId, propertyId, propertyName, httpEngine = mockEngine)
                .getMetaData(campaigns = MetaDataRequest.Campaigns())
        }

        assertNotNull(mockEngine.requestHistory.firstOrNull { it.url.segments.contains("meta-data") })
        assertNotNull(mockEngine.requestHistory.firstOrNull { it.url.segments.contains("custom-metrics") })
    }

    @Test
    fun canAddAndRemoveCustomConsentsInGDPR() = runTest {
        val customVendorId = "5ff4d000a228633ac048be41"
        val categoryId1 = "608bad95d08d3112188e0e36"
        val categoryId2 = "608bad95d08d3112188e0e2f"
        val consentUUID = "uuid_36" // this uuid needs to exist in the backend, i.e. consent services
        val responseCustomConsent = api.customConsentGDPR(
            consentUUID = consentUUID,
            propertyId = propertyId,
            vendors = listOf(customVendorId),
            categories = listOf(categoryId1, categoryId2),
            legIntCategories = emptyList()
        )
        val responseDeleteCustomConsent = api.deleteCustomConsentGDPR(
            consentUUID = consentUUID,
            propertyId = propertyId,
            vendors = listOf(customVendorId),
            categories = listOf(categoryId1, categoryId2),
            legIntCategories = emptyList()
        )

        assertContains(responseCustomConsent.vendors, customVendorId)
        assertContains(responseCustomConsent.categories, categoryId1)
        assertContains(responseCustomConsent.categories, categoryId2)

        assertFalse(responseDeleteCustomConsent.vendors.contains(customVendorId))
        assertFalse(responseDeleteCustomConsent.categories.contains(categoryId1))
        assertFalse(responseDeleteCustomConsent.categories.contains(categoryId2))
    }

    @Test
    fun getGDPRChoiceAcceptAllContainCorrectResponse() = runTest {
        val response = api.getChoiceAll(
            actionType = SPActionType.AcceptAll,
            campaigns = ChoiceAllRequest.ChoiceAllCampaigns(
                ChoiceAllRequest.ChoiceAllCampaigns.Campaign(true),
                ChoiceAllRequest.ChoiceAllCampaigns.Campaign(false),
                ChoiceAllRequest.ChoiceAllCampaigns.Campaign(false)
            ),
        )
        assertTrue(response.gdpr?.consentStatus?.consentedAll)
        assertNotEmpty(response.gdpr?.acceptedVendors)
        assertNotEmpty(response.gdpr?.acceptedCategories)
    }

    @Test
    fun getGDPRChoiceRejectAllContainCorrectResponse() = runTest {
        val response = api.getChoiceAll(
            actionType = SPActionType.RejectAll,
            campaigns = ChoiceAllRequest.ChoiceAllCampaigns(
                ChoiceAllRequest.ChoiceAllCampaigns.Campaign(true),
                ChoiceAllRequest.ChoiceAllCampaigns.Campaign(false),
                ChoiceAllRequest.ChoiceAllCampaigns.Campaign(false)
            ),
        )
        assertTrue(response.gdpr?.consentStatus?.rejectedAny)
        assertIsEmpty(response.gdpr?.acceptedVendors)
        assertIsEmpty(response.gdpr?.acceptedCategories)
    }

    @Test
    fun postGDPRChoiceActionAcceptContainCorrectResponse() = runTest {
        val response = api.postChoiceGDPRAction(
            actionType = SPActionType.AcceptAll,
            request = GDPRChoiceRequest(sendPVData = true, propertyId = 123, sampleRate = 1.0f)
        )
        assertTrue(response.consentStatus?.consentedAll)
        assertNotEmpty(response.acceptedVendors)
        assertNotEmpty(response.acceptedCategories)
    }

    @Test
    fun postGDPRChoiceActionRejectContainCorrectResponse() = runTest {
        val response = api.postChoiceGDPRAction(
            actionType = SPActionType.RejectAll,
            request = GDPRChoiceRequest(sendPVData = true, propertyId = 123, sampleRate = 1.0f)
        )
        assertTrue(response.consentStatus?.rejectedAny)
        assertIsEmpty(response.acceptedVendors)
        assertIsEmpty(response.acceptedCategories)
    }

    @Test
    fun postGDPRChoiceActionSaveExitContainCorrectResponse() = runTest {
        val response = api.postChoiceGDPRAction(
            actionType = SPActionType.SaveAndExit,
            request = GDPRChoiceRequest(
                uuid = "uuid_36",
                sendPVData = true,
                propertyId = propertyId,
                pmSaveAndExitVariables = """{
                    "lan":"EN",
                    "vendors":[{"consent":false,"_id":"5f1b2fbeb8e05c306f2a1eb9","vendorType":"CUSTOM","iabId":null,"legInt":true}],
                    "privacyManagerId":"488393",
                    "categories": [{"_id":"608bad95d08d3112188e0e2f","legInt":true,"iabId":2,"consent":false,"type":"IAB_PURPOSE"}],
                    "specialFeatures":[]
                }""".encodeToJsonObject(),
                sampleRate = 1f,
                idfaStatus = SPIDFAStatus.Accepted
            )
        )
        assertTrue(response.consentStatus?.rejectedAny)
        assertTrue(response.consentStatus?.consentedToAny)
        assertContains(response.acceptedLegIntVendors, "5f1b2fbeb8e05c306f2a1eb9")
        assertContains(response.acceptedLegIntCategories, "608bad95d08d3112188e0e2f")
    }

    @Test
    fun postCCPAChoiceActionAcceptContainCorrectResponse() = runTest {
        val response = api.postChoiceCCPAAction(
            actionType = SPActionType.AcceptAll,
            request = CCPAChoiceRequest(sendPVData = true, propertyId = propertyId, sampleRate = 1.0f)
        )
        assertTrue(response.consentedAll)
        assertEquals(response.status, CCPAConsent.CCPAConsentStatus.ConsentedAll)
    }

    @Test
    fun postCCPAChoiceActionRejectContainCorrectResponse() = runTest {
        val response = api.postChoiceCCPAAction(
            actionType = SPActionType.RejectAll,
            request = CCPAChoiceRequest(sendPVData = true, propertyId = propertyId, sampleRate = 1.0f)
        )
        assertTrue(response.rejectedAll)
        assertEquals(response.status, CCPAConsent.CCPAConsentStatus.RejectedAll)
    }

    @Test
    fun postCCPAChoiceActionSaveExitContainCorrectResponse() = runTest {
        val response = api.postChoiceCCPAAction(
            actionType = SPActionType.SaveAndExit,
            request = CCPAChoiceRequest(
                uuid = "uuid_36",
                pmSaveAndExitVariables =
                    """{"rejectedCategories":["608bae685461ff11a2c2865d"],"rejectedVendors":[],"privacyManagerId":"509688","lan":"EN"}""".encodeToJsonObject(),
                sendPVData = true,
                propertyId = propertyId,
                sampleRate = 1.0f
            )
        )
        assertFalse(response.consentedAll)
        assertFalse(response.rejectedAll)
        assertContains(response.rejectedCategories, "608bae685461ff11a2c2865d")
    }

    @Test
    fun postUSNatChoiceActionAcceptContainCorrectResponse() = runTest {
        val response = api.postChoiceUSNatAction(
            actionType = SPActionType.AcceptAll,
            request = USNatChoiceRequest(sendPVData = true, propertyId = propertyId, sampleRate = 1.0f)
        )
        assertTrue(response.consentStatus.consentedToAll)
    }

    @Test
    fun postUSNatChoiceActionRejectContainCorrectResponse() = runTest {
        val response = api.postChoiceUSNatAction(
            actionType = SPActionType.RejectAll,
            request = USNatChoiceRequest(sendPVData = true, propertyId = propertyId, sampleRate = 1.0f)
        )
        assertTrue(response.consentStatus.rejectedAny)
    }

    @Test
    fun postUSNatChoiceActionSaveExitContainCorrectResponse() = runTest {
        val response = api.postChoiceUSNatAction(
            actionType = SPActionType.SaveAndExit,
            request = USNatChoiceRequest(
                uuid = "uuid_36",
                vendorListId = "65a01016e17a3c7a831ec515",
                pmSaveAndExitVariables =
                    """{"categories":["648c9c48e17a3c7a82360c54"],"lan":"EN","privacyManagerId":"943886","shownCategories":["648c9c48e17a3c7a82360c54"],"vendors":[]}""".encodeToJsonObject(),
                sendPVData = true,
                propertyId = propertyId,
                sampleRate = 1f
            )
        )
        assertTrue(response.consentStatus.rejectedAny)
        assertTrue(response.consentStatus.consentedToAny)
    }
}
