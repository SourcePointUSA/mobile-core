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
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

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

    private fun initCoordinatorFor(client: SPClient = spClient) {
        coordinator = Coordinator(
            accountId = accountId,
            propertyId = propertyId,
            propertyName = propertyName,
            campaigns = campaigns,
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
}
