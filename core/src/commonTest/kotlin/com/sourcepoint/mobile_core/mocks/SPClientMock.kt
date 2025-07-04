package com.sourcepoint.mobile_core.mocks

import com.sourcepoint.mobile_core.models.SPActionType
import com.sourcepoint.mobile_core.models.SPCampaignType
import com.sourcepoint.mobile_core.models.SPError
import com.sourcepoint.mobile_core.models.SPIDFAStatus
import com.sourcepoint.mobile_core.models.consents.GDPRConsent
import com.sourcepoint.mobile_core.network.SPClient
import com.sourcepoint.mobile_core.network.requests.CCPAChoiceRequest
import com.sourcepoint.mobile_core.network.requests.ChoiceAllRequest
import com.sourcepoint.mobile_core.network.requests.ConsentStatusRequest
import com.sourcepoint.mobile_core.network.requests.GDPRChoiceRequest
import com.sourcepoint.mobile_core.network.requests.GlobalCmpChoiceRequest
import com.sourcepoint.mobile_core.network.requests.MessagesRequest
import com.sourcepoint.mobile_core.network.requests.MetaDataRequest
import com.sourcepoint.mobile_core.network.requests.PreferencesChoiceRequest
import com.sourcepoint.mobile_core.network.requests.PvDataRequest
import com.sourcepoint.mobile_core.network.requests.USNatChoiceRequest
import com.sourcepoint.mobile_core.network.responses.CCPAChoiceResponse
import com.sourcepoint.mobile_core.network.responses.ChoiceAllResponse
import com.sourcepoint.mobile_core.network.responses.ConsentStatusResponse
import com.sourcepoint.mobile_core.network.responses.GDPRChoiceResponse
import com.sourcepoint.mobile_core.network.responses.GlobalCmpChoiceResponse
import com.sourcepoint.mobile_core.network.responses.MessagesResponse
import com.sourcepoint.mobile_core.network.responses.MetaDataResponse
import com.sourcepoint.mobile_core.network.responses.PreferencesChoiceResponse
import com.sourcepoint.mobile_core.network.responses.PvDataResponse
import com.sourcepoint.mobile_core.network.responses.USNatChoiceResponse

@Suppress("MemberVisibilityCanBePrivate")
class SPClientMock(
    var original: SPClient? = null,
    var getMetaData: (suspend () -> MetaDataResponse?)? = null,
    var postPvData: (() -> PvDataResponse?)? = null,
    var getConsentStatus: ((authId: String?, metadata: ConsentStatusRequest.MetaData) -> ConsentStatusResponse?)? = null,
    var postChoiceGDPRAction: (() -> GDPRChoiceResponse?)? = null,
    var postChoiceCCPAAction: (() -> CCPAChoiceResponse?)? = null,
    var postChoiceUSNATAction: (() -> USNatChoiceResponse?)? = null,
    var postChoiceGLOBALCMPAction: (() -> GlobalCmpChoiceResponse?)? = null,
    var getChoiceAll: (() -> ChoiceAllResponse?)? = null,
    var getMessages: (() -> MessagesResponse?)? = null,
    var customConsentGDPR: (() -> GDPRConsent?)? = null,
    var deleteCustomConsentGDPR: (() -> GDPRConsent?)? = null,
) : SPClient {
    override suspend fun getMetaData(campaigns: MetaDataRequest.Campaigns) =
        getMetaData?.invoke() ?:
            original?.getMetaData(campaigns) ?:
            MetaDataResponse()

    override suspend fun postPvData(request: PvDataRequest) =
        postPvData?.invoke() ?:
            original?.postPvData(request) ?:
            PvDataResponse()

    override suspend fun getConsentStatus(authId: String?, metadata: ConsentStatusRequest.MetaData) =
        getConsentStatus?.invoke(authId, metadata) ?:
            original?.getConsentStatus(authId, metadata) ?:
            ConsentStatusResponse(consentStatusData = ConsentStatusResponse.ConsentStatusData(), localState = "")

    override suspend fun postChoiceGDPRAction(actionType: SPActionType, request: GDPRChoiceRequest) =
        postChoiceGDPRAction?.invoke() ?:
            original?.postChoiceGDPRAction(actionType, request) ?:
            GDPRChoiceResponse(uuid = "")

    override suspend fun postChoiceCCPAAction(actionType: SPActionType, request: CCPAChoiceRequest) =
        postChoiceCCPAAction?.invoke() ?:
            original?.postChoiceCCPAAction(actionType, request) ?:
            CCPAChoiceResponse(uuid = "")

    override suspend fun postChoiceUSNatAction(actionType: SPActionType, request: USNatChoiceRequest) =
        postChoiceUSNATAction?.invoke() ?:
            original?.postChoiceUSNatAction(actionType, request) ?:
            USNatChoiceResponse(uuid = "")

    override suspend fun postChoiceGlobalCmpAction(
        actionType: SPActionType,
        request: GlobalCmpChoiceRequest
    ): GlobalCmpChoiceResponse =
        postChoiceGLOBALCMPAction?.invoke() ?:
        original?.postChoiceGlobalCmpAction(actionType, request) ?:
        GlobalCmpChoiceResponse(uuid = "")

    override suspend fun postChoicePreferencesAction(actionType: SPActionType, request: PreferencesChoiceRequest) =
        original?.postChoicePreferencesAction(actionType, request) ?:
        PreferencesChoiceResponse(uuid = "")

    override suspend fun getChoiceAll(actionType: SPActionType, campaigns: ChoiceAllRequest.ChoiceAllCampaigns) =
        getChoiceAll?.invoke() ?:
            original?.getChoiceAll(actionType, campaigns) ?:
            ChoiceAllResponse()

    override suspend fun getMessages(request: MessagesRequest) =
        getMessages?.invoke() ?:
            original?.getMessages(request) ?:
            MessagesResponse(campaigns = emptyList(), localState = "", nonKeyedLocalState = "")

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
        original?.postReportIdfaStatus(propertyId, uuid, requestUUID, uuidType, messageId, idfaStatus, iosVersion, partitionUUID)
    }

    override suspend fun customConsentGDPR(
        consentUUID: String,
        propertyId: Int,
        vendors: List<String>,
        categories: List<String>,
        legIntCategories: List<String>
    ) =
        customConsentGDPR?.invoke() ?:
            original?.customConsentGDPR(consentUUID, propertyId, vendors, categories, legIntCategories) ?:
            GDPRConsent()

    override suspend fun deleteCustomConsentGDPR(
        consentUUID: String,
        propertyId: Int,
        vendors: List<String>,
        categories: List<String>,
        legIntCategories: List<String>
    ) =
        deleteCustomConsentGDPR?.invoke() ?:
            original?.deleteCustomConsentGDPR(consentUUID, propertyId, vendors, categories, legIntCategories) ?:
            GDPRConsent()

    override suspend fun errorMetrics(error: SPError) {
        original?.errorMetrics(error)
    }
}
