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
import com.sourcepoint.mobile_core.models.consents.AttCampaign
import com.sourcepoint.mobile_core.models.consents.SPDate
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
    private val campaigns: SPCampaigns,
    private val repository: Repository,
    private val spClient: SourcepointClient,
    private var state: State
): ICoordinator {
    var authId: String? = null
    var idfaStatus: SPIDFAStatus? = SPIDFAStatus.current()
    var includeData: IncludeData = IncludeData()
    var language: SPMessageLanguage = SPMessageLanguage.ENGLISH

    private var needsNewUSNatData = false

    private val needsNewConsentData: Boolean get() =
                needsNewUSNatData || transitionCCPAOptedOut ||
                (state.localVersion != State.VERSION &&
                        (state.gdpr.consents.uuid != null || state.ccpa.consents.uuid != null || state.usNat.consents.uuid != null))

    private val authTransitionCCPAUSNat: Boolean get() = (authId != null && campaigns.usnat?.transitionCCPAAuth == true)

    private val transitionCCPAOptedOut: Boolean get() =
                campaigns.usnat != null &&
                state.ccpa.consents.uuid != null &&
                state.usNat.consents.uuid == null &&
                (state.ccpa.consents.status == CCPAConsent.CCPAConsentStatus.RejectedAll || state.ccpa.consents.status == CCPAConsent.CCPAConsentStatus.RejectedSome)

    private val shouldCallConsentStatus: Boolean get() = (needsNewConsentData || authId != null)

    private val shouldCallMessages: Boolean get() =
                (campaigns.gdpr != null && state.gdpr.consents.consentStatus.consentedAll != true) ||
                campaigns.ccpa != null ||
                (campaigns.ios14 != null && state.ios14.status != SPIDFAStatus.Accepted) ||
                campaigns.usnat != null

    constructor(
        accountId: Int,
        propertyId: Int,
        propertyName: String,
        campaigns: SPCampaigns,
        repository: Repository,
        initialState: State?
    ) : this(
        accountId,
        propertyId,
        propertyName,
        campaigns,
        repository,
        spClient = SourcepointClient(accountId, propertyId, propertyName),
        state = initialState ?: repository.cachedSPState ?: State()
    ) {
        repository.cachedSPState = state
    }

    private fun resetStateIfAuthIdChanged() {
        if (state.authId == null) {
            state.authId = authId
        }

        if (authId != null && state.authId != authId) {
            state = State(authId = authId)
        }
        repository.cachedSPState = state
    }

    suspend fun loadMessages(authId: String?, pubData: JsonObject?): List<MessageToDisplay> {
        state = repository.cachedSPState ?: State()
        repository.cachedSPState = state

        this.authId = authId
        resetStateIfAuthIdChanged()
        var messages: List<MessageToDisplay> = emptyList()
        metaData {
                consentStatus {
                    state.updateGDPRStatusForVendorListChanges()
                    state.updateUSNatStatusForVendorListChanges()
                    messages = try {
                        messages()
                    } catch (error: Throwable) {
                        emptyList<MessageToDisplay>()
                        throw error
                    }
                    pvData(pubData, messages)
                }
        }
        return messages
    }

    private fun handleMetaDataResponse(response: MetaDataResponse) {
        response.gdpr?.let {
            state.gdpr = state.gdpr.resetStateIfVendorListChanges(it.vendorListId)
            state.gdpr = state.gdpr.copy(
                consents = state.gdpr.consents.copy(applies = it.applies),
                metaData = state.gdpr.metaData.copy(
                    additionsChangeDate = it.additionsChangeDate,
                    legalBasisChangeDate = it.legalBasisChangeDate,
                    vendorListId = it.vendorListId
                ),
                childPmId = it.childPmId
            )
            state.gdpr.metaData.updateSampleFields(it.sampleRate)
        }
        response.ccpa?.let {
            state.ccpa = state.ccpa.copy(consents = state.ccpa.consents.copy(applies = it.applies))
            state.ccpa.metaData.updateSampleFields(it.sampleRate)
        }
        response.usnat?.let {
            val previousApplicableSections = state.usNat.metaData.applicableSections
            state.usNat = state.usNat.resetStateIfVendorListChanges(it.vendorListId)
            state.usNat = state.usNat.copy(
                consents = state.usNat.consents.copy(applies = it.applies),
                metaData = state.usNat.metaData.copy(
                    vendorListId = it.vendorListId,
                    additionsChangeDate = it.additionsChangeDate,
                    applicableSections = it.applicableSections
                )
            )
            state.usNat.metaData.updateSampleFields(it.sampleRate)
            if (previousApplicableSections.isNotEmpty() && previousApplicableSections != state.usNat.metaData.applicableSections) {
                needsNewUSNatData = true
            }
        }
        repository.cachedSPState = state
    }

    suspend fun metaData(next: suspend () -> Unit) {
        try {
            val response = spClient.getMetaData(MetaDataRequest.Campaigns(
                gdpr = campaigns.gdpr?.let { MetaDataRequest.Campaigns.Campaign(it.groupPmId) },
                ccpa = campaigns.ccpa?.let { MetaDataRequest.Campaigns.Campaign(it.groupPmId) },
                usnat = campaigns.usnat?.let { MetaDataRequest.Campaigns.Campaign(it.groupPmId) }
            ))
            handleMetaDataResponse(response)
            next()
        } catch (error: Throwable) {
            throw error
        }
    }

    private fun handleConsentStatusResponse(response: ConsentStatusResponse) {
        state.localState = response.localState
        response.consentStatusData.gdpr?.let {
            state.gdpr = state.gdpr.copy(
                consents = state.gdpr.consents.copy(
                    uuid = it.uuid,
                    grants = it.grants,
                    dateCreated = it.dateCreated,
                    expirationDate = it.expirationDate,
                    euconsent = it.euconsent,
                    tcData = it.tcData,
                    consentStatus = it.consentStatus,
                    webConsentPayload = it.webConsentPayload,
                    gcmStatus = it.gcmStatus,
                    legIntCategories = it.legIntCategories,
                    legIntVendors = it.legIntVendors,
                    categories = it.categories,
                    vendors = it.vendors,
                    specialFeatures = it.specialFeatures
                )
            )
        }
        response.consentStatusData.ccpa?.let {
            state.ccpa = state.ccpa.copy(
                consents = state.ccpa.consents.copy(
                    uuid = it.uuid,
                    dateCreated = it.dateCreated,
                    expirationDate = it.expirationDate,
                    status = it.status,
                    rejectedVendors = it.rejectedVendors,
                    rejectedCategories = it.rejectedCategories,
                    rejectedAll = it.rejectedAll,
                    consentedAll = it.consentedAll,
                    webConsentPayload = it.webConsentPayload,
                    gppData = it.gppData
                )
            )
        }
        response.consentStatusData.usnat?.let {
            state.usNat = state.usNat.copy(
                consents = state.usNat.consents.copy(
                    uuid = it.uuid,
                    dateCreated = it.dateCreated,
                    expirationDate = it.expirationDate,
                    consentStrings = it.consentStrings,
                    webConsentPayload = it.webConsentPayload,
                    userConsents = it.userConsents,
                    consentStatus = it.consentStatus,
                    gppData = it.gppData
                )
            )
        }
        repository.cachedSPState = state
    }

    suspend fun consentStatus(next: suspend () -> Unit) {
        if (shouldCallConsentStatus) {
            try {
                val response = spClient.getConsentStatus(
                    authId = authId,
                    metadata = ConsentStatusRequest.MetaData(
                        gdpr = campaigns.gdpr?.let {
                            ConsentStatusRequest.MetaData.Campaign(
                                applies = state.gdpr.consents.applies,
                                dateCreated = state.gdpr.consents.dateCreated,
                                uuid = state.gdpr.consents.uuid,
                                hasLocalData = state.hasGDPRLocalData,
                                idfaStatus = idfaStatus
                            )
                        },
                        usnat = campaigns.usnat?.let {
                            ConsentStatusRequest.MetaData.USNatCampaign(
                                applies = state.usNat.consents.applies,
                                dateCreated = state.usNat.consents.dateCreated,
                                uuid = state.usNat.consents.uuid,
                                hasLocalData = state.hasUSNatLocalData,
                                idfaStatus = idfaStatus,
                                transitionCCPAAuth = authTransitionCCPAUSNat,
                                optedOut = transitionCCPAOptedOut
                            )
                        },
                        ccpa = campaigns.ccpa?.let {
                            ConsentStatusRequest.MetaData.Campaign(
                                applies = state.ccpa.consents.applies,
                                dateCreated = state.ccpa.consents.dateCreated,
                                uuid = state.ccpa.consents.uuid,
                                hasLocalData = state.hasCCPALocalData,
                                idfaStatus = idfaStatus
                            )
                        }
                    )
                )
                state.localVersion = State.VERSION
                handleConsentStatusResponse(response)
            } catch (error: Throwable) {
                throw error
            }
        } else {
            state.localVersion = State.VERSION
        }
        next()
    }

    private fun handleMessagesResponse(response: MessagesResponse): List<MessageToDisplay> {
        state.localState = response.localState
        state.nonKeyedLocalState = response.nonKeyedLocalState

        response.campaigns.forEach {
            when (it.type) {
                SPCampaignType.Gdpr -> state.gdpr = state.gdpr.copy(consents = it.toConsent(default = state.gdpr) as GDPRConsent)
                SPCampaignType.Ccpa -> state.ccpa = state.ccpa.copy(consents = it.toConsent(default = state.ccpa) as CCPAConsent)
                SPCampaignType.UsNat -> state.usNat = state.usNat.copy(consents = it.toConsent(default = state.usNat) as USNatConsent)
                SPCampaignType.IOS14 -> {
                    state.ios14 = state.ios14.copy(
                        messageId = it.messageMetaData?.messageId,
                        partitionUUID = it.messageMetaData?.messagePartitionUUID
                    )
                }
                SPCampaignType.unknown -> return@forEach
            }
        }
        repository.cachedSPState = state
        return response.campaigns.mapNotNull { MessageToDisplay.initFromCampaign(it) }
    }

    suspend fun messages(): List<MessageToDisplay> =
        if (shouldCallMessages) {
            try {
                val response = spClient.getMessages(MessagesRequest(
                    body = MessagesRequest.Body(
                        propertyHref = propertyName,
                        accountId = accountId,
                        campaigns = MessagesRequest.Body.Campaigns(
                            gdpr = campaigns.gdpr?.let {
                                MessagesRequest.Body.Campaigns.GDPR(
                                    targetingParams = it.targetingParams,
                                    hasLocalData = state.hasGDPRLocalData,
                                    consentStatus = state.gdpr.consents.consentStatus
                                )
                            },
                            ios14 = campaigns.ios14?.let {
                                MessagesRequest.Body.Campaigns.IOS14(
                                    targetingParams = it.targetingParams,
                                    idfaStatus = idfaStatus
                                )
                            },
                            ccpa = campaigns.ccpa?.let {
                                MessagesRequest.Body.Campaigns.CCPA(
                                    targetingParams = it.targetingParams,
                                    hasLocalData = state.hasCCPALocalData,
                                    status = state.ccpa.consents.status
                                )
                            },
                            usnat = campaigns.usnat?.let {
                                MessagesRequest.Body.Campaigns.USNat(
                                    targetingParams = it.targetingParams,
                                    hasLocalData = state.hasUSNatLocalData,
                                    consentStatus = state.usNat.consents.consentStatus
                                )
                            }
                        ),
                        consentLanguage = language,
                        campaignEnv = campaigns.environment,
                        idfaStatus = idfaStatus,
                        includeData = includeData
                    ),
                    metadata = MessagesRequest.MetaData(
                        gdpr = MessagesRequest.MetaData.Campaign(state.gdpr.consents.applies),
                        usnat = MessagesRequest.MetaData.Campaign(state.ccpa.consents.applies),
                        ccpa = MessagesRequest.MetaData.Campaign(state.usNat.consents.applies)
                    ),
                    nonKeyedLocalState = state.nonKeyedLocalState,
                    localState = state.localState
                ))
                handleMessagesResponse(response)
            } catch (error: Throwable) {
                throw error
            }
        } else {
            emptyList()
        }

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

    suspend fun pvData(pubData: JsonObject?, messages: List<MessageToDisplay>) {
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
        repository.cachedSPState = state
    }

    private fun gdprPvDataBody(consent: GDPRConsent?, pubData: JsonObject?, messageMetaData: MessagesResponse.MessageMetaData): PvDataRequest =
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

    private fun ccpaPvDataBody(consent: CCPAConsent?, pubData: JsonObject?, messageMetaData: MessagesResponse.MessageMetaData): PvDataRequest =
        if (consent != null) {
            PvDataRequest(
                gdpr = null,
                usnat = null,
                ccpa = PvDataRequest.CCPA(
                    applies = consent.applies,
                    uuid = consent.uuid,
                    accountId = accountId,
                    propertyId = propertyId,
                    consentStatus = ConsentStatus(
                        rejectedAll = consent.rejectedAll,
                        consentedAll = consent.consentedAll,
                        rejectedVendors = consent.rejectedVendors,
                        rejectedCategories = consent.rejectedCategories
                    ),
                    pubData = pubData,
                    messageId = messageMetaData.messageId,
                    sampleRate = state.ccpaMetaData?.sampleRate
                )
            )
        } else {
            PvDataRequest(gdpr = null, ccpa = null, usnat = null)
        }

    private fun usnatPvDataBody(consent: USNatConsent?, pubData: JsonObject?, messageMetaData: MessagesResponse.MessageMetaData): PvDataRequest =
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
        repository.cachedSPState = state
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
                    uuid = state.gdpr?.consents?.uuid,
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
                    uuid = state.ccpa?.consents?.uuid,
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
                    uuid = state.usNat?.consents?.uuid,
                    messageId = action.messageId,
                    vendorListId = state.usNatMetaData?.vendorListId,
                    pubData = action.encodablePubData,
                    pmSaveAndExitVariables = action.pmPayload,
                    sendPVData = state.usNatMetaData?.wasSampled ?: false,
                    propertyId = propertyId,
                    sampleRate = state.usNatMetaData?.sampleRate,
                    idfaStatus = idfaStatus,
                    granularStatus = state.usNat?.consents?.consentStatus?.granularStatus,
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
            state.gdpr?.consents?.tcData = postResponse.tcData ?: emptyMap()
        }
        repository.cachedSPState = state
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
            state.ccpa?.consents?.gppData = postResponse.gppData
        }
        repository.cachedSPState = state
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
            applies = state.usNat?.consents?.applies ?: false,
            dateCreated = postResponse.dateCreated,
            expirationDate = postResponse.expirationDate,
            consentStrings = postResponse.consentStrings,
            webConsentPayload = postResponse.webConsentPayload ?: getResponse?.usnat?.webConsentPayload,
            consentStatus = postResponse.consentStatus,
            gppData = postResponse.gppData,
            userConsents = state.usNat?.consents?.userConsents?.copy(
                categories = postResponse.userConsents.categories,
                vendors = postResponse.userConsents.vendors
            ) ?: USNatConsent.USNatUserConsents()
        )
        repository.cachedSPState = state
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
}
