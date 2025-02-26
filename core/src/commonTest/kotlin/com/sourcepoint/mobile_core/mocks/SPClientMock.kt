package com.sourcepoint.mobile_core.mocks

import com.sourcepoint.mobile_core.models.SPActionType
import com.sourcepoint.mobile_core.models.SPCampaignType
import com.sourcepoint.mobile_core.models.SPError
import com.sourcepoint.mobile_core.models.SPIDFAStatus
import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import com.sourcepoint.mobile_core.models.consents.GDPRConsent
import com.sourcepoint.mobile_core.models.consents.USNatConsent
import com.sourcepoint.mobile_core.network.SPClient
import com.sourcepoint.mobile_core.network.requests.CCPAChoiceRequest
import com.sourcepoint.mobile_core.network.requests.ChoiceAllRequest
import com.sourcepoint.mobile_core.network.requests.ConsentStatusRequest
import com.sourcepoint.mobile_core.network.requests.GDPRChoiceRequest
import com.sourcepoint.mobile_core.network.requests.MessagesRequest
import com.sourcepoint.mobile_core.network.requests.MetaDataRequest
import com.sourcepoint.mobile_core.network.requests.PvDataRequest
import com.sourcepoint.mobile_core.network.requests.USNatChoiceRequest
import com.sourcepoint.mobile_core.network.responses.CCPAChoiceResponse
import com.sourcepoint.mobile_core.network.responses.ChoiceAllResponse
import com.sourcepoint.mobile_core.network.responses.ConsentStatusResponse
import com.sourcepoint.mobile_core.network.responses.GDPRChoiceResponse
import com.sourcepoint.mobile_core.network.responses.MessagesResponse
import com.sourcepoint.mobile_core.network.responses.MetaDataResponse
import com.sourcepoint.mobile_core.network.responses.PvDataResponse
import com.sourcepoint.mobile_core.network.responses.USNatChoiceResponse

class SPClientMock(
    val getMetaData: (metaDataResponse: MetaDataResponse) -> MetaDataResponse = { it },
    val postPvData: () -> PvDataResponse = { PvDataResponse() },
    val getConsentStatus: () -> ConsentStatusResponse = {
        ConsentStatusResponse(
            consentStatusData = ConsentStatusResponse.ConsentStatusData(),
            localState = ""
        )
    },
    val postChoiceGDPRAction: () -> GDPRChoiceResponse = { GDPRChoiceResponse(uuid = "") },
    val postChoiceCCPAAction: () -> CCPAChoiceResponse = { CCPAChoiceResponse(uuid = "") },
    val postChoiceUSNATAction: () -> USNatChoiceResponse = { USNatChoiceResponse(
        consentStatus = ConsentStatus(),
        consentStrings = emptyList(),
        userConsents = USNatConsent.USNatUserConsents()
    ) },
    val getChoiceAll: () -> ChoiceAllResponse = { ChoiceAllResponse() },
    val getMessages: () -> MessagesResponse = { MessagesResponse(
        campaigns = emptyList(),
        localState = "",
        nonKeyedLocalState = ""
    ) },
    val customConsentGDPR: () -> GDPRConsent = { GDPRConsent() },
    val deleteCustomConsentGDPR: () -> GDPRConsent = { GDPRConsent() },

): SPClient {
    var consentStatusCalled = false
    var error: SPError? = null
    var consentStatusCalledWith: ConsentStatusRequest.MetaData? = null
    var metaDataResponse: MetaDataResponse = MetaDataResponse()

    override suspend fun getMetaData(campaigns: MetaDataRequest.Campaigns) = getMetaData(metaDataResponse)
    override suspend fun postPvData(request: PvDataRequest) = postPvData()
    override suspend fun getConsentStatus(authId: String?, metadata: ConsentStatusRequest.MetaData): ConsentStatusResponse {
        consentStatusCalled = true
        if ( error!=null ) {
            throw error as SPError
        } else {
            consentStatusCalledWith = metadata
            return getConsentStatus()
        }
    }
    override suspend fun postChoiceGDPRAction(actionType: SPActionType, request: GDPRChoiceRequest) =
        postChoiceGDPRAction()
    override suspend fun postChoiceCCPAAction(actionType: SPActionType, request: CCPAChoiceRequest) =
        postChoiceCCPAAction()
    override suspend fun postChoiceUSNatAction(actionType: SPActionType, request: USNatChoiceRequest) =
        postChoiceUSNATAction()
    override suspend fun getChoiceAll(actionType: SPActionType, campaigns: ChoiceAllRequest.ChoiceAllCampaigns) =
        getChoiceAll()
    override suspend fun getMessages(request: MessagesRequest) = getMessages()
    override suspend fun postReportIdfaStatus(
        propertyId: Int?,
        uuid: String?,
        requestUUID: String,
        uuidType: SPCampaignType?,
        messageId: Int?,
        idfaStatus: SPIDFAStatus,
        iosVersion: String,
        partitionUUID: String?
    ) {}

    override suspend fun customConsentGDPR(
        consentUUID: String,
        propertyId: Int,
        vendors: List<String>,
        categories: List<String>,
        legIntCategories: List<String>
    ) = customConsentGDPR()

    override suspend fun deleteCustomConsentGDPR(
        consentUUID: String,
        propertyId: Int,
        vendors: List<String>,
        categories: List<String>,
        legIntCategories: List<String>
    ) =  deleteCustomConsentGDPR()

    override suspend fun errorMetrics(error: SPError) {}
}
