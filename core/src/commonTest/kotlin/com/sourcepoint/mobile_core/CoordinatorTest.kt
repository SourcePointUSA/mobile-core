package com.sourcepoint.mobile_core

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
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
import com.sourcepoint.mobile_core.models.LoadMessagesException
import com.sourcepoint.mobile_core.models.SPAction
import com.sourcepoint.mobile_core.models.SPActionType.AcceptAll
import com.sourcepoint.mobile_core.models.SPActionType.RejectAll
import com.sourcepoint.mobile_core.models.SPActionType.SaveAndExit
import com.sourcepoint.mobile_core.models.SPCampaign
import com.sourcepoint.mobile_core.models.SPCampaignType.Ccpa
import com.sourcepoint.mobile_core.models.SPCampaignType.Gdpr
import com.sourcepoint.mobile_core.models.SPCampaignType.UsNat
import com.sourcepoint.mobile_core.models.SPCampaigns
import com.sourcepoint.mobile_core.models.SPMessageLanguage
import com.sourcepoint.mobile_core.models.SPMessageLanguage.ENGLISH
import com.sourcepoint.mobile_core.models.SPPropertyName
import com.sourcepoint.mobile_core.models.consents.GDPRConsent
import com.sourcepoint.mobile_core.models.consents.State
import com.sourcepoint.mobile_core.network.PlatformHttpClient
import com.sourcepoint.mobile_core.network.SPClient
import com.sourcepoint.mobile_core.network.SourcepointClient
import com.sourcepoint.mobile_core.network.requests.ConsentStatusRequest
import com.sourcepoint.mobile_core.network.requests.MetaDataRequest
import com.sourcepoint.mobile_core.network.responses.ConsentStatusResponse
import com.sourcepoint.mobile_core.network.responses.MetaDataResponse
import com.sourcepoint.mobile_core.network.responses.PvDataResponse
import com.sourcepoint.mobile_core.storage.Repository
import com.sourcepoint.mobile_core.utils.now
import com.sourcepoint.mobile_core.utils.runTestWithRetries
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.days
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid.Companion.random

