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

    private fun shouldCallGetChoice(action: SPActionType): Boolean =
        (action == SPActionType.AcceptAll || action == SPActionType.RejectAll)

    override suspend fun getChoiceAll(action: SPActionType, campaigns: ChoiceAllRequest.ChoiceAllCampaigns): ChoiceAllResponse? =
        if (shouldCallGetChoice(action))
            try {
                spClient.getChoiceAll(action,campaigns)
            } catch (error: Throwable) {
                throw error
            }
        else
            null

    suspend fun postChoice(gdprChoiceRequest: GDPRChoiceRequest, action: SPActionType): GDPRChoiceResponse =
        spClient.postChoiceGDPRAction(actionType = action, request = gdprChoiceRequest)

    suspend fun postChoice(ccpaChoiceRequest: CCPAChoiceRequest, action: SPActionType): CCPAChoiceResponse =
        spClient.postChoiceCCPAAction(actionType = action, request = ccpaChoiceRequest)
    suspend fun postChoice(usNatChoiceRequest: USNatChoiceRequest, action: SPAction): USNatChoiceResponse =
        spClient.postChoiceUSNatAction(actionType = action.type, request = usNatChoiceRequest)

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
