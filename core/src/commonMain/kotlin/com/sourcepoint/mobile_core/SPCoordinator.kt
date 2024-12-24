package com.sourcepoint.mobile_core

import com.sourcepoint.mobile_core.models.SPAction
import com.sourcepoint.mobile_core.models.consents.State
import com.sourcepoint.mobile_core.network.requests.ChoiceAllRequest
import com.sourcepoint.mobile_core.network.responses.CCPAChoiceResponse
import com.sourcepoint.mobile_core.network.responses.ChoiceAllResponse
import com.sourcepoint.mobile_core.network.responses.GDPRChoiceResponse
import com.sourcepoint.mobile_core.network.responses.USNatChoiceResponse

interface SPCoordinator {
    @Throws(Exception::class) suspend fun getChoiceAll(
        action: SPAction,
        campaigns: ChoiceAllRequest.ChoiceAllCampaigns
    ): ChoiceAllResponse?

    @Throws(Exception::class) suspend fun postChoiceGDPR(
        action: SPAction,
        postPayloadFromGetCall: ChoiceAllResponse.GDPR.PostPayload?
    ): GDPRChoiceResponse

    @Throws(Exception::class) suspend fun postChoiceCCPA(action: SPAction): CCPAChoiceResponse

    @Throws(Exception::class) suspend fun postChoiceUSNat(action: SPAction): USNatChoiceResponse

    @Throws(Exception::class) suspend fun reportGDPRAction(
        action: SPAction,
        getResponse: ChoiceAllResponse?
    ): GDPRChoiceResponse

    @Throws(Exception::class) suspend fun reportCCPAAction(
        action: SPAction,
        getResponse: ChoiceAllResponse?
    ): CCPAChoiceResponse

    @Throws(Exception::class) suspend fun reportUSNatAction(
        action: SPAction,
        getResponse: ChoiceAllResponse?
    ): USNatChoiceResponse

    @Throws(Exception::class) suspend fun reportAction(
        action: SPAction,
        campaigns: ChoiceAllRequest.ChoiceAllCampaigns
    ): State
}
