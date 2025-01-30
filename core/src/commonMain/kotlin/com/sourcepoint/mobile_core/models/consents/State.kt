package com.sourcepoint.mobile_core.models.consents

import com.sourcepoint.mobile_core.models.SPJson

data class State (
    var gdpr: GDPRConsent?,
    var ccpa: CCPAConsent?,
    var usNat: USNatConsent?,
    var ios14: AttCampaign?,
    var gdprMetaData: GDPRMetaData?,
    var ccpaMetaData: CCPAMetaData?,
    var usNatMetaData: UsNatMetaData?,
    var localState: SPJson?,
    var nonKeyedLocalState: SPJson?
) {
    var localVersion: Int? = null
    val hasGDPRLocalData: Boolean get() = gdpr?.uuid != null
    val hasCCPALocalData: Boolean get() = ccpa?.uuid != null
    val hasUSNatLocalData: Boolean get() = usNat?.uuid != null

    interface SPSampleable {
        val sampleRate: Float
        val wasSampled: Boolean?
        val wasSampledAt: Float?
    }

    data class GDPRMetaData (
        val additionsChangeDate: String,
        val legalBasisChangeDate: String?,
        override val sampleRate: Float = 1f,
        override val wasSampled: Boolean?,
        override val wasSampledAt: Float?,
        val vendorListId: String?
    ): SPSampleable

    data class CCPAMetaData (
        override val sampleRate: Float = 1f,
        override val wasSampled: Boolean?,
        override val wasSampledAt: Float?
    ): SPSampleable

    data class UsNatMetaData (
        val additionsChangeDate: String,
        override val sampleRate:Float = 1f,
        override val wasSampled: Boolean?,
        override val wasSampledAt: Float?,
        val vendorListId: String?,
        val applicableSections: List<Int> = emptyList()
    ): SPSampleable

    fun updateGDPRStatus() {
        if (gdpr == null || gdprMetaData == null)
            return
        var shouldUpdateConsentedAll = false
        if (SPDate(gdpr!!.dateCreated).date < SPDate(gdprMetaData!!.additionsChangeDate).date) {
            gdpr = gdpr!!.copy(consentStatus = gdpr!!.consentStatus.copy(vendorListAdditions = true))
            shouldUpdateConsentedAll = true
        }
        if (gdprMetaData!!.legalBasisChangeDate != null &&
            SPDate(gdpr!!.dateCreated).date < SPDate(gdprMetaData!!.legalBasisChangeDate).date) {
            gdpr = gdpr!!.copy(consentStatus = gdpr!!.consentStatus.copy(legalBasisChanges = true))
            shouldUpdateConsentedAll = true
        }
        if (gdpr!!.consentStatus.consentedAll == true && shouldUpdateConsentedAll) {
            gdpr = gdpr!!.copy(consentStatus = gdpr!!.consentStatus.copy(granularStatus = gdpr!!.consentStatus.granularStatus?.copy(previousOptInAll = true)))
            gdpr = gdpr!!.copy(consentStatus = gdpr!!.consentStatus.copy(consentedAll = false))
        }
    }

    fun updateUSNatStatus() {
        if (usNat == null || usNatMetaData == null)
            return
        if (SPDate(usNat!!.dateCreated).date < SPDate(usNatMetaData!!.additionsChangeDate).date) {
            usNat = usNat!!.copy(consentStatus = usNat!!.consentStatus.copy(vendorListAdditions = true))
            if (usNat!!.consentStatus.consentedAll == true) {
                usNat = usNat!!.copy(consentStatus = usNat!!.consentStatus.copy(granularStatus = usNat!!.consentStatus.granularStatus?.copy(previousOptInAll = true)))
                usNat = usNat!!.copy(consentStatus = usNat!!.consentStatus.copy(consentedAll = false))
            }
        }
    }
    companion object {
        const val version = 4
    }
}
