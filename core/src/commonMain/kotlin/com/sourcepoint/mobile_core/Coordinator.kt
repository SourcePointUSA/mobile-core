package com.sourcepoint.mobile_core

import com.sourcepoint.mobile_core.models.SPAction
import com.sourcepoint.mobile_core.models.SPActionType
import com.sourcepoint.mobile_core.models.SPCampaignType
import com.sourcepoint.mobile_core.models.SPIDFAStatus
import com.sourcepoint.mobile_core.models.consents.CCPAConsent
import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import com.sourcepoint.mobile_core.models.consents.GDPRConsent
import com.sourcepoint.mobile_core.models.MessageToDisplay
import com.sourcepoint.mobile_core.models.SPCampaigns
import com.sourcepoint.mobile_core.models.SPMessageLanguage
import com.sourcepoint.mobile_core.models.consents.State
import com.sourcepoint.mobile_core.models.consents.USNatConsent
import com.sourcepoint.mobile_core.network.SourcepointClient
import com.sourcepoint.mobile_core.network.requests.CCPAChoiceRequest
import com.sourcepoint.mobile_core.network.requests.ChoiceAllRequest
import com.sourcepoint.mobile_core.network.requests.GDPRChoiceRequest
import com.sourcepoint.mobile_core.network.requests.IncludeData
import com.sourcepoint.mobile_core.network.requests.MessagesRequest
import com.sourcepoint.mobile_core.network.requests.MetaDataRequest
import com.sourcepoint.mobile_core.network.requests.PvDataRequest
import com.sourcepoint.mobile_core.network.requests.USNatChoiceRequest
import com.sourcepoint.mobile_core.network.responses.CCPAChoiceResponse
import com.sourcepoint.mobile_core.network.responses.ChoiceAllResponse
import com.sourcepoint.mobile_core.network.responses.GDPRChoiceResponse
import com.sourcepoint.mobile_core.network.responses.MessagesResponse
import com.sourcepoint.mobile_core.network.responses.PvDataResponse
import com.sourcepoint.mobile_core.network.responses.USNatChoiceResponse
import com.sourcepoint.mobile_core.storage.Repository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject

