package com.sourcepoint.mobile_core.network

import com.sourcepoint.mobile_core.models.SPActionType
import com.sourcepoint.mobile_core.models.SPCampaignType
import com.sourcepoint.mobile_core.models.SPError
import com.sourcepoint.mobile_core.models.SPIDFAStatus
import com.sourcepoint.mobile_core.models.consents.GDPRConsent
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

class SourcepointClientMock():SPClient {
    var consentStatusCalled = false
    var error: SPError? = null
    var consentStatusCalledWith: ConsentStatusRequest.MetaData? = null
    var metaDataResponse = MetaDataResponse(gdpr = null, usnat = null, ccpa = null)
    override suspend fun getMetaData(campaigns: MetaDataRequest.Campaigns): MetaDataResponse {
        return metaDataResponse
    }

    override suspend fun postPvData(request: PvDataRequest): PvDataResponse {
        return PvDataResponse(gdpr = null, ccpa = null, usnat = null)
    }

    override suspend fun getConsentStatus(
        authId: String?,
        metadata: ConsentStatusRequest.MetaData
    ): ConsentStatusResponse {
        consentStatusCalled = true
        if ( error!=null ) {
            throw error as SPError
        } else {
            consentStatusCalledWith = metadata
            return ConsentStatusResponse(consentStatusData = ConsentStatusResponse.ConsentStatusData(gdpr = null, usnat = null, ccpa = null), localState = "")
        }
    }

    override suspend fun postChoiceGDPRAction(
        actionType: SPActionType,
        request: GDPRChoiceRequest
    ): GDPRChoiceResponse {
        TODO("Not yet implemented")
    }

    override suspend fun postChoiceCCPAAction(
        actionType: SPActionType,
        request: CCPAChoiceRequest
    ): CCPAChoiceResponse {
        TODO("Not yet implemented")
    }

    override suspend fun postChoiceUSNatAction(
        actionType: SPActionType,
        request: USNatChoiceRequest
    ): USNatChoiceResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getChoiceAll(
        actionType: SPActionType,
        campaigns: ChoiceAllRequest.ChoiceAllCampaigns
    ): ChoiceAllResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getMessages(request: MessagesRequest): MessagesResponse {
        return MessagesResponse(campaigns = emptyList(), localState = "", nonKeyedLocalState = "")
    }

    override suspend fun postReportIdfaStatus(
        propertyId: Int?,
        uuid: String?,
        requestUUID: String,
        uuidType: SPCampaignType?,
        messageId: Int?,
        idfaStatus: SPIDFAStatus,
        iosVersion: String,
        partitionUUID: String?
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun customConsentGDPR(
        consentUUID: String,
        propertyId: Int,
        vendors: List<String>,
        categories: List<String>,
        legIntCategories: List<String>
    ): GDPRConsent {
        TODO("Not yet implemented")
    }

    override suspend fun deleteCustomConsentGDPR(
        consentUUID: String,
        propertyId: Int,
        vendors: List<String>,
        categories: List<String>,
        legIntCategories: List<String>
    ): GDPRConsent {
        TODO("Not yet implemented")
    }

    override suspend fun errorMetrics(error: SPError) {
        TODO("Not yet implemented")
    }
}
