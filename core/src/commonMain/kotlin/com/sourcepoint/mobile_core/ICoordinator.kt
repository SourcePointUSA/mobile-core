package com.sourcepoint.mobile_core

import com.sourcepoint.mobile_core.models.MessageToDisplay
import com.sourcepoint.mobile_core.models.SPAction
import com.sourcepoint.mobile_core.models.consents.State
import com.sourcepoint.mobile_core.network.requests.ChoiceAllRequest
import com.sourcepoint.mobile_core.network.responses.CCPAChoiceResponse
import com.sourcepoint.mobile_core.network.responses.ChoiceAllResponse
import com.sourcepoint.mobile_core.network.responses.GDPRChoiceResponse
import com.sourcepoint.mobile_core.network.responses.USNatChoiceResponse
import kotlinx.serialization.json.JsonObject

interface ICoordinator {
    @Throws(Exception::class) suspend fun reportAction(
        action: SPAction,
        campaigns: ChoiceAllRequest.ChoiceAllCampaigns
    ): State
    @Throws(Exception::class) suspend fun loadMessages(
        authId: String?,
        pubData: JsonObject?
    ): List<MessageToDisplay>
}
