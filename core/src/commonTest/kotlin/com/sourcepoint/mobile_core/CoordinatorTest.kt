package com.sourcepoint.mobile_core

import com.russhwolf.settings.MapSettings
import com.sourcepoint.mobile_core.asserters.assertAllAccepted
import com.sourcepoint.mobile_core.asserters.assertContains
import com.sourcepoint.mobile_core.asserters.assertContainsAllOf
import com.sourcepoint.mobile_core.asserters.assertDefaultConsents
import com.sourcepoint.mobile_core.asserters.assertDoesNotContain
import com.sourcepoint.mobile_core.asserters.assertDoesNotContainAllOf
import com.sourcepoint.mobile_core.asserters.assertFalse
import com.sourcepoint.mobile_core.asserters.assertIsEmpty
import com.sourcepoint.mobile_core.asserters.assertNotEmpty
import com.sourcepoint.mobile_core.asserters.assertTrue
import com.sourcepoint.mobile_core.mocks.SPClientMock
import com.sourcepoint.mobile_core.models.SPAction
import com.sourcepoint.mobile_core.models.SPActionType.*
import com.sourcepoint.mobile_core.models.SPCampaign
import com.sourcepoint.mobile_core.models.SPCampaignType.*
import com.sourcepoint.mobile_core.models.SPCampaigns
import com.sourcepoint.mobile_core.models.SPMessageLanguage.ENGLISH
import com.sourcepoint.mobile_core.models.SPPropertyName
import com.sourcepoint.mobile_core.models.consents.CCPAConsent
import com.sourcepoint.mobile_core.models.consents.GDPRConsent
import com.sourcepoint.mobile_core.models.consents.State
import com.sourcepoint.mobile_core.network.SPClient
import com.sourcepoint.mobile_core.network.SourcepointClient
import com.sourcepoint.mobile_core.network.responses.MetaDataResponse
import com.sourcepoint.mobile_core.storage.Repository
import com.sourcepoint.mobile_core.utils.now
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.days
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid.Companion.random

class CoordinatorTest {
    private val storage = MapSettings()
    private val repository = Repository(storage)
    private val accountId = 22
    private val propertyId = 16893
    private val propertyName = SPPropertyName.create("mobile.multicampaign.demo")
    private val customVendorId = "5ff4d000a228633ac048be41" // "Game Accounts"
    private val categoriesUsedByVendorId = listOf(
        "608bad95d08d3112188e0e36", // "Create profiles for personalised advertising",
        "608bad95d08d3112188e0e2f", // "Use limited data to select advertising",
        "608bad95d08d3112188e0e3d", // "Use profiles to select personalised advertising"
    )
    private val campaigns = SPCampaigns(
        gdpr = SPCampaign(),
        ccpa = SPCampaign(),
        usnat = SPCampaign(),
        ios14 = SPCampaign(),
    )
    private val state = State(accountId = accountId, propertyId = propertyId)
    private val spClient = SourcepointClient(
        accountId = accountId,
        propertyId = propertyId,
        propertyName = propertyName
    )

    private fun getCoordinator(
        authId: String? = null,
        accountId: Int = this.accountId,
        propertyId: Int = this.propertyId,
        propertyName: SPPropertyName = this.propertyName,
        campaigns: SPCampaigns = this.campaigns,
        spClient: SPClient = this.spClient,
        repository: Repository = this.repository,
        state: State = this.state
    ) = Coordinator(
        authId = authId,
        accountId = accountId,
        propertyId = propertyId,
        propertyName = propertyName,
        campaigns = campaigns,
        spClient = spClient,
        repository = repository,
        state = state
    )

    @OptIn(ExperimentalUuidApi::class)
    fun getRandomAuthId() = "mobile-core-testing-${random()}"

    @Test
    fun shouldResetStateIfPropertyDetailsChange() {
        val state = State(propertyId = propertyId, accountId = accountId)
        assertNotEquals(
            getCoordinator(state = state).state,
            getCoordinator(state = state, propertyId = 123).state
        )
        assertNotEquals(
            getCoordinator(state = state).state,
            getCoordinator(state = state, accountId = 123).state
        )
    }

    @Test
    fun shouldNotResetStateIfAuthIdChangesFromNull() = runTest {
        val state = State(accountId = accountId, propertyId = propertyId, authId = null)
        val coordinator = getCoordinator(spClient = SPClientMock(), state = state)
        coordinator.loadMessages(authId = "foo", pubData = null, language = ENGLISH)
        assertSame(coordinator.state, state)
    }

