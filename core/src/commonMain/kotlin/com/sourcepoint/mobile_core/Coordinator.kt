package com.sourcepoint.mobile_core

import com.sourcepoint.mobile_core.models.DeleteCustomConsentGDPRException
import com.sourcepoint.mobile_core.models.InvalidCustomConsentUUIDError
import com.sourcepoint.mobile_core.models.LoadMessagesException
import com.sourcepoint.mobile_core.models.MessageToDisplay
import com.sourcepoint.mobile_core.models.PostCustomConsentGDPRException
import com.sourcepoint.mobile_core.models.ReportActionException
import com.sourcepoint.mobile_core.models.SPAction
import com.sourcepoint.mobile_core.models.SPActionType.AcceptAll
import com.sourcepoint.mobile_core.models.SPActionType.RejectAll
import com.sourcepoint.mobile_core.models.SPActionType.SaveAndExit
import com.sourcepoint.mobile_core.models.SPCampaignType
import com.sourcepoint.mobile_core.models.SPCampaignType.Ccpa
import com.sourcepoint.mobile_core.models.SPCampaignType.Gdpr
import com.sourcepoint.mobile_core.models.SPCampaignType.IOS14
import com.sourcepoint.mobile_core.models.SPCampaignType.UsNat
import com.sourcepoint.mobile_core.models.SPCampaignType.GlobalCmp
import com.sourcepoint.mobile_core.models.SPCampaignType.Preferences
import com.sourcepoint.mobile_core.models.SPCampaigns
import com.sourcepoint.mobile_core.models.SPError
import com.sourcepoint.mobile_core.models.SPIDFAStatus
import com.sourcepoint.mobile_core.models.SPMessageLanguage
import com.sourcepoint.mobile_core.models.SPPropertyName
import com.sourcepoint.mobile_core.models.consents.CCPAConsent
import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import com.sourcepoint.mobile_core.models.consents.GDPRConsent
import com.sourcepoint.mobile_core.models.consents.GlobalCmpConsent
import com.sourcepoint.mobile_core.models.consents.SPUserData
import com.sourcepoint.mobile_core.models.consents.State
import com.sourcepoint.mobile_core.models.consents.USNatConsent
import com.sourcepoint.mobile_core.models.consents.preferencesTargetingParams
import com.sourcepoint.mobile_core.network.SPClient
import com.sourcepoint.mobile_core.network.SourcepointClient
import com.sourcepoint.mobile_core.network.requests.CCPAChoiceRequest
import com.sourcepoint.mobile_core.network.requests.ChoiceAllRequest
import com.sourcepoint.mobile_core.network.requests.ConsentStatusRequest
import com.sourcepoint.mobile_core.network.requests.GDPRChoiceRequest
import com.sourcepoint.mobile_core.network.requests.GlobalCmpChoiceRequest
import com.sourcepoint.mobile_core.network.requests.IncludeData
import com.sourcepoint.mobile_core.network.requests.MessagesRequest
import com.sourcepoint.mobile_core.network.requests.MetaDataRequest
import com.sourcepoint.mobile_core.network.requests.PreferencesChoiceRequest
import com.sourcepoint.mobile_core.network.requests.PvDataRequest
import com.sourcepoint.mobile_core.network.requests.USNatChoiceRequest
import com.sourcepoint.mobile_core.network.responses.ChoiceAllResponse
import com.sourcepoint.mobile_core.network.responses.ConsentStatusResponse
import com.sourcepoint.mobile_core.network.responses.MessagesResponse
import com.sourcepoint.mobile_core.network.responses.MetaDataResponse
import com.sourcepoint.mobile_core.network.responses.PvDataResponse
import com.sourcepoint.mobile_core.storage.Repository
import com.sourcepoint.mobile_core.utils.inOneYear
import com.sourcepoint.mobile_core.utils.now
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonObject

