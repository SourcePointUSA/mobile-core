package com.sourcepoint.mobile_core

import com.russhwolf.settings.MapSettings
import com.sourcepoint.mobile_core.models.SPAction
import com.sourcepoint.mobile_core.models.SPActionType
import com.sourcepoint.mobile_core.models.SPCampaign
import com.sourcepoint.mobile_core.models.SPCampaignType
import com.sourcepoint.mobile_core.models.SPCampaigns
import com.sourcepoint.mobile_core.models.SPPropertyName
import com.sourcepoint.mobile_core.models.consents.State
import com.sourcepoint.mobile_core.network.SourcepointClient
import com.sourcepoint.mobile_core.network.encodeToJsonObject
import com.sourcepoint.mobile_core.storage.Repository
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

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

    @BeforeTest
    fun initCoordinatorState() {
        coordinator = Coordinator(
            accountId = accountId,
            propertyId = propertyId,
            propertyName = propertyName,
            campaigns = campaigns,
            spClient = SourcepointClient(
                accountId = accountId,
                propertyId = propertyId,
                propertyName = propertyName
            ),
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
        coordinator.reportAction(SPAction(SPActionType.AcceptAll, SPCampaignType.Gdpr))
        coordinator.reportAction(SPAction(SPActionType.AcceptAll, SPCampaignType.Ccpa))
        coordinator.reportAction(SPAction(SPActionType.AcceptAll, SPCampaignType.UsNat))
        assertEquals(0, coordinator.loadMessages(authId = null, pubData = null).size)
    }
}