class CoordinatorTest {
    private val storage: Settings get() = MapSettings()
    private val repository: Repository get() = Repository(storage)
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
        httpEngine = PlatformHttpClient.create().engine
    )
    private val saveAndExitActionUsnat = SPAction.init(
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
        """
    )

    private fun getCoordinator(
        authId: String? = null,
        accountId: Int = this.accountId,
        propertyId: Int = this.propertyId,
        propertyName: String = this.propertyName.rawValue ,
        campaigns: SPCampaigns = this.campaigns,
        spClient: SPClient = this.spClient,
        repository: Repository = this.repository,
        state: State = this.state
    ) = Coordinator(
        authId = authId,
        accountId = accountId,
        propertyId = propertyId,
        propertyName = SPPropertyName.create(propertyName),
        campaigns = campaigns,
        spClient = spClient,
        repository = repository,
        state = state
    )

    private suspend fun Coordinator.acceptAllLegislations() {
        reportAction(SPAction(AcceptAll, Gdpr))
        reportAction(SPAction(AcceptAll, Ccpa))
        reportAction(SPAction(AcceptAll, UsNat))
    }

    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    private suspend fun Coordinator.loadMessages(
        authId: String? = null,
        pubData: JsonObject? = null,
        language: SPMessageLanguage = ENGLISH
    ) = loadMessages(authId, pubData, language)

    @OptIn(ExperimentalUuidApi::class)
    fun getRandomAuthId() = "mobile-core-testing-${random()}"

    private fun metaDataResponseUSNat(
        applies: Boolean = false,
        sampleRate: Float = 1f,
        additionsChangeDate: Instant = now(),
        applicableSections: List<Int> = emptyList(),
        vendorListId: String = ""
    ) = MetaDataResponse.MetaDataResponseUSNat(
        applies = applies,
        sampleRate = sampleRate,
        additionsChangeDate = additionsChangeDate,
        applicableSections = applicableSections,
        vendorListId = vendorListId
    )

    private fun consentStatusResponse() = ConsentStatusResponse(
        consentStatusData = ConsentStatusResponse.ConsentStatusData(), localState = ""
    )

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
    fun shouldNotResetStateIfAuthIdChangesFromNull() = runTestWithRetries {
        val state = State(accountId = accountId, propertyId = propertyId, authId = null)
        val coordinator = getCoordinator(spClient = SPClientMock(), state = state)
        coordinator.loadMessages(authId = "foo")
        assertSame(coordinator.state, state)
    }

    @Test
    fun shouldResetStateIfAuthIdChangeFromSomethingToSomethingElse() = runTestWithRetries {
        val state = State(accountId = accountId, propertyId = propertyId, authId = "foo")
        val coordinator = getCoordinator(spClient = SPClientMock(), state = state)
        coordinator.loadMessages(authId = "bar")
        assertNotSame(coordinator.state, state)
    }

    @Test
    fun reportActionReturnsGDPRConsent() = runTestWithRetries {
        val consents = getCoordinator().reportAction(SPAction(type = AcceptAll, campaignType = Gdpr))
        assertNotEmpty(consents.gdpr?.consents?.uuid)
    }

    @Test
    fun reportActionReturnsCCPAConsent() = runTestWithRetries {
        val consents = getCoordinator().reportAction(SPAction(type = RejectAll, campaignType = Ccpa))
        assertNotEmpty(consents.ccpa?.consents?.uuid)
    }

    @Test
    fun reportActionReturnsUSNatConsent() = runTestWithRetries {
        val consents = getCoordinator().reportAction(saveAndExitActionUsnat)
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
    fun deleteCustomGDPRConsentRemovesVendorFromVendorsProperty() = runTestWithRetries {
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
    fun customConsentGDPRAddsVendorToVendorsProperty() = runTestWithRetries {
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
    fun consentIsStoredAfterCallingLoadMessagesAndNoMessagesShouldAppearAfterAcceptingAll() = runTestWithRetries {
        val localStorage = repository
        val coordinator = getCoordinator(repository = localStorage)
        assertEquals(3, coordinator.loadMessages().size)
        assertNotEmpty(localStorage.gppData)
        assertNotEmpty(localStorage.tcData)
        assertNotEmpty(localStorage.uspString)
        assertDefaultConsents(coordinator.userData.gdpr?.consents)
        assertDefaultConsents(coordinator.userData.ccpa?.consents)
        assertDefaultConsents(coordinator.userData.usnat?.consents)

        coordinator.acceptAllLegislations()
        assertNotEmpty(localStorage.gppData)
        assertNotEmpty(localStorage.tcData)
        assertNotEmpty(localStorage.uspString)
        assertAllAccepted(coordinator.userData.gdpr?.consents)
        assertAllAccepted(coordinator.userData.ccpa?.consents)
        assertAllAccepted(coordinator.userData.usnat?.consents)

        assertIsEmpty(coordinator.loadMessages())
    }

    @Test
    fun consentIsLinkedToAuthId() = runTestWithRetries {
        val authId = getRandomAuthId()
        var coordinator = getCoordinator(authId = authId)
        coordinator.reportAction(SPAction(AcceptAll, Gdpr))
        val firstUuid = coordinator.userData.gdpr?.consents?.uuid
        assertNotEmpty(firstUuid)

        coordinator = getCoordinator()
        coordinator.loadMessages(authId = authId)
        assertEquals(firstUuid, coordinator.userData.gdpr?.consents?.uuid)
    }

    @Test
    fun consentStatusNotCalledWhenLocalDataOutdatedNoUuid() = runTestWithRetries {
        var consentStatusCalled = false
        val spClientMock = SPClientMock(getConsentStatus = { _, _ ->
            consentStatusCalled = true
            consentStatusResponse()
        })
        val coordinator = getCoordinator(
            spClient = spClientMock,
            state = State(localVersion = 0, propertyId = propertyId, accountId = accountId)
        )
        coordinator.loadMessages()
        assertFalse(consentStatusCalled)
    }

    @Test
    fun consentStatusCalledWhenLocalDataOutdatedHasUuid() = runTestWithRetries {
        var consentStatusCalled = false
        val spClientMock = SPClientMock(getConsentStatus = { _, _ ->
            consentStatusCalled = true
            consentStatusResponse()
        })
        val coordinator = getCoordinator(spClient = spClientMock, state = State(
            gdpr = State.GDPRState(consents = GDPRConsent(uuid = "foo")),
            localVersion = 0,
            propertyId = propertyId,
            accountId = accountId
        ))
        coordinator.loadMessages()
        assertTrue(consentStatusCalled)
    }

    @Test
    fun consentStatusSetsLocalVersionWhenSucceeds() = runTestWithRetries {
        val coordinator = getCoordinator(spClient =  SPClientMock())
        coordinator.loadMessages()
        assertEquals(State.VERSION, coordinator.state.localVersion)
    }

    @Test
    fun consentStatusLeavesLocalVersionWhenFails() = runTestWithRetries {
        val coordinator = getCoordinator(
            spClient = SPClientMock(getConsentStatus = { _, _ -> throw  Exception() }),
            state = State(
                gdpr = State.GDPRState(consents = GDPRConsent(uuid = "foo")),
                localVersion = 0,
                propertyId = propertyId,
                accountId = accountId
            )
        )
        runCatching { coordinator.loadMessages() }
        assertEquals(0, coordinator.state.localVersion)
    }

    @Test
    fun whenUserHasConsentStoredCallingLoadMessagesDoesNotChangeIt() = runTestWithRetries {
        val coordinator = getCoordinator()
        coordinator.loadMessages()
        val userData = coordinator.userData
        val actionResult = coordinator.reportAction(SPAction(RejectAll, Gdpr))
        assertNotEquals(userData, actionResult)
        coordinator.loadMessages()
        val secondUserData = coordinator.userData
        assertEquals(actionResult, secondUserData)
    }

    @Test
    fun whenUserHasConsentStoredAndExpirationDateGreaterCurrentDate() = runTestWithRetries {
        val coordinator = getCoordinator()
        coordinator.acceptAllLegislations()
        coordinator.state = coordinator.state.copy(
            gdpr = coordinator.state.gdpr.copy(
                consents = coordinator.state.gdpr.consents.copy(expirationDate = now().minus(1.days))
            ),
            ccpa = coordinator.state.ccpa.copy(
                consents = coordinator.state.ccpa.consents.copy(expirationDate = now().minus(1.days))
            ),
            usNat = coordinator.state.usNat.copy(
                consents = coordinator.state.usNat.consents.copy(expirationDate = now().minus(1.days))
            )
        )
        assertEquals(3, coordinator.loadMessages().size)
    }

    @Test
    fun propertyWithSupportLegacyUSPStringReceivesIABUSPrivacyString() = runTestWithRetries {
        val localStorage = repository
        val coordinator = getCoordinator(
            campaigns = SPCampaigns(usnat = SPCampaign(supportLegacyUSPString = true)),
            repository = localStorage
        )
        assertNull(localStorage.uspString)
        coordinator.loadMessages()
        assertNotEmpty(localStorage.uspString)
    }

    @Test
    fun propertyWithAuthIdAndTransitionCCPAAuthSetsFlagInConsentStatusMetadata() = runTestWithRetries {
        var coordinator = getCoordinator(campaigns = SPCampaigns(ccpa = SPCampaign()))
        val authId = getRandomAuthId()
        coordinator.loadMessages(authId = authId)
        coordinator.reportAction(SPAction(type = RejectAll, campaignType = Ccpa))

        coordinator = getCoordinator(campaigns = SPCampaigns(usnat = SPCampaign(transitionCCPAAuth = true)))
        assertNotEmpty(coordinator.loadMessages(authId = authId))
        assertTrue(coordinator.userData.usnat?.consents?.consentStatus?.rejectedAny)
        assertFalse(coordinator.userData.usnat?.consents?.consentStatus?.consentedToAll)
    }

    @Test
    fun handlesCCPAOptOutWhenThereIsNoAuthId() = runTestWithRetries {
        var consentStatusMetaData: ConsentStatusRequest.MetaData? = null
        val spClientMock = SPClientMock(getConsentStatus = { _, metadata ->
            consentStatusMetaData = metadata
            consentStatusResponse()
        })
        val coordinator = getCoordinator(spClient = spClientMock, campaigns = SPCampaigns(usnat = SPCampaign(transitionCCPAAuth = true)))
        coordinator.loadMessages()
        assertNull(consentStatusMetaData?.usnat?.transitionCCPAAuth)
    }

    @Test
    fun resetsSamplingStatusWhenSampleRateChanges() = runTestWithRetries {
        val campaigns = SPCampaigns(gdpr = SPCampaign())
        val spClientMock = SPClientMock(
            original = spClient,
            getMetaData = {
                spClient.getMetaData(
                    campaigns = MetaDataRequest.Campaigns(gdpr = MetaDataRequest.Campaigns.Campaign())
                ).run {
                    copy(gdpr = gdpr?.copy(sampleRate = 0f))
                }
            }
        )
        val coordinator = getCoordinator(
            campaigns = SPCampaigns(gdpr = SPCampaign()),
            spClient = spClientMock
        )
        coordinator.loadMessages()
        assertEquals(0f, coordinator.state.gdpr.metaData.sampleRate, "sampleRate")
        assertEquals(0f, coordinator.state.gdpr.metaData.wasSampledAt, "wasSampledAt")
        assertFalse(coordinator.state.gdpr.metaData.wasSampled)

        val secondCoordinator = getCoordinator(campaigns = campaigns, state = coordinator.state)
        secondCoordinator.loadMessages()
        assertEquals(1f, secondCoordinator.state.gdpr.metaData.sampleRate, "sampleRate")
        assertEquals(1f, secondCoordinator.state.gdpr.metaData.wasSampledAt, "wasSampledAt")
        assertTrue(secondCoordinator.state.gdpr.metaData.wasSampled)
    }

    @Test
    fun reconsentWorksWhenVendorsAreAddedToTheVendorList() = runTestWithRetries {
        val campaigns = SPCampaigns(gdpr = SPCampaign())
        val consents = getCoordinator(campaigns = campaigns).reportAction(SPAction(type = AcceptAll, campaignType = Gdpr))

        val spClientMock = SPClientMock(
            original = spClient,
            getMetaData = {
                val originalMetaData = spClient.getMetaData(
                    campaigns = MetaDataRequest.Campaigns(gdpr = MetaDataRequest.Campaigns.Campaign())
                )
                MetaDataResponse(gdpr = MetaDataResponse.MetaDataResponseGDPR(
                    applies = true,
                    sampleRate = 1.0f,
                    additionsChangeDate = consents.gdpr!!.consents!!.dateCreated + 1.days,
                    legalBasisChangeDate = originalMetaData.gdpr!!.legalBasisChangeDate,
                    vendorListId = originalMetaData.gdpr!!.vendorListId,
                ))
            }
        )
        assertEquals(1, getCoordinator(campaigns = campaigns, spClient = spClientMock).loadMessages().size)
    }

    @Test
    fun reconsentWorksWhenLegalBasisOfVendorsChangeInTheVendorList() = runTestWithRetries {
        val campaigns = SPCampaigns(gdpr = SPCampaign())
        val consents = getCoordinator(campaigns = campaigns).reportAction(SPAction(type = AcceptAll, campaignType = Gdpr))

        val spClientMock = SPClientMock(
            original = spClient,
            getMetaData = {
                val originalMetaData = spClient.getMetaData(
                    campaigns = MetaDataRequest.Campaigns(gdpr = MetaDataRequest.Campaigns.Campaign())
                )
                MetaDataResponse(gdpr = MetaDataResponse.MetaDataResponseGDPR(
                    applies = true,
                    sampleRate = 1.0f,
                    additionsChangeDate = originalMetaData.gdpr!!.additionsChangeDate,
                    legalBasisChangeDate = consents.gdpr!!.consents!!.dateCreated + 1.days,
                    vendorListId = originalMetaData.gdpr!!.vendorListId,
                ))
            }
        )
        assertEquals(1, getCoordinator(campaigns = campaigns, spClient = spClientMock).loadMessages().size)
    }

    @Test
    fun usnatPropertyAdditionsChangeDateBiggerThanConsentDateCreatedShowMessage() = runTestWithRetries {
        val coordinator = getCoordinator(campaigns = SPCampaigns(usnat = SPCampaign()))
        assertEquals(1, coordinator.loadMessages().size)
        coordinator.reportAction(saveAndExitActionUsnat)
        coordinator.state = coordinator.state.copy(
            usNat = coordinator.state.usNat.copy(
                consents = coordinator.state.usNat.consents.copy(
                    dateCreated = coordinator.state.usNat.metaData.additionsChangeDate.minus(1.days)
                )
            )
        )
        assertEquals(1, coordinator.loadMessages().size)
    }

    @Test
    fun whenUsnatApplicableSectionsChangeShouldCallConsentStatus() = runTestWithRetries {
        val firstApplicableSections = listOf(1)
        val differentApplicableSections = listOf(2)
        var consentStatusCalled = false
        val spClientMock = SPClientMock(
            getMetaData = { MetaDataResponse(
                usnat = metaDataResponseUSNat(applicableSections = firstApplicableSections),
            )},
            getConsentStatus = { _, _ ->
                consentStatusCalled = true
                consentStatusResponse()
            }
        )
        val coordinator = getCoordinator(spClient = spClientMock, campaigns = SPCampaigns(usnat = SPCampaign()))
        coordinator.loadMessages()
        assertFalse(consentStatusCalled)
        assertEquals(firstApplicableSections, coordinator.state.usNat.metaData.applicableSections)

        spClientMock.getMetaData = { MetaDataResponse(
            usnat = metaDataResponseUSNat(applicableSections = differentApplicableSections),
        ) }
        coordinator.loadMessages()
        assertTrue(consentStatusCalled)
        assertEquals(differentApplicableSections, coordinator.state.usNat.metaData.applicableSections)
    }

    @Test
    fun flushingConsentWhenGdprVendorListIdChanges() = runTestWithRetries {
        val coordinator = getCoordinator(campaigns = SPCampaigns(gdpr = SPCampaign()))
        assertEquals(1, coordinator.loadMessages().size)
        coordinator.reportAction(SPAction(type = AcceptAll, campaignType = Gdpr))
        coordinator.state = coordinator.state.copy(
            gdpr = coordinator.state.gdpr.copy(
                metaData = coordinator.state.gdpr.metaData.copy(vendorListId = "foo")
            )
        )
        coordinator.reportAction(SPAction(type = AcceptAll, campaignType = Gdpr))
    }

    @Test
    fun flushingConsentWhenUsnatVendorListIdChanges() = runTestWithRetries {
        val coordinator = getCoordinator(campaigns = SPCampaigns(usnat = SPCampaign()))
        assertEquals(1, coordinator.loadMessages().size)
        coordinator.reportAction(SPAction(type = AcceptAll, campaignType = UsNat))
        coordinator.state = coordinator.state.copy(
            usNat = coordinator.state.usNat.copy(
                metaData = coordinator.state.usNat.metaData.copy(vendorListId = "foo")
            )
        )
        coordinator.reportAction(SPAction(type = AcceptAll, campaignType = UsNat))
    }

    @Test
    fun whenALegislationDoesNotApplyMessageIsReturnedRegardless() = runTestWithRetries {
        val localStorage = repository
        val spClientMock = SPClientMock(
            original = spClient,
            getMetaData = { MetaDataResponse(
                ccpa = MetaDataResponse.MetaDataResponseCCPA(applies = false, sampleRate = 1.0f)
            )
        })
        val coordinator = getCoordinator(
            spClient = spClientMock,
            campaigns = SPCampaigns(ccpa = SPCampaign()),
            repository = localStorage
        )
        val messages = coordinator.loadMessages()
        assertEquals(1, messages.size)
        assertNotEmpty(localStorage.uspString)
    }

    @Test
    fun throwsLoadMessagesExceptionIfTheConfigIsWrong() = runTestWithRetries {
        assertFailsWith<LoadMessagesException> { getCoordinator(accountId = -1).loadMessages() }
        assertFailsWith<LoadMessagesException> { getCoordinator(propertyId = -1).loadMessages() }
        assertFailsWith<LoadMessagesException> { getCoordinator(propertyName = "foo").loadMessages() }
    }

    // TODO: add tests for the pvData payload in different circumstances (1st call vs subsequent calls)
    @Test
    fun pvDataIsCalledOnLoadMessage() = runTestWithRetries {
        var pvDataCalled = false
        val coordinator = getCoordinator(spClient = SPClientMock(postPvData = {
            pvDataCalled = true
            PvDataResponse()
        }))
        coordinator.loadMessages()
        assertTrue(pvDataCalled)
    }

    @Test
    fun propertyWithPreferencesCampaign() = runTestWithRetries {
        return@runTestWithRetries //TODO: this test doesn`t work on prod
        val coordinator = getCoordinator(
            accountId = 22,
            propertyId = 38984,
            propertyName = "dmytro.tests.mobile.preferences",
            campaigns = SPCampaigns(preferences = SPCampaign()),
            spClient = SourcepointClient(
                accountId = 22,
                propertyId = 38984
            )
        )
        assertEquals(1, coordinator.loadMessages().size)
        assertEquals("67ee726a0ca4ef1d52c34563", coordinator.state.preferences.metaData.configurationId)
    }
}
