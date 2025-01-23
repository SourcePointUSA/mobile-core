package com.sourcepoint.mobile_core

import com.sourcepoint.mobile_core.models.SPAction
import com.sourcepoint.mobile_core.models.SPActionType
import com.sourcepoint.mobile_core.models.SPCampaignType
import com.sourcepoint.mobile_core.models.SPIDFAStatus
import com.sourcepoint.mobile_core.models.consents.CCPAConsent
import com.sourcepoint.mobile_core.models.consents.ConsentStatus
import com.sourcepoint.mobile_core.models.consents.GDPRConsent
import com.sourcepoint.mobile_core.models.consents.State
import com.sourcepoint.mobile_core.models.consents.USNatConsent
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
): ICoordinator {
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
}

