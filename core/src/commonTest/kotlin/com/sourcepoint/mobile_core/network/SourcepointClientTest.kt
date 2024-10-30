package com.sourcepoint.mobile_core.network

import com.sourcepoint.mobile_core.models.InvalidChoiceAllParamsError
import com.sourcepoint.mobile_core.models.SPActionType
import com.sourcepoint.mobile_core.network.requests.ConsentStatusRequest
import com.sourcepoint.mobile_core.network.requests.MetaDataRequest
import com.sourcepoint.mobile_core.models.SPCampaignEnv
import com.sourcepoint.mobile_core.models.SPClientTimeout
import com.sourcepoint.mobile_core.models.SPIDFAStatus
import com.sourcepoint.mobile_core.models.SPMessageLanguage
import com.sourcepoint.mobile_core.models.SPNetworkError
import com.sourcepoint.mobile_core.models.SPUnableToParseBodyError
import com.sourcepoint.mobile_core.models.consents.CCPAConsent
import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import com.sourcepoint.mobile_core.models.consents.GDPRConsent
import com.sourcepoint.mobile_core.models.consents.USNatConsent
import com.sourcepoint.mobile_core.network.requests.ChoiceAllMetaDataRequest
import com.sourcepoint.mobile_core.network.requests.DefaultRequest
import com.sourcepoint.mobile_core.network.requests.IncludeData
import com.sourcepoint.mobile_core.network.responses.MessagesResponse
import com.sourcepoint.mobile_core.network.requests.MessagesRequest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SourcepointClientTest {
    private val accountId = 22
    private val propertyId = 16893
    private val propertyName = "https://mobile.multicampaign.demo"
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
        assertEquals(response.gdpr!!.applies, true)
        assertEquals(response.gdpr!!.sampleRate, 1.0f)
        assertTrue(response.gdpr!!.vendorListId.isNotEmpty())
        assertTrue(response.gdpr!!.additionsChangeDate.isNotEmpty())
        assertTrue(response.gdpr!!.legalBasisChangeDate.isNotEmpty())

        assertNotNull(response.usnat)
        assertEquals(response.usnat!!.applies, true)
        assertEquals(response.usnat!!.sampleRate, 1.0f)
        assertTrue(response.usnat!!.vendorListId.isNotEmpty())
        assertTrue(response.usnat!!.additionsChangeDate.isNotEmpty())
        assertTrue(response.usnat!!.applicableSections.isNotEmpty())

        assertNotNull(response.ccpa)
        assertEquals(response.ccpa!!.applies, true)
        assertEquals(response.ccpa!!.sampleRate, 1.0f)
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

    private fun assertCampaignConsents(campaign: MessagesResponse.Campaign<*>) {
        when(campaign) {
            is MessagesResponse.GDPR -> assertCampaignConsents(campaign.derivedConsents)
            is MessagesResponse.USNat -> assertCampaignConsents(campaign.derivedConsents)
            is MessagesResponse.CCPA -> assertCampaignConsents(campaign.derivedConsents)
            is MessagesResponse.Ios14 -> assertNull(campaign.derivedConsents)
        }
    }

    private fun assertCampaignConsents(consents: GDPRConsent?) {
        assertNotNull(consents)
        assertTrue(consents.euconsent!!.isNotEmpty())
        assertTrue(consents.tcData.isNotEmpty())
        assertTrue(consents.grants.isNotEmpty())
        assertTrue(consents.dateCreated!!.isNotEmpty())
        assertTrue(consents.expirationDate!!.isNotEmpty())
        assertTrue(consents.webConsentPayload!!.isNotEmpty())
    }

    private fun assertCampaignConsents(consents: USNatConsent?) {
        assertNotNull(consents)
        assertTrue(consents.gppData.isNotEmpty())
        assertTrue(consents.consentStrings.isNotEmpty())
        assertTrue(consents.userConsents.vendors.isEmpty())
        assertTrue(consents.userConsents.categories.isNotEmpty())
        assertTrue(consents.dateCreated!!.isNotEmpty())
        assertTrue(consents.expirationDate!!.isNotEmpty())
        assertTrue(consents.webConsentPayload!!.isNotEmpty())
    }

    private fun assertCampaignConsents(consents: CCPAConsent?) {
        assertNotNull(consents)
        assertTrue(consents.gppData.isNotEmpty())
        assertNotNull(consents.signedLspa)
        assertNotEquals(CCPAConsent.CCPAConsentStatus.RejectedNone, consents.status)
        assertTrue(consents.rejectedCategories.isEmpty())
        assertTrue(consents.rejectedVendors.isEmpty())
        assertTrue(consents.dateCreated!!.isNotEmpty())
        assertTrue(consents.expirationDate!!.isNotEmpty())
        assertTrue(consents.webConsentPayload!!.isNotEmpty())
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
            assertTrue(campaign.message!!.messageJson.isNotEmpty(), "Empty message_json for ${campaign.type}")
            assertNotNull(campaign.messageMetaData, "Empty messageMetaData for ${campaign.type}")
            assertCampaignConsents(campaign)
        }

        assertTrue(response.localState.isNotEmpty())
        assertTrue(response.nonKeyedLocalState.isNotEmpty())
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
    fun choiceAllContainCorrectParams() = runTest {
        val mockEngine = mock()
        SourcepointClient(123, 321, "test", httpEngine = mockEngine).getChoiceAll(
            actionType = SPActionType.AcceptAll,
            accountId = 123,
            propertyId = 321,
            idfaStatus = SPIDFAStatus.Accepted,
            metadata = ChoiceAllMetaDataRequest(ChoiceAllMetaDataRequest.Campaign(true),ChoiceAllMetaDataRequest.Campaign(false),ChoiceAllMetaDataRequest.Campaign(false)),
            includeData = IncludeData()
        )
        val defaultRequest = DefaultRequest()
        assertEquals(Url(
            " https://cdn.privacy-mgmt.com/wrapper/v2/choice/consent-all?env="+defaultRequest.env+"&scriptType="+defaultRequest.scriptType+
                    "&scriptVersion="+defaultRequest.scriptVersion+"&accountId=123&propertyId=321&hasCsp=true&withSiteActions=false&" +
                    "includeCustomVendorsRes=false&idfaStatus=Accepted&metadata=%7B%22gdpr%22%3A%7B%22applies%22%3Atrue%7D%2C%22" +
                    "ccpa%22%3A%7B%22applies%22%3Afalse%7D%2C%22usnat%22%3A%7B%22applies%22%3Afalse%7D%7D&includeData=%7B%22" +
                    "TCData%22%3A%7B%22type%22%3A%22string%22%7D%2C%22webConsentPayload%22%3A%7B%22type%22%3A%22string%22%7D%2C%22" +
                    "localState%22%3A%7B%22type%22%3A%22string%22%7D%2C%22categories%22%3Atrue%2C%22GPPData%22%3A%7B%22uspString%22%3Atrue%7D%7D"),
            mockEngine.requestHistory.last().url
        )
    }

    @Test
    fun choiceAllReturnInvalidChoiceAllParamsError() = runTest {
        assertFailsWith<InvalidChoiceAllParamsError> {
            api.getChoiceAll(
                actionType = SPActionType.Custom,
                accountId = 123,
                propertyId = 321,
                idfaStatus = SPIDFAStatus.Accepted,
                metadata = ChoiceAllMetaDataRequest(
                    ChoiceAllMetaDataRequest.Campaign(true),
                    ChoiceAllMetaDataRequest.Campaign(false),
                    ChoiceAllMetaDataRequest.Campaign(false)
                ),
                includeData = IncludeData()
            )
        }
    }

    @Test
    fun postGDPRChoiceActionAcceptContainCorrectResponse() = runTest {
        val response = api.getChoiceAll(
            actionType = SPActionType.AcceptAll,
            accountId = accountId,
            propertyId = propertyId,
            idfaStatus = SPIDFAStatus.Accepted,
            metadata = ChoiceAllMetaDataRequest(
                ChoiceAllMetaDataRequest.Campaign(true),
                ChoiceAllMetaDataRequest.Campaign(false),
                ChoiceAllMetaDataRequest.Campaign(false)
            ),
            includeData = IncludeData()
        )
        assertTrue(response.gdpr?.consentStatus?.consentedAll == true)
        assertTrue(response.gdpr?.acceptedVendors?.isNotEmpty() == true)
        assertTrue(response.gdpr?.acceptedCategories?.isNotEmpty() == true)
    }

    @Test
    fun postGDPRChoiceActionRejectContainCorrectResponse() = runTest {
        val response = api.getChoiceAll(
            actionType = SPActionType.RejectAll,
            accountId = accountId,
            propertyId = propertyId,
            idfaStatus = SPIDFAStatus.Accepted,
            metadata = ChoiceAllMetaDataRequest(
                ChoiceAllMetaDataRequest.Campaign(true),
                ChoiceAllMetaDataRequest.Campaign(false),
                ChoiceAllMetaDataRequest.Campaign(false)
            ),
            includeData = IncludeData()
        )
        assertTrue(response.gdpr?.consentStatus?.rejectedAny == true)
        assertTrue(response.gdpr?.acceptedVendors?.isEmpty() == true)
        assertTrue(response.gdpr?.acceptedCategories?.isEmpty() == true)
    }
}