    @Test
    fun shouldResetStateIfAuthIdChangeFromSomethingToSomethingElse() = runTest {
        val state = State(accountId = accountId, propertyId = propertyId, authId = "foo")
        val coordinator = getCoordinator(spClient = SPClientMock(), state = state)
        coordinator.loadMessages(authId = "bar", pubData = null, language = ENGLISH)
        assertNotSame(coordinator.state, state)
    }

    @Test
    fun reportActionReturnsGDPRConsent() = runTest {
        val consents = getCoordinator().reportAction(
            action = SPAction(type = AcceptAll, campaignType = Gdpr),
        )
        assertNotEmpty(consents.gdpr?.consents?.uuid)
    }

    @Test
    fun reportActionReturnsCCPAConsent() = runTest {
        val consents = getCoordinator().reportAction(
            action = SPAction(type = RejectAll, campaignType = Ccpa),
        )
        assertNotEmpty(consents.ccpa?.consents?.uuid)
    }

    @Test
    fun reportActionReturnsUSNatConsent() = runTest {
        val coordinator = getCoordinator()
        val consents = coordinator.reportAction(
            action = SPAction.init(
                type = SaveAndExit,
                campaignType = UsNat,
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
        assertNotEmpty(consents.usnat?.consents?.uuid)
    }

    @Test
    fun clearLocalDataResetsRepositoryAndInMemoryState() {
        val coordinator = getCoordinator()
        val previousState = coordinator.state
        coordinator.clearLocalData()
        assertIsEmpty(repository.gppData)
        assertIsEmpty(repository.tcData)
        assertNull(repository.uspString)
        assertNotSame(previousState, coordinator.state)
    }

    @Test
    fun deleteCustomGDPRConsentRemovesVendorFromVendorsProperty() = runTest {
        val coordinator = getCoordinator()
        coordinator.reportAction(SPAction(AcceptAll, Gdpr))
        coordinator.deleteCustomConsentGDPR(
            vendors = listOf(customVendorId),
            categories = categoriesUsedByVendorId,
            legIntCategories = emptyList()
        )
        val consents = coordinator.userData.gdpr?.consents
        assertDoesNotContain(consents?.vendors, customVendorId)
        assertDoesNotContainAllOf(consents?.categories, categoriesUsedByVendorId)
        assertFalse(consents?.grants?.get(customVendorId)?.vendorGrant)
    }

    @Test
    fun customConsentGDPRAddsVendorToVendorsProperty() = runTest {
        val coordinator = getCoordinator()
        coordinator.reportAction(SPAction(RejectAll, Gdpr))
        coordinator.customConsentGDPR(
            vendors = listOf(customVendorId),
            categories = categoriesUsedByVendorId,
            legIntCategories = emptyList()
        )
        val consents = coordinator.userData.gdpr?.consents
        assertContains(consents?.vendors, customVendorId)
        assertContainsAllOf(consents?.categories, categoriesUsedByVendorId)
        assertTrue(consents?.grants?.get(customVendorId)?.vendorGrant)
    }

    @Test
    fun consentIsStoredAfterCallingLoadMessagesAndNoMessagesShouldAppearAfterAcceptingAll() = runTest {
        val coordinator = getCoordinator()
        assertEquals(3, coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH).size)
        assertNotEmpty(repository.gppData)
        assertNotEmpty(repository.tcData)
        assertNotEmpty(repository.uspString)
        assertDefaultConsents(coordinator.userData.gdpr?.consents)
        assertDefaultConsents(coordinator.userData.ccpa?.consents)
        assertDefaultConsents(coordinator.userData.usnat?.consents)

        coordinator.reportAction(SPAction(AcceptAll, Gdpr))
        coordinator.reportAction(SPAction(AcceptAll, Ccpa))
        coordinator.reportAction(SPAction(AcceptAll, UsNat))
        assertNotEmpty(repository.gppData)
        assertNotEmpty(repository.tcData)
        assertNotEmpty(repository.uspString)
        assertAllAccepted(coordinator.userData.gdpr?.consents)
        assertAllAccepted(coordinator.userData.ccpa?.consents)
        assertAllAccepted(coordinator.userData.usnat?.consents)

        assertIsEmpty(coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH))
    }