class Coordinator(
    private val accountId: Int,
    private val propertyId: Int,
    private val propertyName: SPPropertyName,
    private val campaigns: SPCampaigns,
    private val repository: Repository = Repository(),
    private val timeoutInSeconds: Int = 5,
    private val spClient: SPClient = SourcepointClient(
        accountId = accountId,
        propertyId = propertyId,
        requestTimeoutInSeconds = timeoutInSeconds
    ),
    private var authId: String? = null,
    internal var state: State = repository.state ?: State(accountId = accountId, propertyId = propertyId),
): ICoordinator {
    private val idfaStatus: SPIDFAStatus? get() = getIDFAStatus()
    // TODO: implement using expect/actual
    var getIDFAStatus: (() -> SPIDFAStatus?) = { SPIDFAStatus.current() } // workaround for ios
    private val includeData: IncludeData = IncludeData()

    private var needsNewUSNatData = false
    private var needsNewGlobalCmpData = false

    private val needsNewConsentData: Boolean get() =
                needsNewUSNatData || needsNewGlobalCmpData || transitionCCPAOptedOut ||
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
                campaigns.usnat != null || campaigns.preferences != null || campaigns.globalcmp != null

    override val userData: SPUserData
        get() = SPUserData(
            gdpr = campaigns.gdpr?.let {
                SPUserData.SPConsent(
                    consents = state.gdpr.consents,
                    childPmId = state.gdpr.childPmId
                )
            },
            ccpa = campaigns.ccpa?.let {
                SPUserData.SPConsent(
                    consents = state.ccpa.consents,
                    childPmId = state.ccpa.childPmId
                )
            },
            usnat = campaigns.usnat?.let {
                SPUserData.SPConsent(
                    consents = state.usNat.consents,
                    childPmId = state.usNat.childPmId
                )
            },
            preferences = campaigns.preferences?.let {
                SPUserData.SPConsent(
                    consents = state.preferences.consents
                )
            },
            globalcmp = campaigns.globalcmp?.let {
                SPUserData.SPConsent(
                    consents = state.globalcmp.consents,
                    childPmId = state.globalcmp.childPmId
                )
            }
        )

    @Suppress("Unused")
    constructor(
        accountId: Int,
        propertyId: Int,
        propertyName: SPPropertyName,
        campaigns: SPCampaigns,
        timeoutInSeconds: Int = 5,
    ): this(
        accountId = accountId,
        propertyId = propertyId,
        propertyName = propertyName,
        campaigns = campaigns,
        repository = Repository(),
        timeoutInSeconds = timeoutInSeconds
    )

    @Suppress("Unused")
    constructor(
        accountId: Int,
        propertyId: Int,
        propertyName: SPPropertyName,
        campaigns: SPCampaigns,
        timeoutInSeconds: Int = 5,
        state: State? = null
    ): this(
        accountId = accountId,
        propertyId = propertyId,
        propertyName = propertyName,
        campaigns = campaigns,
        state = state ?: Repository().state ?: State(accountId = accountId, propertyId = propertyId),
        timeoutInSeconds = timeoutInSeconds
    )

    init {
        resetStateIfPropertyDetailsChanged()
        persistState()
    }

    // TODO: double check CCPA / USNAT GPPData can be overwriting
    private fun storeLegislationConsent(userData: SPUserData) {
        userData.gdpr?.consents?.tcData.let { repository.tcData = it ?: emptyMap()  }
        userData.ccpa?.consents?.uspstring.let { repository.uspString = it }
        userData.ccpa?.consents?.gppData.let { repository.gppData = it ?: emptyMap() }
        userData.usnat?.consents?.gppData.let { repository.gppData = it ?: emptyMap() }
    }

    private fun resetStateIfPropertyDetailsChanged() {
        if (propertyId != state.propertyId || accountId != state.accountId) {
            state = State(propertyId = propertyId, accountId = accountId)
        }
    }

    private fun persistState() {
        repository.state = state
    }

    private fun resetStateIfAuthIdChanged() {
        if (state.authId == null) {
            state.authId = authId
        }

        if (authId != null && state.authId != authId) {
            state = State(authId = authId, propertyId = propertyId, accountId = accountId)
        }
        persistState()
    }

    override suspend fun loadMessages(
        authId: String?,
        pubData: JsonObject?,
        language: SPMessageLanguage
    ): List<MessageToDisplay> {
        this.authId = authId
        resetStateIfAuthIdChanged()
        var messages: List<MessageToDisplay> = emptyList()
        try {
            metaData {
                consentStatus {
                    state.updateGDPRStatusForVendorListChanges()
                    state.updateUSNatStatusForVendorListChanges()
                    state.updateGlobaCMPStatusForVendorListChanges()
                    messages = messages(language)
                    // TODO: maybe we should use `launch` here and not wait for pvData to return
                    pvData(pubData, messages)
                }
            }
        } catch (error: SPError) {
            throw LoadMessagesException(causedBy = error)
        }
        storeLegislationConsent(userData = userData)
        persistState()
        return messages
    }

    private fun handleMetaDataResponse(response: MetaDataResponse) {
        response.gdpr?.let {
            state.gdpr = state.gdpr.resetStateIfVendorListChanges(it.vendorListId)
            state.gdpr = state.gdpr.copy(
                consents = state.gdpr.consents.copy(applies = it.applies),
                metaData = state.gdpr.metaData.copy(
                    additionsChangeDate = it.additionsChangeDate ?: Instant.DISTANT_PAST,
                    legalBasisChangeDate = it.legalBasisChangeDate ?: Instant.DISTANT_PAST,
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
                    additionsChangeDate = it.additionsChangeDate ?: Instant.DISTANT_PAST,
                    applicableSections = it.applicableSections
                )
            )
            state.usNat.metaData.updateSampleFields(it.sampleRate)
            if (previousApplicableSections.isNotEmpty() && previousApplicableSections != state.usNat.metaData.applicableSections) {
                needsNewUSNatData = true
            }
        }
        response.globalcmp?.let {
            val previousApplicableSections = state.globalcmp.metaData.applicableSections
            state.globalcmp = state.globalcmp.resetStateIfVendorListChanges(it.vendorListId)
            state.globalcmp = state.globalcmp.copy(
                consents = state.globalcmp.consents.copy(applies = it.applies),
                metaData = state.globalcmp.metaData.copy(
                    vendorListId = it.vendorListId,
                    additionsChangeDate = it.additionsChangeDate ?: Instant.DISTANT_PAST,
                    applicableSections = it.applicableSections ?: emptyList(),
                    legislation = it.legislation
                ),
                childPmId = it.childPmId
            )
            state.globalcmp.metaData.updateSampleFields(it.sampleRate)
            if (previousApplicableSections.isNotEmpty() && previousApplicableSections != state.globalcmp.metaData.applicableSections) {
                needsNewGlobalCmpData = true
            }
        }
        response.preferences?.let {
            state.preferences = state.preferences.copy(
                metaData = state.preferences.metaData.copy(
                    configurationId = it.configurationId,
                    additionsChangeDate = it.additionsChangeDate ?: Instant.DISTANT_PAST,
                    legalDocLiveDate = it.legalDocLiveDate
                )
            )
        }
        persistState()
    }

    private suspend fun metaData(next: suspend () -> Unit) {
        handleMetaDataResponse(spClient.getMetaData(MetaDataRequest.Campaigns(
            gdpr = campaigns.gdpr?.let { MetaDataRequest.Campaigns.Campaign(it.groupPmId) },
            ccpa = campaigns.ccpa?.let { MetaDataRequest.Campaigns.Campaign(it.groupPmId) },
            usnat = campaigns.usnat?.let { MetaDataRequest.Campaigns.Campaign(it.groupPmId) },
            globalcmp = campaigns.globalcmp?.let { MetaDataRequest.Campaigns.Campaign(it.groupPmId) },
            preferences = campaigns.preferences?.let { MetaDataRequest.Campaigns.Campaign() }
        )))
        next()
    }

    private fun handleConsentStatusResponse(response: ConsentStatusResponse) {
        state.localVersion = State.VERSION
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
        response.consentStatusData.globalcmp?.let {
            state.globalcmp = state.globalcmp.copy(
                consents = state.globalcmp.consents.copy(
                    uuid = it.uuid,
                    dateCreated = it.dateCreated,
                    expirationDate = it.expirationDate,
                    webConsentPayload = it.webConsentPayload,
                    userConsents = it.userConsents,
                    consentStatus = it.consentStatus
                )
            )
        }
        response.consentStatusData.preferences?.let {
            state.preferences = state.preferences.copy(
                consents = state.preferences.consents.copy(
                    dateCreated = it.dateCreated,
                    status = it.status,
                    rejectedStatus = it.rejectedStatus,
                    uuid = it.uuid
                )
            )
        }
        persistState()
    }

    suspend fun consentStatus(next: suspend () -> Unit) {
        if (shouldCallConsentStatus) {
            handleConsentStatusResponse(spClient.getConsentStatus(
                authId = authId,
                metadata = ConsentStatusRequest.MetaData(
                    gdpr = campaigns.gdpr?.let {
                        ConsentStatusRequest.MetaData.Campaign(
                            applies = state.gdpr.consents.applies,
                            dateCreated = state.gdpr.consents.dateCreated,
                            uuid = state.gdpr.consents.uuid,
                            idfaStatus = idfaStatus
                        )
                    },
                    globalcmp = campaigns.globalcmp?.let {
                        ConsentStatusRequest.MetaData.GlobalCmpCampaign(
                            applies = state.globalcmp.consents.applies,
                            dateCreated = state.globalcmp.consents.dateCreated,
                            uuid = state.globalcmp.consents.uuid
                        )
                    },
                    usnat = campaigns.usnat?.let {
                        ConsentStatusRequest.MetaData.USNatCampaign(
                            applies = state.usNat.consents.applies,
                            dateCreated = state.usNat.consents.dateCreated,
                            uuid = state.usNat.consents.uuid,
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
                            idfaStatus = idfaStatus
                        )
                    },
                    preferences = campaigns.preferences?.let {
                        ConsentStatusRequest.MetaData.PreferencesCampaign(
                            configurationId = state.preferences.metaData.configurationId
                        )
                    }
                )
            ))
        }
        next()
    }

    private fun handleMessagesResponse(response: MessagesResponse): List<MessageToDisplay> {
        state.localState = response.localState
        state.nonKeyedLocalState = response.nonKeyedLocalState

        response.campaigns.forEach {
            when (it.type) {
                Gdpr -> state.gdpr = state.gdpr.copy(
                    consents = it.toConsent(default = state.gdpr.consents) as GDPRConsent,
                    metaData = state.gdpr.metaData.copy(messagePartitionUUID = it.messageMetaData?.messagePartitionUUID)
                )
                Ccpa -> state.ccpa = state.ccpa.copy(
                    consents = it.toConsent(default = state.ccpa.consents) as CCPAConsent,
                    metaData = state.ccpa.metaData.copy(messagePartitionUUID = it.messageMetaData?.messagePartitionUUID)
                )
                UsNat -> state.usNat = state.usNat.copy(
                    consents = it.toConsent(default = state.usNat.consents) as USNatConsent,
                    metaData = state.usNat.metaData.copy(messagePartitionUUID = it.messageMetaData?.messagePartitionUUID)
                )
                GlobalCmp -> state.globalcmp = state.globalcmp.copy(
                    consents = it.toConsent(default = state.globalcmp.consents) as GlobalCmpConsent,
                    metaData = state.globalcmp.metaData.copy(messagePartitionUUID = it.messageMetaData?.messagePartitionUUID)
                )
                IOS14 -> {
                    state.ios14 = state.ios14.copy(
                        messageId = it.messageMetaData?.messageId,
                        partitionUUID = it.messageMetaData?.messagePartitionUUID
                    )
                }
                Preferences -> {
                    state.preferences = state.preferences.copy(
                        consents = state.preferences.consents.copy(messageId = it.messageMetaData?.messageId),
                        metaData = state.preferences.metaData.copy(messagePartitionUUID = it.messageMetaData?.messagePartitionUUID)
                    )
                }
                SPCampaignType.Unknown -> return@forEach
            }
        }
        persistState()
        return response.campaigns.mapNotNull { MessageToDisplay.initFromCampaign(it) }
    }

    private suspend fun messages(language: SPMessageLanguage): List<MessageToDisplay> =
        if (shouldCallMessages) {
            handleMessagesResponse(spClient.getMessages(MessagesRequest(
                body = MessagesRequest.Body(
                    propertyHref = propertyName,
                    accountId = accountId,
                    campaigns = MessagesRequest.Body.Campaigns(
                        gdpr = campaigns.gdpr?.let {
                            MessagesRequest.Body.Campaigns.Campaign(
                                targetingParams = it.targetingParams,
                                hasLocalData = state.hasGDPRLocalData,
                                consentStatus = state.gdpr.consents.consentStatus
                            )
                        },
                        ios14 = campaigns.ios14?.let {
                            MessagesRequest.Body.Campaigns.IOS14Campaign(
                                targetingParams = it.targetingParams,
                                idfaStatus = idfaStatus
                            )
                        },
                        globalcmp = campaigns.globalcmp?.let {
                            MessagesRequest.Body.Campaigns.Campaign(
                                targetingParams = it.targetingParams,
                                hasLocalData = state.hasGlobalCmpLocalData,
                                consentStatus = state.globalcmp.consents.consentStatus
                            )
                        },
                        ccpa = campaigns.ccpa?.let {
                            MessagesRequest.Body.Campaigns.CCPACampaign(
                                targetingParams = it.targetingParams,
                                hasLocalData = state.hasCCPALocalData,
                                status = state.ccpa.consents.status
                            )
                        },
                        usnat = campaigns.usnat?.let {
                            MessagesRequest.Body.Campaigns.Campaign(
                                targetingParams = it.targetingParams,
                                hasLocalData = state.hasUSNatLocalData,
                                consentStatus = state.usNat.consents.consentStatus
                            )
                        },
                        preferences = campaigns.preferences?.let {
                            MessagesRequest.Body.Campaigns.Campaign(
                                targetingParams = it.targetingParams + preferencesTargetingParams(state.preferences),
                                hasLocalData = state.hasPreferencesLocalData
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
                    usnat = MessagesRequest.MetaData.Campaign(state.usNat.consents.applies),
                    ccpa = MessagesRequest.MetaData.Campaign(state.ccpa.consents.applies),
                    globalcmp = MessagesRequest.MetaData.Campaign(state.globalcmp.consents.applies)
                ),
                nonKeyedLocalState = state.nonKeyedLocalState,
                localState = state.localState
            )))
        } else {
            emptyList()
        }

    private fun sample(samplingRate: Float): Boolean =
        IntRange(1, 100).random() in (1..(samplingRate * 100).toInt())

    private fun handlePvDataResponse(response: PvDataResponse) {
        response.gdpr?.let {
            state.gdpr = state.gdpr.copy(consents = state.gdpr.consents.copy(uuid = response.gdpr.uuid))
        }
        response.ccpa?.let {
            state.ccpa = state.ccpa.copy(consents = state.ccpa.consents.copy(uuid = response.ccpa.uuid))
        }
        response.usnat?.let {
            state.usNat = state.usNat.copy(consents = state.usNat.consents.copy(uuid = response.usnat.uuid))
        }
        response.globalcmp?.let {
            state.globalcmp = state.globalcmp.copy(consents = state.globalcmp.consents.copy(uuid = response.globalcmp.uuid))
        }
    }

    private suspend fun sampleAndPvData(campaign: State.SPSampleable, request: PvDataRequest) =
        if (campaign.wasSampled == null) {
            if (sample(campaign.sampleRate)) {
                handlePvDataResponse(spClient.postPvData(request))
                true
            } else {
                false
            }
        } else if (campaign.wasSampled == true) {
            handlePvDataResponse(spClient.postPvData(request))
            true
        } else {
            false
        }

    private suspend fun pvData(pubData: JsonObject?, messages: List<MessageToDisplay>) = coroutineScope {
        val gdprPvData = campaigns.gdpr?.let {
            launch {
                gdprPvData(pubData, messageMetaData = messages.firstOrNull { it.type == Gdpr }?.metaData)
            }
        }
        val ccpaPvData = campaigns.ccpa?.let {
            launch {
                ccpaPvData(pubData, messageMetaData = messages.firstOrNull { it.type == Ccpa }?.metaData)
            }
        }
        val usNatPvData = campaigns.usnat?.let {
            launch {
                usnatPvData(pubData, messageMetaData = messages.firstOrNull { it.type == UsNat }?.metaData)
            }
        }
        val globalCmpPvData = campaigns.globalcmp?.let {
            launch {
                globalcmpPvData(pubData, messageMetaData = messages.firstOrNull { it.type == GlobalCmp }?.metaData)
            }
        }
        gdprPvData?.join()
        ccpaPvData?.join()
        usNatPvData?.join()
        globalCmpPvData?.join()
        persistState()
    }

    private suspend fun gdprPvData(pubData: JsonObject?, messageMetaData: MessagesResponse.MessageMetaData?) {
        val sampled = sampleAndPvData(
            campaign = state.gdpr.metaData,
            request = PvDataRequest(
                ccpa = null,
                usnat = null,
                globalcmp = null,
                gdpr = PvDataRequest.GDPR(
                    applies = state.gdpr.consents.applies,
                    uuid = state.gdpr.consents.uuid,
                    accountId = accountId,
                    propertyId = propertyId,
                    consentStatus = state.gdpr.consents.consentStatus,
                    pubData = pubData,
                    sampleRate = state.gdpr.metaData.sampleRate,
                    euconsent = state.gdpr.consents.euconsent,
                    msgId = messageMetaData?.messageId,
                    categoryId = messageMetaData?.categoryId?.rawValue,
                    subCategoryId = messageMetaData?.subCategoryId?.rawValue,
                    prtnUUID = messageMetaData?.messagePartitionUUID
                )
            )
        )
        state.gdpr = state.gdpr.copy(metaData = state.gdpr.metaData.copy(wasSampled = sampled))
    }

    private suspend fun ccpaPvData(pubData: JsonObject?, messageMetaData: MessagesResponse.MessageMetaData?) {
        val sampled = sampleAndPvData(
            campaign = state.ccpa.metaData,
            request = PvDataRequest(
                gdpr = null,
                usnat = null,
                globalcmp = null,
                ccpa = PvDataRequest.CCPA(
                    applies = state.ccpa.consents.applies,
                    uuid = state.ccpa.consents.uuid,
                    accountId = accountId,
                    propertyId = propertyId,
                    consentStatus = ConsentStatus(
                        rejectedAll = state.ccpa.consents.rejectedAll,
                        consentedAll = state.ccpa.consents.consentedAll,
                        rejectedVendors = state.ccpa.consents.rejectedVendors,
                        rejectedCategories = state.ccpa.consents.rejectedCategories
                    ),
                    pubData = pubData,
                    messageId = messageMetaData?.messageId,
                    sampleRate = state.ccpa.metaData.sampleRate
                )
            )
        )
        state.ccpa = state.ccpa.copy(metaData = state.ccpa.metaData.copy(wasSampled = sampled))
    }

    private suspend fun usnatPvData(pubData: JsonObject?, messageMetaData: MessagesResponse.MessageMetaData?) {
        val sampled = sampleAndPvData(
            campaign = state.usNat.metaData,
            request = PvDataRequest(
                gdpr = null,
                ccpa = null,
                globalcmp = null,
                usnat = PvDataRequest.USNat(
                    applies = state.usNat.consents.applies,
                    uuid = state.usNat.consents.uuid,
                    accountId = accountId,
                    propertyId = propertyId,
                    consentStatus = state.usNat.consents.consentStatus,
                    pubData = pubData,
                    sampleRate = state.usNat.metaData.sampleRate,
                    msgId = messageMetaData?.messageId,
                    categoryId = messageMetaData?.categoryId?.rawValue,
                    subCategoryId = messageMetaData?.subCategoryId?.rawValue,
                    prtnUUID = messageMetaData?.messagePartitionUUID
                )
            )
        )
        state.usNat = state.usNat.copy(metaData = state.usNat.metaData.copy(wasSampled = sampled))
    }

    private suspend fun globalcmpPvData(pubData: JsonObject?, messageMetaData: MessagesResponse.MessageMetaData?) {
        val sampled = sampleAndPvData(
            campaign = state.globalcmp.metaData,
            request = PvDataRequest(
                gdpr = null,
                ccpa = null,
                usnat = null,
                globalcmp = PvDataRequest.GlobalCmp(
                    applies = state.globalcmp.consents.applies,
                    uuid = state.globalcmp.consents.uuid,
                    accountId = accountId,
                    propertyId = propertyId,
                    consentStatus = state.globalcmp.consents.consentStatus,
                    pubData = pubData,
                    sampleRate = state.globalcmp.metaData.sampleRate,
                    msgId = messageMetaData?.messageId,
                    categoryId = messageMetaData?.categoryId?.rawValue,
                    subCategoryId = messageMetaData?.subCategoryId?.rawValue,
                    prtnUUID = messageMetaData?.messagePartitionUUID
                )
            )
        )
        state.globalcmp = state.globalcmp.copy(metaData = state.globalcmp.metaData.copy(wasSampled = sampled))
    }

    private fun handleGetChoiceAll(response: ChoiceAllResponse) {
        response.gdpr?.let {
            state.gdpr = state.gdpr.copy(
                consents = state.gdpr.consents.copy(
                    dateCreated = it.dateCreated ?: now(),
                    expirationDate = it.expirationDate ?: it.dateCreated?.inOneYear() ?: now().inOneYear(),
                    tcData = it.tcData ?: emptyMap(),
                    grants = it.grants,
                    euconsent = it.euconsent,
                    consentStatus = it.consentStatus,
                    gcmStatus = it.gcmStatus
                )
            )
        }
        response.ccpa?.let {
            state.ccpa = state.ccpa.copy(
                consents = state.ccpa.consents.copy(
                    dateCreated = it.dateCreated ?: now(),
                    expirationDate = it.expirationDate ?: it.dateCreated?.inOneYear() ?: now().inOneYear(),
                    status = it.status,
                    gppData = it.gppData,
                )
            )
        }
        response.usnat?.let {
            state.usNat = state.usNat.copy(
                consents = state.usNat.consents.copy(
                    dateCreated = it.dateCreated ?: now(),
                    expirationDate = it.expirationDate ?: it.dateCreated?.inOneYear() ?: now().inOneYear(),
                    consentStatus = it.consentStatus,
                    gppData = it.gppData,
                    consentStrings = it.consentStrings
                )
            )
        }
        response.globalcmp?.let {
            state.globalcmp = state.globalcmp.copy(
                consents = state.globalcmp.consents.copy(
                    dateCreated = it.dateCreated ?: now(),
                    expirationDate = it.expirationDate ?: it.dateCreated?.inOneYear() ?: now().inOneYear(),
                    gpcEnabled = it.gpcEnabled,
                    categories = it.categories,
                    consentStatus = it.consentStatus,
                    webConsentPayload = it.webConsentPayload,
                )
            )
        }
        persistState()
    }

    private suspend fun getChoiceAll(action: SPAction, campaigns: ChoiceAllRequest.ChoiceAllCampaigns): ChoiceAllResponse? {
        val shouldCallGetChoice = (action.type == AcceptAll || action.type == RejectAll)
        if (!shouldCallGetChoice)
            return null

        val response = spClient.getChoiceAll(action.type, campaigns)
        handleGetChoiceAll(response)
        return response
    }

    private suspend fun postChoiceGDPR(
        action: SPAction,
        postPayloadFromGetCall: ChoiceAllResponse.GDPR.PostPayload?
    ) = spClient.postChoiceGDPRAction(
        actionType = action.type,
        request = GDPRChoiceRequest(
            authId = authId,
            uuid = state.gdpr.consents.uuid,
            messageId = action.messageId,
            consentAllRef = postPayloadFromGetCall?.consentAllRef,
            vendorListId = postPayloadFromGetCall?.vendorListId,
            pubData = action.encodablePubData,
            pmSaveAndExitVariables = action.pmPayload,
            sendPVData = state.gdpr.metaData.wasSampled ?: false,
            propertyId = propertyId,
            sampleRate = state.gdpr.metaData.sampleRate,
            idfaStatus = idfaStatus,
            granularStatus = postPayloadFromGetCall?.granularStatus,
            includeData = includeData,
            prtnUUID = state.gdpr.metaData.messagePartitionUUID
        )
    )

    private suspend fun postChoiceCCPA(action: SPAction) = spClient.postChoiceCCPAAction(
        actionType = action.type,
        request = CCPAChoiceRequest(
            authId = authId,
            uuid = state.ccpa.consents.uuid,
            messageId = action.messageId,
            pubData = action.encodablePubData,
            pmSaveAndExitVariables = action.pmPayload,
            sendPVData = state.ccpa.metaData.wasSampled ?: false,
            propertyId = propertyId,
            sampleRate = state.ccpa.metaData.sampleRate,
            includeData = includeData,
            prtnUUID = state.ccpa.metaData.messagePartitionUUID
        )
    )

    private suspend fun postChoiceUSNat(action: SPAction) = spClient.postChoiceUSNatAction(
        actionType = action.type,
        request = USNatChoiceRequest(
            accountId = accountId,
            authId = authId,
            uuid = state.usNat.consents.uuid,
            messageId = action.messageId,
            vendorListId = state.usNat.metaData.vendorListId,
            pubData = action.encodablePubData,
            pmSaveAndExitVariables = action.pmPayload,
            sendPVData = state.usNat.metaData.wasSampled ?: false,
            propertyId = propertyId,
            sampleRate = state.usNat.metaData.sampleRate,
            idfaStatus = idfaStatus,
            granularStatus = state.usNat.consents.consentStatus.granularStatus,
            includeData = includeData,
            prtnUUID = state.usNat.metaData.messagePartitionUUID
        )
    )

    private suspend fun postChoiceGlobalCmp(action: SPAction) = spClient.postChoiceGlobalCmpAction(
        actionType = action.type,
        request = GlobalCmpChoiceRequest(
            accountId = accountId,
            authId = authId,
            uuid = state.globalcmp.consents.uuid,
            messageId = action.messageId,
            vendorListId = state.globalcmp.metaData.vendorListId,
            pubData = action.encodablePubData,
            pmSaveAndExitVariables = action.pmPayload,
            sendPVData = state.globalcmp.metaData.wasSampled ?: false,
            propertyId = propertyId,
            sampleRate = state.globalcmp.metaData.sampleRate,
            granularStatus = state.globalcmp.consents.consentStatus.granularStatus,
            includeData = includeData,
            prtnUUID = state.globalcmp.metaData.messagePartitionUUID
        )
    )

    private suspend fun postChoicePreferences(action: SPAction) = spClient.postChoicePreferencesAction(
        actionType = action.type,
        request = PreferencesChoiceRequest(
            accountId = accountId,
            authId = authId,
            uuid = state.preferences.consents.uuid,
            messageId = action.messageId,
            pmSaveAndExitVariables = action.pmPayload,
            propertyId = propertyId,
            includeData = includeData,
            prtnUUID = state.preferences.metaData.messagePartitionUUID
        )
    )

    private suspend fun reportGDPRAction(action: SPAction, getResponse: ChoiceAllResponse?) {
        val postResponse = postChoiceGDPR(action = action, postPayloadFromGetCall = getResponse?.gdpr?.postPayload)
        state.gdpr = state.gdpr.copy(
            consents = state.gdpr.consents.copy(
                uuid = postResponse.uuid,
                dateCreated = postResponse.dateCreated ?: now(),
                expirationDate = postResponse.expirationDate ?: postResponse.dateCreated?.inOneYear() ?: now().inOneYear(),
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
        )
        if (action.type == SaveAndExit) {
            state.gdpr.consents.tcData = postResponse.tcData ?: emptyMap()
        }
        persistState()
    }

    private suspend fun reportCCPAAction(action: SPAction, getResponse: ChoiceAllResponse?) {
        val postResponse = postChoiceCCPA(action = action)
        state.ccpa = state.ccpa.copy(
            consents = state.ccpa.consents.copy(
                uuid = postResponse.uuid,
                dateCreated = postResponse.dateCreated ?: now(),
                rejectedAll = postResponse.rejectedAll ?: getResponse?.ccpa?.rejectedAll ?: false,
                consentedAll = postResponse.consentedAll ?: getResponse?.ccpa?.consentedAll ?: false,
                status = postResponse.status ?: getResponse?.ccpa?.status ?: CCPAConsent.CCPAConsentStatus.RejectedAll,
                rejectedVendors = postResponse.rejectedVendors ?: getResponse?.ccpa?.rejectedVendors?: emptyList(),
                rejectedCategories = postResponse.rejectedCategories ?: getResponse?.ccpa?.rejectedCategories ?: emptyList(),
                webConsentPayload = postResponse.webConsentPayload ?: getResponse?.ccpa?.webConsentPayload,
            )
        )
        if (action.type == SaveAndExit) {
            state.ccpa.consents.gppData = postResponse.gppData
        }
        persistState()
    }

    private suspend fun reportUSNatAction(action: SPAction, getResponse: ChoiceAllResponse?) {
        val postResponse = postChoiceUSNat(action = action)
        state.usNat = state.usNat.copy(
            consents = state.usNat.consents.copy(
                uuid = postResponse.uuid,
                applies = state.usNat.consents.applies,
                dateCreated = postResponse.dateCreated ?: now(),
                expirationDate = postResponse.expirationDate ?: postResponse.dateCreated?.inOneYear() ?: now().inOneYear(),
                consentStrings = postResponse.consentStrings,
                webConsentPayload = postResponse.webConsentPayload ?: getResponse?.usnat?.webConsentPayload,
                consentStatus = postResponse.consentStatus,
                gppData = postResponse.gppData,
                userConsents = state.usNat.consents.userConsents.copy(
                    categories = postResponse.userConsents.categories,
                    vendors = postResponse.userConsents.vendors
                )
            )
        )
        persistState()
    }

    private suspend fun reportGlobalCmpAction(action: SPAction) {
        val postResponse = postChoiceGlobalCmp(action = action)
        state.globalcmp = state.globalcmp.copy(
            consents = state.globalcmp.consents.copy(
                uuid = postResponse.uuid,
                applies = state.globalcmp.consents.applies,
                dateCreated = postResponse.dateCreated ?: now(),
                expirationDate = postResponse.expirationDate ?: postResponse.dateCreated?.inOneYear() ?: now().inOneYear(),
                consentStatus = postResponse.consentStatus,
                webConsentPayload = postResponse.webConsentPayload,
                userConsents = state.globalcmp.consents.userConsents.copy(
                    categories = postResponse.userConsents.categories,
                    vendors = postResponse.userConsents.vendors
                )
            )
        )
        persistState()
    }

    private suspend fun reportPreferencesAction(action: SPAction) {
        val postResponse = postChoicePreferences(action = action)
        state.preferences = state.preferences.copy(
            consents = state.preferences.consents.copy(
                uuid = postResponse.uuid,
                dateCreated = postResponse.dateCreated,
                status = postResponse.status,
                rejectedStatus = postResponse.rejectedStatus
            )
        )
        persistState()
    }

    override suspend fun reportAction(action: SPAction): SPUserData {
        try {
            val getResponse = getChoiceAll(
                action = action,
                campaigns = ChoiceAllRequest.ChoiceAllCampaigns(
                    gdpr = if (action.campaignType == Gdpr) ChoiceAllRequest.ChoiceAllCampaigns.Campaign(applies = state.gdpr.consents.applies) else null,
                    ccpa = if (action.campaignType == Ccpa) ChoiceAllRequest.ChoiceAllCampaigns.Campaign(applies = state.ccpa.consents.applies) else null,
                    usnat = if (action.campaignType == UsNat) ChoiceAllRequest.ChoiceAllCampaigns.Campaign(applies = state.usNat.consents.applies) else null,
                    globalcmp = if (action.campaignType == GlobalCmp) ChoiceAllRequest.ChoiceAllCampaigns.Campaign(applies = state.globalcmp.consents.applies) else null
                )
            )
            when (action.campaignType) {
                Gdpr -> reportGDPRAction(action = action, getResponse = getResponse)
                Ccpa -> reportCCPAAction(action = action, getResponse = getResponse)
                UsNat -> reportUSNatAction(action = action, getResponse = getResponse)
                GlobalCmp -> reportGlobalCmpAction(action = action)
                Preferences -> reportPreferencesAction(action = action)
                IOS14, SPCampaignType.Unknown -> {}
            }
        } catch (error: SPError) {
            throw ReportActionException(causedBy = error, actionType = action.type, campaignType = action.campaignType)
        } finally {
            storeLegislationConsent(userData = userData)
            persistState()
        }
        return userData
    }

    private fun handleCustomConsentResponse(response: GDPRConsent) {
        state.gdpr = state.gdpr.copy(consents = state.gdpr.consents.copy(
            vendors = response.vendors,
            categories = response.categories,
            legIntVendors = response.legIntVendors,
            legIntCategories = response.legIntCategories,
            specialFeatures = response.specialFeatures,
            grants = response.grants
        ))
        persistState()
    }

    override suspend fun customConsentGDPR(
        vendors: List<String>,
        categories: List<String>,
        legIntCategories: List<String>
    ) {
        if (state.gdpr.consents.uuid.isNullOrEmpty()) {
            throw InvalidCustomConsentUUIDError()
        }
        try {
            handleCustomConsentResponse(spClient.customConsentGDPR(
                consentUUID = state.gdpr.consents.uuid!!,
                propertyId = propertyId,
                vendors = vendors,
                categories = categories,
                legIntCategories = legIntCategories
            ))
        } catch (error: SPError) {
            throw PostCustomConsentGDPRException(causedBy = error)
        }
    }

    override suspend fun deleteCustomConsentGDPR(
        vendors: List<String>,
        categories: List<String>,
        legIntCategories: List<String>
    ) {
        if (state.gdpr.consents.uuid.isNullOrEmpty()) {
            throw InvalidCustomConsentUUIDError()
        }
        try {
            handleCustomConsentResponse(spClient.deleteCustomConsentGDPR(
                consentUUID = state.gdpr.consents.uuid!!,
                propertyId = propertyId,
                vendors = vendors,
                categories = categories,
                legIntCategories = legIntCategories
            ))
        } catch (error: SPError) {
            throw DeleteCustomConsentGDPRException(causedBy = error)
        }
    }

    override suspend fun reportIdfaStatus(osVersion: String, requestUUID: String) {
        var uuid = ""
        var uuidType: SPCampaignType? = null
        userData.gdpr?.consents?.uuid?.let {
            if (it.isNotEmpty()) {
                uuid = it
                uuidType = Gdpr
            }
        }
        userData.ccpa?.consents?.uuid?.let {
            if (it.isNotEmpty()) {
                uuid = it
                uuidType = Ccpa
            }
        }
        userData.usnat?.consents?.uuid?.let {
            if (it.isNotEmpty()) {
                uuid = it
                uuidType = UsNat
            }
        }
        spClient.postReportIdfaStatus(
            propertyId = propertyId,
            uuid = uuid,
            requestUUID = requestUUID,
            uuidType = uuidType,
            messageId = state.ios14.messageId,
            idfaStatus = idfaStatus ?: SPIDFAStatus.Unknown,
            iosVersion = osVersion,
            partitionUUID = state.ios14.partitionUUID
        )
    }

    override fun clearLocalData() {
        repository.clear()
        state = State(accountId = accountId, propertyId = propertyId, authId = authId)
        persistState()
    }

    override suspend fun logError(error: SPError) {
        try { spClient.errorMetrics(error) } catch (_: Throwable) {}
    }

    override fun setTranslateMessage(value: Boolean) {
        includeData.translateMessage = value
    }
}
