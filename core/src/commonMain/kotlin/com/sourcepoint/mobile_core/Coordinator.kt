package com.sourcepoint.mobile_core

import com.sourcepoint.mobile_core.models.SPAction
import com.sourcepoint.mobile_core.models.SPActionType
import com.sourcepoint.mobile_core.models.SPCampaignType
import com.sourcepoint.mobile_core.models.SPIDFAStatus
import com.sourcepoint.mobile_core.models.consents.CCPAConsent
import com.sourcepoint.mobile_core.models.consents.GDPRConsent
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

interface SPCoordinator {
    @Throws(Exception::class) suspend fun getChoiceAll(
        action: SPAction,
        campaigns: ChoiceAllRequest.ChoiceAllCampaigns
    ): ChoiceAllResponse?

    @Throws(Exception::class) suspend fun postChoiceGDPR(
        idfaStatus:SPIDFAStatus,
        includeData: IncludeData,
        authId: String?,
        postPayloadFromGetCall: ChoiceAllResponse.GDPR.PostPayload?,
        action: SPAction
    ): GDPRChoiceResponse

    @Throws(Exception::class) suspend fun postChoiceCCPA(
        includeData: IncludeData,
        authId: String?,
        action: SPAction
    ): CCPAChoiceResponse

    @Throws(Exception::class) suspend fun postChoiceUSNat(
        includeData: IncludeData,
        idfaStatus:SPIDFAStatus,
        authId: String?,
        action: SPAction
    ): USNatChoiceResponse
}
class Coordinator(
    private val accountId: Int,
    private val propertyId: Int,
    private val propertyName: String,
    private val repository: Repository,
    private val spClient: SourcepointClient
): SPCoordinator {
    lateinit var state: State

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

    private fun shouldCallGetChoice(actionType: SPActionType): Boolean =
        (actionType == SPActionType.AcceptAll || actionType == SPActionType.RejectAll)

    override suspend fun getChoiceAll(action: SPAction, campaigns: ChoiceAllRequest.ChoiceAllCampaigns): ChoiceAllResponse? =
        if (!shouldCallGetChoice(action.type))
            null
        else
            try {
                val response = spClient.getChoiceAll(action.type,campaigns)
                handleGetChoiceAll(response, action.campaignType)
                response
            } catch (error: Throwable) {
                throw error
            }

    override suspend fun postChoiceGDPR(
        idfaStatus:SPIDFAStatus,
        includeData: IncludeData,
        authId: String?,
        postPayloadFromGetCall: ChoiceAllResponse.GDPR.PostPayload?,
        action: SPAction
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

    override suspend fun postChoiceCCPA(
        includeData: IncludeData,
        authId: String?,
        action: SPAction
    ): CCPAChoiceResponse =
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

    override suspend fun postChoiceUSNat(
        includeData: IncludeData,
        idfaStatus:SPIDFAStatus,
        authId: String?,
        action: SPAction
    ): USNatChoiceResponse =
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

}

data class State (
    val gdpr: GDPRConsent?,
    val ccpa: CCPAConsent?,
    val usNat:USNatConsent?,
    var gdprMetaData: GDPRMetaData?,
    var ccpaMetaData: CCPAMetaData?,
    var usNatMetaData: UsNatMetaData?
) {
    data class GDPRMetaData (
        var additionsChangeDate: String,
        var legalBasisChangeDate: String?,
        var sampleRate: Float = 1f,
        var wasSampled: Boolean?,
        var wasSampledAt: Float?
    )

    data class CCPAMetaData (
        var sampleRate: Float = 1f,
        var wasSampled: Boolean?,
        var wasSampledAt: Float?
    )

    data class UsNatMetaData (
        var additionsChangeDate: String,
        var sampleRate:Float = 1f,
        var wasSampled: Boolean?,
        var wasSampledAt: Float?,
        var vendorListId: String = "",
        var applicableSections: List<Int> = emptyList()
    )
}
