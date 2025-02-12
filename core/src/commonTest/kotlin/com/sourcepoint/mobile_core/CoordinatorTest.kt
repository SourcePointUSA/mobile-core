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
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CoordinatorTest {
    val storage = MapSettings()
    val repository = Repository(storage)
    lateinit var coordinator: Coordinator

    @BeforeTest
    fun initCoordinatorState(){
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
        val saveAndExitAction = SPAction(
            type = SPActionType.AcceptAll,
            campaignType = SPCampaignType.Gdpr,
            messageId = null,
            pmPayload = "{" +
                    "\"vendors\":[], " +
                    "\"categories\":[], " +
                    "\"specialFeatures\":[], " +
                    "\"lan\":\"EN\", " +
                    "\"privacyManagerId\":\"488393\", " +
                    "\"vendors\":[]" +
                    "}",
            encodablePubData = null
        )
        val state = coordinator.reportAction(saveAndExitAction, ChoiceAllRequest.ChoiceAllCampaigns(
            gdpr = ChoiceAllRequest.ChoiceAllCampaigns.Campaign(true),
            usnat = ChoiceAllRequest.ChoiceAllCampaigns.Campaign(false),
            ccpa = ChoiceAllRequest.ChoiceAllCampaigns.Campaign(false)
        ))
        assertFalse(state.gdpr.consents.uuid.isNullOrEmpty())
    }

    @Test
    fun reportActionReturnsCCPAConsent() = runTest {
        val saveAndExitAction = SPAction(
            type = SPActionType.RejectAll,
            campaignType = SPCampaignType.Ccpa,
            messageId = null,
            pmPayload = "{" +
                    "\"rejectedCategories\":[], " +
                    "\"rejectedVendors\":[], " +
                    "\"lan\":\"EN\", " +
                    "\"privacyManagerId\":\"509688\" " +
                    "}",
            encodablePubData = null
        )
        val state = coordinator.reportAction(saveAndExitAction, ChoiceAllRequest.ChoiceAllCampaigns(
            ccpa = ChoiceAllRequest.ChoiceAllCampaigns.Campaign(true),
            gdpr = ChoiceAllRequest.ChoiceAllCampaigns.Campaign(false),
            usnat = ChoiceAllRequest.ChoiceAllCampaigns.Campaign(false)
        ))
        assertFalse(state.ccpa.consents.uuid.isNullOrEmpty())
    }

    @Test
    fun reportActionReturnsUSNatConsent() = runTest {
        val saveAndExitAction = SPAction(
            type = SPActionType.SaveAndExit,
            campaignType = SPCampaignType.UsNat,
            messageId = null,
            pmPayload = "{" +
                    "\"shownCategories\":[\"6568ae4503cf5cf81eb79fa5\"], " +
                    "\"categories\":[\"6568ae4503cf5cf81eb79fa5\"], " +
                    "\"lan\":\"EN\", " +
                    "\"privacyManagerId\":\"943890\", " +
                    "\"vendors\":[]" +
                    "}",
            encodablePubData = null
        )
        val state = coordinator.reportAction(saveAndExitAction, ChoiceAllRequest.ChoiceAllCampaigns(
            usnat = ChoiceAllRequest.ChoiceAllCampaigns.Campaign(true),
            gdpr = ChoiceAllRequest.ChoiceAllCampaigns.Campaign(false),
            ccpa = ChoiceAllRequest.ChoiceAllCampaigns.Campaign(false)
        ))
        assertFalse(state.usNat.consents.uuid.isNullOrEmpty())
    }
}
