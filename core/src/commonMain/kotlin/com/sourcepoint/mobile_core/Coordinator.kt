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
import com.sourcepoint.mobile_core.network.requests.ConsentStatusRequest
import com.sourcepoint.mobile_core.network.requests.GDPRChoiceRequest
import com.sourcepoint.mobile_core.network.requests.IncludeData
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

    var migratingUser: Boolean = false //This needs storage so must be init from native
    var needsNewUSNatData = false

    private val needsNewConsentData: Boolean get() =
                migratingUser || needsNewUSNatData || transitionCCPAOptedOut ||
                (state.localVersion != State.version &&
                        (state.gdpr?.uuid != null || state.ccpa?.uuid != null || state.usNat?.uuid != null))

    private val authTransitionCCPAUSNat: Boolean get() = (authId != null && campaigns.usnat?.transitionCCPAAuth == true)

    private val transitionCCPAOptedOut: Boolean get() =
                campaigns.usnat != null &&
                state.ccpa?.uuid != null &&
                state.usNat?.uuid == null &&
                (state.ccpa?.status == CCPAConsent.CCPAConsentStatus.RejectedAll || state.ccpa?.status == CCPAConsent.CCPAConsentStatus.RejectedSome)

    val shouldCallConsentStatus: Boolean get() = (needsNewConsentData || authId != null)

    private val shouldCallMessages: Boolean get() =
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

    //region loadMessages
    private fun resetStateIfAuthIdChanged() {
        if (state.storedAuthId == null) {
            state.storedAuthId = authId
        }

        if (authId != null && state.storedAuthId != authId) {
            state.storedAuthId = authId
            if (campaigns.gdpr != null) {
                state.gdpr = GDPRConsent()
                state.gdprMetaData = State.GDPRMetaData()
            }
            if (campaigns.ccpa != null) {
                state.ccpa = CCPAConsent()
                state.ccpaMetaData = State.CCPAMetaData()
            }
        }
    }

    suspend fun loadMessages(authId: String?, pubData: JsonObject): List<MessageToDisplay> {
        //TODO load state from storage
        //TODO save state to storage

        this.authId = authId
        resetStateIfAuthIdChanged()
        var messages: List<MessageToDisplay> = emptyList()
        metaData() {
            runBlocking {
                consentStatus() {
                    state.updateGDPRStatus()
                    state.updateUSNatStatus()
                    messages = try {
                        runBlocking { messages() }
                    } catch (error: Throwable) {
                        emptyList<MessageToDisplay>()
                        throw error
                    }
                    runBlocking { pvData(pubData, messages) }
                }
            }
        }
        return messages
    }
    //endregion

    //region metaData
    private fun metaDataParamsFromState(): MetaDataRequest.Campaigns =
        MetaDataRequest.Campaigns(
            gdpr = if (campaigns.gdpr != null) MetaDataRequest.Campaigns.Campaign(campaigns.gdpr!!.groupPmId) else null,
            ccpa = if (campaigns.ccpa != null) MetaDataRequest.Campaigns.Campaign(campaigns.ccpa!!.groupPmId) else null,
            usnat = if (campaigns.usnat != null) MetaDataRequest.Campaigns.Campaign(campaigns.usnat!!.groupPmId) else null
        )

    private fun handleMetaDataResponse(response: MetaDataResponse) {
        if (response.gdpr != null) {
            val gdprMetaData = response.gdpr
            if (state.gdprMetaData?.vendorListId != null && state.gdprMetaData?.vendorListId != gdprMetaData.vendorListId) {
                state.gdpr = GDPRConsent()
            }
            state.gdpr = state.gdpr?.copy(applies = gdprMetaData.applies)
            state.gdprMetaData = state.gdprMetaData?.copy(
                additionsChangeDate = gdprMetaData.additionsChangeDate,
                legalBasisChangeDate = gdprMetaData.legalBasisChangeDate,
                vendorListId = gdprMetaData.vendorListId
            )
            state.gdprMetaData?.updateSampleFields(gdprMetaData.sampleRate)
            if (campaigns.gdpr?.groupPmId != gdprMetaData.childPmId) {
                //storage.gdprChildPmId = gdprMetaData.childPmId    //TODO storage needs
            }
        }
        if (response.ccpa != null) {
            val ccpaMetaData = response.ccpa
            state.ccpa = state.ccpa?.copy(applies = ccpaMetaData.applies)
            state.ccpaMetaData?.updateSampleFields(ccpaMetaData.sampleRate)
        }
        if (response.usnat != null) {
            val usnatMetaData = response.usnat
            val previousApplicableSections = state.usNatMetaData?.applicableSections ?: emptyList()
            if (state.usNatMetaData?.vendorListId != null && state.usNatMetaData?.vendorListId != usnatMetaData.vendorListId) {
                state.usNat = USNatConsent()
            }
            state.usNat = state.usNat?.copy(applies = usnatMetaData.applies)
            state.usNatMetaData = state.usNatMetaData?.copy(
                vendorListId = usnatMetaData.vendorListId,
                additionsChangeDate = usnatMetaData.additionsChangeDate,
                applicableSections = usnatMetaData.applicableSections
            )
            state.usNatMetaData?.updateSampleFields(usnatMetaData.sampleRate)
            if (previousApplicableSections.isNotEmpty() && previousApplicableSections != state.usNatMetaData?.applicableSections) {
                needsNewUSNatData = true
            }
        }
        //TODO save state to storage
    }

    suspend fun metaData(next: () -> Unit) {
        try {
            val response = spClient.getMetaData(metaDataParamsFromState())
            handleMetaDataResponse(response)
            next()
        } catch (error: Throwable) {
            throw error
        }
    }
    //endregion

    //region consentStatus
    private fun consentStatusParamsFromState(): ConsentStatusRequest.MetaData =
        ConsentStatusRequest.MetaData(
            gdpr = if (state.gdpr != null)
                ConsentStatusRequest.MetaData.Campaign(
                    applies = state.gdpr!!.applies,
                    dateCreated = state.gdpr!!.dateCreated,
                    uuid = state.gdpr!!.uuid,
                    hasLocalData = state.hasGDPRLocalData,
                    idfaStatus = idfaStatus
                ) else null,
            usnat = if (state.usNat != null)
                ConsentStatusRequest.MetaData.USNatCampaign(
                    applies = state.usNat!!.applies,
                    dateCreated = state.usNat!!.dateCreated,
                    uuid = state.usNat!!.uuid,
                    hasLocalData = state.hasUSNatLocalData,
                    idfaStatus = idfaStatus,
                    transitionCCPAAuth = authTransitionCCPAUSNat,
                    optedOut = transitionCCPAOptedOut
                ) else null,
            ccpa = if (state.ccpa != null)
                ConsentStatusRequest.MetaData.Campaign(
                    applies = state.ccpa!!.applies,
                    dateCreated = state.ccpa!!.dateCreated,
                    uuid = state.ccpa!!.uuid,
                    hasLocalData = state.hasCCPALocalData,
                    idfaStatus = idfaStatus
                ) else null
        )

    private fun handleConsentStatusResponse(response: ConsentStatusResponse) {
        state.localState = response.localState
        if (response.consentStatusData.gdpr != null) {
            val respGDPR = response.consentStatusData.gdpr
            state.gdpr = state.gdpr?.copy(
                uuid = respGDPR.uuid,
                grants = respGDPR.grants,
                dateCreated = respGDPR.dateCreated,
                expirationDate = respGDPR.expirationDate,
                euconsent = respGDPR.euconsent,
                tcData = respGDPR.tcData,
                consentStatus = respGDPR.consentStatus,
                webConsentPayload = respGDPR.webConsentPayload,
                gcmStatus = respGDPR.gcmStatus,
                legIntCategories = respGDPR.legIntCategories,
                legIntVendors = respGDPR.legIntVendors,
                categories = respGDPR.categories,
                vendors = respGDPR.vendors,
                specialFeatures = respGDPR.specialFeatures
            )
        }
        if (response.consentStatusData.ccpa != null) {
            val respCCPA = response.consentStatusData.ccpa
            state.ccpa = state.ccpa?.copy(
                uuid = respCCPA.uuid,
                dateCreated = respCCPA.dateCreated,
                expirationDate = respCCPA.expirationDate,
                status = respCCPA.status,
                rejectedVendors = respCCPA.rejectedVendors,
                rejectedCategories = respCCPA.rejectedCategories,
                consentStatus = respCCPA.consentStatus,
                webConsentPayload = respCCPA.webConsentPayload,
                gppData = respCCPA.gppData
            )
        }
        if (response.consentStatusData.usnat != null) {
            val respUSNat = response.consentStatusData.usnat
            state.usNat = state.usNat?.copy(
                uuid = respUSNat.uuid,
                dateCreated = respUSNat.dateCreated,
                expirationDate = respUSNat.expirationDate,
                consentStrings = respUSNat.consentStrings,
                webConsentPayload = respUSNat.webConsentPayload,
                userConsents = respUSNat.userConsents,
                consentStatus = respUSNat.consentStatus,
                gppData = respUSNat.gppData
            )
        }
        //TODO save state to storage
    }

    suspend fun consentStatus(next: () -> Unit) {
        if (shouldCallConsentStatus) {
            try {
                val response = spClient.getConsentStatus(authId = authId, metadata = consentStatusParamsFromState())
                state.localVersion = State.version
                handleConsentStatusResponse(response)
            } catch (error: Throwable) {
                throw error
            }
        } else {
            state.localVersion = State.version
        }
        next()
    }
    //endregion

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
                SPCampaignType.Gdpr -> state.gdpr = it.toConsent(default = state.gdpr) as GDPRConsent
                SPCampaignType.Ccpa -> state.ccpa = it.toConsent(default = state.ccpa) as CCPAConsent
                SPCampaignType.UsNat -> state.usNat = it.toConsent(default = state.usNat) as USNatConsent
                SPCampaignType.IOS14 -> {
                    state.ios14 = state.ios14?.copy(
                        messageId = it.messageMetaData?.messageId,
                        partitionUUID = it.messageMetaData?.messagePartitionUUID
                    )
                }
                SPCampaignType.unknown -> return@forEach
            }
        }
        //TODO save state to storage
        return response.campaigns.mapNotNull { MessageToDisplay.initFromCampaign(it) }
    }

    suspend fun messages(): List<MessageToDisplay> =
        if (shouldCallMessages) {
            try {
                val response = spClient.getMessages(request = messagesParamsFromState())
                handleMessagesResponse(response)
            } catch (error: Throwable) {
                throw error
            }
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
        //TODO save state to storage
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
        //TODO save state to storage
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
        //TODO save state to storage
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
        //TODO save state to storage
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
        //TODO save state to storage
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

