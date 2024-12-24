package com.sourcepoint.mobile_core

import com.russhwolf.settings.MapSettings
import com.sourcepoint.mobile_core.models.SPAction
import com.sourcepoint.mobile_core.models.SPActionType
import com.sourcepoint.mobile_core.models.SPCampaignType
import com.sourcepoint.mobile_core.models.consents.CCPAConsent
import com.sourcepoint.mobile_core.models.consents.GDPRConsent
import com.sourcepoint.mobile_core.models.consents.State
import com.sourcepoint.mobile_core.models.consents.USNatConsent
import com.sourcepoint.mobile_core.network.requests.ChoiceAllRequest
import com.sourcepoint.mobile_core.network.requests.MetaDataRequest
import com.sourcepoint.mobile_core.storage.Repository
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class CoordinatorTest {
    val storage = MapSettings()
    val repository = Repository(storage)
    val coordinator = Coordinator(accountId = 22, propertyId = 16893, propertyName = "https://mobile.multicampaign.demo",  repository)

    @Test
    fun metaDataIsCached() = runTest {
        val campaigns = MetaDataRequest.Campaigns(gdpr = MetaDataRequest.Campaigns.Campaign())
        val metaData = coordinator.getMetaData(campaigns)
        assertContains(metaData, "/meta-data")
        assertContains(storage.keys, "MetaData")
    }

    @BeforeTest
    fun initCoordinatorState(){
        coordinator.state = State(
            gdpr = GDPRConsent(),
            ccpa = CCPAConsent(),
            usNat = USNatConsent(),
            gdprMetaData = null,
            ccpaMetaData = null,
            usNatMetaData = null
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
        assertFalse(state.gdpr?.uuid.isNullOrEmpty())
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
        assertFalse(state.ccpa?.uuid.isNullOrEmpty())
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
        assertFalse(state.usNat?.uuid.isNullOrEmpty())
    }
}