    @Test
    fun consentIsLinkedToAuthId() = runTest {
        val authId = getRandomAuthId()
        var coordinator = getCoordinator(authId = authId)
        coordinator.reportAction(SPAction(AcceptAll, Gdpr))
        val firstUuid = coordinator.userData.gdpr?.consents?.uuid
        assertNotEmpty(firstUuid)

        coordinator = getCoordinator()
        coordinator.loadMessages(authId = authId, pubData = null, language = ENGLISH)
        assertEquals(firstUuid, coordinator.userData.gdpr?.consents?.uuid)
    }

//    @Test
//    fun consentStatusNotCalledWhenLocalDataOutdatedNoUuid() = runTest {
//        val spClientMock = SPClientMock()
//        val coordinator = getCoordinator(spClient = spClientMock)
//        repository.state = State(localVersion = 0, propertyId = propertyId, accountId = accountId)
//        coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH)
//        assertFalse(spClientMock.consentStatusCalled)
//    }

//    @Test
//    fun consentStatusCalledWhenLocalDataOutdatedHasUuid() = runTest {
//        val spClientMock = SPClientMock()
//        val coordinator = getCoordinator(spClient = spClientMock)
//        val consent = GDPRConsent(uuid = "test")
//        coordinator.state = State(gdpr = State.GDPRState(consents = consent), localVersion = 0, propertyId = propertyId, accountId = accountId)
//        coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH)
//        assertTrue(spClientMock.consentStatusCalled)
//        //Check that consentStatus is not called again after it's been called first time
//        spClientMock.consentStatusCalled = false
//        coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH)
//        assertFalse(spClientMock.consentStatusCalled)
//    }

