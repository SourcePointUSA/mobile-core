package com.sourcepoint.mobile_core

import com.sourcepoint.mobile_core.models.SPAction
import com.sourcepoint.mobile_core.models.SPActionType
import com.sourcepoint.mobile_core.models.SPCampaignType
import com.sourcepoint.mobile_core.models.SPIDFAStatus
import com.sourcepoint.mobile_core.models.consents.CCPAConsent
import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import com.sourcepoint.mobile_core.models.consents.State
import com.sourcepoint.mobile_core.network.SourcepointClient
import com.sourcepoint.mobile_core.network.requests.CCPAChoiceRequest
import com.sourcepoint.mobile_core.network.requests.ChoiceAllRequest
import com.sourcepoint.mobile_core.network.requests.GDPRChoiceRequest
import com.sourcepoint.mobile_core.network.requests.IncludeData
import com.sourcepoint.mobile_core.network.requests.MetaDataRequest
import com.sourcepoint.mobile_core.network.requests.USNatChoiceRequest
import com.sourcepoint.mobile_core.network.responses.CCPAChoiceResponse
import com.sourcepoint.mobile_core.network.responses.ChoiceAllResponse
import com.sourcepoint.mobile_core.network.responses.GDPRChoiceResponse
import com.sourcepoint.mobile_core.network.responses.USNatChoiceResponse
import com.sourcepoint.mobile_core.storage.Repository

