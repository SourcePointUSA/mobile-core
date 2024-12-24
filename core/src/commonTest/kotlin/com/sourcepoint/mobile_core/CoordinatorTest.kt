package com.sourcepoint.mobile_core

import com.russhwolf.settings.MapSettings
import com.sourcepoint.mobile_core.models.SPAction
import com.sourcepoint.mobile_core.models.SPActionType
import com.sourcepoint.mobile_core.models.SPCampaignType
import com.sourcepoint.mobile_core.models.consents.USNatConsent
import com.sourcepoint.mobile_core.network.encodeToJsonObject
import com.sourcepoint.mobile_core.network.requests.ChoiceAllRequest
import com.sourcepoint.mobile_core.network.requests.MetaDataRequest
import com.sourcepoint.mobile_core.storage.Repository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class CoordinatorTest {

    @Test
    fun metaDataIsCached() = runTest {
        val storage = MapSettings()
        val repository = Repository(storage)
        val coordinator = Coordinator(accountId = 22, propertyId = 16893, propertyName = "",  repository)
        val campaigns = MetaDataRequest.Campaigns(gdpr = MetaDataRequest.Campaigns.Campaign())
        val metaData = coordinator.getMetaData(campaigns)
        assertContains(metaData, "/meta-data")
        assertContains(storage.keys, "MetaData")
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
        val storage = MapSettings()
        val repository = Repository(storage)
        val coordinator = Coordinator(accountId = 22, propertyId = 16893, propertyName = "", repository)
        coordinator.state = State(
            gdpr = null,
            ccpa = null,
            usNat = USNatConsent(),
            gdprMetaData = null,
            ccpaMetaData = null,
            usNatMetaData = null
        )
        val state = coordinator.reportAction(saveAndExitAction, ChoiceAllRequest.ChoiceAllCampaigns(usnat = ChoiceAllRequest.ChoiceAllCampaigns.Campaign(true)))
        assertTrue(!state.usNat?.uuid.isNullOrEmpty())
    }
}