    @Test
    fun consentStatusSetsLocalVersionWhenSucceds() = runTest {
        val spClientMock = SPClientMock()
        val coordinator = getCoordinator(spClient = spClientMock)
        val consent = GDPRConsent(uuid = "test")
        repository.state = State(gdpr = State.GDPRState(consents = consent), localVersion = 0, propertyId = propertyId, accountId = accountId)
        coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH)
        assertEquals(State.VERSION, coordinator.state.localVersion)
    }

    @Test
    fun consentStatusLeavesLocalVersionWhenFails() = runTest {
        val spClientMock = SPClientMock()
        val coordinator = getCoordinator(spClient = spClientMock)
        val consent = GDPRConsent(uuid = "test")
        coordinator.state = State(gdpr = State.GDPRState(consents = consent), localVersion = 0, propertyId = propertyId, accountId = accountId)
        coordinator.persistState()
        try { coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH) } catch (_: Throwable) { }
        assertEquals(0, coordinator.state.localVersion)
    }

    @Test
    fun whenUserHasConsentStoredKeepsItUnchanged() = runTest {
        val coordinator = getCoordinator()
        coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH)
        val userData = coordinator.userData
        val actionResult = coordinator.reportAction(SPAction(RejectAll, Gdpr))
        assertNotEquals(userData, actionResult)
        coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH)
        val secondUserData = coordinator.userData
        assertEquals(actionResult, secondUserData)
    }

    @Test
    fun whenUserHasConsentStoredAndExpirationDataGreaterCurrentDate() = runTest {
        val coordinator = getCoordinator()
        val messages = coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH)
        assertNotEmpty(messages.filter { it.type == Gdpr })
        assertNotEmpty(messages.filter { it.type == Ccpa })
        coordinator.reportAction(SPAction(AcceptAll, Gdpr))
        coordinator.reportAction(SPAction(AcceptAll, Ccpa))
        assertTrue(coordinator.state.gdpr.consents.consentStatus.consentedAll)
        assertEquals(CCPAConsent.CCPAConsentStatus.ConsentedAll, coordinator.state.ccpa.consents.status)

        val secondMessages = coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH)
        assertIsEmpty(secondMessages.filter { it.type == Gdpr })
        assertIsEmpty(secondMessages.filter { it.type == Ccpa })
        assertTrue(coordinator.state.gdpr.consents.consentStatus.consentedAll)
        assertEquals(CCPAConsent.CCPAConsentStatus.ConsentedAll, coordinator.state.ccpa.consents.status)

        coordinator.state = coordinator.state.copy(
            gdpr = coordinator.state.gdpr.copy(
                consents = coordinator.state.gdpr.consents.copy(
                    expirationDate = now().minus(1.days)
                )
            )
        )
        coordinator.state = coordinator.state.copy(
            ccpa = coordinator.state.ccpa.copy(
                consents = coordinator.state.ccpa.consents.copy(
                    expirationDate = now().minus(1.days)
                )
            )
        )
        coordinator.persistState()

        val thirdMessages = coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH)
        assertNotEmpty(thirdMessages.filter { it.type == Gdpr })
        assertNotEmpty(thirdMessages.filter { it.type == Ccpa })
        assertFalse(coordinator.state.gdpr.consents.consentStatus.consentedAll)
        assertEquals(CCPAConsent.CCPAConsentStatus.RejectedNone, coordinator.state.ccpa.consents.status)
    }

    @Test
    fun propertyWithSupportLegacyUSPStringReceivesIABUSPrivacyString() = runTest {
        val optedOutUspString = "1YYN"
        val optedInUspString = "1YNN"
        val coordinator = getCoordinator(campaigns = SPCampaigns(usnat = SPCampaign(supportLegacyUSPString = true)))
        coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH)
        val newUserUspString = coordinator.userData.usnat?.consents?.gppData?.get("IABUSPrivacy_String")?.content
        assertEquals(optedInUspString, newUserUspString)
        val rejectedAction = SPAction(type = RejectAll, campaignType = UsNat)
        val userData = coordinator.reportAction(rejectedAction)
        val actionUspString = userData.usnat?.consents?.gppData?.get("IABUSPrivacy_String")?.content
        assertEquals(optedOutUspString, actionUspString)
    }

    @Test
    fun propertyWithAuthIdPersistsConsent() = runTest {
        var coordinator = getCoordinator(campaigns = SPCampaigns(usnat = SPCampaign()))
        val saveAndExitAction = SPAction.init(type = SaveAndExit, campaignType = UsNat, pmPayload = """
                {
                    "shownCategories": ["6568ae4503cf5cf81eb79fa5"],
                    "categories": ["6568ae4503cf5cf81eb79fa5"],
                    "lan": "EN",
                    "privacyManagerId": "943890",
                    "vendors": []
                }
                """)
        val randomUuid = getRandomAuthId()
        coordinator.loadMessages(authId = randomUuid, pubData = null, language = ENGLISH)
        val actionUserData = coordinator.reportAction(saveAndExitAction)
        assertNotNull(actionUserData.usnat?.consents?.uuid )
        assertEquals(actionUserData.usnat?.consents?.uuid, coordinator.userData.usnat?.consents?.uuid)

        coordinator = getCoordinator(campaigns = SPCampaigns(usnat = SPCampaign()))
        val messages = coordinator.loadMessages(authId = randomUuid, pubData = null, language = ENGLISH)
        assertIsEmpty(messages)
        assertEquals(actionUserData.usnat?.consents?.uuid, coordinator.userData.usnat?.consents?.uuid)
    }

//    @Test
//    fun propertyWithAuthIdAndTransitionCCPAAuthSetsFlagInConsentStatusMetadata() = runTest {
//        var coordinator = getCoordinator(campaigns = SPCampaigns(ccpa = SPCampaign()))
//        val randomUuid = getRandomAuthId()
//        coordinator.loadMessages(authId = randomUuid, pubData = null, language = ENGLISH)
//        val actionResult = coordinator.reportAction(SPAction(type = RejectAll, campaignType = Ccpa))
//        assertEquals(CCPAConsent.CCPAConsentStatus.RejectedAll, actionResult.ccpa?.consents?.status)
//
//        coordinator = getCoordinator(campaigns = SPCampaigns(usnat = SPCampaign(transitionCCPAAuth = true)))
//        val messages = coordinator.loadMessages(authId = randomUuid, pubData = null, language = ENGLISH)
//        assertIsEmpty(messages)
//        assertTrue(coordinator.userData.usnat?.consents?.consentStatus?.rejectedAny)
//        assertFalse(coordinator.userData.usnat?.consents?.consentStatus?.consentedToAll)
//    }

