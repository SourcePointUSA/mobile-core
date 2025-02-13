package com.sourcepoint.mobile_core

import com.russhwolf.settings.MapSettings
import com.sourcepoint.mobile_core.models.SPAction
import com.sourcepoint.mobile_core.models.SPActionType
import com.sourcepoint.mobile_core.models.SPCampaign
import com.sourcepoint.mobile_core.models.SPCampaignType
import com.sourcepoint.mobile_core.models.SPCampaigns
import com.sourcepoint.mobile_core.models.consents.State
import com.sourcepoint.mobile_core.network.SourcepointClient
import com.sourcepoint.mobile_core.network.requests.ChoiceAllRequest
import com.sourcepoint.mobile_core.storage.Repository
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse

class CoordinatorTest {
    private val storage = MapSettings()
    private val repository = Repository(storage)
    private lateinit var coordinator: Coordinator

    @BeforeTest
    fun initCoordinatorState() {
        coordinator = Coordinator(
            accountId = 22,
            propertyId = 16893,
            propertyName = "https://mobile.multicampaign.demo",
            campaigns = SPCampaigns(
                gdpr = SPCampaign(),
                ccpa = SPCampaign(),
                usnat = SPCampaign(),
                ios14 = SPCampaign(),
            ),
            spClient = SourcepointClient(accountId = 22, propertyId = 16893, propertyName = "https://mobile.multicampaign.demo"),
            repository = repository,
            state = State()
        )
    }

    @Test
    fun reportActionReturnsGDPRConsent() = runTest {
        val consents = coordinator.reportAction(
            action = SPAction(type = SPActionType.AcceptAll, campaignType = SPCampaignType.Gdpr),
            campaigns = ChoiceAllRequest.ChoiceAllCampaigns(
                gdpr = ChoiceAllRequest.ChoiceAllCampaigns.Campaign(true)
            )
        )
        assertFalse(consents.gdpr?.consents?.uuid.isNullOrEmpty())
    }

    @Test
    fun reportActionReturnsCCPAConsent() = runTest {
        val consents = coordinator.reportAction(
            action = SPAction(type = SPActionType.RejectAll, campaignType = SPCampaignType.Ccpa),
            campaigns = ChoiceAllRequest.ChoiceAllCampaigns(
                ccpa = ChoiceAllRequest.ChoiceAllCampaigns.Campaign(true)
            )
        )
        assertFalse(consents.ccpa?.consents?.uuid.isNullOrEmpty())
    }

    @Test
    fun reportActionReturnsUSNatConsent() = runTest {
        val consents = coordinator.reportAction(
            action = SPAction(
                type = SPActionType.SaveAndExit,
                campaignType = SPCampaignType.UsNat,
                messageId = null,
                pmPayload = """
                {
                    "shownCategories": ["6568ae4503cf5cf81eb79fa5"],
                    "categories": ["6568ae4503cf5cf81eb79fa5"],
                    "lan": "EN",
                    "privacyManagerId": "943890",
                    "vendors": []
                }
                """,
                encodablePubData = null
            ),
            campaigns = ChoiceAllRequest.ChoiceAllCampaigns(
                usnat = ChoiceAllRequest.ChoiceAllCampaigns.Campaign(true)
            )
        )
        assertFalse(consents.usnat?.consents?.uuid.isNullOrEmpty())
    }
}