class Coordinator(
    private val accountId: Int,
    private val propertyId: Int,
    private val propertyName: String,
    private val repository: Repository,
    private val spClient: SourcepointClient
): ICoordinator {
    lateinit var state: State
    var authId: String? = null
    var idfaStatus: SPIDFAStatus? = SPIDFAStatus.current()
    var includeData: IncludeData = IncludeData()
    var language: SPMessageLanguage = SPMessageLanguage.ENGLISH
    lateinit var campaigns: SPCampaigns

    val shouldCallMessages: Boolean get() =
                (campaigns.gdpr != null && state.gdpr?.consentStatus?.consentedAll != true) ||
                campaigns.ccpa != null ||
                (campaigns.ios14 != null && state.ios14?.status != SPIDFAStatus.Accepted) ||
                campaigns.usnat != null


    constructor(accountId: Int, propertyId: Int, propertyName: String) : this(
        accountId,
        propertyId,
        propertyName,
        repository = Repository(),
        spClient = SourcepointClient(accountId, propertyId, propertyName)
    )
    constructor(accountId: Int, propertyId: Int, propertyName: String, repository: Repository) : this(
        accountId,
        propertyId,
        propertyName,
        repository,
        spClient = SourcepointClient(accountId, propertyId, propertyName)
    )

    suspend fun getMetaData(campaigns: MetaDataRequest.Campaigns): String {
        val metaDataResponse = spClient.getMetaData(campaigns)
        val message = """
            The return of /meta-data is:
            $metaDataResponse
            
            The cached version of /meta-data is:
            ${repository.cachedMetaData}
        """
        repository.cachedMetaData = metaDataResponse.toString()
        return message
    }

    //region messages
    private fun messagesParamsFromState(): MessagesRequest =
        MessagesRequest(
            body = MessagesRequest.Body(
                propertyHref = propertyName,
                accountId = accountId,
                campaigns = MessagesRequest.Body.Campaigns(
                    gdpr = if (campaigns.gdpr != null)
                        MessagesRequest.Body.Campaigns.GDPR(
                            targetingParams = campaigns.gdpr!!.targetingParams,
                            hasLocalData = state.hasGDPRLocalData,
                            consentStatus = state.gdpr?.consentStatus
                        ) else null,
                    ios14 = if (campaigns.ios14 != null)
                        MessagesRequest.Body.Campaigns.IOS14(
                            targetingParams = campaigns.ios14!!.targetingParams,
                            idfaStatus = idfaStatus
                        ) else null,
                    ccpa = if (campaigns.ccpa != null)
                        MessagesRequest.Body.Campaigns.CCPA(
                            targetingParams = campaigns.ccpa!!.targetingParams,
                            hasLocalData = state.hasCCPALocalData,
                            status = state.ccpa?.status
                        ) else null,
                    usnat = if (campaigns.usnat != null)
                        MessagesRequest.Body.Campaigns.USNat(
                            targetingParams = campaigns.usnat!!.targetingParams,
                            hasLocalData = state.hasUSNatLocalData,
                            consentStatus = state.usNat?.consentStatus
                        ) else null
                ),
                consentLanguage = language,
                campaignEnv = campaigns.environment,
                idfaStatus = idfaStatus,
                includeData = includeData
            ),
            metadata = MessagesRequest.MetaData(
                gdpr = MessagesRequest.MetaData.Campaign(state.gdpr?.applies ?: false),
                usnat = MessagesRequest.MetaData.Campaign(state.ccpa?.applies ?: false),
                ccpa = MessagesRequest.MetaData.Campaign(state.usNat?.applies ?: false)
            ),
            nonKeyedLocalState = state.nonKeyedLocalState,
            localState = state.localState
        )
    
    private fun handleMessagesResponse(response: MessagesResponse): List<MessageToDisplay> {
        state.localState = response.localState
        state.nonKeyedLocalState = response.nonKeyedLocalState

        response.campaigns.forEach {
            when (it.type) {
                SPCampaignType.Gdpr -> state.gdpr = it.toConsent(state.gdpr) as GDPRConsent
                SPCampaignType.Ccpa -> state.ccpa = it.toConsent(state.ccpa) as CCPAConsent
                SPCampaignType.UsNat -> state.usNat = it.toConsent(state.usNat) as USNatConsent
                SPCampaignType.IOS14 -> {
                    state.ios14 = state.ios14?.copy(
                        messageId = it.messageMetaData?.messageId,
                        partitionUUID = it.messageMetaData?.messagePartitionUUID
                    )
                }
                SPCampaignType.unknown -> return@forEach
            }
        }
        return response.campaigns.mapNotNull { MessageToDisplay.initFromCampaign(it) }
    }

    suspend fun messages(): List<MessageToDisplay> =
        if (shouldCallMessages) {
            val response = spClient.getMessages(request = messagesParamsFromState())
            handleMessagesResponse(response)
        } else {
            emptyList()
        }
    //endregion

    //region pvData
    private fun sample(samplingRate: Float): Boolean =
        IntRange(1, 100).random() in (1..(samplingRate * 100).toInt())

    private fun handlePvDataResponse(response: PvDataResponse) {
        if (response.gdpr != null) {
            state.gdpr = (state.gdpr ?: GDPRConsent()).copy(uuid = response.gdpr.uuid)
        }
        if (response.ccpa != null) {
            state.ccpa = (state.ccpa ?: CCPAConsent()).copy(uuid = response.ccpa.uuid)
        }
        if (response.usnat != null) {
            state.usNat = (state.usNat ?: USNatConsent()).copy(uuid = response.usnat.uuid)
        }
    }

    private suspend fun sampleAndPvData(campaign: State.SPSampleable, request: PvDataRequest): Boolean {
        if (campaign.wasSampled == null) {
            if (sample(samplingRate = campaign.sampleRate)) {
                val response = spClient.postPvData(request = request)
                handlePvDataResponse(response)
                return true
            } else {
                return false
            }
        } else {
            if (campaign.wasSampled == true) {
                val response = spClient.postPvData(request = request)
                handlePvDataResponse(response)
                return true
            }
            else {
                return false
            }
        }
    }

    suspend fun pvData(pubData: JsonObject, messages: List<MessageToDisplay>) {
        coroutineScope {
            if (campaigns.gdpr != null && state.gdprMetaData != null) {
                launch {
                    val sampled = sampleAndPvData(
                        state.gdprMetaData!!,
                        gdprPvDataBody(
                            consent = state.gdpr,
                            pubData = pubData,
                            messageMetaData = messages.first { it.type == SPCampaignType.Gdpr }.metaData
                        ))
                    state.gdprMetaData = state.gdprMetaData!!.copy(wasSampled = sampled)
                }
            }
            if (campaigns.ccpa != null && state.ccpaMetaData != null) {
                launch {
                    val sampled = sampleAndPvData(
                        state.ccpaMetaData!!,
                        ccpaPvDataBody(
                            consent = state.ccpa,
                            pubData = pubData,
                            messageMetaData = messages.first { it.type == SPCampaignType.Ccpa }.metaData
                        ))
                    state.ccpaMetaData = state.ccpaMetaData!!.copy(wasSampled = sampled)
                }
            }
            if (campaigns.usnat != null && state.usNatMetaData != null) {
                launch {
                    val sampled = sampleAndPvData(
                        state.usNatMetaData!!,
                        usnatPvDataBody(
                            consent = state.usNat,
                            pubData = pubData,
                            messageMetaData = messages.first { it.type == SPCampaignType.UsNat }.metaData
                        ))
                    state.usNatMetaData = state.usNatMetaData!!.copy(wasSampled = sampled)
                }
            }
        }
    }

    private fun gdprPvDataBody(consent: GDPRConsent?, pubData: JsonObject, messageMetaData: MessagesResponse.MessageMetaData): PvDataRequest =
        if (consent != null) {
            PvDataRequest(
                ccpa = null,
                usnat = null,
                gdpr = PvDataRequest.GDPR(
                    applies = consent.applies,
                    uuid = consent.uuid,
                    accountId = accountId,
                    propertyId = propertyId,
                    consentStatus = consent.consentStatus,
                    pubData = pubData,
                    sampleRate = state.gdprMetaData?.sampleRate,
                    euconsent = consent.euconsent,
                    msgId = messageMetaData.messageId,
                    categoryId = messageMetaData.categoryId.rawValue,
                    subCategoryId = messageMetaData.subCategoryId.rawValue,
                    prtnUUID = messageMetaData.messagePartitionUUID
                )
            )
        } else {
            PvDataRequest(gdpr = null, ccpa = null, usnat = null)
        }

    private fun ccpaPvDataBody(consent: CCPAConsent?, pubData: JsonObject, messageMetaData: MessagesResponse.MessageMetaData): PvDataRequest =
        if (consent != null) {
            PvDataRequest(
                gdpr = null,
                usnat = null,
                ccpa = PvDataRequest.CCPA(
                    applies = consent.applies,
                    uuid = consent.uuid,
                    accountId = accountId,
                    propertyId = propertyId,
                    consentStatus = consent.consentStatus,
                    pubData = pubData,
                    messageId = messageMetaData.messageId,
                    sampleRate = state.ccpaMetaData?.sampleRate
                )
            )
        } else {
            PvDataRequest(gdpr = null, ccpa = null, usnat = null)
        }

    private fun usnatPvDataBody(consent: USNatConsent?, pubData: JsonObject, messageMetaData: MessagesResponse.MessageMetaData): PvDataRequest =
        if (consent != null) {
            PvDataRequest(
                gdpr = null,
                ccpa = null,
                usnat = PvDataRequest.USNat(
                    applies = consent.applies,
                    uuid = consent.uuid,
                    accountId = accountId,
                    propertyId = propertyId,
                    consentStatus = consent.consentStatus,
                    pubData = pubData,
                    sampleRate = state.usNatMetaData?.sampleRate,
                    msgId = messageMetaData.messageId,
                    categoryId = messageMetaData.categoryId.rawValue,
                    subCategoryId = messageMetaData.subCategoryId.rawValue,
                    prtnUUID = messageMetaData.messagePartitionUUID
                )
            )
        } else {
            PvDataRequest(gdpr = null, ccpa = null, usnat = null)
        }
    //endregion

    //region reportAction
    private fun handleGetChoiceAll(response: ChoiceAllResponse) {
        if (response.gdpr != null) {
            state.gdpr = (state.gdpr ?: GDPRConsent()).copy(
                dateCreated = response.gdpr.dateCreated,
                expirationDate = response.gdpr.expirationDate,
                tcData = response.gdpr.tcData ?: emptyMap(),
                grants = response.gdpr.grants,
                euconsent = response.gdpr.euconsent,
                consentStatus = response.gdpr.consentStatus,
                childPmId = response.gdpr.childPmId,
                gcmStatus = response.gdpr.gcmStatus
            )
        }
        if (response.ccpa != null) {
            state.ccpa = (state.ccpa ?: CCPAConsent()).copy(
                dateCreated = response.ccpa.dateCreated,
                expirationDate = response.ccpa.expirationDate,
                status = response.ccpa.status,
                gppData = response.ccpa.gppData,
                uspstring = response.ccpa.uspstring,
            )
        }
        if (response.usnat != null) {
            state.usNat = (state.usNat ?: USNatConsent()).copy(
                dateCreated = response.usnat.dateCreated,
                expirationDate = response.usnat.expirationDate,
                consentStatus = response.usnat.consentStatus,
                gppData = response.usnat.gppData,
                consentStrings = response.usnat.consentStrings
            )
        }
    }

    private suspend fun getChoiceAll(action: SPAction, campaigns: ChoiceAllRequest.ChoiceAllCampaigns): ChoiceAllResponse? {
        val shouldCallGetChoice: Boolean = (action.type == SPActionType.AcceptAll || action.type == SPActionType.RejectAll)
        if (!shouldCallGetChoice)
            return null

        try {
            val response = spClient.getChoiceAll(action.type, campaigns)
            handleGetChoiceAll(response)
            return response
        } catch (error: Throwable) {
            throw error
        }
    }

    private suspend fun postChoiceGDPR(
        action: SPAction,
        postPayloadFromGetCall: ChoiceAllResponse.GDPR.PostPayload?
    ): GDPRChoiceResponse =
        try {
            spClient.postChoiceGDPRAction(
                actionType = action.type,
                request = GDPRChoiceRequest(
                    authId = authId,
                    uuid = state.gdpr?.uuid,
                    messageId = action.messageId,
                    consentAllRef = postPayloadFromGetCall?.consentAllRef,
                    vendorListId = postPayloadFromGetCall?.vendorListId,
                    pubData = action.encodablePubData,
                    pmSaveAndExitVariables = action.pmPayload,
                    sendPVData = state.gdprMetaData?.wasSampled ?: false,
                    propertyId = propertyId,
                    sampleRate = state.gdprMetaData?.sampleRate,
                    idfaStatus = idfaStatus,
                    granularStatus = postPayloadFromGetCall?.granularStatus,
                    includeData = includeData
                )
            )
        }
        catch (error: Throwable) {
            throw error
        }

    private suspend fun postChoiceCCPA(action: SPAction): CCPAChoiceResponse =
        try {
            spClient.postChoiceCCPAAction(
                actionType = action.type,
                request = CCPAChoiceRequest(
                    authId = authId,
                    uuid = state.ccpa?.uuid,
                    messageId = action.messageId,
                    pubData = action.encodablePubData,
                    pmSaveAndExitVariables = action.pmPayload,
                    sendPVData = state.ccpaMetaData?.wasSampled ?: false,
                    propertyId = propertyId,
                    sampleRate = state.ccpaMetaData?.sampleRate,
                    includeData = includeData
                )
            )
        }
        catch (error: Throwable) {
            throw error
        }

    private suspend fun postChoiceUSNat(action: SPAction): USNatChoiceResponse =
        try {
            spClient.postChoiceUSNatAction(
                actionType = action.type,
                request = USNatChoiceRequest(
                    authId = authId,
                    uuid = state.usNat?.uuid,
                    messageId = action.messageId,
                    vendorListId = state.usNatMetaData?.vendorListId,
                    pubData = action.encodablePubData,
                    pmSaveAndExitVariables = action.pmPayload,
                    sendPVData = state.usNatMetaData?.wasSampled ?: false,
                    propertyId = propertyId,
                    sampleRate = state.usNatMetaData?.sampleRate,
                    idfaStatus = idfaStatus,
                    granularStatus = state.usNat?.consentStatus?.granularStatus,
                    includeData = includeData
                )
            )
        }
        catch (error: Throwable) {
            throw error
        }

    private fun handleGPDRPostChoice(
        action: SPAction,
        getResponse: ChoiceAllResponse?,
        postResponse: GDPRChoiceResponse
    ) {
        state.gdpr = (state.gdpr ?: GDPRConsent()).copy(
            uuid = postResponse.uuid,
            dateCreated = postResponse.dateCreated,
            expirationDate = postResponse.expirationDate,
            consentStatus = postResponse.consentStatus ?: getResponse?.gdpr?.consentStatus ?: ConsentStatus(),
            euconsent = postResponse.euconsent ?: getResponse?.gdpr?.euconsent,
            grants = postResponse.grants ?: getResponse?.gdpr?.grants ?: emptyMap(),
            webConsentPayload = postResponse.webConsentPayload ?: getResponse?.gdpr?.webConsentPayload,
            gcmStatus = postResponse.gcmStatus ?: getResponse?.gdpr?.gcmStatus,
            legIntCategories = postResponse.acceptedLegIntCategories ?: getResponse?.gdpr?.acceptedLegIntCategories ?: emptyList(),
            legIntVendors = postResponse.acceptedLegIntVendors ?: getResponse?.gdpr?.acceptedLegIntVendors ?: emptyList(),
            vendors = postResponse.acceptedVendors ?: getResponse?.gdpr?.acceptedVendors ?: emptyList(),
            categories = postResponse.acceptedCategories ?: getResponse?.gdpr?.acceptedCategories ?: emptyList(),
            specialFeatures = postResponse.acceptedSpecialFeatures ?: getResponse?.gdpr?.acceptedSpecialFeatures ?: emptyList()
        )
        if (action.type == SPActionType.SaveAndExit) {
            state.gdpr?.tcData = postResponse.tcData ?: emptyMap()
        }
    }

    private suspend fun reportGDPRAction(action: SPAction, getResponse: ChoiceAllResponse?): GDPRChoiceResponse =
        try {
            val response = postChoiceGDPR(
                action = action,
                postPayloadFromGetCall = getResponse?.gdpr?.postPayload
            )
            handleGPDRPostChoice(action, getResponse, response)
            response
        }
        catch (error: Throwable) {
            throw error
        }

    private fun handleCCPAPostChoice(
        action: SPAction,
        getResponse: ChoiceAllResponse?,
        postResponse: CCPAChoiceResponse
    ) {
        state.ccpa = (state.ccpa ?: CCPAConsent()).copy(
            uuid = postResponse.uuid,
            dateCreated = postResponse.dateCreated,
            status = postResponse.status ?: getResponse?.ccpa?.status ?: CCPAConsent.CCPAConsentStatus.RejectedAll,
            rejectedVendors = postResponse.rejectedVendors ?: getResponse?.ccpa?.rejectedVendors?: emptyList(),
            rejectedCategories = postResponse.rejectedCategories ?: getResponse?.ccpa?.rejectedCategories ?: emptyList(),
            webConsentPayload = postResponse.webConsentPayload ?: getResponse?.ccpa?.webConsentPayload,
            uspstring = postResponse.uspstring ?: getResponse?.ccpa?.uspstring
        )
        if (action.type == SPActionType.SaveAndExit) {
            state.ccpa?.gppData = postResponse.gppData
        }
    }

    private suspend fun reportCCPAAction(action: SPAction, getResponse: ChoiceAllResponse?): CCPAChoiceResponse =
        try {
            val response = postChoiceCCPA(
                action = action
            )
            handleCCPAPostChoice(action, getResponse, response)
            response
        }
        catch (error: Throwable) {
            throw error
        }

    private fun handleUSNatPostChoice(
        action: SPAction,
        getResponse: ChoiceAllResponse?,
        postResponse: USNatChoiceResponse
    ) {
        state.usNat = (state.usNat ?: USNatConsent()).copy(
            uuid = postResponse.uuid,
            applies = state.usNat?.applies ?: false,
            dateCreated = postResponse.dateCreated,
            expirationDate = postResponse.expirationDate,
            consentStrings = postResponse.consentStrings,
            webConsentPayload = postResponse.webConsentPayload ?: getResponse?.usnat?.webConsentPayload,
            consentStatus = postResponse.consentStatus,
            gppData = postResponse.gppData,
            userConsents = state.usNat?.userConsents?.copy(
                categories = postResponse.userConsents.categories,
                vendors = postResponse.userConsents.vendors
            ) ?: USNatConsent.USNatUserConsents()
        )
    }

    private suspend fun reportUSNatAction(action: SPAction, getResponse: ChoiceAllResponse?): USNatChoiceResponse =
        try {
            val response = postChoiceUSNat(
                action = action
            )
            handleUSNatPostChoice(action, getResponse, response)
            response
        }
        catch (error: Throwable) {
            throw error
        }

    override suspend fun reportAction(action: SPAction, campaigns: ChoiceAllRequest.ChoiceAllCampaigns): State {
        try {
            val getResponse = getChoiceAll(
                action = action,
                campaigns = campaigns
            )
            when (action.campaignType) {
                SPCampaignType.Gdpr -> reportGDPRAction(
                    action = action,
                    getResponse = getResponse
                )
                SPCampaignType.Ccpa -> reportCCPAAction(
                    action = action,
                    getResponse = getResponse
                )
                SPCampaignType.UsNat -> reportUSNatAction(
                    action = action,
                    getResponse = getResponse
                )
                SPCampaignType.IOS14, SPCampaignType.unknown -> throw IllegalStateException()
            }
        }
        catch (error: Exception) {
            throw error
        }
        return state
    }
    //endregion
}