//    @Test
//    fun handlesCCPAoptoutWhenThereNoAuthID() = runTest {
//        val spClientMock = SPClientMock()
//        val coordinator = getCoordinator(spClient = spClientMock, campaigns = SPCampaigns(usnat = SPCampaign(transitionCCPAAuth = true)))
//        coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH)
//        assertNull(spClientMock.consentStatusCalledWith?.usnat?.transitionCCPAAuth)
//    }

    @Test
    fun usnatPropertyAdditionsChangeDateBiggerThanConsentDateCreatedShowMessage() = runTest {
        val coordinator = getCoordinator(campaigns = SPCampaigns(usnat = SPCampaign()))
        val messages = coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH)
        assertNotEmpty(messages)
        val saveAndExitAction = SPAction.init(type = SaveAndExit, campaignType = UsNat, pmPayload = """
                {
                    "shownCategories": ["6568ae4503cf5cf81eb79fa5"],
                    "categories": ["6568ae4503cf5cf81eb79fa5"],
                    "lan": "EN",
                    "privacyManagerId": "943890",
                    "vendors": []
                }
                """)
        coordinator.reportAction(saveAndExitAction)
        val secondMessages = coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH)
        assertIsEmpty(secondMessages)

        coordinator.state = coordinator.state.copy(
            usNat = coordinator.state.usNat.copy(
                consents = coordinator.state.usNat.consents.copy(
                    dateCreated = coordinator.state.usNat.metaData.additionsChangeDate.minus(1.days)
                )
            )
        )
        coordinator.persistState()
        val thirdMessages = coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH)
        assertNotEmpty(thirdMessages)
    }

//    @Test
//    fun whenUsnatApplicableSectionsChangeShouldCallConsentStatus() = runTest {
//        val firstApplicableSection = 1
//        val differentApplicableSection = 2
//        val spClientMock = SPClientMock()
//        spClientMock.metaDataResponse = MetaDataResponse(
//            gdpr = null,
//            usnat = MetaDataResponse.MetaDataResponseUSNat(
//                applies = true,
//                sampleRate = 1f,
//                additionsChangeDate = now(),
//                applicableSections = listOf(firstApplicableSection),
//                vendorListId = ""
//            ),
//            ccpa = null
//        )
//        val coordinator = getCoordinator(spClient = spClientMock, campaigns = SPCampaigns(usnat = SPCampaign()))
//        coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH)
//        assertFalse(spClientMock.consentStatusCalled)
//
//        spClientMock.metaDataResponse = MetaDataResponse(
//            gdpr = null,
//            usnat = MetaDataResponse.MetaDataResponseUSNat(
//                applies = true,
//                sampleRate = 1f,
//                additionsChangeDate = now(),
//                applicableSections = listOf(differentApplicableSection),
//                vendorListId = ""
//            ),
//            ccpa = null
//        )
//        coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH)
//        assertTrue(spClientMock.consentStatusCalled)
//    }

    @Test
    fun flushingConsentWhenGdprVendorListIdChanges() = runTest {
        val coordinator = getCoordinator()
        val messages = coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH)
        assertNotEmpty(messages.filter { it.type == Gdpr })
        coordinator.reportAction(SPAction(type = AcceptAll, campaignType = Gdpr))
        coordinator.state = coordinator.state.copy(
            gdpr = coordinator.state.gdpr.copy(
                metaData = coordinator.state.gdpr.metaData.copy(vendorListId = "foo")
            )
        )
        val secondMessages = coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH)
        assertNotEmpty(secondMessages.filter { it.type == Gdpr })
    }

    @Test
    fun flushingConsentWhenUsnatVendorListIdChanges() = runTest {
        val coordinator = getCoordinator()
        val messages = coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH)
        assertNotEmpty(messages.filter { it.type == UsNat })
        coordinator.reportAction(SPAction(type = AcceptAll, campaignType = UsNat))
        coordinator.state = coordinator.state.copy(
            usNat = coordinator.state.usNat.copy(
                metaData = coordinator.state.usNat.metaData.copy(
                    vendorListId = "foo"
                )
            )
        )
        val secondMessages = coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH)
        assertNotEmpty(secondMessages.filter { it.type == UsNat })
    }

    @Test
    fun whenALegislationDoesNotApplyMessageIsReturnedRegardless() = runTest {
        val spClientMock = SPClientMock(
            original = spClient,
            getMetaData = { MetaDataResponse(
                ccpa = MetaDataResponse.MetaDataResponseCCPA(applies = false, sampleRate = 1.0f)
            )
        })
        val coordinator = getCoordinator(spClient = spClientMock, campaigns = SPCampaigns(ccpa = SPCampaign()))
        val messages = coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH)
        assertEquals(1, messages.size)
        assertNotEmpty(repository.uspString)
    }
}
