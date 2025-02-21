package com.sourcepoint.mobile_core

import com.russhwolf.settings.MapSettings
import com.sourcepoint.mobile_core.mocks.SPClientMock
import com.sourcepoint.mobile_core.models.SPAction
import com.sourcepoint.mobile_core.models.SPActionType
import com.sourcepoint.mobile_core.models.SPCampaign
import com.sourcepoint.mobile_core.models.SPCampaignType
import com.sourcepoint.mobile_core.models.SPCampaigns
import com.sourcepoint.mobile_core.models.SPMessageLanguage
import com.sourcepoint.mobile_core.models.SPPropertyName
import com.sourcepoint.mobile_core.models.consents.State
import com.sourcepoint.mobile_core.network.SPClient
import com.sourcepoint.mobile_core.network.SourcepointClient
import com.sourcepoint.mobile_core.network.encodeToJsonObject
import com.sourcepoint.mobile_core.storage.Repository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class CoordinatorTest {
    private val storage = MapSettings()
    private val repository = Repository(storage)
    private val accountId = 22
    private val propertyId = 16893
    private val propertyName = SPPropertyName.create("mobile.multicampaign.demo")
    private val campaigns = SPCampaigns(
        gdpr = SPCampaign(),
        ccpa = SPCampaign(),
        usnat = SPCampaign(),
        ios14 = SPCampaign(),
    )
    private val state = State(accountId = accountId, propertyId = propertyId)

    private fun getCoordinator(
        accountId: Int = this.accountId,
        propertyId: Int = this.propertyId,
        propertyName: SPPropertyName = this.propertyName,
        campaigns: SPCampaigns = this.campaigns,
        spClient: SPClient = SourcepointClient(
            accountId = accountId,
            propertyId = propertyId,
            propertyName = propertyName
        ),
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
        coordinator.loadMessages(authId = "foo", pubData = null, language = SPMessageLanguage.ENGLISH)
        assertSame(coordinator.state, state)
    }

    @Test
    fun shouldResetStateIfAuthIdChangeFromSomethingToSomethingElse() = runTest {
        val state = State(accountId = accountId, propertyId = propertyId, authId = "foo")
        val coordinator = getCoordinator(spClient = SPClientMock(), state = state)
        coordinator.loadMessages(authId = "bar", pubData = null, language = SPMessageLanguage.ENGLISH)
        assertNotSame(coordinator.state, state)
    }

    @Test
    fun reportActionReturnsGDPRConsent() = runTest {
        val consents = getCoordinator().reportAction(
            action = SPAction(type = SPActionType.AcceptAll, campaignType = SPCampaignType.Gdpr),
        )
        assertFalse(consents.gdpr?.consents?.uuid.isNullOrEmpty())
    }

    @Test
    fun reportActionReturnsCCPAConsent() = runTest {
        val consents = getCoordinator().reportAction(
            action = SPAction(type = SPActionType.RejectAll, campaignType = SPCampaignType.Ccpa),
        )
        assertFalse(consents.ccpa?.consents?.uuid.isNullOrEmpty())
    }

    @Test
    fun reportActionReturnsUSNatConsent() = runTest {
        val consents = getCoordinator().reportAction(
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
                """.encodeToJsonObject(),
            )
        )
        assertFalse(consents.usnat?.consents?.uuid.isNullOrEmpty())
    }

    @Test
    fun noMessagesShouldAppearAfterAcceptingAll() = runTest {
        val coordinator = getCoordinator()
        assertEquals(3, coordinator.loadMessages(authId = null, pubData = null, language = SPMessageLanguage.ENGLISH).size)
        coordinator.reportAction(SPAction(SPActionType.AcceptAll, SPCampaignType.Gdpr))
        coordinator.reportAction(SPAction(SPActionType.AcceptAll, SPCampaignType.Ccpa))
        coordinator.reportAction(SPAction(SPActionType.AcceptAll, SPCampaignType.UsNat))
        assertEquals(0, coordinator.loadMessages(authId = null, pubData = null, language = SPMessageLanguage.ENGLISH).size)
    }
}
