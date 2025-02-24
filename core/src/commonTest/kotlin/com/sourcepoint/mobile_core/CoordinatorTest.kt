package com.sourcepoint.mobile_core

import com.russhwolf.settings.MapSettings
import com.sourcepoint.mobile_core.models.SPAction
import com.sourcepoint.mobile_core.models.SPActionType
import com.sourcepoint.mobile_core.models.SPCampaign
import com.sourcepoint.mobile_core.models.SPCampaignType
import com.sourcepoint.mobile_core.models.SPCampaigns
import com.sourcepoint.mobile_core.models.SPError
import com.sourcepoint.mobile_core.models.SPPropertyName
import com.sourcepoint.mobile_core.models.consents.CCPAConsent
import com.sourcepoint.mobile_core.models.consents.GDPRConsent
import com.sourcepoint.mobile_core.models.consents.State
import com.sourcepoint.mobile_core.network.SPClient
import com.sourcepoint.mobile_core.network.SourcepointClient
import com.sourcepoint.mobile_core.network.SourcepointClientMock
import com.sourcepoint.mobile_core.storage.Repository
import com.sourcepoint.mobile_core.utils.now
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CoordinatorTest {
    private val storage = MapSettings()
    private val repository = Repository(storage)
    private lateinit var coordinator: Coordinator
    private val accountId = 22
    private val propertyId = 16893
    private val propertyName = SPPropertyName.create("mobile.multicampaign.demo")
    private val campaigns = SPCampaigns(
        gdpr = SPCampaign(),
        ccpa = SPCampaign(),
        usnat = SPCampaign(),
        ios14 = SPCampaign(),
    )
    private var spClientMock = SourcepointClientMock()
    private val spClient = SourcepointClient(
        accountId = accountId,
        propertyId = propertyId,
        propertyName = propertyName
    )

    private fun initCoordinatorFor(forCampaigns: SPCampaigns = campaigns, client: SPClient = spClient) {
        coordinator = Coordinator(
            accountId = accountId,
            propertyId = propertyId,
            propertyName = propertyName,
            campaigns = forCampaigns,
            spClient = client,
            repository = repository,
            state = State()
        )
    }

    @BeforeTest
    fun initCoordinatorState() {
        coordinator = Coordinator(
            accountId = accountId,
            propertyId = propertyId,
            propertyName = propertyName,
            campaigns = campaigns,
            spClient = spClient,
            repository = repository,
            state = State()
        )
    }

    @Test
    fun reportActionReturnsGDPRConsent() = runTest {
        val consents = coordinator.reportAction(
            action = SPAction(type = SPActionType.AcceptAll, campaignType = SPCampaignType.Gdpr, pmPayload = "{}"),
        )
        assertFalse(consents.gdpr?.consents?.uuid.isNullOrEmpty())
    }

    @Test
    fun reportActionReturnsCCPAConsent() = runTest {
        val consents = coordinator.reportAction(
            action = SPAction(type = SPActionType.RejectAll, campaignType = SPCampaignType.Ccpa, pmPayload = "{}"),
        )
        assertFalse(consents.ccpa?.consents?.uuid.isNullOrEmpty())
    }

    @Test
    fun reportActionReturnsUSNatConsent() = runTest {
        val consents = coordinator.reportAction(
            action = SPAction(
                type = SPActionType.SaveAndExit,
                campaignType = SPCampaignType.UsNat,
                pmPayload = """
                {
                    "shownCategories": ["6568ae4503cf5cf81eb79fa5"],
                    "categories": ["6568ae4503cf5cf81eb79fa5"],
                    "lan": "EN",
                    "privacyManagerId": "943890",
                    "vendors": []
                }
                """,
            )
        )
        assertFalse(consents.usnat?.consents?.uuid.isNullOrEmpty())
    }

    @Test
    fun noMessagesShouldAppearAfterAcceptingAll() = runTest {
        assertEquals(3, coordinator.loadMessages(authId = null, pubData = null).size)
        coordinator.reportAction(SPAction(SPActionType.AcceptAll, SPCampaignType.Gdpr, pmPayload = "{}"))
        coordinator.reportAction(SPAction(SPActionType.AcceptAll, SPCampaignType.Ccpa, pmPayload = "{}"))
        coordinator.reportAction(SPAction(SPActionType.AcceptAll, SPCampaignType.UsNat, pmPayload = "{}"))
        assertEquals(0, coordinator.loadMessages(authId = null, pubData = null).size)
    }

    @Test
    fun consentStatusCalledWhenAuthIdPassed() = runTest {
        initCoordinatorFor(client = spClientMock)
        coordinator.loadMessages(authId = "test", pubData = null)
        assertTrue(spClientMock.consentStatusCalled)
        assertTrue(coordinator.shouldCallConsentStatus)
    }

    @Test
    fun consentStatusNotCalledWhenLocalDataOutdatedNoUuid() = runTest {
        initCoordinatorFor(client = spClientMock)
        repository.state = State(localVersion = 0)
        coordinator.loadMessages(authId = null, pubData = null)
        assertFalse(spClientMock.consentStatusCalled)
    }

    @Test
    fun consentStatusCalledWhenLocalDataOutdatedHasUuid() = runTest {
        initCoordinatorFor(client = spClientMock)
        val consent = GDPRConsent(uuid = "test")
        repository.state = State(gdpr = State.GDPRState(consents = consent), localVersion = 0)
        coordinator.loadMessages(authId = null, pubData = null)
        assertTrue(spClientMock.consentStatusCalled)
        //Check that consentStatus is not called again after it's been called first time
        spClientMock.consentStatusCalled = false
        coordinator.loadMessages(authId = null, pubData = null)
        assertFalse(spClientMock.consentStatusCalled)
    }

    @Test
    fun consentStatusSetsLocalVersionWhenSucceds() = runTest {
        initCoordinatorFor(client = spClientMock)
        val consent = GDPRConsent(uuid = "test")
        repository.state = State(gdpr = State.GDPRState(consents = consent), localVersion = 0)
        coordinator.loadMessages(authId = null, pubData = null)
        assertEquals(State.VERSION, repository.state.localVersion)
    }

    @Test
    fun consentStatusLeavesLocalVersionWhenFails() = runTest {
        initCoordinatorFor(client = spClientMock)
        val consent = GDPRConsent(uuid = "test")
        repository.state = State(gdpr = State.GDPRState(consents = consent), localVersion = 0)
        spClientMock.error = SPError()
        try { coordinator.loadMessages(authId = null, pubData = null) } catch (_: Throwable) { }
        assertEquals(0, repository.state.localVersion)
    }

    @Test
    fun whenUserHasConsentStoredKeepsItUnchanged() = runTest {
        coordinator.loadMessages(authId = null, pubData = null)
        val userData = coordinator.userData
        val actionResult = coordinator.reportAction(SPAction(SPActionType.RejectAll, SPCampaignType.Gdpr, pmPayload = "{}"))
        assertNotEquals(userData, actionResult)
        coordinator.loadMessages(authId = null, pubData = null)
        val secondUserData = coordinator.userData
        assertEquals(actionResult, secondUserData)
    }

    @Test
    fun whenUserHasConsentStoredAndExpirationDataGreaterCurrentDate() = runTest {
        //erases consent data and returns a message
        val messages = coordinator.loadMessages(authId = null, pubData = null)
        assertNotEquals(0, messages.filter { it.type == SPCampaignType.Gdpr }.size)
        assertNotEquals(0, messages.filter { it.type == SPCampaignType.Ccpa }.size)
        coordinator.reportAction(SPAction(SPActionType.AcceptAll, SPCampaignType.Gdpr, pmPayload = "{}"))
        coordinator.reportAction(SPAction(SPActionType.AcceptAll, SPCampaignType.Ccpa, pmPayload = "{}"))
        assertTrue(repository.state.gdpr.consents.consentStatus.consentedAll == true)
        assertEquals(CCPAConsent.CCPAConsentStatus.ConsentedAll, repository.state.ccpa.consents.status)

        val secondMessages = coordinator.loadMessages(authId = null, pubData = null)
        assertEquals(0, secondMessages.filter { it.type == SPCampaignType.Gdpr }.size)
        assertEquals(0, secondMessages.filter { it.type == SPCampaignType.Ccpa }.size)
        assertTrue(repository.state.gdpr.consents.consentStatus.consentedAll == true)
        assertEquals(CCPAConsent.CCPAConsentStatus.ConsentedAll, repository.state.ccpa.consents.status)

        repository.state = repository.state.copy(
            gdpr = repository.state.gdpr.copy(
                consents = repository.state.gdpr.consents.copy(
                    expirationDate = now().minus(1.days)
                )))
        repository.state = repository.state.copy(
            ccpa = repository.state.ccpa.copy(
                consents = repository.state.ccpa.consents.copy(
                    expirationDate = now().minus(1.days)
                )))

        val thirdMessages = coordinator.loadMessages(authId = null, pubData = null)
        assertNotEquals(0, thirdMessages.filter { it.type == SPCampaignType.Gdpr }.size)
        assertNotEquals(0, thirdMessages.filter { it.type == SPCampaignType.Ccpa }.size)
        assertFalse(repository.state.gdpr.consents.consentStatus.consentedAll == true)
        assertEquals(null, repository.state.ccpa.consents.status)
    }

    //@Test //FIXME messages always return optedInUspString
    fun propertyWithSupportLegacyUSPStringReceivesIABUSPrivacyString() = runTest {
        val optedOutUspString = "1YYN"
        val optedInUspString = "1YNN"
        initCoordinatorFor(forCampaigns = SPCampaigns(usnat = SPCampaign(supportLegacyUSPString = true)))
        coordinator.loadMessages(authId = null, pubData = null)
        val newUserUspString = coordinator.userData.usnat?.consents?.gppData?.get("IABUSPrivacy_String")?.content
        assertEquals(optedOutUspString, newUserUspString)
        val saveAndExitAcceptAction = SPAction(
            type = SPActionType.SaveAndExit,
            campaignType = SPCampaignType.UsNat,
            pmPayload = """
                {
                    "shownCategories": [
                                        "65a6a785cc78fac48ab34e65",
                                        "65a6a785cc78fac48ab34e6a",
                                        "65a6a785cc78fac48ab34e6f",
                                        "65a6a785cc78fac48ab34e74",
                                        "65a6a785cc78fac48ab34e79",
                                        "65a6a785cc78fac48ab34e7e",
                                        "65a6a785cc78fac48ab34e83",
                                        "65a6a785cc78fac48ab34e88",
                                        "65a6a785cc78fac48ab34e8d",
                                        "65a6a785cc78fac48ab34e92",
                                        "65a6a785cc78fac48ab34e97",
                                        "65a6a785cc78fac48ab34e9c"
                                        ],
                    "categories": [
                                        "65a6a785cc78fac48ab34e65",
                                        "65a6a785cc78fac48ab34e6a",
                                        "65a6a785cc78fac48ab34e6f",
                                        "65a6a785cc78fac48ab34e74",
                                        "65a6a785cc78fac48ab34e79",
                                        "65a6a785cc78fac48ab34e7e",
                                        "65a6a785cc78fac48ab34e83",
                                        "65a6a785cc78fac48ab34e88",
                                        "65a6a785cc78fac48ab34e8d",
                                        "65a6a785cc78fac48ab34e92",
                                        "65a6a785cc78fac48ab34e97",
                                        "65a6a785cc78fac48ab34e9c",
                                        "648c9c48e17a3c7a82360c54"
                                        ],
                    "lan": "EN",
                    "privacyManagerId": "995256",
                    "vendors": []
                }
                """
        )
        val userData = coordinator.reportAction(saveAndExitAcceptAction)
        val actionUspString = userData.usnat?.consents?.gppData?.get("IABUSPrivacy_String")?.content
        assertEquals(optedInUspString, actionUspString)
    }

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun propertyWithAuthIdPersistsConsent() = runTest {
        initCoordinatorFor(forCampaigns = SPCampaigns(usnat = SPCampaign()))
        val saveAndExitAction = SPAction(type = SPActionType.SaveAndExit, campaignType = SPCampaignType.UsNat, pmPayload = """
                {
                    "shownCategories": ["6568ae4503cf5cf81eb79fa5"],
                    "categories": ["6568ae4503cf5cf81eb79fa5"],
                    "lan": "EN",
                    "privacyManagerId": "943890",
                    "vendors": []
                }
                """)
        val randomUuid = Uuid.random().toString()
        coordinator.loadMessages(authId = randomUuid, pubData = null)
        val actionUserData = coordinator.reportAction(saveAndExitAction)
        assertTrue(actionUserData.usnat?.consents?.uuid != null)
        assertEquals(actionUserData.usnat?.consents?.uuid, coordinator.userData.usnat?.consents?.uuid)

        initCoordinatorFor(forCampaigns = SPCampaigns(usnat = SPCampaign()))
        assertTrue(coordinator.userData.usnat?.consents?.uuid == null)
        val messages = coordinator.loadMessages(authId = randomUuid, pubData = null)
        assertEquals(0, messages.size)
        assertEquals(actionUserData.usnat?.consents?.uuid, coordinator.userData.usnat?.consents?.uuid)
    }

    @OptIn(ExperimentalUuidApi::class)
    //@Test //FIXME messages always return usnat campaign
    fun propertyWithAuthIdAndTransitionCCPAAuthSetsFlagInConsentStatusMetadata() = runTest {
        initCoordinatorFor(forCampaigns = SPCampaigns(ccpa = SPCampaign()))
        val randomUuid = Uuid.random().toString()
        coordinator.loadMessages(authId = randomUuid, pubData = null)
        val actionResult = coordinator.reportAction(SPAction(type = SPActionType.RejectAll, campaignType = SPCampaignType.Ccpa, pmPayload = "{}"))
        assertEquals(CCPAConsent.CCPAConsentStatus.RejectedAll, actionResult.ccpa?.consents?.status)

        initCoordinatorFor(forCampaigns = SPCampaigns(usnat = SPCampaign(transitionCCPAAuth = true)))
        val messages = coordinator.loadMessages(authId = randomUuid, pubData = null)
        assertEquals(0, messages.size)
        assertTrue(coordinator.userData.usnat?.consents?.consentStatus?.rejectedAny == true)
        assertFalse(coordinator.userData.usnat?.consents?.consentStatus?.consentedToAll == true)
    }

    @Test
    fun handlesCCPAoptoutWhenThereNoAuthID() = runTest {
        initCoordinatorFor(client = spClientMock, forCampaigns = SPCampaigns(usnat = SPCampaign(transitionCCPAAuth = true)))
        coordinator.loadMessages(authId = null, pubData = null)
        assertNull(spClientMock.consentStatusCalledWith?.usnat?.transitionCCPAAuth)
    }

    @Test
    fun usnatPropertyAdditionsChangeDateBiggerThanConsentDateCreatedShowMessage() = runTest {
        initCoordinatorFor(forCampaigns = SPCampaigns(usnat = SPCampaign()))
        val messages = coordinator.loadMessages(authId = null, pubData = null)
        assertEquals(1, messages.size)
        val saveAndExitAction = SPAction(type = SPActionType.SaveAndExit, campaignType = SPCampaignType.UsNat, pmPayload = """
                {
                    "shownCategories": ["6568ae4503cf5cf81eb79fa5"],
                    "categories": ["6568ae4503cf5cf81eb79fa5"],
                    "lan": "EN",
                    "privacyManagerId": "943890",
                    "vendors": []
                }
                """)
        coordinator.reportAction(saveAndExitAction)
        val secondMessages = coordinator.loadMessages(authId = null, pubData = null)
        assertEquals(0, secondMessages.size)

        repository.state = repository.state.copy(
            usNat = repository.state.usNat.copy(
                consents = repository.state.usNat.consents.copy(
                    dateCreated = repository.state.usNat.metaData.additionsChangeDate.minus(1.days)
                )))
        val thirdMessages = coordinator.loadMessages(authId = null, pubData = null)
        assertEquals(1, thirdMessages.size)
    }
}
