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
import com.sourcepoint.mobile_core.models.consents.State
import com.sourcepoint.mobile_core.network.SPClient
import com.sourcepoint.mobile_core.network.SourcepointClient
import com.sourcepoint.mobile_core.network.responses.MetaDataResponse
import com.sourcepoint.mobile_core.storage.Repository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

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
        accountId: Int = this.accountId,
        propertyId: Int = this.propertyId,
        propertyName: SPPropertyName = this.propertyName,
        campaigns: SPCampaigns = this.campaigns,
        spClient: SPClient = this.spClient,
        repository: Repository = this.repository,
        state: State = this.state
    ) = Coordinator(
        accountId = accountId,
        propertyId = propertyId,
        propertyName = propertyName,
        campaigns = campaigns,
        spClient = spClient,
        repository = repository,
        state = state
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

        assertEquals(0, coordinator.loadMessages(authId = null, pubData = null, language = ENGLISH).size)
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
