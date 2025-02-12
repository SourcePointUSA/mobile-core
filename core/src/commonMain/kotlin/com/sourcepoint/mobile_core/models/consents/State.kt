package com.sourcepoint.mobile_core.models.consents

import com.sourcepoint.mobile_core.models.SPJson
import kotlinx.serialization.Serializable

@Serializable
data class State (
    var gdpr: GDPRConsent? = null,
    var ccpa: CCPAConsent? = null,
    var usNat: USNatConsent? = null,
    var ios14: AttCampaign? = null,
    var gdprMetaData: GDPRMetaData? = null,
    var ccpaMetaData: CCPAMetaData? = null,
    var usNatMetaData: UsNatMetaData? = null,
    var localState: SPJson? = null,
    var nonKeyedLocalState: SPJson? = null,
    var storedAuthId: String? = null,
    var localVersion: Int? = null
) {
    companion object {
        const val version = 4
    }

    val hasGDPRLocalData: Boolean get() = gdpr?.uuid != null
    val hasCCPALocalData: Boolean get() = ccpa?.uuid != null
    val hasUSNatLocalData: Boolean get() = usNat?.uuid != null

    interface SPSampleable {
        var sampleRate: Float
        var wasSampled: Boolean?
        var wasSampledAt: Float?

        fun updateSampleFields(newSampleRate: Float) {
            sampleRate = newSampleRate
            if (sampleRate != wasSampledAt) {
                wasSampledAt = sampleRate
                wasSampled = null
            }
        }
    }

    @Serializable
    data class GDPRMetaData (
        val additionsChangeDate: String = SPDate.now().toString(),
        val legalBasisChangeDate: String? = null,
        override var sampleRate: Float = 1f,
        override var wasSampled: Boolean? = null,
        override var wasSampledAt: Float? = null,
        val vendorListId: String? = null
    ): SPSampleable

    @Serializable
    data class CCPAMetaData (
        override var sampleRate: Float = 1f,
        override var wasSampled: Boolean? = null,
        override var wasSampledAt: Float? = null
    ): SPSampleable

    @Serializable
    data class UsNatMetaData (
        val additionsChangeDate: String = SPDate.now().toString(),
        override var sampleRate:Float = 1f,
        override var wasSampled: Boolean? = null,
        override var wasSampledAt: Float? = null,
        val vendorListId: String? = null,
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
}