class Coordinator(
    private val accountId: Int,
    private val propertyId: Int,
    private val propertyName: String,
    private val repository: Repository,
    private val spClient: SourcepointClient
): SPCoordinator {
    lateinit var state: State
    var authId: String? = null
    var idfaStatus: SPIDFAStatus? = SPIDFAStatus.current()
    var includeData: IncludeData = IncludeData()

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

    private fun handleGetChoiceAll(response: ChoiceAllResponse, campaign: SPCampaignType) {
        if (response.gdpr != null && campaign == SPCampaignType.Gdpr) {
            state.gdpr?.dateCreated = response.gdpr.dateCreated
            state.gdpr?.expirationDate = response.gdpr.expirationDate
            state.gdpr?.tcData = response.gdpr.tcData?: emptyMap()
            state.gdpr?.grants = response.gdpr.grants
            state.gdpr?.euconsent = response.gdpr.euconsent
            state.gdpr?.consentStatus = response.gdpr.consentStatus
            //state.gdpr?.childPmId = gdpr.childPmId
            state.gdpr?.gcmStatus = response.gdpr.gcmStatus
        }
        if (response.ccpa != null && campaign == SPCampaignType.Ccpa) {
            state.ccpa?.dateCreated = response.ccpa.dateCreated
            state.ccpa?.expirationDate = response.ccpa.expirationDate
            state.ccpa?.status = response.ccpa.status
            state.ccpa?.gppData = response.ccpa.gppData
        }
        if (response.usnat != null && campaign == SPCampaignType.UsNat) {
            state.usNat?.dateCreated = response.usnat.dateCreated
            state.usNat?.expirationDate = response.usnat.expirationDate
            state.usNat?.consentStatus = response.usnat.consentStatus
            state.usNat?.gppData = response.usnat.gppData
            state.usNat?.consentStrings = response.usnat.consentStrings
            state.usNat?.consentStatus?.consentedToAll = response.usnat.consentedToAll
            state.usNat?.consentStatus?.rejectedAny = response.usnat.rejectedAny
            state.usNat?.consentStatus?.granularStatus?.gpcStatus = response.usnat.gpcEnabled
        }
    }

    override suspend fun getChoiceAll(action: SPAction, campaigns: ChoiceAllRequest.ChoiceAllCampaigns): ChoiceAllResponse? {
        val shouldCallGetChoice: Boolean = (action.type == SPActionType.AcceptAll || action.type == SPActionType.RejectAll)
        if (!shouldCallGetChoice)
            return null

        try {
            val response = spClient.getChoiceAll(action.type, campaigns)
            handleGetChoiceAll(response, action.campaignType)
            return response
        } catch (error: Throwable) {
            throw error
        }
    }

    override suspend fun postChoiceGDPR(
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

    override suspend fun postChoiceCCPA(action: SPAction): CCPAChoiceResponse =
        try {
            spClient.postChoiceCCPAAction(
                actionType = action.type,
                request = CCPAChoiceRequest(
                    authId = authId,
                    uuid = state.ccpa?.uuid,
                    messageId = action.messageId,
                    pubData = action.encodablePubData,
                    pmSaveAndExitVariables = action.pmPayload,
                    sendPVData = state.ccpaMetaData?.wasSampled?: false,
                    propertyId = propertyId,
                    sampleRate = state.ccpaMetaData?.sampleRate,
                    includeData = includeData
                )
            )
        }
        catch (error: Throwable) {
            throw error
        }

    override suspend fun postChoiceUSNat(action: SPAction): USNatChoiceResponse =
        try {
            spClient.postChoiceUSNatAction(
                actionType = action.type,
                request = USNatChoiceRequest(
                    authId = authId,
                    uuid = state.usNat?.uuid,
                    messageId = action.messageId,
                    vendorListId = action.messageId,
                    pubData = action.encodablePubData,
                    pmSaveAndExitVariables = action.pmPayload,
                    sendPVData = state.usNatMetaData?.wasSampled?: false,
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
        if (action.type == SPActionType.SaveAndExit) {
            state.gdpr?.tcData = postResponse.tcData?: emptyMap()
        }
        state.gdpr?.uuid = postResponse.uuid
        state.gdpr?.dateCreated = postResponse.dateCreated
        state.gdpr?.expirationDate = postResponse.expirationDate
        state.gdpr?.consentStatus = postResponse.consentStatus?: getResponse?.gdpr?.consentStatus?: ConsentStatus()
        state.gdpr?.euconsent = postResponse.euconsent?: getResponse?.gdpr?.euconsent
        state.gdpr?.grants = postResponse.grants?: getResponse?.gdpr?.grants?: emptyMap()
        state.gdpr?.webConsentPayload = postResponse.webConsentPayload?: getResponse?.gdpr?.webConsentPayload
        state.gdpr?.gcmStatus = postResponse.gcmStatus?: getResponse?.gdpr?.gcmStatus
        state.gdpr?.legIntCategories = postResponse.acceptedLegIntCategories?: getResponse?.gdpr?.acceptedLegIntCategories?: emptyList()
        state.gdpr?.legIntVendors = postResponse.acceptedLegIntVendors?: getResponse?.gdpr?.acceptedLegIntVendors?: emptyList()
        state.gdpr?.vendors = postResponse.acceptedVendors?: getResponse?.gdpr?.acceptedVendors?: emptyList()
        state.gdpr?.categories = postResponse.acceptedCategories?: getResponse?.gdpr?.acceptedCategories?: emptyList()
        state.gdpr?.specialFeatures = postResponse.acceptedSpecialFeatures?: getResponse?.gdpr?.acceptedSpecialFeatures?: emptyList()
    }

    override suspend fun reportGDPRAction(action: SPAction, getResponse: ChoiceAllResponse?): GDPRChoiceResponse =
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
        if (action.type == SPActionType.SaveAndExit) {
            state.ccpa?.gppData = postResponse.gppData
        }
        state.ccpa?.uuid = postResponse.uuid
        state.ccpa?.dateCreated = postResponse.dateCreated
        state.ccpa?.status = postResponse.status?: getResponse?.ccpa?.status?: CCPAConsent.CCPAConsentStatus.RejectedAll
        state.ccpa?.rejectedVendors = postResponse.rejectedVendors?: getResponse?.ccpa?.rejectedVendors?: emptyList()
        state.ccpa?.rejectedCategories = postResponse.rejectedCategories?: getResponse?.ccpa?.rejectedCategories?: emptyList()
        state.ccpa?.webConsentPayload = postResponse.webConsentPayload?: getResponse?.ccpa?.webConsentPayload
    }

    override suspend fun reportCCPAAction(action: SPAction, getResponse: ChoiceAllResponse?): CCPAChoiceResponse =
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
        state.usNat?.uuid = postResponse.uuid
        state.usNat?.applies = state.usNat?.applies?: false
        state.usNat?.dateCreated = postResponse.dateCreated
        state.usNat?.expirationDate = postResponse.expirationDate
        state.usNat?.consentStrings = postResponse.consentStrings
        state.usNat?.webConsentPayload = postResponse.webConsentPayload?: getResponse?.usnat?.webConsentPayload
        state.usNat?.userConsents?.categories = postResponse.userConsents.categories
        state.usNat?.userConsents?.vendors = postResponse.userConsents.vendors
        state.usNat?.consentStatus = postResponse.consentStatus
        state.usNat?.gppData = postResponse.gppData
    }

    override suspend fun reportUSNatAction(action: SPAction, getResponse: ChoiceAllResponse?): USNatChoiceResponse =
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

